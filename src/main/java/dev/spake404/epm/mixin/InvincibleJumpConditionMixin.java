package dev.spake404.epm.mixin;

import com.p1nero.invincible.conditions.JumpCondition;
import dev.spake404.epm.MomentumAirAttackWindowState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = JumpCondition.class, remap = false)
public abstract class InvincibleJumpConditionMixin {
	@Inject(method = "predicate(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Z", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$acceptMomentumAirAttackWindow(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (MomentumAirAttackWindowState.isInAirAttackWindow(playerPatch)
				&& !playerPatch.getOriginal().onGround()) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}
}
