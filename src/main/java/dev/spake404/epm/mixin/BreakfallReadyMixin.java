package dev.spake404.epm.mixin;

import com.alrex.parcool.common.action.impl.BreakfallReady;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BreakfallReady.class, remap = false)
public abstract class BreakfallReadyMixin {
	@Inject(method = "startBreakfall", at = @At("HEAD"), require = 0)
	private void epm$delayNaturalSprinterDash(Player player, Parkourability parkourability, IStamina stamina, boolean justTimed, CallbackInfo callback) {
		EPMClientHooks.markBreakfallStarted(player);
	}
}
