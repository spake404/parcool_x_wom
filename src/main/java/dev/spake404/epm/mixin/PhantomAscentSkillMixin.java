package dev.spake404.epm.mixin;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.demolition.DemolitionLeapCatJumpHandler;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

@Mixin(value = PhantomAscentSkill.class, remap = false)
public abstract class PhantomAscentSkillMixin {
	@Inject(method = "lambda$onInitiate$1", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disablePhantomAscentForBlockedStates(SkillContainer skillContainer, MovementInputEvent event, CallbackInfo callback) {
		Player player = event.getPlayerPatch().getOriginal();
		if ((!EPMClientHooks.isForcedDemolitionPhantomAscent(player)
				&& DemolitionLeapCatJumpHandler.shouldSuppressPhantomAscent(player))
				|| parcoolxwom$shouldBlockUnderwaterSwimming(player)
				|| EPMClientHooks.isHoldingPhantomAscentBlockedWeapon(player)) {
			skillContainer.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.valueOf(isJumpPressed()));
			callback.cancel();
			return;
		}

		if (EPMClientHooks.shouldCancelPhantomAscentForJumpArbitration(skillContainer, event)) {
			callback.cancel();
		}
	}

	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;isHoldingAny()Z"
			),
			require = 0
	)
	private boolean parcoolxwom$allowForcedDemolitionPhantomWhileHolding(LocalPlayerPatch playerPatch, SkillContainer skillContainer, MovementInputEvent event) {
		boolean original = playerPatch.isHoldingAny();
		Player player = event.getPlayerPatch().getOriginal();
		if (original && EPMClientHooks.isForcedDemolitionPhantomAscent(player)) {
			EPMClientHooks.logForcedDemolitionPhantomBypass(player, "phantom_force_demolition_bypass_holding", true);
			return false;
		}
		return original;
	}

	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lyesman/epicfight/api/animation/types/EntityState;inaction()Z"
			),
			require = 0
	)
	private boolean parcoolxwom$allowForcedDemolitionPhantomDuringInaction(EntityState entityState, SkillContainer skillContainer, MovementInputEvent event) {
		boolean original = entityState.inaction();
		Player player = event.getPlayerPatch().getOriginal();
		if (original && EPMClientHooks.isForcedDemolitionPhantomAscent(player)) {
			EPMClientHooks.logForcedDemolitionPhantomBypass(player, "phantom_force_demolition_bypass_inaction", true);
			return false;
		}
		return original;
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
	private void parcoolxwom$markNativePhantomAscentStarted(SkillContainer skillContainer, MovementInputEvent event, CallbackInfo callback) {
		EPMClientHooks.markNativePhantomAscentStarted(event.getPlayerPatch().getOriginal());
	}

	@ModifyConstant(method = "lambda$onInitiate$2", constant = @Constant(floatValue = 2.5F), require = 0)
	private static float parcoolxwom$useConfiguredPhantomAscentFallProtectionThreshold(float original) {
		return EPMConfig.phantomAscentFallProtectionDamageThreshold();
	}

	private static boolean parcoolxwom$shouldBlockUnderwaterSwimming(Player player) {
		return EPMConfig.disablePhantomAscentUnderwaterSwimming()
				&& player != null
				&& player.isSwimming()
				&& player.isUnderWater();
	}

	private static boolean isJumpPressed() {
		try {
			return InputManager.isActionActive(MinecraftInputAction.JUMP);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
