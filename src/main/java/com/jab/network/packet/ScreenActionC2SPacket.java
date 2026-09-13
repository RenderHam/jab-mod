package com.jab.network.packet;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.util.BlockSide;
import com.jab.util.UrlUtil;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScreenActionC2SPacket {
	public static final int MAX_INTERACTION_SQR = 64 * 64;
	private static final long RATE_LIMIT_MS = 200;
	private static final Map<UUID, Long> lastSent = new ConcurrentHashMap<>();

	public static void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
							  FriendlyByteBuf buf, PacketSender responseSender) {
		BlockPos pos = buf.readBlockPos();
		BlockSide side = BlockSide.lenientValueOf(buf.readUtf());
		String url = buf.readUtf(2048);

		if (player == null) return;
		long now = System.currentTimeMillis();
		Long last = lastSent.get(player.getUUID());
		if (last != null && now - last < RATE_LIMIT_MS) return;
		lastSent.put(player.getUUID(), now);

		server.execute(() -> {
			if (!(player.level() instanceof ServerLevel level)) return;
			if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_INTERACTION_SQR) return;
			if (!(level.getBlockEntity(pos) instanceof ScreenBlockEntity sbe)) return;
			if (sbe.getScreen(side) == null) return;

			String sanitized = UrlUtil.sanitize(url);
			if (!UrlUtil.isValidLength(sanitized)) return;
			try {
				java.net.URI uri = new java.net.URI(sanitized);
				String scheme = uri.getScheme();
				if (scheme != null && !(scheme.equals("http") || scheme.equals("https") || scheme.equals("about"))) return;
			} catch (Exception ignored) {
				return;
			}
			sbe.setUrl(side, sanitized);
		});
	}
}
