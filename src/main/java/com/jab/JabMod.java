package com.jab;

import com.jab.command.JabCommand;
import com.jab.config.JabConfig;
import com.jab.network.ModNetworking;
import com.jab.registry.ModBlockEntities;
import com.jab.registry.ModBlocks;
import com.jab.registry.ModItems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

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

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("tab"), FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.jab"))
				.icon(() -> new ItemStack(ModBlocks.SCREEN_BLOCK))
				.displayItems((ctx, entries) -> entries.accept(ModBlocks.SCREEN_BLOCK))
				.build());
	}

	public static String id(String path) {
		return MOD_ID + ":" + path;
	}
}
