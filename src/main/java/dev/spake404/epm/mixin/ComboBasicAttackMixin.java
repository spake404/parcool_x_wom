package dev.spake404.epm.mixin;

import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.phantom.MomentumAirAttackWindowState;
import dev.spake404.epm.wom.spider.SpiderTechniquesState;
import com.p1nero.invincible.api.skill.ComboNode;
import com.p1nero.invincible.skill.ComboBasicAttack;
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
	private void parcoolxwom$adjustComboAttackExecutableState(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (ModCompat.isWomLoaded()) {
			SpiderTechniquesState.debugAttackState(playerPatch, "ComboBasicAttack.isExecutableState", callback.getReturnValueZ());
			if (SpiderTechniquesState.shouldBlockAttack(playerPatch)) {
				callback.setReturnValue(Boolean.FALSE);
				return;
			}
		}

		if (!callback.getReturnValueZ() && EPMClientHooks.tryPrepareWallJumpAttackHandoff(playerPatch)) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "executeNodeOnServer(Lyesman/epicfight/skill/SkillContainer;Lcom/p1nero/invincible/api/skill/ComboNode;IJ)V", at = @At("RETURN"))
	private void parcoolxwom$consumeMomentumComboAttackWindow(SkillContainer skillContainer, ComboNode node, int pressTick, long pressTime, CallbackInfo callback) {
		ServerPlayerPatch serverPlayerPatch = skillContainer.getServerExecutor();
		if (serverPlayerPatch != null && MomentumAirAttackWindowState.isInAirAttackWindow(serverPlayerPatch)) {
			MomentumAirAttackWindowState.consume(serverPlayerPatch);
		}
	}
}
