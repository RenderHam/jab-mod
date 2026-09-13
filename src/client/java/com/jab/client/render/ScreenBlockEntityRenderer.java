package com.jab.client.render;

import de.keksuccino.rinku.RinkuBrowser;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.data.ScreenData;
import com.jab.registry.ModBlockEntities;
import com.jab.util.BlockSide;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * Draws each screen as a textured quad floating a hair in front of its wall face.
 * The texture is the Rinku browser's frame, updated by Rinku itself.
 */
public class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
	private static final float Z_FIGHT_EPSILON = 0.001f;
	private static final int LIGHT_FULLBRIGHT = 0xF000F0;
	private static final int VIEW_DISTANCE = 64;

	public ScreenBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	public static void register() {
		BlockEntityRenderers.register(ModBlockEntities.SCREEN_BLOCK_ENTITY, ScreenBlockEntityRenderer::new);
	}

	@Override
	public void render(ScreenBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		List<ScreenData> screens = be.getScreens();
		if (screens == null || screens.isEmpty()) return;

		BlockPos pos = be.getBlockPos();

		for (ScreenData screen : screens) {
			RinkuBrowser browser = ScreenBrowserManager.getBrowser(pos, screen.side());
			if (browser == null) continue;
			if (!browser.isTextureReady()) continue;

			ResourceLocation texId = browser.getTextureIdentifier();
			if (texId == null) continue;

			renderTexturedQuad(screen, pos, texId, poseStack, bufferSource);
		}
	}

	private void renderTexturedQuad(ScreenData screen, BlockPos pos, ResourceLocation texId, PoseStack poseStack, MultiBufferSource bufferSource) {
		BlockSide side = screen.side();
		float w = screen.width();
		float h = screen.height();
		float nx = (float) side.faceX;
		float ny = (float) side.faceY;
		float nz = (float) side.faceZ;

		// Shift the quad slightly off the block face to avoid z-fighting.
		float faceX = (side.faceX > 0 ? 1 : 0) + (side.faceX * Z_FIGHT_EPSILON);
		float faceY = (side.faceY > 0 ? 1 : 0) + (side.faceY * Z_FIGHT_EPSILON);
		float faceZ = (side.faceZ > 0 ? 1 : 0) + (side.faceZ * Z_FIGHT_EPSILON);

		float sx = faceX + (side.rightX < 0 ? 1 : 0) + (side.upX < 0 ? 1 : 0);
		float sy = faceY + (side.rightY < 0 ? 1 : 0) + (side.upY < 0 ? 1 : 0);
		float sz = faceZ + (side.rightZ < 0 ? 1 : 0) + (side.upZ < 0 ? 1 : 0);

		float rx = side.rightX * w;
		float ry = side.rightY * w;
		float rz = side.rightZ * w;
		float ux = side.upX * h;
		float uy = side.upY * h;
		float uz = side.upZ * h;

		RenderType renderType = RenderType.entityCutoutNoCull(texId, false);
		VertexConsumer consumer = bufferSource.getBuffer(renderType);

		com.mojang.math.Matrix4f mat = poseStack.last().pose();

		consumer.vertex(mat, sx, sy, sz).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LIGHT_FULLBRIGHT).normal(nx, ny, nz).endVertex();
		consumer.vertex(mat, sx + rx, sy + ry, sz + rz).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LIGHT_FULLBRIGHT).normal(nx, ny, nz).endVertex();
		consumer.vertex(mat, sx + rx + ux, sy + ry + uy, sz + rz + uz).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LIGHT_FULLBRIGHT).normal(nx, ny, nz).endVertex();
		consumer.vertex(mat, sx + ux, sy + uy, sz + uz).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LIGHT_FULLBRIGHT).normal(nx, ny, nz).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(ScreenBlockEntity be) {
		return false;
	}

	@Override
	public int getViewDistance() {
		return VIEW_DISTANCE;
	}
}
