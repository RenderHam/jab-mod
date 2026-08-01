package com.jab.client.browser;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;

import com.jab.JabMod;

import org.cef.network.CefCookieManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Thin wrapper around MCEF's browser lifecycle. MCEF initialization is async,
 * so browsers can only be created after the init callback has fired.
 */
public class BrowserManager {
	private static final Map<Integer, MCEFBrowser> browsers = new HashMap<>();
	private static boolean initialized = false;

	public static void init() {
		if (initialized) return;
		MCEF.scheduleForInit(success -> {
			if (success) {
				initialized = true;
				JabMod.LOGGER.info("MCEF initialized");
			} else {
				JabMod.LOGGER.error("Failed to initialize MCEF");
			}
		});
	}

	public static MCEFBrowser createBrowser(String url, boolean transparent) {
		if (!initialized) return null;
		MCEFBrowser browser = MCEF.createBrowser(url, transparent);
		if (browser != null) {
			browsers.put(browser.getIdentifier(), browser);
		}
		return browser;
	}

	public static MCEFBrowser createBrowser(String url, boolean transparent, int width, int height) {
		if (!initialized) {
			JabMod.LOGGER.warn("MCEF not initialized yet, cannot create browser (url={})", url);
			return null;
		}
		MCEFBrowser browser = MCEF.createBrowser(url, transparent, width, height);
		if (browser != null) {
			browsers.put(browser.getIdentifier(), browser);
			JabMod.LOGGER.info("Created browser id={} url={} {}x{}", browser.getIdentifier(), url, width, height);
		} else {
			JabMod.LOGGER.warn("MCEF.createBrowser returned null (url={})", url);
		}
		return browser;
	}

	public static void destroyBrowser(MCEFBrowser browser) {
		if (browser == null) return;
		browsers.remove(browser.getIdentifier());
		browser.close();
	}

	public static boolean isInitialized() {
		return initialized;
	}

	private static void cleanup() {
		Iterator<MCEFBrowser> it = browsers.values().iterator();
		while (it.hasNext()) {
			MCEFBrowser browser = it.next();
			browser.close();
			it.remove();
		}
	}

	/** Called when the client shuts down. Closes every browser and wipes browsing data. */
	public static void shutdown() {
		ScreenBrowserManager.cleanup();
		cleanup();
		try {
			CefCookieManager.getGlobalManager().deleteCookies("", "");
		} catch (Exception e) {
			JabMod.LOGGER.warn("Failed to delete cookies on shutdown", e);
		}
	}
}
