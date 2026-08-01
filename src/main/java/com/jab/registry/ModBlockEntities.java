package com.jab.registry;

import com.jab.JabMod;
import com.jab.blockentity.ScreenBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
	public static final BlockEntityType<ScreenBlockEntity> SCREEN_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(ScreenBlockEntity::new, ModBlocks.SCREEN_BLOCK).build();

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JabMod.id("screen_block_entity"), SCREEN_BLOCK_ENTITY);
	}
}
