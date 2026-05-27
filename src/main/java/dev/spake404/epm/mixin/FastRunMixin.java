package dev.spake404.epm.mixin;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.EPMClientHooks;
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
		if (EPMClientHooks.shouldStopFastRunForTaczShoot(player)) {
			this.toggleStatus = false;
			cir.setReturnValue(Boolean.FALSE);
			return;
		}

		if (EPMClientHooks.shouldKeepFastRunDuringVault(player)
				|| EPMClientHooks.shouldKeepFastRunAfterWallJump(player, stamina)
				|| EPMClientHooks.shouldRestoreFastRunAfterTaczShoot(player, stamina)) {
			this.toggleStatus = true;
			cir.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "onClientTick", at = @At("RETURN"), require = 0)
	private void parcoolxwom$keepToggleDuringVault(Player player, Parkourability parkourability, IStamina stamina, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
		if (EPMClientHooks.shouldStopFastRunForTaczShoot(player)) {
			this.toggleStatus = false;
			return;
		}

		if (EPMClientHooks.shouldPreserveFastRunToggleDuringVault(player)
				|| EPMClientHooks.shouldPreserveFastRunToggleAfterWallJump(player, stamina)
				|| EPMClientHooks.shouldPreserveFastRunAfterTaczShoot(player, stamina)) {
			this.toggleStatus = true;
		}
	}
}
