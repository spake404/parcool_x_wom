package dev.spake404.epm.skill.sandevistan.client.blur;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import dev.spake404.epm.skill.sandevistan.client.SandevistanPerformanceDiagnostics;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanMaskRenderer;
import dev.spake404.epm.skill.sandevistan.mixin.SandevistanPostChainAccessor;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public final class SandevistanEdgeBlurRenderer {
	private static final ResourceLocation POST_CHAIN = ResourceLocation.fromNamespaceAndPath(
			EPM.MODID,
			"shaders/post/sandevistan_edge_blur.json");
	private static final ResourceManagerReloadListener RELOAD_LISTENER = resourceManager -> reloadRequested = true;
	private static PostChain chain;
	private static boolean initializationFailed;
	private static volatile boolean reloadRequested;
	private static int renderWidth = -1;
	private static int renderHeight = -1;
	private static long mainTargetGeneration = -1L;
	private static int mainFramebuffer = -1;
	private static int mainColorTexture = -1;
	private static int mainDepthTexture = -1;
	private static float previousIntensity;
	private static float intensity;
	private static boolean wasSandevistanActive;
	private static int warpElapsedTicks;
	private static float previousWarpPulse;
	private static float warpPulse;

	private SandevistanEdgeBlurRenderer() {
	}

	public static ResourceManagerReloadListener reloadListener() {
		return RELOAD_LISTENER;
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		previousIntensity = intensity;
		previousWarpPulse = warpPulse;
		boolean sandevistanActive = minecraft.level != null
				&& minecraft.player != null
				&& SandevistanStateView.isActive(minecraft.player);
		boolean blurActive = EPMConfig.sandevistanEdgeBlurEnabled() && sandevistanActive;
		int transitionTicks = blurActive
				? EPMConfig.sandevistanFilterFadeInTicks()
				: EPMConfig.sandevistanFilterFadeOutTicks();
		float step = 1.0F / Math.max(1, transitionTicks);
		intensity = Mth.clamp(intensity + (blurActive ? step : -step), 0.0F, 1.0F);

		if (!sandevistanActive) {
			warpElapsedTicks = 0;
			warpPulse = 0.0F;
		} else if (!wasSandevistanActive) {
			warpElapsedTicks = 0;
			warpPulse = EPMConfig.sandevistanEdgeWarpEnabled() ? calculateWarpPulse(0) : 0.0F;
		} else if (EPMConfig.sandevistanEdgeWarpEnabled()) {
			warpElapsedTicks++;
			warpPulse = calculateWarpPulse(warpElapsedTicks);
		} else {
			warpPulse = 0.0F;
		}
		wasSandevistanActive = sandevistanActive;
	}

	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			return;
		}

		float activation = Mth.lerp(event.getPartialTick(), previousIntensity, intensity)
				* EPMConfig.sandevistanEdgeBlurIntensity();
		float warpActivation = Mth.lerp(event.getPartialTick(), previousWarpPulse, warpPulse);
		float filterStrength = SandevistanFilterRenderer.colorGradeStrength(event.getPartialTick());
		if (activation <= 1.0E-3F && warpActivation <= 1.0E-3F && filterStrength <= 1.0E-3F) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null || !ensureChain(minecraft)) {
			return;
		}

		resizeIfNeeded(minecraft);
		EffectInstance effect = firstEffect();
		if (effect == null) {
			return;
		}

		float blurStart = EPMConfig.sandevistanEdgeBlurStart();
		float blurFull = Math.max(blurStart + 1.0E-3F, EPMConfig.sandevistanEdgeBlurFull());
		float warpStart = EPMConfig.sandevistanEdgeWarpStart();
		float warpFull = Math.max(warpStart + 1.0E-3F, EPMConfig.sandevistanEdgeWarpFull());
		effect.safeGetUniform("center").set(0.5F, 0.5F);
		effect.safeGetUniform("intensity").set(activation);
		effect.safeGetUniform("strength").set(EPMConfig.sandevistanEdgeBlurStrength());
		effect.safeGetUniform("blurStart").set(blurStart);
		effect.safeGetUniform("blurFull").set(Mth.clamp(blurFull, 0.0F, 1.0F));
		effect.safeGetUniform("samples").set(activation > 1.0E-3F
				? EPMConfig.sandevistanEdgeBlurSamples()
				: 0);
		effect.safeGetUniform("warpPulse").set(warpActivation);
		effect.safeGetUniform("warpStrength").set(EPMConfig.sandevistanEdgeWarpStrength());
		effect.safeGetUniform("warpStart").set(warpStart);
		effect.safeGetUniform("warpFull").set(Mth.clamp(warpFull, 0.0F, 1.0F));
		effect.safeGetUniform("chromaticStrength").set(EPMConfig.sandevistanChromaticAberrationEnabled()
				? EPMConfig.sandevistanChromaticAberrationStrength()
				: 0.0F);
		RenderTarget mainTarget = minecraft.getMainRenderTarget();
		int maskTexture = SandevistanMaskRenderer.colorTextureId();
		effect.setSampler("MaskSampler", () -> maskTexture >= 0
				? maskTexture
				: mainTarget.getColorTextureId());
		effect.safeGetUniform("maskEnabled").set(maskTexture >= 0 ? 1.0F : 0.0F);
		effect.safeGetUniform("filterStrength").set(filterStrength);
		int filterColor = SandevistanFilterRenderer.colorGradeColor();
		effect.safeGetUniform("filterColor").set(
				(filterColor >> 16 & 0xFF) / 255.0F,
				(filterColor >> 8 & 0xFF) / 255.0F,
				(filterColor & 0xFF) / 255.0F);
		effect.safeGetUniform("filterDarkness").set(SandevistanFilterRenderer.maximumDarkness());
		effect.safeGetUniform("filterDebug").set(SandevistanFilterRenderer.debugGreenScreen() ? 1.0F : 0.0F);
		long startedNanos = System.nanoTime();
		chain.process(event.getPartialTick());
		SandevistanPerformanceDiagnostics.recordPostProcess(System.nanoTime() - startedNanos);
		mainTarget.bindWrite(true);
	}

	public static float activationFlashStrength(float partialTick) {
		if (!EPMConfig.sandevistanActivationFlashEnabled()) {
			return 0.0F;
		}

		float pulse = Mth.clamp(Mth.lerp(partialTick, previousWarpPulse, warpPulse), 0.0F, 1.0F);
		return EPMConfig.sandevistanActivationFlashStrength() * pulse * pulse * pulse;
	}

	private static float calculateWarpPulse(int elapsedTicks) {
		int durationTicks = Math.max(1, EPMConfig.sandevistanEdgeWarpDurationTicks());
		if (elapsedTicks >= durationTicks) {
			return 0.0F;
		}

		int riseTicks = Mth.clamp(EPMConfig.sandevistanEdgeWarpRiseTicks(), 0, durationTicks);
		if (riseTicks > 0 && elapsedTicks < riseTicks) {
			return smoothStep((elapsedTicks + 1.0F) / riseTicks);
		}

		int fadeTicks = Math.max(1, durationTicks - riseTicks);
		float fade = (durationTicks - elapsedTicks) / (float) fadeTicks;
		return smoothStep(Mth.clamp(fade, 0.0F, 1.0F));
	}

	private static float smoothStep(float value) {
		float clamped = Mth.clamp(value, 0.0F, 1.0F);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static boolean ensureChain(Minecraft minecraft) {
		if (reloadRequested) {
			releaseChain();
			reloadRequested = false;
			initializationFailed = false;
		}
		RenderTarget mainTarget = minecraft.getMainRenderTarget();
		long generation = SandevistanMaskRenderer.mainTargetGeneration();
		if (chain != null && (mainTargetGeneration != generation
				|| mainFramebuffer != mainTarget.frameBufferId
				|| mainColorTexture != mainTarget.getColorTextureId()
				|| mainDepthTexture != mainTarget.getDepthTextureId())) {
			releaseChain();
			initializationFailed = false;
		}
		if (chain != null) {
			return true;
		}
		if (initializationFailed) {
			return false;
		}

		try {
			chain = new PostChain(
					minecraft.getTextureManager(),
					minecraft.getResourceManager(),
					mainTarget,
					POST_CHAIN);
			mainTargetGeneration = generation;
			mainFramebuffer = mainTarget.frameBufferId;
			mainColorTexture = mainTarget.getColorTextureId();
			mainDepthTexture = mainTarget.getDepthTextureId();
			renderWidth = -1;
			renderHeight = -1;
			return true;
		} catch (IOException | JsonSyntaxException exception) {
			initializationFailed = true;
			EPM.LOGGER.error("Unable to initialize the Sandevistan edge blur shader", exception);
			return false;
		}
	}

	private static void resizeIfNeeded(Minecraft minecraft) {
		int width = minecraft.getMainRenderTarget().width;
		int height = minecraft.getMainRenderTarget().height;
		if (width == renderWidth && height == renderHeight) {
			return;
		}

		chain.resize(width, height);
		renderWidth = width;
		renderHeight = height;
	}

	private static EffectInstance firstEffect() {
		List<PostPass> passes = ((SandevistanPostChainAccessor) (Object) chain).epm$getPasses();
		return passes.isEmpty() ? null : passes.get(0).getEffect();
	}

	private static void releaseChain() {
		if (chain != null) {
			chain.close();
			chain = null;
		}
		renderWidth = -1;
		renderHeight = -1;
		mainTargetGeneration = -1L;
		mainFramebuffer = -1;
		mainColorTexture = -1;
		mainDepthTexture = -1;
	}
}
