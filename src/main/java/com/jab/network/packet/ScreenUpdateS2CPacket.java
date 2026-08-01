package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.data.ScreenData;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Targeted update for a single screen (URL, resolution or audio mode changed). */
public record ScreenUpdateS2CPacket(BlockPos pos, ScreenData screen) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ScreenUpdateS2CPacket> ID =
			new CustomPacketPayload.Type<>(Identifier.parse(JabMod.id("screen_update")));

	public static final StreamCodec<ByteBuf, ScreenUpdateS2CPacket> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ScreenUpdateS2CPacket::pos,
			ScreenDataStream.STREAM_CODEC, ScreenUpdateS2CPacket::screen,
			ScreenUpdateS2CPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
