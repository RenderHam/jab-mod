package com.jab.client.browser;

import com.cinemamod.mcef.MCEFBrowser;

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
	private static final Map<String, Float> lastVolumes = new HashMap<>();
	private static int tickCounter = 0;

	private static String key(BlockPos pos, BlockSide side) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + side.ordinal();
	}

	public static void sync(BlockPos pos, List<ScreenData> screens) {
		Set<String> activeDynamic = new HashSet<>();
		for (ScreenData s : screens) {
			if (s.audioMode == ScreenData.AudioMode.DYNAMIC) {
				String k = key(pos, s.side);
				activeDynamic.add(k);
				lastVolumes.putIfAbsent(k, 1.0f);
			}
		}
		String prefix = pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":";
		lastVolumes.keySet().removeIf(k -> k.startsWith(prefix) && !activeDynamic.contains(k));
	}

	public static void updateScreen(BlockPos pos, ScreenData screen) {
		String k = key(pos, screen.side);
		if (screen.audioMode == ScreenData.AudioMode.DYNAMIC) {
			lastVolumes.putIfAbsent(k, 1.0f);
		} else {
			lastVolumes.remove(k);
		}
	}

	public static void tick() {
		if (lastVolumes.isEmpty()) return;

		// Re-evaluating volume every tick is wasteful; once every 5 ticks is plenty.
		tickCounter++;
		if (tickCounter % 5 != 0) return;

		var mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		Vec3 playerPos = mc.player.position();

		for (var entry : lastVolumes.entrySet()) {
			String k = entry.getKey();
			try {
				String[] parts = k.split(":");
				String[] posParts = parts[0].split(",");
				int x = Integer.parseInt(posParts[0]);
				int y = Integer.parseInt(posParts[1]);
				int z = Integer.parseInt(posParts[2]);
				BlockSide side = BlockSide.values()[Integer.parseInt(parts[1])];
				BlockPos pos = new BlockPos(x, y, z);

				Vec3 center = Vec3.atCenterOf(pos);
				float dist = (float) playerPos.distanceTo(center);
				float volume = Math.max(0.0f, Math.min(1.0f, 1.0f - dist / 64.0f));
				if (volume < 0.01f) volume = 0.0f;

				float lastVol = entry.getValue();
				if (Math.abs(volume - lastVol) > 0.01f) {
					MCEFBrowser browser = ScreenBrowserManager.getBrowser(pos, side);
					if (browser != null) {
						String js = "document.querySelectorAll('video,audio').forEach(function(e){e.volume=" + volume + "})";
						browser.executeJavaScript(js, "", 0);
						lastVolumes.put(k, volume);
					}
				}
			} catch (Exception e) {
				lastVolumes.remove(k);
			}
		}
	}
}
