package com.jab.client.render;

import com.cinemamod.mcef.MCEFBrowser;

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

import org.joml.Vector3f;

/**
 * Draws each screen as a textured quad floating a hair in front of its wall face.
 * The texture is the MCEF browser's frame, updated by MCEF itself.
 */
public class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity, ScreenBlockEntityRenderState> {
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
			MCEFBrowser browser = ScreenBrowserManager.getBrowser(state.pos, screen.side);
			if (browser == null) continue;
			if (!browser.isTextureReady()) continue;

			Identifier texId = browser.getTextureIdentifier();
			if (texId == null) continue;

			renderTexturedQuad(screen, state.pos, texId, poseStack, submitNodeCollector);
		}
	}

	private void renderTexturedQuad(ScreenData screen, BlockPos pos, Identifier texId, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		BlockSide side = screen.side;
		float w = screen.width;
		float h = screen.height;

		// Shift the quad slightly off the block face to avoid z-fighting.
		float eps = 0.001f;
		float faceX = (side.fx > 0 ? 1 : 0) + (side.fx * eps);
		float faceY = (side.fy > 0 ? 1 : 0) + (side.fy * eps);
		float faceZ = (side.fz > 0 ? 1 : 0) + (side.fz * eps);

		float sx = faceX + (side.rx < 0 ? 1 : 0) + (side.ux < 0 ? 1 : 0);
		float sy = faceY + (side.ry < 0 ? 1 : 0) + (side.uy < 0 ? 1 : 0);
		float sz = faceZ + (side.rz < 0 ? 1 : 0) + (side.uz < 0 ? 1 : 0);

		Vector3f p0 = new Vector3f(sx, sy, sz);
		Vector3f p1 = new Vector3f(sx + side.rx * w, sy + side.ry * w, sz + side.rz * w);
		Vector3f p2 = new Vector3f(sx + side.rx * w + side.ux * h, sy + side.ry * w + side.uy * h, sz + side.rz * w + side.uz * h);
		Vector3f p3 = new Vector3f(sx + side.ux * h, sy + side.uy * h, sz + side.uz * h);

		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(texId, false), (pose, consumer) -> {
			var mat = pose.pose();
			float nx = (float) side.fx;
			float ny = (float) side.fy;
			float nz = (float) side.fz;
			consumer.addVertex(mat, p0.x, p0.y, p0.z).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(0xF000F0);
			consumer.addVertex(mat, p1.x, p1.y, p1.z).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(0xF000F0);
			consumer.addVertex(mat, p2.x, p2.y, p2.z).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(0xF000F0);
			consumer.addVertex(mat, p3.x, p3.y, p3.z).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, nz).setLight(0xF000F0);
		});
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 64;
	}
}
