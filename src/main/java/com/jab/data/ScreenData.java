package com.jab.data;

import com.jab.config.JabConfig;
import com.jab.util.BlockSide;

import net.minecraft.nbt.CompoundTag;

public class ScreenData {
	public enum AudioMode {
		GLOBAL,
		DYNAMIC
	}

	public BlockSide side;
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
		data.side = BlockSide.values()[tag.getByteOr("Side", (byte) 0)];
		data.width = tag.getIntOr("Width", 0);
		data.height = tag.getIntOr("Height", 0);
		data.resX = tag.getIntOr("ResX", 0);
		data.resY = tag.getIntOr("ResY", 0);
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
