package dev.spake404.parcool_x_wom.mixin;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.parcool_x_wom.ParcoolXWom;
import dev.spake404.parcool_x_wom.ParcoolXWomClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FastRun.class, remap = false)
public abstract class FastRunMixin {
	@Shadow
	private boolean toggleStatus;

	@Inject(method = "canContinue", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$continueDuringVault(Player player, Parkourability parkourability, IStamina stamina, CallbackInfoReturnable<Boolean> cir) {
		if (ParcoolXWomClientHooks.shouldKeepFastRunDuringVault(player)) {
			this.toggleStatus = true;
			cir.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "onClientTick", at = @At("RETURN"), require = 0)
	private void parcoolxwom$keepToggleDuringVault(Player player, Parkourability parkourability, IStamina stamina, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
		if (ParcoolXWomClientHooks.shouldPreserveFastRunToggleDuringVault(player)) {
			this.toggleStatus = true;
		}
	}

	@Inject(method = "canContinue", at = @At("RETURN"), require = 0)
	private void parcoolxwom$debugVaultFastRunStop(Player player, Parkourability parkourability, IStamina stamina, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && ParcoolXWomClientHooks.wasHoldingFastRunDuringVault(player)) {
			ParcoolXWom.LOGGER.info(
					"[ParCool x WOM] FastRun after Vault failed: toggleStatus={}, sprinting={}, exhausted={}, waterOrBubble={}, vehicle={}, fallFlying={}, crawling={}, swimming={}, shift={}, vaultHold={}",
					this.toggleStatus,
					player.isSprinting(),
					stamina.isExhausted(),
					player.isInWaterOrBubble(),
					player.getVehicle() != null,
					player.isFallFlying(),
					player.isVisuallyCrawling(),
					player.isSwimming(),
					player.isShiftKeyDown(),
					ParcoolXWomClientHooks.wasHoldingFastRunDuringVault(player));
		}
	}
}
