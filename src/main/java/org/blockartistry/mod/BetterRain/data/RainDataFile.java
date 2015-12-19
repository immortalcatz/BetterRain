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

package org.blockartistry.mod.BetterRain.data;

import java.util.ArrayList;
import java.util.List;

import org.blockartistry.mod.BetterRain.BetterRain;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.Constants;

public class RainDataFile extends WorldSavedData {

	private final static String IDENTIFIER = BetterRain.MOD_ID;

	private final class NBT {
		public final static String ENTRIES = "e";
	};

	private final List<RainData> dataList = new ArrayList<RainData>();

	public RainDataFile() {
		this(IDENTIFIER);
	}

	public RainDataFile(final String id) {
		super(id);
	}

	public static RainDataFile get(final World world) {
		RainDataFile data = (RainDataFile) world.loadItemData(RainDataFile.class, IDENTIFIER);
		if (data == null) {
			data = new RainDataFile();
			world.setItemData(IDENTIFIER, data);
		}
		data.markDirty();
		return data;
	}
	
	public RainData get(final int dimensionId) {
		for(final RainData data: this.dataList)
			if(data.getDimensionId() == dimensionId)
				return data;
		final RainData data = new RainData(dimensionId);
		this.dataList.add(data);
		return data;
	}
	
	public static RainData getRainData(final World world) {
		return get(world).get(world.provider.dimensionId);
	}

	@Override
	public void readFromNBT(final NBTTagCompound nbt) {
		final NBTTagList list = nbt.getTagList(NBT.ENTRIES, Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++) {
			final NBTTagCompound tag = list.getCompoundTagAt(i);
			final RainData data = new RainData();
			data.readFromNBT(tag);
			this.dataList.add(data);
		}
	}

	@Override
	public void writeToNBT(final NBTTagCompound nbt) {
		final NBTTagList list = new NBTTagList();
		for (final RainData data : this.dataList) {
			final NBTTagCompound tag = new NBTTagCompound();
			data.writeToNBT(tag);
			list.appendTag(tag);
		}
		nbt.setTag(NBT.ENTRIES, list);
	}
}
