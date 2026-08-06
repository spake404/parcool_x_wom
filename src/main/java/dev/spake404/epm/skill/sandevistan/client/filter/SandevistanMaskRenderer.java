package dev.spake404.epm.skill.sandevistan.client.filter;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import dev.spake404.epm.skill.sandevistan.client.SandevistanAfterimageRenderer;
import dev.spake404.epm.skill.sandevistan.client.SandevistanClientTickClock;
import dev.spake404.epm.skill.sandevistan.mixin.SandevistanEntityRenderDispatcherAccessor;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

public final class SandevistanMaskRenderer {
	private static final int RELEASE_DELAY_TICKS = 100;
	private static final AtomicLong MAIN_TARGET_GENERATION = new AtomicLong();
	private static final ResourceManagerReloadListener RELOAD_LISTENER = resourceManager -> invalidateMainTarget();
	private static TextureTarget maskTarget;
	private static long targetGeneration = -1L;
	private static int sourceFramebuffer = -1;
	private static int sourceColorTexture = -1;
	private static int sourceDepthTexture = -1;
	private static int sourceWidth = -1;
	private static int sourceHeight = -1;
	private static volatile boolean releaseRequested;
	private static boolean maskReady;
	private static boolean initializationFailed;
	private static int inactiveTicks;

	private SandevistanMaskRenderer() {
	}

	public static ResourceManagerReloadListener reloadListener() {
		return RELOAD_LISTENER;
	}

	public static void invalidateMainTarget() {
		MAIN_TARGET_GENERATION.incrementAndGet();
		releaseRequested = true;
		maskReady = false;
	}

	public static long mainTargetGeneration() {
		return MAIN_TARGET_GENERATION.get();
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || !SandevistanFilterRenderer.isColorGradeVisible()) {
			maskReady = false;
			if (++inactiveTicks >= RELEASE_DELAY_TICKS) {
				releaseRequested = true;
			}
		} else {
			inactiveTicks = 0;
		}

		if (releaseRequested && RenderSystem.isOnRenderThread()) {
			releaseTarget();
			releaseRequested = false;
			initializationFailed = false;
		}
	}

	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			return;
		}

		maskReady = false;
		if (SandevistanFilterRenderer.colorGradeStrength(event.getPartialTick()) <= 1.0E-3F) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}

		RenderTarget mainTarget = minecraft.getMainRenderTarget();
		TextureTarget target = ensureTarget(mainTarget);
		if (target == null) {
			return;
		}

		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean depthMaskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		try {
			bufferSource.endBatch();
			target.clear(Minecraft.ON_OSX);
			target.copyDepthFrom(mainTarget);
			target.bindWrite(true);
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(false);
			renderMaskedPlayers(minecraft, event, bufferSource);
			bufferSource.endBatch();
			SandevistanAfterimageRenderer.renderMask(event);
			maskReady = true;
		} catch (RuntimeException | LinkageError exception) {
			if (!initializationFailed) {
				EPM.LOGGER.error("Unable to render the independent Sandevistan mask", exception);
			}
			initializationFailed = true;
			releaseRequested = true;
			maskReady = false;
		} finally {
			bufferSource.endBatch();
			mainTarget.bindWrite(true);
			RenderSystem.depthMask(depthMaskEnabled);
			if (depthTestEnabled) {
				RenderSystem.enableDepthTest();
			} else {
				RenderSystem.disableDepthTest();
			}
		}
	}

	public static boolean isReady() {
		return maskReady && maskTarget != null;
	}

	public static int colorTextureId() {
		return isReady() ? maskTarget.getColorTextureId() : -1;
	}

	private static TextureTarget ensureTarget(RenderTarget mainTarget) {
		long generation = mainTargetGeneration();
		boolean sourceChanged = targetGeneration != generation
				|| sourceFramebuffer != mainTarget.frameBufferId
				|| sourceColorTexture != mainTarget.getColorTextureId()
				|| sourceDepthTexture != mainTarget.getDepthTextureId()
				|| sourceWidth != mainTarget.width
				|| sourceHeight != mainTarget.height;
		if (sourceChanged) {
			releaseTarget();
			initializationFailed = false;
		}

		if (maskTarget != null) {
			return maskTarget;
		}
		if (initializationFailed || mainTarget.width <= 0 || mainTarget.height <= 0) {
			return null;
		}

		try {
			maskTarget = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
			maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
			maskTarget.setFilterMode(GL11.GL_LINEAR);
			targetGeneration = generation;
			sourceFramebuffer = mainTarget.frameBufferId;
			sourceColorTexture = mainTarget.getColorTextureId();
			sourceDepthTexture = mainTarget.getDepthTextureId();
			sourceWidth = mainTarget.width;
			sourceHeight = mainTarget.height;
			return maskTarget;
		} catch (RuntimeException | LinkageError exception) {
			initializationFailed = true;
			EPM.LOGGER.error("Unable to create the independent Sandevistan mask target", exception);
			return null;
		}
	}

	private static void renderMaskedPlayers(
			Minecraft minecraft,
			RenderLevelStageEvent event,
			MultiBufferSource.BufferSource bufferSource) {
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		Camera camera = event.getCamera();
		Vec3 cameraPosition = camera.getPosition();
		Frustum frustum = minecraft.levelRenderer.getFrustum();
		boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
		boolean renderShadow = ((SandevistanEntityRenderDispatcherAccessor) dispatcher).epm$getRenderShadow();
		dispatcher.setRenderShadow(false);
		try {
			for (Player player : minecraft.level.players()) {
				if (!shouldMask(minecraft, player) || firstPerson && player == minecraft.player) {
					continue;
				}
				if (!dispatcher.shouldRender(
						player,
						frustum,
						cameraPosition.x,
						cameraPosition.y,
						cameraPosition.z)) {
					continue;
				}

				float partialTick = SandevistanClientTickClock.localPartialTick(player, event.getPartialTick());
				double renderX = Mth.lerp(partialTick, player.xOld, player.getX()) - cameraPosition.x;
				double renderY = Mth.lerp(partialTick, player.yOld, player.getY()) - cameraPosition.y;
				double renderZ = Mth.lerp(partialTick, player.zOld, player.getZ()) - cameraPosition.z;
				float renderedYaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
				int packedLight = dispatcher.getPackedLightCoords(player, partialTick);
				PoseStack poseStack = event.getPoseStack();
				dispatcher.render(
						player,
						renderX,
						renderY,
						renderZ,
						renderedYaw,
						partialTick,
						poseStack,
						bufferSource,
						packedLight);
			}
		} finally {
			dispatcher.setRenderShadow(renderShadow);
		}
	}

	private static boolean shouldMask(Minecraft minecraft, Player player) {
		return player == minecraft.player || SandevistanStateView.isActive(player);
	}

	private static void releaseTarget() {
		if (maskTarget != null) {
			maskTarget.destroyBuffers();
			maskTarget = null;
		}
		targetGeneration = -1L;
		sourceFramebuffer = -1;
		sourceColorTexture = -1;
		sourceDepthTexture = -1;
		sourceWidth = -1;
		sourceHeight = -1;
		maskReady = false;
	}
}
