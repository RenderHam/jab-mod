package com.jab.util;

import com.jab.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Raycasts from a player's eyes to find a screen block wall's origin and face.
 */
public final class WallRaycast {
	private static final double REACH_DISTANCE = 20.0;
	private static final float UNKNOWN_PITCH = 1.0f;

	private WallRaycast() {}

	/** Result of a wall raycast: the origin block pos, the face side, and the hit block pos. */
	public record Result(BlockPos hitPos, BlockPos origin, BlockSide side) {}

	/**
	 * Casts a ray from the player and resolves the wall origin.
	 * Returns null when the player is not looking at a screen block.
	 */
	public static Result raycast(Player player, Level world) {
		var hit = player.pick(REACH_DISTANCE, UNKNOWN_PITCH, false);
		if (!(hit instanceof BlockHitResult bhr)) return null;
		if (bhr.getType() != HitResult.Type.BLOCK) return null;

		BlockSide side = BlockSide.fromDirection(bhr.getDirection());
		BlockPos pos = bhr.getBlockPos();

		if (!world.getBlockState(pos).is(ModBlocks.SCREEN_BLOCK)) return null;

		BlockPos.MutableBlockPos origin = pos.mutable();
		Multiblock.findOrigin(world, origin, side);

		return new Result(pos, origin.immutable(), side);
	}
}
