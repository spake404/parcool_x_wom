package dev.spake404.parcool_x_wom.mixin;

import com.hm.efn.comboevents.condition.state.EFNAirborneCondition;
import dev.spake404.parcool_x_wom.PhantomAscentAirAttackState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = EFNAirborneCondition.class, remap = false)
public abstract class EFNAirborneConditionMixin {
	@Inject(method = "predicate(Lyesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch;)Z", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$acceptPhantomAscentAirAttackWindow(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(playerPatch)
				&& !playerPatch.getOriginal().onGround()) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}
}
