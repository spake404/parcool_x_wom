package dev.spake404.epm.skill.sandevistan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.skill.sandevistan.client.SandevistanRenderTimeContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class SandevistanEntityRenderDispatcherMixin {
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
}
