package com.jab.block;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.config.JabConfig;
import com.jab.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_TE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HAS_TE) ? new ScreenBlockEntity(pos, state) : null;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && state.is(ModBlocks.SCREEN_BLOCK) && !state.getValue(HAS_TE)) {
			destroyOriginForWallContaining(level, pos);
		}
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
		for (Direction dir : Direction.values()) {
			BlockPos neighbor = pos.relative(dir);
			BlockState neighborState = level.getBlockState(neighbor);
			if (neighborState.is(ModBlocks.SCREEN_BLOCK)) {
				if (neighborState.getValue(HAS_TE)) {
					BlockEntity be = level.getBlockEntity(neighbor);
					if (be instanceof ScreenBlockEntity sbe) sbe.onDestroy();
				} else {
					destroyOriginForWallContaining(level, neighbor);
				}
				return;
			}
		}
		super.wasExploded(level, pos, explosion);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (state.is(ModBlocks.SCREEN_BLOCK)) {
			if (state.getValue(HAS_TE)) {
				BlockEntity be = level.getBlockEntity(pos);
				if (be instanceof ScreenBlockEntity sbe) sbe.onDestroy();
			} else {
				destroyOriginForWallContaining(level, pos);
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/**
	 * Breaking any non-origin block of a wall must also kill the display on the origin,
	 * otherwise the wall would be stuck in a broken state. Flood-fills from the broken
	 * block until it reaches the origin block entity.
	 */
	private static void destroyOriginForWallContaining(Level level, BlockPos brokenPos) {
		BlockPos origin = com.jab.util.Multiblock.floodFind(level, brokenPos, pos -> {
			BlockState s = level.getBlockState(pos);
			if (!s.getValue(HAS_TE)) return false;
			return level.getBlockEntity(pos) instanceof ScreenBlockEntity;
		});
		if (origin != null && level.getBlockEntity(origin) instanceof ScreenBlockEntity sbe) {
			sbe.onDestroy();
		}
	}
}
