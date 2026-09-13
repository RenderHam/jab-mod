package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.data.ScreenData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Sent to players whenever the full state of a wall changes (screen added/removed). */
public record ScreenStateS2CPacket(BlockPos pos, List<ScreenData> screens) {
	public static final ResourceLocation ID = JabMod.id("screen_state");

	public static void encode(FriendlyByteBuf buf, BlockPos pos, List<ScreenData> screens) {
		buf.writeBlockPos(pos);
		buf.writeVarInt(screens.size());
		for (ScreenData screen : screens) {
			ScreenDataStream.encode(buf, screen);
		}
	}

	public static ScreenStateS2CPacket decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		int count = buf.readVarInt();
		List<ScreenData> screens = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			screens.add(ScreenDataStream.decode(buf));
		}
		return new ScreenStateS2CPacket(pos, screens);
	}
}
