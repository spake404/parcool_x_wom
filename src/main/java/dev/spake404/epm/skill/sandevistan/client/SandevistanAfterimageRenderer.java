package dev.spake404.epm.skill.sandevistan.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public final class SandevistanAfterimageRenderer {
	private static final List<SandevistanAfterimageParticle> VISIBLE_AFTERIMAGES = new ArrayList<>();

	private SandevistanAfterimageRenderer() {
	}

	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			return;
		}

		List<SandevistanAfterimageParticle> afterimages = SandevistanClientState.afterimagesForRendering();
		VISIBLE_AFTERIMAGES.clear();
		if (afterimages.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Frustum frustum = minecraft.levelRenderer.getFrustum();
		for (SandevistanAfterimageParticle afterimage : afterimages) {
			if (afterimage.isVisible(frustum)) {
				VISIBLE_AFTERIMAGES.add(afterimage);
			}
		}
		if (VISIBLE_AFTERIMAGES.isEmpty()) {
			SandevistanPerformanceDiagnostics.recordAfterimageBatch(0L, 0, afterimages.size());
			return;
		}

		long startedNanos = System.nanoTime();
		renderBatch(event, VISIBLE_AFTERIMAGES);
		SandevistanPerformanceDiagnostics.recordAfterimageBatch(
				System.nanoTime() - startedNanos,
				VISIBLE_AFTERIMAGES.size(),
				afterimages.size() - VISIBLE_AFTERIMAGES.size());
	}

	public static void renderMask(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && !VISIBLE_AFTERIMAGES.isEmpty()) {
			renderBatch(event, VISIBLE_AFTERIMAGES);
		}
	}

	private static void renderBatch(
			RenderLevelStageEvent event,
			List<SandevistanAfterimageParticle> afterimages) {
		PoseStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushPose();
		modelViewStack.mulPoseMatrix(event.getPoseStack().last().pose());
		RenderSystem.applyModelViewMatrix();
		try {
			for (SandevistanAfterimageParticle afterimage : afterimages) {
				afterimage.renderAfterimage(event.getCamera(), event.getPartialTick());
			}
		} finally {
			modelViewStack.popPose();
			RenderSystem.applyModelViewMatrix();
		}
	}
}
