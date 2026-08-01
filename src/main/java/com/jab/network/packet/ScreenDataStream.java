package com.jab.network.packet;

import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ScreenDataStream {
	public static final StreamCodec<ByteBuf, ScreenData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, d -> d.side.ordinal(),
			ByteBufCodecs.VAR_INT, d -> d.width,
			ByteBufCodecs.VAR_INT, d -> d.height,
			ByteBufCodecs.VAR_INT, d -> d.resX,
			ByteBufCodecs.VAR_INT, d -> d.resY,
			ByteBufCodecs.STRING_UTF8, d -> d.url,
			ByteBufCodecs.VAR_INT, d -> d.audioMode.ordinal(),
			(side, w, h, rx, ry, url, am) -> {
				ScreenData d = new ScreenData();
				d.side = BlockSide.values()[side];
				d.width = w;
				d.height = h;
				d.resX = rx;
				d.resY = ry;
				d.url = url;
				d.audioMode = ScreenData.AudioMode.values()[am];
				return d;
			}
	);
}
