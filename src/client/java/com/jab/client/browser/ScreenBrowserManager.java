package com.jab.client.browser;

import com.cinemamod.mcef.MCEFBrowser;

import com.jab.JabMod;
import com.jab.blockentity.ScreenBlockEntity;
import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import org.cef.network.CefCookieManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns one browser per screen (keyed by origin position + block side) and keeps the
 * client-side screen data in sync with the server. Screen data that arrives before
 * the wall's block entity exists is parked in {@link #pendingScreens} until it shows up.
 */
public class ScreenBrowserManager {
	private static final Map<String, MCEFBrowser> browserMap = new HashMap<>();
	private static final Map<BlockPos, List<ScreenData>> pendingScreens = new HashMap<>();

	private static String key(BlockPos pos, BlockSide side) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + side.ordinal();
	}

	public static void sync(BlockPos pos, List<ScreenData> screens) {
		Map<String, ScreenData> newScreens = new HashMap<>();
		for (ScreenData s : screens) {
			String k = key(pos, s.side);
			newScreens.put(k, s);
			if (!browserMap.containsKey(k)) {
				MCEFBrowser browser = BrowserManager.createBrowser(s.url, false, s.resX, s.resY);
				if (browser != null) {
					browserMap.put(k, browser);
				} else {
					JabMod.LOGGER.warn("Failed to create browser for key={}", k);
				}
			}
		}

		// Drop browsers for screens that no longer exist.
		var iter = browserMap.entrySet().iterator();
		while (iter.hasNext()) {
			var entry = iter.next();
			if (!newScreens.containsKey(entry.getKey())) {
				BrowserManager.destroyBrowser(entry.getValue());
				iter.remove();
			}
		}

		AudioModeHandler.sync(pos, screens);
	}

	public static MCEFBrowser getBrowser(BlockPos pos, BlockSide side) {
		return browserMap.get(key(pos, side));
	}

	public static void updateScreen(BlockPos pos, ScreenData screen) {
		String k = key(pos, screen.side);
		MCEFBrowser browser = browserMap.get(k);
		if (browser != null) {
			// Only reload when the URL actually changed so resizing or audio toggles
			// don't interrupt the page.
			String current = browser.getURL();
			if (current == null || !current.equals(screen.url)) {
				browser.loadURL(screen.url);
			}
		} else {
			browser = BrowserManager.createBrowser(screen.url, false, screen.resX, screen.resY);
			if (browser != null) {
				browserMap.put(k, browser);
			}
		}
		AudioModeHandler.updateScreen(pos, screen);
	}

	public static void storePending(BlockPos pos, List<ScreenData> screens) {
		pendingScreens.put(pos.immutable(), screens);
	}

	/** Applies an update to the parked data (or parks it if nothing is parked yet). */
	public static void applyUpdate(BlockPos pos, ScreenData update) {
		BlockPos key = pos.immutable();
		List<ScreenData> list = pendingScreens.get(key);
		if (list == null) {
			list = new ArrayList<>();
			pendingScreens.put(key, list);
		}
		for (ScreenData s : list) {
			if (s.side == update.side) {
				s.url = update.url;
				s.resX = update.resX;
				s.resY = update.resY;
				s.audioMode = update.audioMode;
				s.active = update.active;
				return;
			}
		}
		list.add(update);
	}

	/** Called every tick; moves parked screen data into the block entity once it exists. */
	public static void applyPending() {
		if (pendingScreens.isEmpty()) return;
		Level world = Minecraft.getInstance().level;
		if (world == null) return;
		var iter = pendingScreens.entrySet().iterator();
		while (iter.hasNext()) {
			var entry = iter.next();
			if (world.getBlockEntity(entry.getKey()) instanceof ScreenBlockEntity sbe) {
				sbe.replaceAllScreens(entry.getValue());
				iter.remove();
			}
		}
	}

	public static void removeAllInChunk(ChunkPos chunkPos) {
		int cx = chunkPos.getMinBlockX();
		int cz = chunkPos.getMinBlockZ();
		boolean removed = false;
		var iter = browserMap.entrySet().iterator();
		while (iter.hasNext()) {
			var entry = iter.next();
			String[] parts = entry.getKey().split("[:,]");
			try {
				int bx = Integer.parseInt(parts[0]);
				int bz = Integer.parseInt(parts[2]);
				if (bx >= cx && bx < cx + 16 && bz >= cz && bz < cz + 16) {
					BrowserManager.destroyBrowser(entry.getValue());
					iter.remove();
					removed = true;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		if (removed) {
			try {
				CefCookieManager.getGlobalManager().deleteCookies("", "");
			} catch (Exception e) {
				JabMod.LOGGER.warn("Failed to delete cookies on chunk unload", e);
			}
		}
	}

	public static void cleanup() {
		for (var browser : browserMap.values()) {
			BrowserManager.destroyBrowser(browser);
		}
		browserMap.clear();
		pendingScreens.clear();
		try {
			CefCookieManager.getGlobalManager().deleteCookies("", "");
		} catch (Exception e) {
			JabMod.LOGGER.warn("Failed to delete cookies on cleanup", e);
		}
	}
}
