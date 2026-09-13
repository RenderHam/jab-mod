package com.jab.registry;

import com.jab.JabMod;
import com.jab.block.ScreenBlock;

import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class ModBlocks {
	public static final Block SCREEN_BLOCK = new ScreenBlock(BlockBehaviour.Properties.of(Material.STONE)
			.strength(1.5f, 6.0f)
			.noOcclusion());

	public static void register() {
		Registry.register(Registry.BLOCK, JabMod.id("screen_block"), SCREEN_BLOCK);
	}
}
