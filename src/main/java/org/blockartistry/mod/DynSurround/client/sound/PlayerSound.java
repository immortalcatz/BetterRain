/* This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
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

package org.blockartistry.mod.DynSurround.client.sound;

import java.util.Random;

import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.client.EnvironStateHandler.EnvironState;
import org.blockartistry.mod.DynSurround.util.XorShiftRandom;

import codechicken.lib.math.MathHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
class PlayerSound extends MovingSound {

	private static final float DONE_VOLUME_THRESHOLD = 0.001F;
	private static final Random RANDOM = new XorShiftRandom();

	private final SoundEffect sound;

	public PlayerSound(final SoundEffect sound) {
		super(new ResourceLocation(sound.sound));

		// Don't set volume to 0; MC will optimize out
		this.sound = sound;
		this.volume = sound.volume;
		this.field_147663_c = sound.getPitch(RANDOM);
		this.repeat = sound.repeatDelay == 0;

		// Repeat delay
		this.field_147665_h = 0;

		final EntityPlayer player = EnvironState.getPlayer();
		// Initial position
		this.xPosF = MathHelper.floor_double(player.posX);
		this.yPosF = MathHelper.floor_double(player.posY + 1);
		this.zPosF = MathHelper.floor_double(player.posZ);
	}

	public void fadeAway() {
		this.volume = 0.0F;
		this.donePlaying = true;
	}

	public boolean sameSound(final SoundEffect snd) {
		return this.sound.equals(snd);
	}

	@Override
	public void update() {
		if (this.donePlaying)
			return;

		if (this.volume <= DONE_VOLUME_THRESHOLD) {
			this.donePlaying = true;
		} else if (EnvironState.getPlayer() != null) {
			final EntityPlayer player = EnvironState.getPlayer();
			this.xPosF = MathHelper.floor_double(player.posX);
			this.yPosF = MathHelper.floor_double(player.posY + 1);
			this.zPosF = MathHelper.floor_double(player.posZ);
		}
	}

	@Override
	public float getVolume() {
		return this.volume * ModOptions.masterSoundScaleFactor;
	}

	public void setVolume(final float volume) {
		this.volume = volume;
	}

	@Override
	public String toString() {
		return this.sound.toString();
	}

	@Override
	public boolean equals(final Object anObj) {
		if (this == anObj)
			return true;
		if (anObj instanceof PlayerSound)
			return this.sameSound(((PlayerSound) anObj).sound);
		if (anObj instanceof SoundEffect)
			return this.sameSound((SoundEffect) anObj);
		return false;
	}
}
