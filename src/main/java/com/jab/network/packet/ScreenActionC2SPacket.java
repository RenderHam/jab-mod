package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.blockentity.ScreenBlockEntity;
import com.jab.util.BlockSide;
import com.jab.util.UrlUtil;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Client-to-server request to change one screen's URL from the GUI. */
public record ScreenActionC2SPacket(BlockPos pos, BlockSide side, String url) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ScreenActionC2SPacket> ID =
			new CustomPacketPayload.Type<>(Identifier.parse(JabMod.id("screen_action")));

	public static final StreamCodec<ByteBuf, ScreenActionC2SPacket> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ScreenActionC2SPacket::pos,
			ByteBufCodecs.STRING_UTF8, p -> p.side.name(),
			ByteBufCodecs.STRING_UTF8, ScreenActionC2SPacket::url,
			(pos, side, url) -> {
				BlockSide sideEnum;
				try {
					sideEnum = BlockSide.valueOf(side);
				} catch (IllegalArgumentException e) {
					sideEnum = BlockSide.BOTTOM;
				}
				return new ScreenActionC2SPacket(pos, sideEnum, url);
			}
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void handle(ScreenActionC2SPacket payload, ServerPlayNetworking.Context ctx) {
		ServerPlayer player = ctx.player();
		if (player == null) return;
		ctx.server().execute(() -> {
			if (!(player.level() instanceof ServerLevel level)) return;
			if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 4096) return;
			if (!(level.getBlockEntity(payload.pos()) instanceof ScreenBlockEntity sbe)) return;
			if (sbe.getScreen(payload.side()) == null) return;

			String url = UrlUtil.sanitize(payload.url());
			if (UrlUtil.isValidLength(url)) {
				sbe.setUrl(payload.side(), url);
			}
		});
	}
}
