package dev.spake404.epm.skill.sandevistan.client.debug;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.ModList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class SandevistanRenderDiagnostics {
	private static final String LOG_PREFIX = "[EPM/SandevistanRender]";
	private static final int ACTIVATION_TRACE_FRAMES = 8;
	private static final int DEACTIVATION_TRACE_FRAMES = 4;
	private static final FloatBuffer DEPTH_READ_BUFFER = BufferUtils.createFloatBuffer(1);
	private static boolean initialized;
	private static boolean active;
	private static int traceFramesRemaining;
	private static int frameIndex;

	private SandevistanRenderDiagnostics() {
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			initialized = false;
			active = false;
			traceFramesRemaining = 0;
			frameIndex = 0;
			return;
		}

		boolean currentActive = SandevistanStateView.isActive(minecraft.player);
		if (!initialized) {
			initialized = true;
			active = currentActive;
			if (currentActive) {
				startTrace("joined_active", ACTIVATION_TRACE_FRAMES);
			}
			return;
		}

		if (currentActive != active) {
			active = currentActive;
			startTrace(currentActive ? "activated" : "deactivated",
					currentActive ? ACTIVATION_TRACE_FRAMES : DEACTIVATION_TRACE_FRAMES);
		}
	}

	public static void beforeStencilEnable(RenderTarget target) {
		traceFramesRemaining = Math.max(traceFramesRemaining, ACTIVATION_TRACE_FRAMES);
		logSnapshot("stencil_enable_before", target, false);
	}

	public static void afterStencilEnable(RenderTarget target) {
		logSnapshot("stencil_enable_after", target, false);
	}

	public static void onRenderStageStart(RenderLevelStageEvent event) {
		if (!isTracing() || !isSelectedStage(event.getStage())) {
			return;
		}
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
			frameIndex++;
		}
		logSnapshot("stage_highest:" + event.getStage(), Minecraft.getInstance().getMainRenderTarget(), true);
	}

	public static void afterStencilClear(RenderLevelStageEvent event) {
		if (isTracing() && event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
			logSnapshot("stencil_clear_after", Minecraft.getInstance().getMainRenderTarget(), true);
		}
	}

	public static void beforePostChain(RenderTarget target) {
		if (isTracing()) {
			logSnapshot("post_chain_before", target, true);
		}
	}

	public static void afterPostChain(RenderTarget target) {
		if (isTracing()) {
			logSnapshot("post_chain_after", target, true);
		}
	}

	public static void onRenderStageEnd(RenderLevelStageEvent event) {
		if (!isTracing() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			return;
		}
		logSnapshot("stage_lowest:" + event.getStage(), Minecraft.getInstance().getMainRenderTarget(), true);
		traceFramesRemaining--;
	}

	private static void startTrace(String transition, int frames) {
		traceFramesRemaining = Math.max(traceFramesRemaining, frames);
		frameIndex = 0;
		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget target = minecraft.getMainRenderTarget();
		EPM.LOGGER.info(
				"{} transition={} traceFrames={} distantHorizons={} oculus={} mainFbo={} colorTex={} depthTex={} stencilEnabled={} size={}x{}",
				LOG_PREFIX,
				transition,
				traceFramesRemaining,
				ModList.get().isLoaded("distanthorizons"),
				ModList.get().isLoaded("oculus"),
				target.frameBufferId,
				target.getColorTextureId(),
				target.getDepthTextureId(),
				target.isStencilEnabled(),
				target.width,
				target.height);
	}

	private static void logSnapshot(String point, RenderTarget target, boolean includeDepthSamples) {
		if (!RenderSystem.isOnRenderThread()) {
			EPM.LOGGER.info("{} point={} frame={} active={} renderThread=false", LOG_PREFIX, point, frameIndex, active);
			return;
		}

		int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
		int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		boolean mainTargetBound = drawFramebuffer == target.frameBufferId;
		int framebufferStatus = mainTargetBound
				? GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER)
				: -1;
		String depthAttachment = mainTargetBound
				? attachment(GL30.GL_DEPTH_ATTACHMENT)
				: "unbound";
		String stencilAttachment = mainTargetBound
				? attachment(GL30.GL_STENCIL_ATTACHMENT)
				: "unbound";
		String depthSamples = includeDepthSamples && mainTargetBound
				? depthSamples(target)
				: "unavailable";

		EPM.LOGGER.info(
				"{} point={} frame={} active={} mainFbo={} colorTex={} depthTex={} stencilEnabled={} size={}x{} drawFbo={} readFbo={} fboStatus={} depthAttachment={} stencilAttachment={} depthTest={} depthMask={} depthFunc={} stencilTest={} depthSamples={}",
				LOG_PREFIX,
				point,
				frameIndex,
				active,
				target.frameBufferId,
				target.getColorTextureId(),
				target.getDepthTextureId(),
				target.isStencilEnabled(),
				target.width,
				target.height,
				drawFramebuffer,
				readFramebuffer,
				hex(framebufferStatus),
				depthAttachment,
				stencilAttachment,
				GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
				GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
				GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
				GL11.glIsEnabled(GL11.GL_STENCIL_TEST),
				depthSamples);
	}

	private static String attachment(int attachmentPoint) {
		int objectType = GL30.glGetFramebufferAttachmentParameteri(
				GL30.GL_DRAW_FRAMEBUFFER,
				attachmentPoint,
				GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
		int objectName = objectType == GL11.GL_NONE
				? 0
				: GL30.glGetFramebufferAttachmentParameteri(
						GL30.GL_DRAW_FRAMEBUFFER,
						attachmentPoint,
						GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
		return hex(objectType) + ':' + objectName;
	}

	private static String depthSamples(RenderTarget target) {
		if (!target.useDepth || target.width <= 0 || target.height <= 0) {
			return "none";
		}
		int centerX = target.width / 2;
		int centerY = target.height / 2;
		int leftX = target.width / 4;
		int rightX = target.width * 3 / 4;
		int lowerY = target.height / 4;
		return "center=" + sampleDepth(centerX, centerY)
				+ ",left=" + sampleDepth(leftX, centerY)
				+ ",right=" + sampleDepth(rightX, centerY)
				+ ",lower=" + sampleDepth(centerX, lowerY);
	}

	private static float sampleDepth(int x, int y) {
		DEPTH_READ_BUFFER.clear();
		GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, DEPTH_READ_BUFFER);
		return Math.round(DEPTH_READ_BUFFER.get(0) * 1_000_000.0F) / 1_000_000.0F;
	}

	private static boolean isTracing() {
		return traceFramesRemaining > 0;
	}

	private static boolean isSelectedStage(RenderLevelStageEvent.Stage stage) {
		return stage == RenderLevelStageEvent.Stage.AFTER_SKY
				|| stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS
				|| stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES
				|| stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
				|| stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES
				|| stage == RenderLevelStageEvent.Stage.AFTER_WEATHER
				|| stage == RenderLevelStageEvent.Stage.AFTER_LEVEL;
	}

	private static String hex(int value) {
		return value < 0 ? "n/a" : "0x" + Integer.toHexString(value);
	}
}
