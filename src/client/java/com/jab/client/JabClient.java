package com.jab.client;

import de.keksuccino.rinku.Rinku;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.AudioModeHandler;
import com.jab.client.browser.BrowserManager;
import com.jab.client.browser.PendingScreenCache;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.client.gui.BrowserScreen;
import com.jab.client.network.ClientNetworking;
import com.jab.client.render.ScreenBlockEntityRenderer;
import com.jab.data.ScreenData;
import com.jab.registry.ModBlocks;
import com.jab.util.BlockSide;
import com.jab.util.Multiblock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class JabClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Browsing data should never be persisted between sessions.
		Rinku.getSettings().setUseCache(false);
		ClientNetworking.register();
		BrowserManager.init();
		ScreenBlockEntityRenderer.register();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BrowserManager.shutdown());

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				client.execute(ScreenBrowserManager::onPlayerDisconnect));

		// Recreate browsers when chunks (re)load and drop them when chunks unload.
		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			for (var be : chunk.getBlockEntities().values()) {
				if (be instanceof ScreenBlockEntity sbe) {
					ScreenBrowserManager.sync(sbe.getBlockPos(), sbe.getScreens());
				}
			}
		});

		ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
				ScreenBrowserManager.removeAllInChunk(chunk.getPos()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			AudioModeHandler.tick();
			ScreenBrowserManager.tick();
			PendingScreenCache.tickFlush();
		});

		// Right-clicking a screen wall opens the browser view.
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()
					&& hand == InteractionHand.MAIN_HAND
					&& player.getMainHandItem().isEmpty()
					&& world.getBlockState(hitResult.getBlockPos()).getBlock() == ModBlocks.SCREEN_BLOCK) {
				BlockSide side = BlockSide.fromDirection(hitResult.getDirection());
				BlockPos origin = Multiblock.resolveOrigin(world, hitResult.getBlockPos(), side);
				if (world.getBlockEntity(origin) instanceof ScreenBlockEntity sbe) {
					ScreenData scr = sbe.getScreen(side);
					if (scr != null) {
						ScreenBrowserManager.sync(origin, sbe.getScreens());
						Minecraft.getInstance().setScreen(
								new BrowserScreen(origin, side.ordinal(), scr.url()));
						return InteractionResult.SUCCESS;
					}
				}
			}
			return InteractionResult.PASS;
		});
	}
}
