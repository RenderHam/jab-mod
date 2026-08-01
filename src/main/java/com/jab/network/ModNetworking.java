package com.jab.network;

import com.jab.network.packet.ScreenStateS2CPacket;
import com.jab.network.packet.ScreenUpdateS2CPacket;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModNetworking {
	public static void register() {
		PayloadTypeRegistry.playS2C().register(ScreenStateS2CPacket.ID, ScreenStateS2CPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(ScreenUpdateS2CPacket.ID, ScreenUpdateS2CPacket.CODEC);
	}
}
