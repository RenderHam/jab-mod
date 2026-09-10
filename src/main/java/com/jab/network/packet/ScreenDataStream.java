package com.jab.network.packet;

import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ScreenDataStream {
	public static final StreamCodec<ByteBuf, ScreenData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, d -> d.side().name(),
			ByteBufCodecs.VAR_INT, ScreenData::width,
			ByteBufCodecs.VAR_INT, ScreenData::height,
			ByteBufCodecs.VAR_INT, ScreenData::resolutionX,
			ByteBufCodecs.VAR_INT, ScreenData::resolutionY,
			ByteBufCodecs.STRING_UTF8, ScreenData::url,
			ByteBufCodecs.STRING_UTF8, d -> d.audioMode().name(),
		(side, w, h, rx, ry, url, am) -> {
			BlockSide sideEnum;
			try {
				sideEnum = BlockSide.valueOf(side);
			} catch (IllegalArgumentException e) {
				sideEnum = BlockSide.BOTTOM;
			}
			ScreenData.AudioMode audioMode;
			try {
				audioMode = ScreenData.AudioMode.valueOf(am);
			} catch (IllegalArgumentException e) {
				audioMode = ScreenData.AudioMode.GLOBAL;
			}
			return ScreenData.decode(sideEnum, w, h, rx, ry, url, audioMode);
		}
	);
}
