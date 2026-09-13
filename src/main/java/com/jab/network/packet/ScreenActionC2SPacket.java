package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.blockentity.ScreenBlockEntity;
import com.jab.util.BlockSide;
import com.jab.util.UrlUtil;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

/** Client-to-server request to change one screen's URL from the GUI. */
public record ScreenActionC2SPacket(BlockPos pos, BlockSide side, String url) {

	public static final int MAX_INTERACTION_SQR = 64 * 64;
	private static final long RATE_LIMIT_MS = 200;
	private static final java.util.Map<java.util.UUID, Long> lastSent = new java.util.concurrent.ConcurrentHashMap<>();

	public static final ResourceLocation ID = JabMod.id("screen_action");

	public static void encode(FriendlyByteBuf buf, BlockPos pos, BlockSide side, String url) {
		buf.writeBlockPos(pos);
		buf.writeUtf(side.name());
		buf.writeUtf(url, 2048);
	}

	public static ScreenActionC2SPacket decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		String sideName = buf.readUtf();
		String url = buf.readUtf(2048);
		return new ScreenActionC2SPacket(pos, BlockSide.lenientValueOf(sideName), url);
	}

	public static void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
			FriendlyByteBuf buf, PacketSender responseSender) {
		ScreenActionC2SPacket payload = decode(buf);
		if (player == null) return;
		long now = System.currentTimeMillis();
		Long last = lastSent.get(player.getUUID());
		if (last != null && now - last < RATE_LIMIT_MS) return;
		lastSent.put(player.getUUID(), now);

		server.execute(() -> {
			if (!(player.level instanceof ServerLevel level)) return;
			if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > MAX_INTERACTION_SQR) return;
			if (!(level.getBlockEntity(payload.pos()) instanceof ScreenBlockEntity sbe)) return;
			if (sbe.getScreen(payload.side()) == null) return;

			String url = UrlUtil.sanitize(payload.url());
			if (!UrlUtil.isValidLength(url)) return;
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
