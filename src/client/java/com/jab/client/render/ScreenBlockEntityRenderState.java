package com.jab.client.render;

import com.jab.data.ScreenData;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * In 1.19.2 there is no BlockEntityRenderState. This is a simple data holder
 * used internally by the renderer — it does NOT extend any MC class.
 */
public class ScreenBlockEntityRenderState {
	public List<ScreenData> screens;
	public BlockPos pos;
}
