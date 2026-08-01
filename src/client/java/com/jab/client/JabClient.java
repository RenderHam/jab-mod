package com.jab.client;

import com.cinemamod.mcef.MCEF;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.AudioModeHandler;
import com.jab.client.browser.BrowserManager;
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
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;

public class JabClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Browsing data should never be persisted between sessions.
		MCEF.getSettings().setUseCache(false);
		ClientNetworking.register();
		BrowserManager.init();
		ScreenBlockEntityRenderer.register();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BrowserManager.shutdown());

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
			ScreenBrowserManager.applyPending();
		});

		// Right-clicking a screen wall opens the browser view.
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide() && world.getBlockState(hitResult.getBlockPos()).getBlock() == ModBlocks.SCREEN_BLOCK) {
				BlockSide side = BlockSide.fromDirection(hitResult.getDirection());
				BlockPos.MutableBlockPos origin = hitResult.getBlockPos().mutable();
				Multiblock.findOrigin(world, origin, side);
				if (world.getBlockEntity(origin) instanceof ScreenBlockEntity sbe) {
					ScreenData scr = sbe.getScreen(side);
					if (scr != null) {
						ScreenBrowserManager.sync(origin.immutable(), sbe.getScreens());
						Minecraft.getInstance().setScreen(
								new BrowserScreen(origin.immutable(), side.ordinal(), scr.url, scr.resX, scr.resY));
						return InteractionResult.SUCCESS;
					}
				}
			}
			return InteractionResult.PASS;
		});
	}
}
