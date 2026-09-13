package com.jab.network;

import com.jab.network.packet.ScreenActionC2SPacket;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {
	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(ScreenActionC2SPacket.ID, ScreenActionC2SPacket::handle);
	}
}
