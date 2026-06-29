package dev.spake404.epm.mixin;

import dev.spake404.epm.compat.ModCompat;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.ssrcamerafixes.handler.WallClimbBodyLockHandler", remap = false)
public abstract class SsrWallClimbBodyLockHandlerMixin {
	@Inject(method = "onPlayerTickEnd", at = @At("HEAD"), cancellable = true)
	private static void epm$disableSsrWallClimbBodyLock(TickEvent.PlayerTickEvent event, CallbackInfo ci) {
		if (ModCompat.isWomLoaded()) {
			ci.cancel();
		}
	}
}
