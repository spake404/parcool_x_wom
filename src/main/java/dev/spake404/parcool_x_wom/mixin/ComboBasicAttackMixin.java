package dev.spake404.parcool_x_wom.mixin;

import com.p1nero.invincible.api.skill.ComboNode;
import com.p1nero.invincible.skill.ComboBasicAttack;
import dev.spake404.parcool_x_wom.PhantomAscentAirAttackState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Mixin(value = ComboBasicAttack.class, remap = false)
public abstract class ComboBasicAttackMixin {
	@Inject(method = "isExecutableState", at = @At("RETURN"), cancellable = true)
	private void parcoolxwom$allowPhantomAscentComboAttack(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValueZ() && PhantomAscentAirAttackState.canUseBasicAttackAfterPhantomAscent(playerPatch)) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "executeNodeOnServer(Lyesman/epicfight/skill/SkillContainer;Lcom/p1nero/invincible/api/skill/ComboNode;IJ)V", at = @At("RETURN"))
	private void parcoolxwom$consumePhantomAscentComboAttackWindow(SkillContainer skillContainer, ComboNode node, int pressTick, long pressTime, CallbackInfo callback) {
		ServerPlayerPatch serverPlayerPatch = skillContainer.getServerExecutor();
		if (serverPlayerPatch != null && PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(serverPlayerPatch)) {
			PhantomAscentAirAttackState.consume(serverPlayerPatch.getOriginal());
		}
	}
}
