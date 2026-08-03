package dev.spake404.epm.skill.sandevistan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.client.SandevistanParticleTickClock;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleEngine.class, priority = 1500)
public abstract class SandevistanParticleRenderMixin {
	@Shadow
	@Nullable
	private ClientLevel level;

	@Unique
	@Nullable
	private Particle epm$currentParticle;

	@Unique
	private boolean epm$capturedParticle;

	@Unique
	private boolean epm$appliedLocalPartialTick;

	@Unique
	private static boolean epm$warnedAboutRenderFallback;

	@Inject(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
			at = @At("HEAD"),
			require = 0,
			remap = false)
	private void epm$beginParticleRenderPass(
			PoseStack poseStack,
			MultiBufferSource.BufferSource bufferSource,
			LightTexture lightTexture,
			Camera camera,
			float partialTick,
			Frustum frustum,
			CallbackInfo callbackInfo) {
		epm$currentParticle = null;
		epm$capturedParticle = false;
		epm$appliedLocalPartialTick = false;
	}

	@ModifyVariable(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
			at = @At(value = "STORE"),
			ordinal = 0,
			require = 0,
			remap = false)
	private Particle epm$captureRenderedParticle(Particle particle) {
		epm$currentParticle = particle;
		epm$capturedParticle = true;
		return particle;
	}

	@ModifyArg(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"),
			index = 2,
			require = 0)
	private float epm$useParticleLocalPartialTick(float globalPartialTick) {
		Particle particle = epm$currentParticle;
		if (particle == null) {
			return globalPartialTick;
		}

		epm$appliedLocalPartialTick = true;
		return SandevistanParticleTickClock.localPartialTick(particle, this.level, globalPartialTick);
	}

	@Inject(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
			at = @At("RETURN"),
			require = 0,
			remap = false)
	private void epm$endParticleRenderPass(
			PoseStack poseStack,
			MultiBufferSource.BufferSource bufferSource,
			LightTexture lightTexture,
			Camera camera,
			float partialTick,
			Frustum frustum,
			CallbackInfo callbackInfo) {
		if (epm$capturedParticle && !epm$appliedLocalPartialTick && !epm$warnedAboutRenderFallback) {
			epm$warnedAboutRenderFallback = true;
			EPM.LOGGER.warn("Sandevistan particle render interpolation hook was replaced by another mod; particle tick slowdown remains active without local render smoothing");
		}

		epm$currentParticle = null;
	}
}
