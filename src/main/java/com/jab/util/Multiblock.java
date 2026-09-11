package com.jab.util;

import com.jab.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class Multiblock {
	private static final int MAX_ORIGIN_STEPS = 32;
	private static final int MAX_MEASURE_STEPS = 64;

	public record WallSize(int width, int height) {}

	/**
	 * Flood-fills from start across connected screen blocks to find the first position
	 * matching the predicate. Bounded by maxScreenSize² to avoid runaway.
	 */
	public static BlockPos floodFind(Level world, BlockPos start, java.util.function.Predicate<BlockPos> isTarget) {
		java.util.Set<Long> visited = new java.util.HashSet<>();
		java.util.Queue<BlockPos> queue = new java.util.ArrayDeque<>();
		int maxArea = com.jab.config.JabConfig.get().maxScreenSize() * com.jab.config.JabConfig.get().maxScreenSize();
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			long k = cur.asLong();
			if (!visited.add(k)) continue;
			if (visited.size() > maxArea) break;
			if (!world.getBlockState(cur).is(ModBlocks.SCREEN_BLOCK)) continue;
			if (isTarget.test(cur)) return cur;
			for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
				queue.add(cur.relative(dir));
			}
		}
		return null;
	}

	/**
	 * Resolves the origin block for a screen block hit by a ray.
	 * The caller must verify the hit block is a screen block before calling.
	 */
	public static BlockPos resolveOrigin(Level world, BlockPos hitPos, BlockSide side) {
		BlockPos.MutableBlockPos origin = hitPos.mutable();
		findOrigin(world, origin, side);
		return origin.immutable();
	}

	/**
	 * Walks the wall backwards along the -right and -up vectors until it finds the origin block.
	 * The origin is the bottom-left block of the wall when looking at it.
	 */
	public static void findOrigin(Level world, BlockPos.MutableBlockPos pos, BlockSide side) {
		int steps = 0;
		do {
			pos.move(-side.rightX, -side.rightY, -side.rightZ);
			if (++steps > MAX_ORIGIN_STEPS || !world.isInWorldBounds(pos)) break;
		} while (world.getBlockState(pos).is(ModBlocks.SCREEN_BLOCK));
		if (steps <= MAX_ORIGIN_STEPS && world.isInWorldBounds(pos)) {
			pos.move(side.rightX, side.rightY, side.rightZ);
		}
		steps = 0;
		do {
			pos.move(-side.upX, -side.upY, -side.upZ);
			if (++steps > MAX_ORIGIN_STEPS || !world.isInWorldBounds(pos)) break;
		} while (world.getBlockState(pos).is(ModBlocks.SCREEN_BLOCK));
		if (steps <= MAX_ORIGIN_STEPS && world.isInWorldBounds(pos)) {
			pos.move(side.upX, side.upY, side.upZ);
		}
	}

	public static WallSize measure(Level world, BlockPos origin, BlockSide side) {
		int width = 0, height = 0;
		BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
		bp.set(origin);
		do {
			bp.move(side.upX, side.upY, side.upZ);
			height++;
		} while (height < MAX_MEASURE_STEPS && world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK));
		bp.set(origin);
		do {
			bp.move(side.rightX, side.rightY, side.rightZ);
			width++;
		} while (width < MAX_MEASURE_STEPS && world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK));
		return new WallSize(width, height);
	}

	/**
	 * Verifies the wall is a solid rectangle of screen blocks and returns the first
	 * offending position, or null if the wall is valid.
	 */
	public static BlockPos check(Level world, BlockPos origin, int width, int height, BlockSide side) {
		BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				bp.set(origin.getX() + side.rightX * x + side.upX * y,
						origin.getY() + side.rightY * x + side.upY * y,
						origin.getZ() + side.rightZ * x + side.upZ * y);
				if (!world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK)) {
					return bp.immutable();
				}
			}
		}
		// The wall must not have stray screen blocks glued to any of its four edges.
		bp.set(origin.getX() - side.rightX, origin.getY() - side.rightY, origin.getZ() - side.rightZ);
		for (int y = 0; y < height; y++) {
			if (world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK)) return bp.immutable();
			bp.move(side.upX, side.upY, side.upZ);
		}
		bp.set(origin.getX() + side.rightX * width, origin.getY() + side.rightY * width, origin.getZ() + side.rightZ * width);
		for (int y = 0; y < height; y++) {
			if (world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK)) return bp.immutable();
			bp.move(side.upX, side.upY, side.upZ);
		}
		bp.set(origin.getX() - side.upX, origin.getY() - side.upY, origin.getZ() - side.upZ);
		for (int x = 0; x < width; x++) {
			if (world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK)) return bp.immutable();
			bp.move(side.rightX, side.rightY, side.rightZ);
		}
		bp.set(origin.getX() + side.upX * height, origin.getY() + side.upY * height, origin.getZ() + side.upZ * height);
		for (int x = 0; x < width; x++) {
			if (world.getBlockState(bp).is(ModBlocks.SCREEN_BLOCK)) return bp.immutable();
			bp.move(side.rightX, side.rightY, side.rightZ);
		}
		return null;
	}
}
