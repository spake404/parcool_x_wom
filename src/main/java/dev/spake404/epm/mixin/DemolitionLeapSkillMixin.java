package dev.spake404.epm.mixin;

import dev.spake404.epm.demolition.DemolitionLeapAirJumpHandler;
import dev.spake404.epm.demolition.DemolitionLeapCatJumpHandler;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMParCoolGate;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.skill.mover.DemolitionLeapSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = DemolitionLeapSkill.class, remap = false)
public abstract class DemolitionLeapSkillMixin {
	@Shadow
	public abstract KeyMapping getKeyMapping();

	@Inject(method = "gatherHoldArguments", at = @At("HEAD"), cancellable = true)
	private void epm$holdActualContainerSlot(SkillContainer container, ControlEngine controlEngine, FriendlyByteBuf buffer, CallbackInfo callback) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		controlEngine.setHoldingKey(container.getSlot(), getKeyMapping());
		container.getExecutor().startSkillHolding((HoldableSkill) (Object) this);
		callback.cancel();
	}

	@Inject(method = "isExecutableState", at = @At("HEAD"), cancellable = true)
	private void epm$onlyStartFromShiftSpacePath(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (playerPatch != null
				&& playerPatch.isLogicalClient()
				&& DemolitionLeapCatJumpHandler.isShiftSpaceReplacementEnabled()
				&& !DemolitionLeapCatJumpHandler.isAuthorizedDemolitionLeapStart(playerPatch)) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}

	@Inject(method = "executeOnClient", at = @At("TAIL"))
	private void epm$openAirDoubleJumpWindow(SkillContainer container, FriendlyByteBuf buffer, CallbackInfo callback) {
		DemolitionLeapAirJumpHandler.markLaunched(container);
	}
}
