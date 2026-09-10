package com.jab.data;

import com.jab.config.JabConfig;
import com.jab.util.BlockSide;

import net.minecraft.nbt.CompoundTag;

public class ScreenData {
	public enum AudioMode {
		GLOBAL,
		DYNAMIC
	}

	private BlockSide side = BlockSide.BOTTOM;
	private int width;
	private int height;
	private int resolutionX;
	private int resolutionY;
	private String url = "";
	private AudioMode audioMode = AudioMode.GLOBAL;

	public ScreenData() {
	}

	public static ScreenData create(BlockSide side, int width, int height) {
		ScreenData data = new ScreenData();
		data.side = side;
		data.width = width;
		data.height = height;
		data.url = JabConfig.get().defaultUrl();
		data.resolutionX = JabConfig.get().defaultResolutionX();
		data.resolutionY = JabConfig.get().defaultResolutionY();
		return data;
	}

	public static ScreenData decode(BlockSide side, int width, int height, int resX, int resY, String url, AudioMode audioMode) {
		ScreenData data = new ScreenData();
		data.side = side;
		data.width = width;
		data.height = height;
		data.resolutionX = Math.max(1, resX);
		data.resolutionY = Math.max(1, resY);
		data.url = url != null ? url : "";
		data.audioMode = audioMode;
		return data;
	}

	public void copyFrom(ScreenData other) {
		this.url = other.url;
		this.resolutionX = other.resolutionX;
		this.resolutionY = other.resolutionY;
		this.audioMode = other.audioMode;
	}

	// --- Getters ---

	public BlockSide side() {
		return side;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int resolutionX() {
		return resolutionX;
	}

	public int resolutionY() {
		return resolutionY;
	}

	public String url() {
		return url;
	}

	public AudioMode audioMode() {
		return audioMode;
	}

	// --- Setters (mutable fields only) ---

	public void setUrl(String url) {
		this.url = url;
	}

	public void setAudioMode(AudioMode audioMode) {
		this.audioMode = audioMode;
	}

	// --- Serialization ---

	public CompoundTag serialize() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Side", side.name());
		tag.putInt("Width", width);
		tag.putInt("Height", height);
		tag.putInt("ResX", resolutionX);
		tag.putInt("ResY", resolutionY);
		tag.putString("Url", url);
		tag.putString("AudioMode", audioMode.name());
		return tag;
	}

	public static ScreenData deserialize(CompoundTag tag) {
		ScreenData data = new ScreenData();
		String sideName = tag.getStringOr("Side", "BOTTOM");
		try {
			data.side = BlockSide.valueOf(sideName);
		} catch (IllegalArgumentException e) {
			data.side = BlockSide.BOTTOM;
		}
		data.width = tag.getIntOr("Width", 1);
		data.height = tag.getIntOr("Height", 1);
		data.resolutionX = Math.max(1, tag.getIntOr("ResX", JabConfig.get().defaultResolutionX()));
		data.resolutionY = Math.max(1, tag.getIntOr("ResY", JabConfig.get().defaultResolutionY()));
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
