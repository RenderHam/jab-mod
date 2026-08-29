package com.jab.network.packet;

import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ScreenDataStream {
	public static final StreamCodec<ByteBuf, ScreenData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, d -> d.side().ordinal(),
			ByteBufCodecs.VAR_INT, ScreenData::width,
			ByteBufCodecs.VAR_INT, ScreenData::height,
			ByteBufCodecs.VAR_INT, ScreenData::resolutionX,
			ByteBufCodecs.VAR_INT, ScreenData::resolutionY,
			ByteBufCodecs.STRING_UTF8, ScreenData::url,
			ByteBufCodecs.VAR_INT, d -> d.audioMode().ordinal(),
		(side, w, h, rx, ry, url, am) -> ScreenData.decode(
				BlockSide.values()[Math.clamp(side, 0, BlockSide.values().length - 1)],
				w, h, rx, ry, url,
				ScreenData.AudioMode.values()[Math.clamp(am, 0, ScreenData.AudioMode.values().length - 1)])
	);
}
