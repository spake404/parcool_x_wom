package dev.spake404.epm.mixin;

import dev.spake404.epm.GliderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = DodgeSkill.class, remap = false)
public abstract class DodgeSkillMixin {
	@Inject(method = "isExecutableState", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$disableEpicFightDodgeWhileGliding(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (playerPatch != null && GliderCompat.isGlidingWithActiveGlider(playerPatch.getOriginal())) {
			callback.setReturnValue(false);
		}
	}
}
