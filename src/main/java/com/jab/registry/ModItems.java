package com.jab.registry;

import com.jab.JabMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
	public static final Item SCREEN_BLOCK_ITEM = new BlockItem(ModBlocks.SCREEN_BLOCK, new Item.Properties());

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(JabMod.MOD_ID, "screen_block"), SCREEN_BLOCK_ITEM);
	}
}
