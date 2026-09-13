package com.jab.client.network;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.PendingScreenCache;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.data.ScreenData;
import com.jab.network.packet.ScreenActionC2SPacket;
import com.jab.network.packet.ScreenStateS2CPacket;
import com.jab.network.packet.ScreenUpdateS2CPacket;
import com.jab.util.BlockSide;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ClientNetworking {
	public static void sendUrl(BlockPos pos, BlockSide side, String url) {
		FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
		ScreenActionC2SPacket.encode(buf, pos, side, url);
		ClientPlayNetworking.send(ScreenActionC2SPacket.ID, buf);
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ScreenStateS2CPacket.ID, ClientNetworking::handleScreenState);
		ClientPlayNetworking.registerGlobalReceiver(ScreenUpdateS2CPacket.ID, ClientNetworking::handleScreenUpdate);
	}

	private static void handleScreenState(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ScreenStateS2CPacket payload = ScreenStateS2CPacket.decode(buf);
		client.execute(() -> {
			Level world = client.level;
			if (world == null) return;

			ScreenBrowserManager.sync(payload.pos(), payload.screens());

			BlockEntity be = world.getBlockEntity(payload.pos());
			if (be instanceof ScreenBlockEntity sbe) {
				sbe.replaceAllScreens(payload.screens());
			} else {
				PendingScreenCache.store(payload.pos(), payload.screens());
			}
		});
	}

	private static void handleScreenUpdate(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ScreenUpdateS2CPacket payload = ScreenUpdateS2CPacket.decode(buf);
		client.execute(() -> {
			Level world = client.level;
			if (world == null) return;

			ScreenData update = payload.screen();
			ScreenBrowserManager.updateScreen(payload.pos(), update);

			BlockEntity be = world.getBlockEntity(payload.pos());
			if (be instanceof ScreenBlockEntity sbe) {
				ScreenData existing = sbe.getScreen(update.side());
				if (existing != null) {
					existing.copyFrom(update);
				} else {
					PendingScreenCache.applyUpdate(payload.pos(), update);
				}
			} else {
				PendingScreenCache.applyUpdate(payload.pos(), update);
			}
		});
	}
}
