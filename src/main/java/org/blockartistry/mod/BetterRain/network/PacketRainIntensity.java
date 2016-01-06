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

package org.blockartistry.mod.BetterRain.network;

import org.blockartistry.mod.BetterRain.client.rain.RainProperties;
import org.blockartistry.mod.BetterRain.util.PlayerUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class PacketRainIntensity implements IMessage, IMessageHandler<PacketRainIntensity, IMessage> {

	/**
	 * Strength of rainfall
	 */
	private float intensity;

	/**
	 * Dimension where the rainfall is occurring
	 */
	private int dimension;
	
	/**
	 * Current phase of the rain cycle.
	 */
	private int rainPhase;

	public PacketRainIntensity() {
	}

	public PacketRainIntensity(final float intensity, final int rainPhase, final int dimension) {
		this.intensity = intensity;
		this.rainPhase = rainPhase;
		this.dimension = dimension;
	}

	public void fromBytes(final ByteBuf buf) {
		this.intensity = buf.readFloat();
		this.rainPhase = buf.readByte();
		this.dimension = buf.readInt();
	}

	public void toBytes(final ByteBuf buf) {
		buf.writeFloat(this.intensity);
		buf.writeByte(this.rainPhase);
		buf.writeInt(this.dimension);
	}

	public IMessage onMessage(final PacketRainIntensity message, final MessageContext ctx) {
		// If the player is in the dimension set the intensity.  Otherwise
		// ignore.
		if (message.dimension == PlayerUtils.getClientPlayerDimension()) {
			RainProperties.setIntensity(message.intensity);
			RainProperties.setRainPhase(message.rainPhase);
		}
		return null;
	}
}
