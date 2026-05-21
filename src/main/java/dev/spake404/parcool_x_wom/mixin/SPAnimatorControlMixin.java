package dev.spake404.parcool_x_wom.mixin;

import dev.spake404.parcool_x_wom.ParcoolXWomClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = SPAnimatorControl.class, remap = false)
public abstract class SPAnimatorControlMixin {
	@Inject(method = "lambda$onArrive$0", at = @At("HEAD"), cancellable = true, require = 1)
	private void parcoolxwom$delayPhantomAscentAirAttack(LivingEntityPatch<?> entityPatch, CallbackInfo callback) {
		if (entityPatch instanceof PlayerPatch<?> playerPatch
				&& ParcoolXWomClientHooks.delayPhantomAscentAirAttackAnimation(playerPatch,
						(AnimatorControlPacketAccessor) this,
						(SPAnimatorControlAccessor) this)) {
			callback.cancel();
		}
	}
}
