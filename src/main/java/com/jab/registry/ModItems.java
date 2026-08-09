package com.jab.registry;

import com.jab.JabMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
	private static Item.Properties props(String name) {
		return new Item.Properties().setId(ResourceKey.create(
				BuiltInRegistries.ITEM.key(),
				Identifier.fromNamespaceAndPath(JabMod.MOD_ID, name)));
	}

	public static final Item SCREEN_BLOCK_ITEM = new BlockItem(ModBlocks.SCREEN_BLOCK, props("screen_block"));

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, JabMod.id("screen_block"), SCREEN_BLOCK_ITEM);
	}
}
