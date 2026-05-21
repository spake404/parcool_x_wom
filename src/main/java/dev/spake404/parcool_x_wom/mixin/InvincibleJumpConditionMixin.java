package dev.spake404.parcool_x_wom.mixin;

import com.p1nero.invincible.conditions.JumpCondition;
import dev.spake404.parcool_x_wom.PhantomAscentAirAttackState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = JumpCondition.class, remap = false)
public abstract class InvincibleJumpConditionMixin {
	@Inject(method = "predicate(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Z", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$acceptPhantomAscentAirAttackWindow(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(playerPatch)
				&& !playerPatch.getOriginal().onGround()) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}
}
