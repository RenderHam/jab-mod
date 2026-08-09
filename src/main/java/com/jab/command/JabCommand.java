package com.jab.command;

import com.jab.block.ScreenBlock;
import com.jab.blockentity.ScreenBlockEntity;
import com.jab.config.JabConfig;
import com.jab.data.ScreenData;
import com.jab.registry.ModBlocks;
import com.jab.util.BlockSide;
import com.jab.util.Multiblock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class JabCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jab")
				.then(Commands.literal("create")
						.executes(ctx -> create(ctx, null))
						.then(Commands.argument("url", StringArgumentType.greedyString())
								.executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "url")))))
				.then(Commands.literal("remove")
						.executes(JabCommand::remove))
				.then(Commands.literal("url")
						.then(Commands.argument("url", StringArgumentType.greedyString())
								.executes(JabCommand::setUrl)))
				.then(Commands.literal("audio")
						.then(Commands.literal("global")
								.executes(ctx -> setAudioMode(ctx, ScreenData.AudioMode.GLOBAL)))
						.then(Commands.literal("dynamic")
								.executes(ctx -> setAudioMode(ctx, ScreenData.AudioMode.DYNAMIC))))
				.then(Commands.literal("debug")
						.executes(JabCommand::debug))
		);
	}

	/**
	 * Turns the wall the player is looking at into a display. The wall must be a solid
	 * rectangle of screen blocks (2x2 minimum); the origin block gets the block entity.
	 */
	private static int create(CommandContext<CommandSourceStack> ctx, String url) throws CommandSyntaxException {
		var source = ctx.getSource();
		var player = source.getPlayerOrException();
		var world = player.level();

		var look = lookAtScreenBlock(player, world);
		if (look == null) {
			source.sendFailure(Component.literal("You must be looking at a screen block wall"));
			return 0;
		}

		BlockPos originPos = look.origin;
		int[] size = Multiblock.measure(world, originPos, look.side);
		if (size[0] < 2 || size[1] < 2) {
			source.sendFailure(Component.literal("Screen must be at least 2x2 blocks"));
			return 0;
		}
		if (size[0] > JabConfig.maxScreenSize || size[1] > JabConfig.maxScreenSize) {
			source.sendFailure(Component.literal("Screen too large (max " + JabConfig.maxScreenSize + " blocks)"));
			return 0;
		}

		BlockPos err = Multiblock.check(world, originPos, size[0], size[1], look.side);
		if (err != null) {
			source.sendFailure(Component.literal("Screen wall has a missing block at " + err.toShortString()));
			return 0;
		}

		if (world.getBlockEntity(originPos) instanceof ScreenBlockEntity sbe) {
			if (sbe.getScreen(look.side) != null) {
				source.sendFailure(Component.literal("A display already exists on this face"));
				return 0;
			}
			addDisplay(sbe, look.side, size, url);
			source.sendSuccess(() -> Component.literal("Created display (" + size[0] + "x" + size[1] + ")"), true);
			return 1;
		}

		world.setBlock(originPos, world.getBlockState(originPos).setValue(ScreenBlock.HAS_TE, true), 3);
		if (world.getBlockEntity(originPos) instanceof ScreenBlockEntity sbe) {
			addDisplay(sbe, look.side, size, url);
			source.sendSuccess(() -> Component.literal("Created display (" + size[0] + "x" + size[1] + ")"), true);
			return 1;
		}

		source.sendFailure(Component.literal("Failed to create display"));
		return 0;
	}

	private static void addDisplay(ScreenBlockEntity sbe, BlockSide side, int[] size, String url) {
		sbe.addScreen(side, size[0], size[1]);
		if (url != null) sbe.setUrl(side, url);
	}

	private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		var source = ctx.getSource();
		var player = source.getPlayerOrException();
		var world = player.level();

		var result = findScreenBE(player, world);
		if (result == null) return 0;

		result.be().onDestroy();
		source.sendSuccess(() -> Component.literal("Display removed"), true);

		return 1;
	}

	private static int setUrl(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		var source = ctx.getSource();
		var player = source.getPlayerOrException();
		var world = player.level();
		String url = StringArgumentType.getString(ctx, "url");

		var result = findScreenBE(player, world);
		if (result == null) return 0;

		if (result.be().setUrl(result.side(), url)) {
			source.sendSuccess(() -> Component.literal("URL set to " + url), true);
			return 1;
		}
		source.sendFailure(Component.literal("No display on this face"));
		return 0;
	}

	private static int setAudioMode(CommandContext<CommandSourceStack> ctx, ScreenData.AudioMode mode) throws CommandSyntaxException {
		var source = ctx.getSource();
		var player = source.getPlayerOrException();
		var world = player.level();

		var result = findScreenBE(player, world);
		if (result == null) return 0;

		if (result.be().setAudioMode(result.side(), mode)) {
			source.sendSuccess(() -> Component.literal("Audio mode set to " + mode.name().toLowerCase()), true);
			return 1;
		}
		source.sendFailure(Component.literal("No display on this face"));
		return 0;
	}

	private static Pair findScreenBE(ServerPlayer player, Level world) {
		var hit = player.pick(20.0, 1.0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendSystemMessage(Component.literal("You must be looking at a screen block"));
			return null;
		}

		BlockHitResult bhr = (BlockHitResult) hit;
		BlockSide side = BlockSide.fromDirection(bhr.getDirection());
		BlockPos pos = bhr.getBlockPos();

		if (world.getBlockState(pos).getBlock() != ModBlocks.SCREEN_BLOCK) {
			player.sendSystemMessage(Component.literal("You must look at a screen block"));
			return null;
		}

		BlockPos.MutableBlockPos origin = pos.mutable();
		Multiblock.findOrigin(world, origin, side);
		BlockPos originPos = origin.immutable();

		BlockEntity be = world.getBlockEntity(originPos);
		if (!(be instanceof ScreenBlockEntity sbe)) {
			player.sendSystemMessage(Component.literal("No display found on this wall"));
			return null;
		}

		return new Pair(sbe, side);
	}

	/**
	 * Resolves the wall the player is looking at to its origin block and relevant face.
	 * Returns null when the player is not looking at a screen block.
	 */
	private static WallLook lookAtScreenBlock(ServerPlayer player, Level world) {
		var hit = player.pick(20.0, 1.0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) return null;

		BlockHitResult bhr = (BlockHitResult) hit;
		BlockSide side = BlockSide.fromDirection(bhr.getDirection());
		BlockPos pos = bhr.getBlockPos();

		if (world.getBlockState(pos).getBlock() != ModBlocks.SCREEN_BLOCK) return null;

		BlockPos.MutableBlockPos origin = pos.mutable();
		Multiblock.findOrigin(world, origin, side);

		return new WallLook(origin.immutable(), side);
	}

	private static int debug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		var source = ctx.getSource();
		var player = source.getPlayerOrException();
		var world = player.level();

		source.sendSuccess(() -> Component.literal("§6=== JAB Debug ==="), false);
		source.sendSuccess(() -> Component.literal("§eServer-side only (client info via log)"), false);

		var hit = player.pick(20.0, 1.0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			source.sendSuccess(() -> Component.literal("§cNot looking at a block"), false);
			source.sendSuccess(() -> Component.literal("§6======================"), false);
			return 1;
		}

		BlockHitResult bhr = (BlockHitResult) hit;
		BlockSide side = BlockSide.fromDirection(bhr.getDirection());
		BlockPos lookPos = bhr.getBlockPos();

		source.sendSuccess(() -> Component.literal("§eLooked at: §f" + lookPos.toShortString() + " §7side=" + side), false);
		source.sendSuccess(() -> Component.literal("§eBlock: §f" + world.getBlockState(lookPos).getBlock()), false);

		if (world.getBlockState(lookPos).getBlock() != ModBlocks.SCREEN_BLOCK) {
			source.sendSuccess(() -> Component.literal("§cNot a screen block — nothing to debug"), false);
			source.sendSuccess(() -> Component.literal("§6======================"), false);
			return 1;
		}

		int[] fromHit = Multiblock.measure(world, lookPos, side);
		source.sendSuccess(() -> Component.literal("§eFrom-hit wall size: §f" + fromHit[0] + "x" + fromHit[1]), false);

		BlockPos.MutableBlockPos origin = lookPos.mutable();
		Multiblock.findOrigin(world, origin, side);
		BlockPos originPos = origin.immutable();
		source.sendSuccess(() -> Component.literal("§eOrigin: §f" + originPos.toShortString()), false);

		int[] size = Multiblock.measure(world, originPos, side);
		source.sendSuccess(() -> Component.literal("§eWall size: §f" + size[0] + "x" + size[1]), false);

		BlockPos gap = Multiblock.check(world, originPos, size[0], size[1], side);
		if (gap != null) {
			source.sendSuccess(() -> Component.literal("§cGap at: §f" + gap.toShortString()), false);
		} else {
			source.sendSuccess(() -> Component.literal("§aWall contiguous"), false);
		}

		if (world.getBlockEntity(originPos) instanceof ScreenBlockEntity sbe) {
			source.sendSuccess(() -> Component.literal("§eHas BE: §ayes"), false);
			source.sendSuccess(() -> Component.literal("§eScreens: §f" + sbe.getScreens().size()), false);
			for (ScreenData sd : sbe.getScreens()) {
				source.sendSuccess(() -> Component.literal("§7  [side=" + sd.side
						+ " §7w=" + sd.width + " h=" + sd.height
						+ " §7url=§f" + sd.url
						+ " §7res=§f" + sd.resX + "x" + sd.resY
						+ " §7audio=§f" + sd.audioMode.name().toLowerCase() + "]"), false);
			}
		} else {
			source.sendSuccess(() -> Component.literal("§cHas BE: no"), false);
		}

		source.sendSuccess(() -> Component.literal("§6======================"), false);
		return 1;
	}

	private record Pair(ScreenBlockEntity be, BlockSide side) {}

	private record WallLook(BlockPos origin, BlockSide side) {}
}
