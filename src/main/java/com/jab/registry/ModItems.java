package com.jab.registry;

import com.jab.JabMod;

import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class ModItems {
	public static final Item SCREEN_BLOCK_ITEM = new BlockItem(ModBlocks.SCREEN_BLOCK,
			new Item.Properties().tab(CreativeModeTab.TAB_MISC));

	public static void register() {
		Registry.register(Registry.ITEM, JabMod.id("screen_block"), SCREEN_BLOCK_ITEM);
	}
}
