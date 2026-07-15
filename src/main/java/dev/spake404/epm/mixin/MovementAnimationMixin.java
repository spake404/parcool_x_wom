package dev.spake404.epm.mixin;

import dev.spake404.epm.animation.MovementAnimationSpeedController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = MovementAnimation.class, remap = false)
public abstract class MovementAnimationMixin {
	@Inject(method = "getPlaySpeed", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$extendPlayerMovementAnimationSpeed(
			LivingEntityPatch<?> entityPatch,
			DynamicAnimation animation,
			CallbackInfoReturnable<Float> callback) {
		callback.setReturnValue(MovementAnimationSpeedController.modifyPlaySpeed(
				entityPatch,
				animation,
				callback.getReturnValueF()));
	}
}
