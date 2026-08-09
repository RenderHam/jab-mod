package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.blockentity.ScreenBlockEntity;
import com.jab.util.BlockSide;

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
			ByteBufCodecs.VAR_INT, p -> p.side.ordinal(),
			ByteBufCodecs.STRING_UTF8, ScreenActionC2SPacket::url,
			(pos, side, url) -> new ScreenActionC2SPacket(
					pos,
					BlockSide.values()[Math.min(side, BlockSide.values().length - 1)],
					url)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void handle(ScreenActionC2SPacket payload, ServerPlayNetworking.Context ctx) {
		ServerPlayer player = ctx.player();
		if (player == null) return;
		ctx.server().execute(() -> {
			ServerLevel level = (ServerLevel) player.level();
			if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 4096) return;
			if (!(level.getBlockEntity(payload.pos()) instanceof ScreenBlockEntity sbe)) return;
			if (sbe.getScreen(payload.side) == null) return;

			String url = payload.url();
			if (url != null && url.length() <= 2048 && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:"))) {
				sbe.setUrl(payload.side(), url);
			}
		});
	}
}
