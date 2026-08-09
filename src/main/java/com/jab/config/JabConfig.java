package com.jab.config;

import com.jab.JabMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

public class JabConfig {
	public static int maxScreenSize = 8;
	public static int defaultResolutionX = 1920;
	public static int defaultResolutionY = 1080;
	public static int loadDistance = 32;
	public static int unloadDistance = 48;
	public static int maxBrowsers = 16;
	public static String defaultUrl = "https://www.google.com";

	private static final File configFile = new File("config/jab.properties");

	public static void load() {
		if (!configFile.exists()) {
			save();
			return;
		}
		try (FileReader reader = new FileReader(configFile)) {
			Properties props = new Properties();
			props.load(reader);
			maxScreenSize = parseInt(props, "maxScreenSize", 8);
			defaultResolutionX = parseInt(props, "defaultResolutionX", 1920);
			defaultResolutionY = parseInt(props, "defaultResolutionY", 1080);
			loadDistance = parseInt(props, "loadDistance", 32);
			unloadDistance = parseInt(props, "unloadDistance", 48);
			maxBrowsers = Math.max(1, parseInt(props, "maxBrowsers", 16));
			defaultUrl = props.getProperty("defaultUrl", "https://www.google.com");
		} catch (Exception e) {
			JabMod.LOGGER.warn("Failed to load config", e);
		}
	}

	public static void save() {
		try {
			configFile.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(configFile)) {
				Properties props = new Properties();
				props.setProperty("maxScreenSize", String.valueOf(maxScreenSize));
				props.setProperty("defaultResolutionX", String.valueOf(defaultResolutionX));
				props.setProperty("defaultResolutionY", String.valueOf(defaultResolutionY));
				props.setProperty("loadDistance", String.valueOf(loadDistance));
				props.setProperty("unloadDistance", String.valueOf(unloadDistance));
				props.setProperty("maxBrowsers", String.valueOf(maxBrowsers));
				props.setProperty("defaultUrl", defaultUrl);
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
			return def;
		}
	}
}
