package com.jab.client.browser;

import de.keksuccino.rinku.RinkuBrowser;

import com.jab.JabMod;
import com.jab.client.gui.BrowserScreen;
import com.jab.config.JabConfig;
import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns one browser per screen (keyed by origin position + block side) and keeps the
 * client-side screen data in sync with the server. Browsers outside the configured
 * unload distance are destroyed and recreated once the player comes back into load
 * distance, and the {@link JabConfig#maxBrowsers} cap parks extra screens instead of
 * creating their browsers.
 */
public class ScreenBrowserManager {
	private static final Map<Long, EnumMap<BlockSide, RinkuBrowser>> browserMap = new HashMap<>();
	private static final Map<Long, Map<BlockSide, ScreenData>> desiredScreens = new HashMap<>();
	private static final Deque<CreationRequest> creationQueue = new ArrayDeque<>();
	private static long tickCounter = 0;
	private static long capWarnTime = 0;
	private static final int MAX_CREATIONS_PER_TICK = 2;

	private record CreationRequest(BlockPos pos, BlockSide side, ScreenData screen) {}

	private static long key(BlockPos pos) {
		return pos.asLong();
	}

	private static void destroyBrowserWithCleanup(BlockPos pos, BlockSide side, RinkuBrowser browser, String reason) {
		logDestroy(pos, side, browser, reason);
		BrowserManager.destroyBrowser(browser);
		AudioModeHandler.remove(pos, side);
	}

	private static void logDestroy(BlockPos pos, BlockSide side, RinkuBrowser browser, String reason) {
		JabMod.LOGGER.info("Closed browser id={} pos={} side={} url={} reason={}",
				browser.getIdentifier(), pos, side, browser.getURL(), reason);
	}

	private static boolean isGuiOpen(BlockPos pos, BlockSide side) {
		net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
		return screen instanceof BrowserScreen bs && bs.getPos().equals(pos) && bs.getSide() == side;
	}

	private static double distanceSqToPlayer(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return Double.MAX_VALUE;
		return mc.player.distanceToSqr(Vec3.atCenterOf(pos));
	}

	private static int activeBrowserCount() {
		int count = 0;
		for (var m : browserMap.values()) count += m.size();
		return count;
	}

	private static void createBrowser(BlockPos pos, BlockSide side, ScreenData s, boolean force) {
		if (!force) {
			creationQueue.add(new CreationRequest(pos, side, s));
			return;
		}
		if (activeBrowserCount() >= JabConfig.get().maxBrowsers()) {
			JabMod.LOGGER.warn("Browser cap reached ({}), but GUI is open — creating anyway", JabConfig.get().maxBrowsers());
		}
		doCreate(pos, side, s);
	}

	private static void doCreate(BlockPos pos, BlockSide side, ScreenData s) {
		if (activeBrowserCount() >= JabConfig.get().maxBrowsers()) {
			long now = System.currentTimeMillis();
			if (now - capWarnTime > 10_000) {
				JabMod.LOGGER.warn("Browser cap reached ({}): parking screen at {} side={} until capacity frees up",
						JabConfig.get().maxBrowsers(), pos, side);
				capWarnTime = now;
			}
			return;
		}
		RinkuBrowser browser = BrowserManager.createBrowser(s.url(), false, s.resolutionX(), s.resolutionY());
		if (browser != null) {
			browserMap.computeIfAbsent(key(pos), k -> new EnumMap<>(BlockSide.class)).put(side, browser);
		} else {
			JabMod.LOGGER.warn("Failed to create browser for pos={} side={}", pos, side);
		}
	}

	public static void sync(BlockPos pos, List<ScreenData> screens) {
		Map<BlockSide, ScreenData> newScreens = new EnumMap<>(BlockSide.class);
		for (ScreenData s : screens) {
			newScreens.put(s.side(), s);
		}
		desiredScreens.put(key(pos), newScreens);

		boolean destroyed = false;
		var alive = browserMap.get(key(pos));
		if (alive != null) {
			var iter = alive.entrySet().iterator();
			while (iter.hasNext()) {
				var entry = iter.next();
				if (!newScreens.containsKey(entry.getKey())) {
					destroyBrowserWithCleanup(pos, entry.getKey(), entry.getValue(), "screen-removed");
					iter.remove();
					destroyed = true;
				}
			}
			if (alive.isEmpty()) browserMap.remove(key(pos));
		}

		AudioModeHandler.sync(pos, screens);
		if (destroyed) {
			BrowserManager.wipeBrowsingData();
			BrowserManager.resetCursor();
		}
	}

	public static void tick() {
		tickCounter++;
		if (tickCounter % 5 != 0) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		int created = 0;
		while (!creationQueue.isEmpty() && created < MAX_CREATIONS_PER_TICK) {
			CreationRequest req = creationQueue.poll();
			if (activeBrowserCount() >= JabConfig.get().maxBrowsers()) break;
			if (!desiredScreens.containsKey(key(req.pos()))) continue;
			doCreate(req.pos(), req.side(), req.screen());
			created++;
		}

		if (desiredScreens.isEmpty()) return;

		boolean destroyed = false;
		for (var entry : desiredScreens.entrySet()) {
			BlockPos pos = BlockPos.of(entry.getKey());
			Map<BlockSide, ScreenData> screens = entry.getValue();
			Map<BlockSide, RinkuBrowser> alive = browserMap.get(entry.getKey());
			double d2 = distanceSqToPlayer(pos);
			boolean withinUnload = d2 <= (double) JabConfig.get().unloadDistance() * JabConfig.get().unloadDistance();
			boolean inRange = d2 <= (double) JabConfig.get().loadDistance() * JabConfig.get().loadDistance();
			for (var sEntry : screens.entrySet()) {
				BlockSide side = sEntry.getKey();
				ScreenData screen = sEntry.getValue();
				boolean guiOpen = isGuiOpen(pos, side);

				RinkuBrowser browser = alive != null ? alive.get(side) : null;
				if (browser != null) {
					if (!guiOpen && !withinUnload) {
						destroyBrowserWithCleanup(pos, side, browser, "distance-unload");
						alive.remove(side);
						if (alive.isEmpty()) browserMap.remove(entry.getKey());
						destroyed = true;
					}
				} else if (guiOpen || inRange) {
					createBrowser(pos, side, screen, guiOpen);
				}
			}
		}

		desiredScreens.entrySet().removeIf(e -> e.getValue().isEmpty());
		if (destroyed) {
			BrowserManager.wipeBrowsingData();
			BrowserManager.resetCursor();
		}
	}

	public static RinkuBrowser getBrowser(BlockPos pos, BlockSide side) {
		Map<BlockSide, RinkuBrowser> alive = browserMap.get(key(pos));
		return alive != null ? alive.get(side) : null;
	}

	public static ScreenData getDesiredScreen(BlockPos pos, BlockSide side) {
		Map<BlockSide, ScreenData> desired = desiredScreens.get(key(pos));
		return desired != null ? desired.get(side) : null;
	}

	/** Forces a browser to exist for a screen (used when the GUI opens). */
	public static void ensureBrowser(BlockPos pos, BlockSide side) {
		if (getBrowser(pos, side) != null) return;
		Map<BlockSide, ScreenData> desired = desiredScreens.get(key(pos));
		if (desired == null) return;
		ScreenData s = desired.get(side);
		if (s == null) return;
		createBrowser(pos, side, s, true);
	}

	public static void updateScreen(BlockPos pos, ScreenData screen) {
		desiredScreens.computeIfAbsent(key(pos), k -> new EnumMap<>(BlockSide.class)).put(screen.side(), screen);

		RinkuBrowser browser = getBrowser(pos, screen.side());
		if (browser != null) {
			String current = browser.getURL();
			if (current == null || !current.equals(screen.url())) {
				browser.loadURL(screen.url());
			}
		}
		AudioModeHandler.updateScreen(pos, screen);
	}

	public static void removeAllInChunk(ChunkPos chunkPos) {
		int cx = chunkPos.getMinBlockX();
		int cz = chunkPos.getMinBlockZ();
		boolean destroyed = false;
		List<Long> removeDesired = new ArrayList<>();
		for (var entry : desiredScreens.entrySet()) {
			BlockPos pos = BlockPos.of(entry.getKey());
			if (pos.getX() < cx || pos.getX() >= cx + 16 || pos.getZ() < cz || pos.getZ() >= cz + 16) continue;
			Map<BlockSide, RinkuBrowser> alive = browserMap.remove(entry.getKey());
			if (alive != null) {
				for (var bEntry : alive.entrySet()) {
					destroyBrowserWithCleanup(pos, bEntry.getKey(), bEntry.getValue(), "chunk-unload");
					entry.getValue().remove(bEntry.getKey());
					destroyed = true;
				}
			}
			PendingScreenCache.remove(entry.getKey());
			removeDesired.add(entry.getKey());
		}
		for (Long k : removeDesired) {
			desiredScreens.remove(k);
		}
		if (destroyed) {
			BrowserManager.wipeBrowsingData();
			BrowserManager.resetCursor();
		}
	}

	/** Called when the player disconnects; wipes every browser and parked data. */
	public static void onPlayerDisconnect() {
		int count = activeBrowserCount();
		destroyAll("player-quit");
		JabMod.LOGGER.info("Player quit: destroyed {} browser(s), wiping browsing data", count);
	}

	/** Called during shutdown; destroys every browser and logs the total. */
	public static void onShutdownCleanup() {
		int count = activeBrowserCount();
		destroyAll("shutdown");
		JabMod.LOGGER.info("Shutdown: destroyed {} browser(s)", count);
	}

	private static void destroyAll(String reason) {
		for (var entry : browserMap.entrySet()) {
			BlockPos pos = BlockPos.of(entry.getKey());
			for (var bEntry : entry.getValue().entrySet()) {
				try {
					logDestroy(pos, bEntry.getKey(), bEntry.getValue(), reason);
					BrowserManager.destroyBrowser(bEntry.getValue());
				} catch (Exception e) {
					JabMod.LOGGER.warn("Failed to destroy browser at {} side={}: {}", pos, bEntry.getKey(), e.getMessage());
				}
			}
		}
		browserMap.clear();
		desiredScreens.clear();
		creationQueue.clear();
		PendingScreenCache.clear();
		AudioModeHandler.clearAll();
		BrowserManager.wipeBrowsingData();
		BrowserManager.resetCursor();
	}
}
