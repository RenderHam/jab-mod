package com.jab.network;

import com.jab.network.packet.ScreenActionC2SPacket;
import com.jab.network.packet.ScreenStateS2CPacket;
import com.jab.network.packet.ScreenUpdateS2CPacket;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {
	public static void register() {
		PayloadTypeRegistry.playS2C().register(ScreenStateS2CPacket.ID, ScreenStateS2CPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(ScreenUpdateS2CPacket.ID, ScreenUpdateS2CPacket.CODEC);
		PayloadTypeRegistry.playC2S().register(ScreenActionC2SPacket.ID, ScreenActionC2SPacket.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ScreenActionC2SPacket.ID, ScreenActionC2SPacket::handle);
	}
}
