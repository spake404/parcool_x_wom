package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.config.EPMConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.InputAction;

@Mixin(targets = "reascer.wom.skill.mover.NaturalSprinterSkill", remap = false)
public abstract class NaturalSprinterSkillMixin {
	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lyesman/epicfight/api/client/input/InputManager;isActionActive(Lyesman/epicfight/api/client/input/action/InputAction;)Z",
					ordinal = 0
			),
			require = 0
	)
	private static boolean parcoolxwom$disableNaturalSprinterSlide(InputAction action) {
		return !EPMParCoolGate.allowCrossModSkillCompat() && InputManager.isActionActive(action);
	}

	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z",
					ordinal = 0
			),
			require = 0
	)
	private static boolean parcoolxwom$disableNativeFastRunSprintBranch(Player player) {
		if (!parcoolxwom$ownsFastRunSprintBranch()) {
			return player.isSprinting();
		}
		if (EPMConfig.debugNaturalSprinterFastRunStepState()) {
			EPM.LOGGER.info(
					"[EPM/NaturalSprinterStepPulse] phase=block_wom_native_sprinting_branch fastRunStartStepAnimation={}",
					Boolean.valueOf(EPMConfig.fastRunStartStepAnimation()));
		}
		return false;
	}

	@Redirect(
			method = "updateContainer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z",
					ordinal = 0
			),
			require = 0
	)
	private boolean parcoolxwom$disableNativeFastRunStartupTimer(Player player) {
		if (!parcoolxwom$ownsFastRunSprintBranch(player)) {
			return player.isSprinting();
		}
		if (EPMConfig.debugNaturalSprinterFastRunStepState()) {
			EPM.LOGGER.info(
					"[EPM/NaturalSprinterStepPulse] phase=block_wom_native_update_container_sprinting_branch fastRunStartStepAnimation={}",
					Boolean.valueOf(EPMConfig.fastRunStartStepAnimation()));
		}
		return false;
	}

	private static boolean parcoolxwom$ownsFastRunSprintBranch() {
		return parcoolxwom$ownsFastRunSprintBranch(null);
	}

	private static boolean parcoolxwom$ownsFastRunSprintBranch(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.customFastRunAnimations()
				&& (EPMClientHooks.isFastRunKeyDown()
						|| EPMClientHooks.isFastRunKeyRecentlyPressed()
						|| EPMClientHooks.isParCoolFastRunDoing(player));
	}
}
