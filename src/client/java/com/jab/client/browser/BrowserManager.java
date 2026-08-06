package com.jab.client.browser;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;

import com.jab.JabMod;

import org.cef.network.CefCookieManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Thin wrapper around Rinku's browser lifecycle. Rinku initialization is async,
 * so browsers can only be created after the init callback has fired.
 */
public class BrowserManager {
	private static final Map<Integer, RinkuBrowser> browsers = new HashMap<>();
	private static boolean initialized = false;

	public static void init() {
		if (initialized) return;
		Rinku.scheduleForInit(success -> {
			if (success) {
				initialized = true;
				JabMod.LOGGER.info("Rinku initialized");
			} else {
				JabMod.LOGGER.error("Failed to initialize Rinku");
			}
		});
	}

	public static RinkuBrowser createBrowser(String url, boolean transparent) {
		if (!initialized) return null;
		RinkuBrowser browser = Rinku.createBrowser(url, transparent);
		if (browser != null) {
			browsers.put(browser.getIdentifier(), browser);
		}
		return browser;
	}

	public static RinkuBrowser createBrowser(String url, boolean transparent, int width, int height) {
		if (!initialized) {
			JabMod.LOGGER.warn("Rinku not initialized yet, cannot create browser (url={})", url);
			return null;
		}
		RinkuBrowser browser = Rinku.createBrowser(url, transparent, width, height);
		if (browser != null) {
			browsers.put(browser.getIdentifier(), browser);
			JabMod.LOGGER.info("Created browser id={} url={} {}x{}", browser.getIdentifier(), url, width, height);
		} else {
			JabMod.LOGGER.warn("Rinku.createBrowser returned null (url={})", url);
		}
		return browser;
	}

	public static void destroyBrowser(RinkuBrowser browser) {
		if (browser == null) return;
		browsers.remove(browser.getIdentifier());
		browser.close();
	}

	public static boolean isInitialized() {
		return initialized;
	}

	private static void cleanup() {
		Iterator<RinkuBrowser> it = browsers.values().iterator();
		while (it.hasNext()) {
			RinkuBrowser browser = it.next();
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
