package com.jab.client.network;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.PendingScreenCache;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.data.ScreenData;
import com.jab.network.ModNetworking;
import com.jab.network.packet.ScreenDataStream;
import com.jab.util.BlockSide;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class ClientNetworking {
	public static void sendUrl(BlockPos pos, BlockSide side, String url) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(pos);
		buf.writeUtf(side.name());
		buf.writeUtf(url, 2048);
		ClientPlayNetworking.send(ModNetworking.SCREEN_ACTION, buf);
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SCREEN_STATE, (client, handler, buf, sender) -> {
			BlockPos pos = buf.readBlockPos();
			int count = buf.readVarInt();
			List<ScreenData> screens = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				screens.add(ScreenDataStream.decode(buf));
			}
			client.execute(() -> {
				Level world = client.level;
				if (world == null) return;

				ScreenBrowserManager.sync(pos, screens);

				BlockEntity be = world.getBlockEntity(pos);
				if (be instanceof ScreenBlockEntity sbe) {
					sbe.replaceAllScreens(screens);
				} else {
					PendingScreenCache.store(pos, screens);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SCREEN_UPDATE, (client, handler, buf, sender) -> {
			BlockPos pos = buf.readBlockPos();
			ScreenData update = ScreenDataStream.decode(buf);
			client.execute(() -> {
				Level world = client.level;
				if (world == null) return;

				ScreenBrowserManager.updateScreen(pos, update);

				BlockEntity be = world.getBlockEntity(pos);
				if (be instanceof ScreenBlockEntity sbe) {
					ScreenData existing = sbe.getScreen(update.side());
					if (existing != null) {
						existing.copyFrom(update);
					} else {
						PendingScreenCache.applyUpdate(pos, update);
					}
				} else {
					PendingScreenCache.applyUpdate(pos, update);
				}
			});
		});
	}
}
