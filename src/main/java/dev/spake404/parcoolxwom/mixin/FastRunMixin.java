package dev.spake404.parcoolxwom.mixin;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;

import dev.spake404.parcoolxwom.ParCoolXWomClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FastRun.class, remap = false)
public abstract class FastRunMixin {
	@Inject(method = "canContinue", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$continueAfterVault(Player player, Parkourability parkourability, IStamina stamina, CallbackInfoReturnable<Boolean> cir) {
		if (ParCoolXWomClientHooks.shouldKeepFastRunAfterVault(player)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
