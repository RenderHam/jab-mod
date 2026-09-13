package com.jab.network.packet;

import com.jab.data.ScreenData;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ScreenStateS2CPacket {
	public static FriendlyByteBuf encode(BlockPos pos, List<ScreenData> screens) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(pos);
		buf.writeVarInt(screens.size());
		for (ScreenData s : screens) {
			ScreenDataStream.encode(buf, s);
		}
		return buf;
	}

	public static record Data(BlockPos pos, List<ScreenData> screens) {}

	public static Data decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		int count = buf.readVarInt();
		List<ScreenData> screens = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			screens.add(ScreenDataStream.decode(buf));
		}
		return new Data(pos, screens);
	}
}
