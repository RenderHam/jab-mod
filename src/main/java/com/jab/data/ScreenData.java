package com.jab.data;

import com.jab.config.JabConfig;
import com.jab.util.BlockSide;

import net.minecraft.nbt.CompoundTag;

public class ScreenData {
	public enum AudioMode {
		GLOBAL,
		DYNAMIC;

		public static AudioMode lenientValueOf(String name) {
			try {
				return AudioMode.valueOf(name);
			} catch (IllegalArgumentException e) {
				return GLOBAL;
			}
		}
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
		data.width = Math.clamp(width, 1, 32);
		data.height = Math.clamp(height, 1, 32);
		data.resolutionX = Math.clamp(resX, 1, 7680);
		data.resolutionY = Math.clamp(resY, 1, 4320);
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
		data.side = BlockSide.lenientValueOf(tag.contains("Side") ? tag.getString("Side") : "BOTTOM");
		data.width = Math.clamp(tag.contains("Width") ? tag.getInt("Width") : 1, 1, 32);
		data.height = Math.clamp(tag.contains("Height") ? tag.getInt("Height") : 1, 1, 32);
		data.resolutionX = Math.clamp(tag.contains("ResX") ? tag.getInt("ResX") : JabConfig.get().defaultResolutionX(), 1, 7680);
		data.resolutionY = Math.clamp(tag.contains("ResY") ? tag.getInt("ResY") : JabConfig.get().defaultResolutionY(), 1, 4320);
		data.url = tag.contains("Url") ? tag.getString("Url") : "";
		data.audioMode = AudioMode.lenientValueOf(tag.contains("AudioMode") ? tag.getString("AudioMode") : "GLOBAL");
		return data;
	}
}
