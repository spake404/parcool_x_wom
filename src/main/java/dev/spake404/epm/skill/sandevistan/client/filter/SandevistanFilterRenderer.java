package dev.spake404.epm.skill.sandevistan.client.filter;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import dev.spake404.epm.skill.sandevistan.client.blur.SandevistanEdgeBlurRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class SandevistanFilterRenderer {
	private static final int LEGACY_OVERLAY_COLOR = 0x065F35;
	private static final int DEFAULT_GRADE_COLOR = 0x3CFF48;
	private static final float MAX_FILTER_STRENGTH = 0.8F;
	private static final float MAX_DARK_ALPHA = 20.0F;
	private static float previousIntensity;
	private static float intensity;
	private static boolean stencilWriteActive;

	private SandevistanFilterRenderer() {
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		previousIntensity = intensity;
		boolean active = EPMConfig.sandevistanFilterEnabled()
				&& minecraft.level != null
				&& minecraft.player != null
				&& SandevistanStateView.isActive(minecraft.player);
		int transitionTicks = active
				? EPMConfig.sandevistanFilterFadeInTicks()
				: EPMConfig.sandevistanFilterFadeOutTicks();
		float step = 1.0F / Math.max(1, transitionTicks);
		intensity = Mth.clamp(intensity + (active ? step : -step), 0.0F, 1.0F);

	}

	public static void clearStencil(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !isVisible()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget target = minecraft.getMainRenderTarget();
		if (!target.isStencilEnabled()) {
			return;
		}

		target.bindWrite(false);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.clearStencil(0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
		resetStencilState();
	}

	public static void renderFilter(RenderGuiEvent.Pre event) {
		float activation = Mth.lerp(event.getPartialTick(), previousIntensity, intensity);
		float strength = activation * EPMConfig.sandevistanFilterIntensity() * MAX_FILTER_STRENGTH;
		float flashStrength = SandevistanEdgeBlurRenderer.activationFlashStrength(event.getPartialTick());
		if (strength <= 1.0E-3F && flashStrength <= 1.0E-3F) {
			return;
		}

		GuiGraphics graphics = event.getGuiGraphics();
		int width = event.getWindow().getGuiScaledWidth();
		int height = event.getWindow().getGuiScaledHeight();
		if (strength > 1.0E-3F && Minecraft.getInstance().getMainRenderTarget().isStencilEnabled()) {
			GL11.glEnable(GL11.GL_STENCIL_TEST);
			RenderSystem.stencilMask(0x00);
			RenderSystem.stencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
			RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
			renderColorGrade(graphics, width, height, strength);
			resetStencilState();
		}

		if (flashStrength > 1.0E-3F) {
			renderWhiteFlash(graphics, width, height, flashStrength);
		}
	}

	private static void renderWhiteFlash(GuiGraphics graphics, int width, int height, float strength) {
		boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		graphics.flush();
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int alpha = Mth.clamp(Math.round(strength * 255.0F), 0, 255);
		drawFullscreenQuad(graphics, width, height, alpha << 24 | 0x00FFFFFF);
		graphics.flush();
		if (!blendEnabled) {
			RenderSystem.disableBlend();
		}
		if (depthTestEnabled) {
			RenderSystem.enableDepthTest();
		}
	}

	private static void renderColorGrade(GuiGraphics graphics, int width, int height, float strength) {
		int gradeColor = EPMConfig.sandevistanFilterColor();
		if (gradeColor == LEGACY_OVERLAY_COLOR) {
			gradeColor = DEFAULT_GRADE_COLOR;
		}
		if (EPMConfig.sandevistanFilterDebugGreenScreen()) {
			renderDebugGreenScreen(graphics, width, height, strength, gradeColor);
			return;
		}

		int red = gradedChannel((gradeColor >> 16) & 0xFF, strength);
		int green = gradedChannel((gradeColor >> 8) & 0xFF, strength);
		int blue = gradedChannel(gradeColor & 0xFF, strength);
		int multiplierColor = 0xFF000000 | (red << 16) | (green << 8) | blue;
		int darkAlpha = Mth.clamp(Math.round(MAX_DARK_ALPHA * strength), 0, 255);
		boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

		graphics.flush();
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
		drawFullscreenQuad(graphics, width, height, multiplierColor);

		RenderSystem.defaultBlendFunc();
		if (darkAlpha > 0) {
			drawFullscreenQuad(graphics, width, height, darkAlpha << 24);
		}
		if (!blendEnabled) {
			RenderSystem.disableBlend();
		}
		if (depthTestEnabled) {
			RenderSystem.enableDepthTest();
		}
	}

	private static void renderDebugGreenScreen(
			GuiGraphics graphics,
			int width,
			int height,
			float strength,
			int gradeColor) {
		int red = gradedChannel((gradeColor >> 16) & 0xFF, strength);
		int green = gradedChannel((gradeColor >> 8) & 0xFF, strength);
		int blue = gradedChannel(gradeColor & 0xFF, strength);
		graphics.fill(0, 0, width, height, 0xFF000000 | (red << 16) | (green << 8) | blue);
		int darkAlpha = Mth.clamp(Math.round(MAX_DARK_ALPHA * strength), 0, 255);
		if (darkAlpha > 0) {
			graphics.fill(0, 0, width, height, darkAlpha << 24);
		}
		graphics.flush();
	}

	private static void drawFullscreenQuad(GuiGraphics graphics, int width, int height, int color) {
		float alpha = (color >>> 24 & 0xFF) / 255.0F;
		float red = (color >> 16 & 0xFF) / 255.0F;
		float green = (color >> 8 & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;
		Matrix4f pose = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(pose, width, height, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(pose, width, 0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(pose, 0.0F, 0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
		buffer.vertex(pose, 0.0F, height, 0.0F).color(red, green, blue, alpha).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}

	private static int gradedChannel(int target, float strength) {
		return Mth.clamp(Math.round(Mth.lerp(strength, 255.0F, target)), 0, 255);
	}

	public static boolean beginEntityMask(Entity entity, MultiBufferSource bufferSource) {
		if (!(entity instanceof Player player) || !shouldMask(player)) {
			return false;
		}
		flush(bufferSource);
		return beginStencilWrite();
	}

	public static boolean beginLocalHandMask(MultiBufferSource bufferSource) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !shouldMask(minecraft.player)) {
			return false;
		}
		flush(bufferSource);
		return beginStencilWrite();
	}

	public static boolean beginAfterimageBatchMask() {
		Minecraft minecraft = Minecraft.getInstance();
		return isVisible() && minecraft.player != null && beginStencilWrite();
	}

	public static void endMask() {
		resetStencilState();
	}

	public static void endMask(MultiBufferSource bufferSource) {
		flush(bufferSource);
		resetStencilState();
	}

	public static boolean pauseMask(MultiBufferSource bufferSource) {
		if (!stencilWriteActive) {
			return false;
		}

		flush(bufferSource);
		resetStencilState();
		return true;
	}

	public static void resumeMask(MultiBufferSource bufferSource) {
		flush(bufferSource);
		beginStencilWrite();
	}

	private static boolean shouldMask(Player player) {
		if (!isVisible()) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		return player == minecraft.player || SandevistanStateView.isActive(player);
	}

	private static boolean beginStencilWrite() {
		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		if (!target.isStencilEnabled()) {
			return false;
		}

		GL11.glEnable(GL11.GL_STENCIL_TEST);
		RenderSystem.stencilMask(0xFF);
		RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		stencilWriteActive = true;
		return true;
	}

	private static void resetStencilState() {
		RenderSystem.stencilMask(0x00);
		RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
		RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		GL11.glDisable(GL11.GL_STENCIL_TEST);
		stencilWriteActive = false;
	}

	private static void flush(MultiBufferSource bufferSource) {
		if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
			immediate.endBatch();
		}
	}

	private static boolean isVisible() {
		return previousIntensity > 1.0E-3F || intensity > 1.0E-3F;
	}
}
