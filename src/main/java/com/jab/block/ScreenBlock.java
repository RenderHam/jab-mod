package com.jab.block;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.registry.ModBlocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The screen block. A wall of these blocks forms a multiblock display; only the origin
 * block (bottom-left when looking at the wall) holds a block entity with the screen data.
 */
public class ScreenBlock extends BaseEntityBlock {
	public static final BooleanProperty HAS_TE = BooleanProperty.create("has_te");

	public ScreenBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HAS_TE, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(ScreenBlock::new);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_TE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HAS_TE) ? new ScreenBlockEntity(pos, state) : null;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && state.is(ModBlocks.SCREEN_BLOCK) && !state.getValue(HAS_TE)) {
			destroyOriginForWallContaining(level, pos);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
		if (state.is(ModBlocks.SCREEN_BLOCK) && !state.getValue(HAS_TE)) {
			destroyOriginForWallContaining(level, pos);
		}
		super.onExplosionHit(state, level, pos, explosion, dropConsumer);
	}

	/**
	 * Breaking any non-origin block of a wall must also kill the display on the origin,
	 * otherwise the wall would be stuck in a broken state. Flood-fills from the broken
	 * block until it reaches the origin block entity.
	 */
	private static void destroyOriginForWallContaining(Level level, BlockPos brokenPos) {
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new LinkedList<>();
		queue.add(brokenPos);
		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			if (!visited.add(cur)) continue;
			if (visited.size() > 200) break;
			BlockState curState = level.getBlockState(cur);
			if (!curState.is(ModBlocks.SCREEN_BLOCK)) continue;
			if (curState.getValue(HAS_TE)) {
				BlockEntity be = level.getBlockEntity(cur);
				if (be instanceof ScreenBlockEntity sbe) {
					sbe.onDestroy();
					return;
				}
			}
			for (Direction dir : Direction.values()) {
				queue.add(cur.relative(dir));
			}
		}
	}
}
