package dev.spake404.epm.skill.sandevistan.client.filter;

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
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class SandevistanFilterRenderer {
	private static final int LEGACY_OVERLAY_COLOR = 0x065F35;
	private static final int DEFAULT_GRADE_COLOR = 0x3CFF48;
	private static final float MAX_FILTER_STRENGTH = 0.8F;
	private static final float MAX_DARK_ALPHA = 20.0F;
	private static float previousIntensity;
	private static float intensity;

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

	public static float colorGradeStrength(float partialTick) {
		return Mth.lerp(partialTick, previousIntensity, intensity)
				* EPMConfig.sandevistanFilterIntensity()
				* MAX_FILTER_STRENGTH;
	}

	public static boolean isColorGradeVisible() {
		return previousIntensity > 1.0E-3F || intensity > 1.0E-3F;
	}

	public static int colorGradeColor() {
		int gradeColor = EPMConfig.sandevistanFilterColor();
		return gradeColor == LEGACY_OVERLAY_COLOR ? DEFAULT_GRADE_COLOR : gradeColor;
	}

	public static float maximumDarkness() {
		return MAX_DARK_ALPHA / 255.0F;
	}

	public static boolean debugGreenScreen() {
		return EPMConfig.sandevistanFilterDebugGreenScreen();
	}

	public static void renderFilter(RenderGuiEvent.Pre event) {
		float flashStrength = SandevistanEdgeBlurRenderer.activationFlashStrength(event.getPartialTick());
		if (flashStrength <= 1.0E-3F) {
			return;
		}

		renderWhiteFlash(
				event.getGuiGraphics(),
				event.getWindow().getGuiScaledWidth(),
				event.getWindow().getGuiScaledHeight(),
				flashStrength);
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
}
