package com.jab.config;

import com.jab.JabMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public class JabConfig {
	public static final String DEFAULT_URL = "https://www.google.com";

	private static JabConfig INSTANCE;

	private final int maxScreenSize;
	private final int defaultResolutionX;
	private final int defaultResolutionY;
	private final int loadDistance;
	private final int unloadDistance;
	private final int maxBrowsers;
	private final String defaultUrl;

	private JabConfig(int maxScreenSize, int defaultResolutionX, int defaultResolutionY,
			int loadDistance, int unloadDistance, int maxBrowsers, String defaultUrl) {
		this.maxScreenSize = maxScreenSize;
		this.defaultResolutionX = defaultResolutionX;
		this.defaultResolutionY = defaultResolutionY;
		this.loadDistance = loadDistance;
		this.unloadDistance = unloadDistance;
		this.maxBrowsers = maxBrowsers;
		this.defaultUrl = defaultUrl;
	}

	public static JabConfig get() {
		if (INSTANCE == null) {
			throw new IllegalStateException("JabConfig not loaded yet");
		}
		return INSTANCE;
	}

	public int maxScreenSize() { return maxScreenSize; }
	public int defaultResolutionX() { return defaultResolutionX; }
	public int defaultResolutionY() { return defaultResolutionY; }
	public int loadDistance() { return loadDistance; }
	public int unloadDistance() { return unloadDistance; }
	public int maxBrowsers() { return maxBrowsers; }
	public String defaultUrl() { return defaultUrl; }

	private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("jab.properties").toFile();

	public static void load() {
		int maxScreenSize = 8;
		int defaultResolutionX = 1920;
		int defaultResolutionY = 1080;
		int loadDistance = 32;
		int unloadDistance = 48;
		int maxBrowsers = 16;
		String defaultUrl = DEFAULT_URL;

		if (configFile.exists()) {
			try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
				Properties props = new Properties();
				props.load(reader);
				maxScreenSize = Math.min(32, Math.max(2, parseInt(props, "maxScreenSize", 8)));
				defaultResolutionX = Math.min(7680, Math.max(1, parseInt(props, "defaultResolutionX", 1920)));
				defaultResolutionY = Math.min(4320, Math.max(1, parseInt(props, "defaultResolutionY", 1080)));
				loadDistance = Math.min(128, Math.max(4, parseInt(props, "loadDistance", 32)));
				int rawUnload = parseInt(props, "unloadDistance", 48);
				unloadDistance = Math.min(128, Math.max(loadDistance, rawUnload));
				if (rawUnload < loadDistance) {
					JabMod.LOGGER.warn("Config unloadDistance ({}) < loadDistance ({}), clamped to {}", rawUnload, loadDistance, unloadDistance);
				}
				maxBrowsers = Math.min(32, Math.max(1, parseInt(props, "maxBrowsers", 16)));
				String url = props.getProperty("defaultUrl", DEFAULT_URL);
				defaultUrl = (url == null || url.isBlank()) ? DEFAULT_URL : url.trim();
			} catch (Exception e) {
				JabMod.LOGGER.warn("Failed to load config", e);
			}
		}

		INSTANCE = new JabConfig(maxScreenSize, defaultResolutionX, defaultResolutionY,
				loadDistance, unloadDistance, maxBrowsers, defaultUrl);
		save();
	}

	public static void save() {
		if (INSTANCE == null) return;
		try {
			if (configFile.getParentFile() != null) configFile.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
				Properties props = new Properties();
				props.setProperty("maxScreenSize", String.valueOf(INSTANCE.maxScreenSize));
				props.setProperty("defaultResolutionX", String.valueOf(INSTANCE.defaultResolutionX));
				props.setProperty("defaultResolutionY", String.valueOf(INSTANCE.defaultResolutionY));
				props.setProperty("loadDistance", String.valueOf(INSTANCE.loadDistance));
				props.setProperty("unloadDistance", String.valueOf(INSTANCE.unloadDistance));
				props.setProperty("maxBrowsers", String.valueOf(INSTANCE.maxBrowsers));
				props.setProperty("defaultUrl", INSTANCE.defaultUrl);
				props.store(writer, "Just A Browser Mod configuration");
			}
		} catch (Exception e) {
			JabMod.LOGGER.warn("Failed to save config", e);
		}
	}

	private static int parseInt(Properties props, String key, int def) {
		try {
			return Integer.parseInt(props.getProperty(key, String.valueOf(def)));
		} catch (NumberFormatException e) {
			JabMod.LOGGER.warn("Invalid config value for '{}': '{}', using default {}", key, props.getProperty(key), def);
			return def;
		}
	}
}
