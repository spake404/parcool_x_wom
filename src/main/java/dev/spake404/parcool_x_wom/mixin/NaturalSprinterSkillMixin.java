package dev.spake404.parcool_x_wom.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;

@Mixin(targets = "reascer.wom.skill.mover.NaturalSprinterSkill", remap = false)
public abstract class NaturalSprinterSkillMixin {
	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lyesman/epicfight/api/client/input/InputManager;isActionActive(Lyesman/epicfight/api/client/input/action/InputAction;)Z"
			),
			require = 0
	)
	private static boolean parcoolxwom$disableNaturalSprinterSlide(InputAction action) {
		return action != MinecraftInputAction.SNEAK && InputManager.isActionActive(action);
	}
}
