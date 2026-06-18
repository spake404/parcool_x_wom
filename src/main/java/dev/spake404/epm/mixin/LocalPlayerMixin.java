package dev.spake404.epm.mixin;

import dev.spake404.epm.DemolitionLeapCatJumpHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
	private void epm$suppressDemolitionLeapComboSneak(CallbackInfoReturnable<Boolean> callback) {
		if (DemolitionLeapCatJumpHandler.shouldSuppressSneak((LocalPlayer) (Object) this)) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}
}
