package com.jab.network;

import com.jab.JabMod;
import com.jab.network.packet.ScreenActionC2SPacket;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class ModNetworking {
	public static final ResourceLocation SCREEN_ACTION = new ResourceLocation(JabMod.MOD_ID, "screen_action");
	public static final ResourceLocation SCREEN_STATE = new ResourceLocation(JabMod.MOD_ID, "screen_state");
	public static final ResourceLocation SCREEN_UPDATE = new ResourceLocation(JabMod.MOD_ID, "screen_update");

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(SCREEN_ACTION, ScreenActionC2SPacket::handle);
	}
}
