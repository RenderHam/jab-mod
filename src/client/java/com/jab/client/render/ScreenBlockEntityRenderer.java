package com.jab.client.render;

import de.keksuccino.rinku.RinkuBrowser;

import com.jab.blockentity.ScreenBlockEntity;
import com.jab.client.browser.ScreenBrowserManager;
import com.jab.data.ScreenData;
import com.jab.registry.ModBlockEntities;
import com.jab.util.BlockSide;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws each screen as a textured quad floating a hair in front of its wall face.
 * The texture is the Rinku browser's frame, updated by Rinku itself.
 */
public class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity, ScreenBlockEntityRenderState> {
	private static final float Z_FIGHT_EPSILON = 0.001f;
	private static final int LIGHT_FULLBRIGHT = 0xF000F0;
	private static final int VIEW_DISTANCE = 64;

	public ScreenBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	public static void register() {
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.SCREEN_BLOCK_ENTITY, ScreenBlockEntityRenderer::new);
	}

	@Override
	public ScreenBlockEntityRenderState createRenderState() {
		return new ScreenBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(ScreenBlockEntity be, ScreenBlockEntityRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		state.screens = be.getScreens();
		state.pos = be.getBlockPos();
	}

	@Override
	public void submit(ScreenBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
		if (state.screens == null || state.screens.isEmpty()) return;

		for (ScreenData screen : state.screens) {
			RinkuBrowser browser = ScreenBrowserManager.getBrowser(state.pos, screen.side());
			if (browser == null) continue;
			if (!browser.isTextureReady()) continue;

			Identifier texId = browser.getTextureIdentifier();
			if (texId == null) continue;

			renderTexturedQuad(screen, state.pos, texId, poseStack, submitNodeCollector);
		}
	}

	private void renderTexturedQuad(ScreenData screen, BlockPos pos, Identifier texId, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
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

		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(texId, false), (pose, consumer) -> {
			var mat = pose.pose();
			consumer.addVertex(mat, sx, sy, sz).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(LIGHT_FULLBRIGHT);
			consumer.addVertex(mat, sx + rx, sy + ry, sz + rz).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(LIGHT_FULLBRIGHT);
			consumer.addVertex(mat, sx + rx + ux, sy + ry + uy, sz + rz + uz).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(LIGHT_FULLBRIGHT);
			consumer.addVertex(mat, sx + ux, sy + uy, sz + uz).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(LIGHT_FULLBRIGHT);
		});
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return false;
	}

	@Override
	public int getViewDistance() {
		return VIEW_DISTANCE;
	}
}
