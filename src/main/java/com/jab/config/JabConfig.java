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
	public static int maxResolution = 3840;
	public static int loadDistance = 32;
	public static int unloadDistance = 48;
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
			maxResolution = parseInt(props, "maxResolution", 3840);
			loadDistance = parseInt(props, "loadDistance", 32);
			unloadDistance = parseInt(props, "unloadDistance", 48);
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
				props.setProperty("maxResolution", String.valueOf(maxResolution));
				props.setProperty("loadDistance", String.valueOf(loadDistance));
				props.setProperty("unloadDistance", String.valueOf(unloadDistance));
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
