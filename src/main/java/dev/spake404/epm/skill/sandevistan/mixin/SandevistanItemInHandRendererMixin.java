package dev.spake404.epm.skill.sandevistan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class SandevistanItemInHandRendererMixin {
	@Unique
	private boolean epm$sandevistanHandMask;

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"))
	private void epm$beginSandevistanHandMask(
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource.BufferSource bufferSource,
			LocalPlayer player,
			int packedLight,
			CallbackInfo callbackInfo) {
		epm$sandevistanHandMask = SandevistanFilterRenderer.beginLocalHandMask(bufferSource);
	}

	@Inject(method = "renderHandsWithItems", at = @At("RETURN"))
	private void epm$endSandevistanHandMask(
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource.BufferSource bufferSource,
			LocalPlayer player,
			int packedLight,
			CallbackInfo callbackInfo) {
		if (epm$sandevistanHandMask) {
			SandevistanFilterRenderer.endMask(bufferSource);
			epm$sandevistanHandMask = false;
		}
	}
}
