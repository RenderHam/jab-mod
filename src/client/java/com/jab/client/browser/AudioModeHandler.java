package com.jab.client.browser;

import de.keksuccino.rinku.RinkuBrowser;

import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles the dynamic audio mode: when a screen is set to DYNAMIC, the volume of every
 * video/audio element on the page is driven by the distance between the player and the
 * wall, with a 64 block falloff. GLOBAL mode leaves the page volume untouched.
 */
public class AudioModeHandler {
	private static final Map<Long, Map<BlockSide, AudioState>> dynamicScreens = new HashMap<>();
	private static int tickCounter = 0;

	private static final class AudioState {
		float lastVolume = 1.0f;

		AudioState() {
		}
	}

	public static void sync(BlockPos pos, List<ScreenData> screens) {
		long k = pos.asLong();
		Map<BlockSide, AudioState> bySide = dynamicScreens.computeIfAbsent(k, key -> new HashMap<>());
		Set<BlockSide> keep = new HashSet<>();
		for (ScreenData s : screens) {
			if (s.audioMode() == ScreenData.AudioMode.DYNAMIC) {
				keep.add(s.side());
				bySide.putIfAbsent(s.side(), new AudioState());
			}
		}
		// Restore volume for sides that left DYNAMIC
		for (BlockSide side : new HashSet<>(bySide.keySet())) {
			if (!keep.contains(side)) {
				RinkuBrowser browser = ScreenBrowserManager.getBrowser(pos, side);
				if (browser != null) {
					browser.executeJavaScript("document.querySelectorAll('video,audio').forEach(function(e){e.volume=1.0})", "", 0);
				}
			}
		}
		bySide.keySet().retainAll(keep);
		if (bySide.isEmpty()) dynamicScreens.remove(k);
	}

	public static void updateScreen(BlockPos pos, ScreenData screen) {
		long k = pos.asLong();
		if (screen.audioMode() == ScreenData.AudioMode.DYNAMIC) {
			dynamicScreens.computeIfAbsent(k, key -> new HashMap<>()).put(screen.side(), new AudioState());
		} else {
			Map<BlockSide, AudioState> byPos = dynamicScreens.get(k);
			if (byPos != null) {
				if (byPos.remove(screen.side()) != null) {
					// Restore volume to 1.0 when leaving DYNAMIC — otherwise page stays ducked.
					RinkuBrowser browser = ScreenBrowserManager.getBrowser(pos, screen.side());
					if (browser != null) {
						browser.executeJavaScript("document.querySelectorAll('video,audio').forEach(function(e){e.volume=1.0})", "", 0);
					}
				}
				if (byPos.isEmpty()) dynamicScreens.remove(k);
			}
		}
	}

	public static void remove(BlockPos pos, BlockSide side) {
		long k = pos.asLong();
		Map<BlockSide, AudioState> byPos = dynamicScreens.get(k);
		if (byPos != null) {
			byPos.remove(side);
			if (byPos.isEmpty()) dynamicScreens.remove(k);
		}
	}

	public static void clearAll() {
		dynamicScreens.clear();
	}

	public static void tick() {
		if (dynamicScreens.isEmpty()) return;

		// Re-evaluating volume every tick is wasteful; once every 5 ticks is plenty.
		tickCounter++;
		if (tickCounter % 5 != 0) return;

		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		Vec3 playerCenter = mc.player.getEyePosition();

		for (var entry : dynamicScreens.entrySet()) {
			BlockPos origin = BlockPos.of(entry.getKey());
			for (var sideEntry : entry.getValue().entrySet()) {
				BlockSide side = sideEntry.getKey();
				AudioState state = sideEntry.getValue();
				ScreenData screen = ScreenBrowserManager.getDesiredScreen(origin, side);
				Vec3 center;
				if (screen != null) {
					double cx = origin.getX() + 0.5 + side.rightX * screen.width() * 0.5 + side.upX * screen.height() * 0.5 + side.faceX * 0.5;
					double cy = origin.getY() + 0.5 + side.rightY * screen.width() * 0.5 + side.upY * screen.height() * 0.5 + side.faceY * 0.5;
					double cz = origin.getZ() + 0.5 + side.rightZ * screen.width() * 0.5 + side.upZ * screen.height() * 0.5 + side.faceZ * 0.5;
					center = new Vec3(cx, cy, cz);
				} else {
					center = Vec3.atCenterOf(origin);
				}
				float dist = (float) playerCenter.distanceTo(center);
				float volume = Math.max(0.0f, Math.min(1.0f, 1.0f - dist / 64.0f));
				if (volume < 0.01f) volume = 0.0f;

				if (Math.abs(volume - state.lastVolume) > 0.01f) {
					RinkuBrowser browser = ScreenBrowserManager.getBrowser(origin, side);
					if (browser != null) {
						String js = String.format(java.util.Locale.ROOT, "document.querySelectorAll('video,audio').forEach(function(e){e.volume=%f})", volume);
						browser.executeJavaScript(js, "", 0);
						state.lastVolume = volume;
					}
				}
			}
		}
	}
}
