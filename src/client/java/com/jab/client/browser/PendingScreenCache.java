package com.jab.client.browser;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stashes screen data that arrives via network before the wall's block entity
 * exists on the client. Flushed into the BE every tick once it shows up.
 */
public final class PendingScreenCache {
	private static final Map<Long, List<ScreenData>> pending = new HashMap<>();

	private PendingScreenCache() {}

	public static void store(BlockPos pos, List<ScreenData> screens) {
		pending.put(pos.asLong(), screens);
	}

	/** Applies an update to parked data, or parks it if nothing is parked yet. */
	public static void applyUpdate(BlockPos pos, ScreenData update) {
		long k = pos.asLong();
		List<ScreenData> list = pending.get(k);
		if (list == null) {
			list = new ArrayList<>();
			pending.put(k, list);
		}
		for (ScreenData s : list) {
			if (s.side() == update.side()) {
				s.copyFrom(update);
				return;
			}
		}
		list.add(update);
	}

	/** Moves parked screen data into the block entity once it exists. */
	public static void tickFlush() {
		if (pending.isEmpty()) return;
		Level world = Minecraft.getInstance().level;
		if (world == null) return;
		var iter = pending.entrySet().iterator();
		while (iter.hasNext()) {
			var entry = iter.next();
			if (world.getBlockEntity(BlockPos.of(entry.getKey())) instanceof ScreenBlockEntity sbe) {
				sbe.replaceAllScreens(entry.getValue());
				iter.remove();
			}
		}
	}

	public static void remove(long key) {
		pending.remove(key);
	}

	public static void clear() {
		pending.clear();
	}
}
