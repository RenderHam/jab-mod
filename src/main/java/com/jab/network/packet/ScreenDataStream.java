package com.jab.network.packet;

import com.jab.data.ScreenData;
import com.jab.util.BlockSide;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ScreenDataStream {
	public static final StreamCodec<ByteBuf, ScreenData> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public ScreenData decode(ByteBuf buf) {
			String side = ByteBufCodecs.STRING_UTF8.decode(buf);
			int w = ByteBufCodecs.VAR_INT.decode(buf);
			int h = ByteBufCodecs.VAR_INT.decode(buf);
			int rx = ByteBufCodecs.VAR_INT.decode(buf);
			int ry = ByteBufCodecs.VAR_INT.decode(buf);
			String url = ByteBufCodecs.stringUtf8(2048).decode(buf);
			String am = ByteBufCodecs.STRING_UTF8.decode(buf);
			return ScreenData.decode(BlockSide.lenientValueOf(side), w, h, rx, ry, url, ScreenData.AudioMode.lenientValueOf(am));
		}

		@Override
		public void encode(ByteBuf buf, ScreenData data) {
			ByteBufCodecs.STRING_UTF8.encode(buf, data.side().name());
			ByteBufCodecs.VAR_INT.encode(buf, data.width());
			ByteBufCodecs.VAR_INT.encode(buf, data.height());
			ByteBufCodecs.VAR_INT.encode(buf, data.resolutionX());
			ByteBufCodecs.VAR_INT.encode(buf, data.resolutionY());
			ByteBufCodecs.stringUtf8(2048).encode(buf, data.url());
			ByteBufCodecs.STRING_UTF8.encode(buf, data.audioMode().name());
		}
	};
}
