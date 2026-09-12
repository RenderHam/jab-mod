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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Client-to-server request to change one screen's URL from the GUI. */
public record ScreenActionC2SPacket(BlockPos pos, BlockSide side, String url) implements CustomPacketPayload {

	public static final int MAX_INTERACTION_SQR = 64 * 64;
	private static final long RATE_LIMIT_MS = 200;
	private static final java.util.Map<java.util.UUID, Long> lastSent = new java.util.concurrent.ConcurrentHashMap<>();

	public static final CustomPacketPayload.Type<ScreenActionC2SPacket> ID =
			new CustomPacketPayload.Type<>(ResourceLocation.parse(JabMod.id("screen_action")));

	public static final StreamCodec<ByteBuf, ScreenActionC2SPacket> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ScreenActionC2SPacket::pos,
			ByteBufCodecs.STRING_UTF8, p -> p.side.name(),
			ByteBufCodecs.stringUtf8(2048), ScreenActionC2SPacket::url,
			(pos, side, url) -> new ScreenActionC2SPacket(pos, BlockSide.lenientValueOf(side), url)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void handle(ScreenActionC2SPacket payload, ServerPlayNetworking.Context ctx) {
		ServerPlayer player = ctx.player();
		if (player == null) return;
		// Rate limit: 5 per second per player
		long now = System.currentTimeMillis();
		Long last = lastSent.get(player.getUUID());
		if (last != null && now - last < RATE_LIMIT_MS) return;
		lastSent.put(player.getUUID(), now);

		ctx.server().execute(() -> {
			if (!(player.level() instanceof ServerLevel level)) return;
			if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > MAX_INTERACTION_SQR) return;
			if (!(level.getBlockEntity(payload.pos()) instanceof ScreenBlockEntity sbe)) return;
			if (sbe.getScreen(payload.side()) == null) return;

			String url = UrlUtil.sanitize(payload.url());
			if (!UrlUtil.isValidLength(url)) return;
			// Basic URI validation — allow http/https/about only
			try {
				java.net.URI uri = new java.net.URI(url);
				String scheme = uri.getScheme();
				if (scheme != null && !(scheme.equals("http") || scheme.equals("https") || scheme.equals("about"))) return;
			} catch (Exception ignored) {
				return;
			}
			sbe.setUrl(payload.side(), url);
		});
	}
}
