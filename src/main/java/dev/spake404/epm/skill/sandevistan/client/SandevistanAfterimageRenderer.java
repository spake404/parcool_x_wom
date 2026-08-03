package dev.spake404.epm.skill.sandevistan.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public final class SandevistanAfterimageRenderer {
	private SandevistanAfterimageRenderer() {
	}

	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			return;
		}

		List<SandevistanAfterimageParticle> afterimages = SandevistanClientState.afterimagesForRendering();
		if (afterimages.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Frustum frustum = minecraft.levelRenderer.getFrustum();
		List<SandevistanAfterimageParticle> visibleAfterimages = new ArrayList<>(afterimages.size());
		for (SandevistanAfterimageParticle afterimage : afterimages) {
			if (afterimage.isVisible(frustum)) {
				visibleAfterimages.add(afterimage);
			}
		}
		if (visibleAfterimages.isEmpty()) {
			SandevistanPerformanceDiagnostics.recordAfterimageBatch(0L, 0, afterimages.size());
			return;
		}

		long startedNanos = System.nanoTime();
		PoseStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushPose();
		modelViewStack.mulPoseMatrix(event.getPoseStack().last().pose());
		RenderSystem.applyModelViewMatrix();
		boolean masking = false;
		try {
			masking = SandevistanFilterRenderer.beginAfterimageBatchMask();
			for (SandevistanAfterimageParticle afterimage : visibleAfterimages) {
				afterimage.renderAfterimage(event.getCamera(), event.getPartialTick());
			}
		} finally {
			if (masking) {
				SandevistanFilterRenderer.endMask();
			}
			modelViewStack.popPose();
			RenderSystem.applyModelViewMatrix();
			SandevistanPerformanceDiagnostics.recordAfterimageBatch(
					System.nanoTime() - startedNanos,
					visibleAfterimages.size(),
					afterimages.size() - visibleAfterimages.size());
		}
	}
}
