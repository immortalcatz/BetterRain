/*
 * This file is part of BetterRain, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.mod.BetterRain.server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.List;

import org.blockartistry.mod.BetterRain.ModLog;
import org.blockartistry.mod.BetterRain.ModOptions;
import org.blockartistry.mod.BetterRain.data.RainPhase;
import org.blockartistry.mod.BetterRain.data.AuroraData;
import org.blockartistry.mod.BetterRain.data.AuroraPreset;
import org.blockartistry.mod.BetterRain.data.ColorPair;
import org.blockartistry.mod.BetterRain.data.DimensionEffectData;
import org.blockartistry.mod.BetterRain.data.EffectType;
import org.blockartistry.mod.BetterRain.network.Network;
import org.blockartistry.mod.BetterRain.util.ElementRule;
import org.blockartistry.mod.BetterRain.util.PlayerUtils;
import org.blockartistry.mod.BetterRain.util.WorldUtils;

public final class ServerEffectHandler {

	private static final float RESET = -10.0F;

	private static final boolean AURORA_ENABLE = ModOptions.getAuroraEnable();

	// Offset from the player location so they can see it
	// without looking straight up.
	private static final int Z_OFFSET = -150;

	// Minimum distance between auroras, squared
	private static final long MIN_AURORA_DISTANCE_SQ = 400 * 400;

	public static void initialize() {
		FMLCommonHandler.instance().bus().register(new ServerEffectHandler());
	}

	private final ElementRule rule = ModOptions.getDimensionRule();
	
	private static TIntObjectHashMap<Float> rainStrengths = new TIntObjectHashMap<Float>();
	
	private static void processRainCycle(final World world, final DimensionEffectData data) {
		Float lastStrength = rainStrengths.get(data.getDimensionId());
		if(lastStrength == null)
			lastStrength = new Float(0.0F);

		RainPhase rainState = RainPhase.values()[data.getRainPhase()];
		final float currentRainStrength = world.getRainStrength(1.0F);
		
		if(currentRainStrength <= 0.0F)
			rainState = RainPhase.NOT_RAINING;
		else if(currentRainStrength == 1.0F)
			rainState = RainPhase.RAINING;
		else if(currentRainStrength > lastStrength)
			rainState = RainPhase.STARTING;
		else if(currentRainStrength < lastStrength)
			rainState = RainPhase.STOPPING;
		data.setRainPhase(rainState.ordinal());
		rainStrengths.put(data.getDimensionId(), new Float(currentRainStrength));
	}

	@SubscribeEvent
	public void tickEvent(final TickEvent.WorldTickEvent event) {

		if (event.phase == Phase.END) {
			if (AURORA_ENABLE)
				processAuroras(event);
			return;
		}

		float sendIntensity = RESET;
		int sendPhase = RainPhase.NOT_RAINING.ordinal();
		final World world = event.world;
		final int dimensionId = world.provider.dimensionId;

		// Have to be a surface world and match the dimension rule
		if (world.provider.isSurfaceWorld() && rule.isOk(dimensionId)) {
			final DimensionEffectData data = DimensionEffectData.get(world);
			if (world.getRainStrength(1.0F) > 0.0F) {
				if (data.getRainIntensity() == 0.0F) {
					data.randomizeRain();
					ModLog.info(String.format("dim %d rain strength set to %f", dimensionId, data.getRainIntensity()));
				}
			} else if (data.getRainIntensity() > 0.0F) {
				ModLog.info(String.format("dim %d rain has stopped", dimensionId));
				data.setRainIntensity(0.0F);
			}
			processRainCycle(world, data);
			sendIntensity = data.getRainIntensity();
			sendPhase = data.getRainPhase();
		}

		// Set the rain intensity for all players in the current
		// dimension.
		Network.sendRainIntensity(sendIntensity, sendPhase, dimensionId);
	}

	private static boolean isAuroraInRange(final EntityPlayerMP player, final List<AuroraData> data) {
		for (final AuroraData aurora : data) {
			final long deltaX = aurora.posX - (int) player.posX;
			final long deltaZ = aurora.posZ - (int) player.posZ + Z_OFFSET;
			final long distSq = deltaX * deltaX + deltaZ * deltaZ;
			if (distSq <= MIN_AURORA_DISTANCE_SQ)
				return true;
		}

		return false;
	}

	private static final int CHECK_INTERVAL = 100; // Ticks
	private static long lastTimeCheck = 0;

	protected void processAuroras(final TickEvent.WorldTickEvent event) {

		final World world = event.world;
		if (world == null || !WorldUtils.hasSky(world))
			return;

		final List<AuroraData> data = DimensionEffectData.get(world).getAuroraList();
		final long time = WorldUtils.getWorldTime(world);

		// Daylight hours clear the aurora list
		if (WorldUtils.isDaytime(time)) {
			data.clear();
		} else if (lastTimeCheck != time && time % CHECK_INTERVAL == 0) {
			lastTimeCheck = time;
			@SuppressWarnings("unchecked")
			final List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;

			for (final EntityPlayerMP player : players) {
				if (!EffectType.hasAuroraEffect(PlayerUtils.getPlayerBiome(player)))
					continue;
				if (isAuroraInRange(player, data))
					continue;

				final int colorSet = ColorPair.randomId();
				final int preset = AuroraPreset.randomId();
				//final int colorSet = ColorPair.testId();
				// final int preset = AuroraPreset.testId();
				data.add(new AuroraData(player, Z_OFFSET, colorSet, preset));
			}

			for (final AuroraData a : data) {
				Network.sendAurora(a, world.provider.dimensionId);
			}
		}
	}
}
