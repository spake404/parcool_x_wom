package dev.spake404.epm.mixin;

import com.hm.efn.comboevents.condition.state.EFNAirborneCondition;
import dev.spake404.epm.MomentumAirAttackWindowState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = EFNAirborneCondition.class, remap = false)
public abstract class EFNAirborneConditionMixin {
	@Inject(method = "predicate(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Z", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$acceptMomentumAirAttackWindow(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (MomentumAirAttackWindowState.isInAirAttackWindow(playerPatch)
				&& !playerPatch.getOriginal().onGround()) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}
}
