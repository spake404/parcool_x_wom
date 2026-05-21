package dev.spake404.parcool_x_wom.mixin;

import dev.spake404.parcool_x_wom.ParcoolXWomClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

@Mixin(value = PhantomAscentSkill.class, remap = false)
public abstract class PhantomAscentSkillMixin {
	@Inject(method = "lambda$onInitiate$1", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disablePhantomAscentForBlockedWeapons(SkillContainer skillContainer, MovementInputEvent event, CallbackInfo callback) {
		if (ParcoolXWomClientHooks.isHoldingPhantomAscentBlockedWeapon(event.getPlayerPatch().getOriginal())) {
			skillContainer.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.valueOf(isJumpPressed()));
			callback.cancel();
		}
	}

	@Inject(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lyesman/epicfight/skill/SkillDataManager;setDataSync(Lyesman/epicfight/skill/SkillDataKey;Ljava/lang/Object;)V",
					shift = At.Shift.AFTER
			),
			require = 1
	)
	private void parcoolxwom$markPhantomAscentAirAttackWindow(SkillContainer skillContainer, MovementInputEvent event, CallbackInfo callback) {
		ParcoolXWomClientHooks.markPhantomAscentAirAttackWindow(event.getPlayerPatch().getOriginal());
	}

	private static boolean isJumpPressed() {
		try {
			return InputManager.isActionActive(MinecraftInputAction.JUMP);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
