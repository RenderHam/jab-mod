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
			if (s.audioMode == ScreenData.AudioMode.DYNAMIC) {
				keep.add(s.side);
				bySide.putIfAbsent(s.side, new AudioState());
			}
		}
		bySide.keySet().retainAll(keep);
		if (bySide.isEmpty()) dynamicScreens.remove(k);
	}

	public static void updateScreen(BlockPos pos, ScreenData screen) {
		long k = pos.asLong();
		if (screen.audioMode == ScreenData.AudioMode.DYNAMIC) {
			dynamicScreens.computeIfAbsent(k, key -> new HashMap<>()).put(screen.side, new AudioState());
		} else {
			Map<BlockSide, AudioState> byPos = dynamicScreens.get(k);
			if (byPos != null) {
				byPos.remove(screen.side);
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
			BlockPos pos = BlockPos.of(entry.getKey());
			Vec3 center = Vec3.atCenterOf(pos);
			for (var sideEntry : entry.getValue().entrySet()) {
				AudioState state = sideEntry.getValue();
				float dist = (float) playerCenter.distanceTo(center);
				float volume = Math.max(0.0f, Math.min(1.0f, 1.0f - dist / 64.0f));
				if (volume < 0.01f) volume = 0.0f;

				if (Math.abs(volume - state.lastVolume) > 0.01f) {
					RinkuBrowser browser = ScreenBrowserManager.getBrowser(pos, sideEntry.getKey());
					if (browser != null) {
						String js = "document.querySelectorAll('video,audio').forEach(function(e){e.volume=" + volume + "})";
						browser.executeJavaScript(js, "", 0);
						state.lastVolume = volume;
					}
				}
			}
		}
	}
}