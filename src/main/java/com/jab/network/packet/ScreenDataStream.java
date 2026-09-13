package com.jab.network.packet;

import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import net.minecraft.network.FriendlyByteBuf;

public class ScreenDataStream {
	public static void encode(FriendlyByteBuf buf, ScreenData data) {
		buf.writeUtf(data.side().name());
		buf.writeVarInt(data.width());
		buf.writeVarInt(data.height());
		buf.writeVarInt(data.resolutionX());
		buf.writeVarInt(data.resolutionY());
		buf.writeUtf(data.url(), 2048);
		buf.writeUtf(data.audioMode().name());
	}

	public static ScreenData decode(FriendlyByteBuf buf) {
		String side = buf.readUtf();
		int w = buf.readVarInt();
		int h = buf.readVarInt();
		int rx = buf.readVarInt();
		int ry = buf.readVarInt();
		String url = buf.readUtf(2048);
		String am = buf.readUtf();
		return ScreenData.decode(
			BlockSide.lenientValueOf(side), w, h, rx, ry, url, ScreenData.AudioMode.lenientValueOf(am));
	}
}
