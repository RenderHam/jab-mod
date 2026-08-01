package com.jab.util;

import com.jab.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class Multiblock {
	private static final int MAX_ORIGIN_STEPS = 256;

	/**
	 * Walks the wall backwards along the -r and -u vectors until it finds the origin block.
	 * The origin is the bottom-left block of the wall when looking at it.
	 */
	public static void findOrigin(Level world, BlockPos.MutableBlockPos pos, BlockSide side) {
		int steps = 0;
		do {
			pos.move(-side.rx, -side.ry, -side.rz);
			if (++steps > MAX_ORIGIN_STEPS || !world.isInWorldBounds(pos)) break;
		} while (world.getBlockState(pos).getBlock() == ModBlocks.SCREEN_BLOCK);
		if (steps <= MAX_ORIGIN_STEPS && world.isInWorldBounds(pos)) {
			pos.move(side.rx, side.ry, side.rz);
		}
		steps = 0;
		do {
			pos.move(-side.ux, -side.uy, -side.uz);
			if (++steps > MAX_ORIGIN_STEPS || !world.isInWorldBounds(pos)) break;
		} while (world.getBlockState(pos).getBlock() == ModBlocks.SCREEN_BLOCK);
		if (steps <= MAX_ORIGIN_STEPS && world.isInWorldBounds(pos)) {
			pos.move(side.ux, side.uy, side.uz);
		}
	}

	public static int[] measure(Level world, BlockPos origin, BlockSide side) {
		int width = 0, height = 0;
		BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
		bp.set(origin);
		do {
			bp.move(side.ux, side.uy, side.uz);
			height++;
		} while (world.getBlockState(bp).getBlock() == ModBlocks.SCREEN_BLOCK);
		bp.set(origin);
		do {
			bp.move(side.rx, side.ry, side.rz);
			width++;
		} while (world.getBlockState(bp).getBlock() == ModBlocks.SCREEN_BLOCK);
		return new int[]{width, height};
	}

	/**
	 * Verifies the wall is a solid rectangle of screen blocks and returns the first
	 * offending position, or null if the wall is valid.
	 */
	public static BlockPos check(Level world, BlockPos origin, int width, int height, BlockSide side) {
		BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				bp.set(origin.getX() + side.rx * x + side.ux * y,
						origin.getY() + side.ry * x + side.uy * y,
						origin.getZ() + side.rz * x + side.uz * y);
				if (world.getBlockState(bp).getBlock() != ModBlocks.SCREEN_BLOCK) {
					return bp.immutable();
				}
			}
		}
		// The wall must not have stray screen blocks glued to any of its four edges.
		bp.set(origin.getX() - side.rx, origin.getY() - side.ry, origin.getZ() - side.rz);
		for (int y = 0; y < height; y++) {
			if (world.getBlockState(bp).getBlock() == ModBlocks.SCREEN_BLOCK) return bp.immutable();
			bp.move(side.ux, side.uy, side.uz);
		}
		bp.set(origin.getX() + side.rx * width, origin.getY() + side.ry * width, origin.getZ() + side.rz * width);
		for (int y = 0; y < height; y++) {
			if (world.getBlockState(bp).getBlock() == ModBlocks.SCREEN_BLOCK) return bp.immutable();
			bp.move(side.ux, side.uy, side.uz);
		}
		bp.set(origin.getX() - side.ux, origin.getY() - side.uy, origin.getZ() - side.uz);
		for (int x = 0; x < width; x++) {
			if (world.getBlockState(bp).getBlock() == ModBlocks.SCREEN_BLOCK) return bp.immutable();
			bp.move(side.rx, side.ry, side.rz);
		}
		bp.set(origin.getX() + side.ux * height, origin.getY() + side.uy * height, origin.getZ() + side.uz * height);
		for (int x = 0; x < width; x++) {
			if (world.getBlockState(bp).getBlock() == ModBlocks.SCREEN_BLOCK) return bp.immutable();
			bp.move(side.rx, side.ry, side.rz);
		}
		return null;
	}
}
