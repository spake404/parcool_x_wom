package dev.spake404.epm.skill.sandevistan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.skill.sandevistan.client.SandevistanRenderTimeContext;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class SandevistanEntityRenderDispatcherMixin {
	@Unique
	private boolean epm$sandevistanMaskPausedForShadow;

	@Inject(method = "render", at = @At("HEAD"), require = 0)
	private <E extends Entity> void epm$beginSandevistanRenderTime(
			E entity,
			double renderX,
			double renderY,
			double renderZ,
			float renderedYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo callbackInfo) {
		SandevistanRenderTimeContext.push(partialTick);
	}

	@Inject(method = "render", at = @At("RETURN"), require = 0)
	private <E extends Entity> void epm$endSandevistanRenderTime(
			E entity,
			double renderX,
			double renderY,
			double renderZ,
			float renderedYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo callbackInfo) {
		SandevistanRenderTimeContext.pop();
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
					shift = At.Shift.BEFORE),
			require = 0)
	private <E extends Entity> void epm$pauseSandevistanMaskForShadow(
			E entity,
			double renderX,
			double renderY,
			double renderZ,
			float renderedYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo callbackInfo) {
		epm$sandevistanMaskPausedForShadow = SandevistanFilterRenderer.pauseMask(bufferSource);
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
					shift = At.Shift.AFTER),
			require = 0)
	private <E extends Entity> void epm$resumeSandevistanMaskAfterShadow(
			E entity,
			double renderX,
			double renderY,
			double renderZ,
			float renderedYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo callbackInfo) {
		if (epm$sandevistanMaskPausedForShadow) {
			SandevistanFilterRenderer.resumeMask(bufferSource);
			epm$sandevistanMaskPausedForShadow = false;
		}
	}
}
