package dev.spake404.epm.mixin;

import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.Dodge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.alrex.parcool.common.info.ClientSetting$Remote", remap = false)
public abstract class ClientSettingRemoteMixin {
	@Inject(method = "getPossibilityOf", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disableDodge(Class<? extends Action> action, CallbackInfoReturnable<Boolean> callback) {
		if (action == Dodge.class) {
			callback.setReturnValue(false);
		}
	}
}
