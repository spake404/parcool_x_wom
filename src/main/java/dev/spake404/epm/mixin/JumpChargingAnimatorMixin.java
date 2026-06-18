package dev.spake404.epm.mixin;

import com.alrex.parcool.client.animation.PlayerModelRotator;
import com.alrex.parcool.client.animation.PlayerModelTransformer;
import com.alrex.parcool.client.animation.impl.JumpChargingAnimator;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.DemolitionLeapCatJumpHandler;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = JumpChargingAnimator.class, remap = false)
public abstract class JumpChargingAnimatorMixin {
	@Inject(method = "shouldRemoved", at = @At("HEAD"), cancellable = true)
	private void epm$removeForTaczGun(Player player, Parkourability parkourability, CallbackInfoReturnable<Boolean> callback) {
		if (EPMClientHooks.shouldSuppressJumpChargingForTacz(player)
				|| DemolitionLeapCatJumpHandler.shouldSuppressJumpChargingForDemolitionLeap(player)) {
			callback.setReturnValue(true);
		}
	}

	@Inject(method = "animatePre", at = @At("HEAD"), cancellable = true)
	private void epm$skipPoseForTaczGunOrDemolitionLeap(Player player, Parkourability parkourability, PlayerModelTransformer transformer, CallbackInfoReturnable<Boolean> callback) {
		if (EPMClientHooks.shouldSuppressJumpChargingForTacz(player)
				|| DemolitionLeapCatJumpHandler.shouldReplaceChargeJumpAnimator(player)) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "rotatePre", at = @At("HEAD"), cancellable = true)
	private void epm$skipRotationForTaczGunOrDemolitionLeap(Player player, Parkourability parkourability, PlayerModelRotator rotator, CallbackInfoReturnable<Boolean> callback) {
		if (EPMClientHooks.shouldSuppressJumpChargingForTacz(player)
				|| DemolitionLeapCatJumpHandler.shouldReplaceChargeJumpAnimator(player)) {
			callback.setReturnValue(false);
		}
	}
}
