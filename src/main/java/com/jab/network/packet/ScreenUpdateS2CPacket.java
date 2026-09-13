package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.data.ScreenData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Targeted update for a single screen (URL, resolution or audio mode changed). */
public record ScreenUpdateS2CPacket(BlockPos pos, ScreenData screen) {
	public static final ResourceLocation ID = JabMod.id("screen_update");

	public static void encode(FriendlyByteBuf buf, BlockPos pos, ScreenData screen) {
		buf.writeBlockPos(pos);
		ScreenDataStream.encode(buf, screen);
	}

	public static ScreenUpdateS2CPacket decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		ScreenData screen = ScreenDataStream.decode(buf);
		return new ScreenUpdateS2CPacket(pos, screen);
	}
}
