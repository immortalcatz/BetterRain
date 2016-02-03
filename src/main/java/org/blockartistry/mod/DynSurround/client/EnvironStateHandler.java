/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
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

package org.blockartistry.mod.DynSurround.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.data.BiomeRegistry;
import org.blockartistry.mod.DynSurround.data.DimensionRegistry;
import org.blockartistry.mod.DynSurround.event.DiagnosticEvent;
import org.blockartistry.mod.DynSurround.util.PlayerUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class EnvironStateHandler implements IClientEffectHandler {

	// Diagnostic strings to display in the debug HUD
	private static List<String> diagnostics = new ArrayList<String>();

	public static List<String> getDiagnostics() {
		return diagnostics;
	}

	public static class EnvironState {
		// State that is gathered from the various sources
		// to avoid requery. Used during the tick.
		private static String conditions = "";
		private static String biomeName = "";
		private static BiomeGenBase playerBiome = null;
		private static int dimensionId;
		private static String dimensionName;
		private static EntityPlayer player;
		private static World world;

		private static int tickCounter;

		public static String getConditions() {
			return conditions;
		}

		public static BiomeGenBase getPlayerBiome() {
			return playerBiome;
		}

		public static String getBiomeName() {
			return biomeName;
		}

		public static int getDimensionId() {
			return dimensionId;
		}

		public static String getDimensionName() {
			return dimensionName;
		}

		public static EntityPlayer getPlayer() {
			return player;
		}

		public static boolean isPlayer(final Entity entity) {
			if (entity instanceof EntityPlayer) {
				final EntityPlayer ep = (EntityPlayer) entity;
				return ep.getUniqueID().equals(player.getUniqueID());
			}
			return false;
		}

		public static boolean isPlayer(final UUID id) {
			return player.getUniqueID().equals(id);
		}

		public static boolean isPlayerHurt() {
			return (player.getHealth() / player.getMaxHealth()) <= 0.40F;
		}

		public static boolean isPlayerHungry() {
			return ((float)player.getFoodStats().getFoodLevel() / 20.0F) <= 0.40F;
		}

		public static World getWorld() {
			return world;
		}

		public static int getTickCounter() {
			return tickCounter;
		}

		public static double distanceToPlayer(final double x, final double y, final double z) {
			if (player == null)
				return Double.MAX_VALUE;
			return player.getDistanceSq(x, y, z);
		}
	}

	private static final String CONDITION_TOKEN_HURT = "hurt";
	private static final String CONDITION_TOKEN_HUNGRY = "hungry";
	private static final char CONDITION_SEPARATOR = '#';

	private static String getPlayerConditions(final EntityPlayer player) {
		final StringBuilder builder = new StringBuilder();
		if (EnvironState.isPlayerHurt())
			builder.append(CONDITION_SEPARATOR).append(CONDITION_TOKEN_HURT);
		if (EnvironState.isPlayerHungry())
			builder.append(CONDITION_SEPARATOR).append(CONDITION_TOKEN_HUNGRY);
		builder.append(CONDITION_SEPARATOR);
		return builder.toString();
	}

	@Override
	public void process(final World world, final EntityPlayer player) {
		EnvironState.player = player;
		EnvironState.world = world;
		EnvironState.conditions = DimensionRegistry.getConditions(world) + getPlayerConditions(player);
		EnvironState.playerBiome = PlayerUtils.getPlayerBiome(player, false);
		EnvironState.biomeName = BiomeRegistry.resolveName(EnvironState.playerBiome);
		EnvironState.dimensionId = world.provider.getDimensionId();
		EnvironState.dimensionName = world.provider.getDimensionName();

		if (!Minecraft.getMinecraft().isGamePaused())
			EnvironState.tickCounter++;

		// Gather diagnostics if needed
		if (ModOptions.getEnableDebugLogging()) {
			final DiagnosticEvent.Gather gather = new DiagnosticEvent.Gather(world, player);
			MinecraftForge.EVENT_BUS.post(gather);
			diagnostics = gather.output;
		}
	}

	@Override
	public boolean hasEvents() {
		return true;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void diagnostics(final DiagnosticEvent.Gather event) {
		event.output.add("Dim: " + EnvironState.getDimensionId() + "/" + EnvironState.getDimensionName());
		event.output.add("Biome: " + EnvironState.getBiomeName());
		event.output.add("Conditions: " + EnvironState.getConditions());
	}

}
