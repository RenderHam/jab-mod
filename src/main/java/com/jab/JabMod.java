package com.jab;

import com.jab.command.JabCommand;
import com.jab.config.JabConfig;
import com.jab.network.ModNetworking;
import com.jab.registry.ModBlockEntities;
import com.jab.registry.ModBlocks;
import com.jab.registry.ModItems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.world.item.CreativeModeTabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabMod implements ModInitializer {
	public static final String MOD_ID = "jab";
	public static final Logger LOGGER = LoggerFactory.getLogger("jab");

	@Override
	public void onInitialize() {
		JabConfig.load();
		ModBlocks.register();
		ModItems.register();
		ModBlockEntities.register();
		ModNetworking.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				JabCommand.register(dispatcher));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(entries -> entries.accept(ModBlocks.SCREEN_BLOCK));
	}

	public static String id(String path) {
		return MOD_ID + ":" + path;
	}
}
