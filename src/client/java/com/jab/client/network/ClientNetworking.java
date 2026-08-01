package com.jab.client.network;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.data.ScreenData;
import com.jab.network.packet.ScreenStateS2CPacket;
import com.jab.network.packet.ScreenUpdateS2CPacket;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ClientNetworking {
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ScreenStateS2CPacket.ID, (payload, ctx) -> {
			ctx.client().execute(() -> {
				Level world = ctx.client().level;
				if (world == null) return;

				// Browsers can be created even if the block entity isn't in the world yet.
				ScreenBrowserManager.sync(payload.pos(), payload.screens());

				BlockEntity be = world.getBlockEntity(payload.pos());
				if (be instanceof ScreenBlockEntity sbe) {
					sbe.replaceAllScreens(payload.screens());
				} else {
					// The wall's block update may still be in flight; stash the data and
					// apply it once the block entity shows up.
					ScreenBrowserManager.storePending(payload.pos(), payload.screens());
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(ScreenUpdateS2CPacket.ID, (payload, ctx) -> {
			ctx.client().execute(() -> {
				Level world = ctx.client().level;
				if (world == null) return;

				ScreenData update = payload.screen();
				ScreenBrowserManager.updateScreen(payload.pos(), update);

				BlockEntity be = world.getBlockEntity(payload.pos());
				if (be instanceof ScreenBlockEntity sbe) {
					ScreenData existing = sbe.getScreen(update.side);
					if (existing != null) {
						existing.url = update.url;
						existing.resX = update.resX;
						existing.resY = update.resY;
						existing.audioMode = update.audioMode;
						existing.active = update.active;
					} else {
						ScreenBrowserManager.applyUpdate(payload.pos(), update);
					}
				} else {
					ScreenBrowserManager.applyUpdate(payload.pos(), update);
				}
			});
		});
	}
}
