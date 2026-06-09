package dev.spake404.epm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.venturecraft.gliders.client.layer.PlayerGliderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerGliderLayer.class, remap = false)
public abstract class PlayerGliderLayerMixin {
	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$hideGliderModelDuringPhantomAscentDelay(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callback) {
		if (EPMClientHooks.shouldDelayGliderOpeningAnimation(entity)) {
			callback.cancel();
		}
	}
}
