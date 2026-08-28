package com.jab.data;

import com.jab.config.JabConfig;
import com.jab.util.BlockSide;

import net.minecraft.nbt.CompoundTag;

public class ScreenData {
	public enum AudioMode {
		GLOBAL,
		DYNAMIC
	}

	public BlockSide side = BlockSide.BOTTOM;
	public int width;
	public int height;
	public int resX;
	public int resY;
	public String url;
	public AudioMode audioMode = AudioMode.GLOBAL;

	public ScreenData() {
		this.url = JabConfig.defaultUrl;
		this.resX = JabConfig.defaultResolutionX;
		this.resY = JabConfig.defaultResolutionY;
	}

	public CompoundTag serialize() {
		CompoundTag tag = new CompoundTag();
		tag.putByte("Side", (byte) side.ordinal());
		tag.putInt("Width", width);
		tag.putInt("Height", height);
		tag.putInt("ResX", resX);
		tag.putInt("ResY", resY);
		tag.putString("Url", url);
		tag.putString("AudioMode", audioMode.name());
		return tag;
	}

	public static ScreenData deserialize(CompoundTag tag) {
		ScreenData data = new ScreenData();
		int sideOrd = tag.getByteOr("Side", (byte) 0);
		data.side = BlockSide.values()[Math.clamp(sideOrd, 0, BlockSide.values().length - 1)];
		data.width = tag.getIntOr("Width", 0);
		data.height = tag.getIntOr("Height", 0);
		data.resX = Math.max(1, tag.getIntOr("ResX", JabConfig.defaultResolutionX));
		data.resY = Math.max(1, tag.getIntOr("ResY", JabConfig.defaultResolutionY));
		data.url = tag.getStringOr("Url", "");
		String am = tag.getStringOr("AudioMode", "GLOBAL");
		try {
			data.audioMode = AudioMode.valueOf(am);
		} catch (IllegalArgumentException e) {
			data.audioMode = AudioMode.GLOBAL;
		}
		return data;
	}
}
