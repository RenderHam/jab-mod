package com.jab.client.browser;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;

import com.jab.JabMod;

import net.minecraft.client.Minecraft;

import org.cef.network.CefCookieManager;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
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
		wipeBrowsingData();
		resetCursor();
	}

	/** Session-only privacy: every browser destroy clears the in-memory cookie jar. */
	public static void wipeBrowsingData() {
		try {
			CefCookieManager.getGlobalManager().deleteCookies("", "");
		} catch (Exception e) {
			JabMod.LOGGER.warn("Failed to delete cookies", e);
		}
	}

	public static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Restores the cursor to Minecraft's own state after a browser dies. Rinku swaps
	 * GLFW cursor settings while hovering a page (hidden over canvas, custom handle over
	 * links) and leaves them stale or freed when the browser is destroyed. With a GUI
	 * open the visible OS cursor is restored; outside a GUI the captured/invisible
	 * gameplay state is restored so the OS cursor never pops back in mid-game.
	 */
	public static void resetCursor() {
		try {
			long win = Minecraft.getInstance().getWindow().handle();
			if (win != 0) {
				boolean guiOpen = Minecraft.getInstance().screen != null;
				int mode = guiOpen ? GLFW.GLFW_CURSOR_NORMAL : GLFW.GLFW_CURSOR_DISABLED;
				GLFW.glfwSetInputMode(win, GLFW.GLFW_CURSOR, mode);
				GLFW.glfwSetCursor(win, 0L);
			}
		} catch (Exception e) {
			JabMod.LOGGER.warn("Failed to reset cursor", e);
		}
	}

	/** Called when the client shuts down. Closes every browser and wipes browsing data. */
	public static void shutdown() {
		ScreenBrowserManager.onShutdownCleanup();
	}
}