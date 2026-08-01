package com.jab.network.packet;

import com.jab.JabMod;
import com.jab.data.ScreenData;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Sent to players whenever the full state of a wall changes (screen added/removed). */
public record ScreenStateS2CPacket(BlockPos pos, List<ScreenData> screens) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ScreenStateS2CPacket> ID =
			new CustomPacketPayload.Type<>(Identifier.parse(JabMod.id("screen_state")));

	public static final StreamCodec<ByteBuf, ScreenStateS2CPacket> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ScreenStateS2CPacket::pos,
			ByteBufCodecs.collection(ArrayList::new, ScreenDataStream.STREAM_CODEC), ScreenStateS2CPacket::screens,
			ScreenStateS2CPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
