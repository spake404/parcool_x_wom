package dev.spake404.epm.mixin;

import dev.spake404.epm.epicfightx.EpicFightXCombatMasteryCompat;
import dev.spake404.epm.EPM;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(targets = "yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch", remap = false)
public abstract class AbstractClientPlayerPatchMixin {
	@Inject(method = "updateMotion", at = @At("HEAD"), require = 0)
	private void epm$syncCombatMasteryVanillaSprintBeforeMotion(boolean considerInaction, CallbackInfo callback) {
		if ((Object) this instanceof LocalPlayerPatch playerPatch) {
			EpicFightXCombatMasteryCompat.syncVanillaSprintBeforeEpicFightMotion(playerPatch);
		}
	}
}
