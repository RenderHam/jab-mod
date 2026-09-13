package com.jab.network.packet;

import com.jab.data.ScreenData;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class ScreenUpdateS2CPacket {
	public static FriendlyByteBuf encode(BlockPos pos, ScreenData screen) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(pos);
		ScreenDataStream.encode(buf, screen);
		return buf;
	}

	public static record Data(BlockPos pos, ScreenData screen) {}

	public static Data decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		ScreenData screen = ScreenDataStream.decode(buf);
		return new Data(pos, screen);
	}
}
