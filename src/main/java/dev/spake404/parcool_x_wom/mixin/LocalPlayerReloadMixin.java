package dev.spake404.parcool_x_wom.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerReload;
import dev.spake404.parcool_x_wom.ParcoolXWomClientHooks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerReload.class, remap = false)
public abstract class LocalPlayerReloadMixin {
	@Shadow
	private LocalPlayer player;

	@Inject(method = "reload", at = @At("HEAD"), require = 0)
	private void parcoolxwom$suppressAutoDashAfterRecentShoot(CallbackInfo callback) {
		ParcoolXWomClientHooks.suppressAutoFastRunDashForTaczReload(this.player);
	}
}
