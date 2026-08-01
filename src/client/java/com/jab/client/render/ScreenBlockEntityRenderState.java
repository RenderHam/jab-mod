package com.jab.client.render;

import com.jab.data.ScreenData;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

import java.util.List;

public class ScreenBlockEntityRenderState extends BlockEntityRenderState {
	public List<ScreenData> screens;
	public BlockPos pos;
}
