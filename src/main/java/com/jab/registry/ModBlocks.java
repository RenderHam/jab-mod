package com.jab.registry;

import com.jab.JabMod;
import com.jab.block.ScreenBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {
	public static final Block SCREEN_BLOCK = new ScreenBlock(BlockBehaviour.Properties.of()
			.strength(1.5f, 6.0f)
			.pushReaction(PushReaction.DESTROY)
			.noOcclusion());

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, JabMod.id("screen_block"), SCREEN_BLOCK);
	}
}
