package dev.spake404.epm.mixin;

import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Vault.class, remap = false)
public abstract class VaultChainMixin {
	@Inject(method = "canContinue", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$finishEarlyForCloseChain(Player player, Parkourability parkourability, IStamina stamina,
			CallbackInfoReturnable<Boolean> callback) {
		if (Boolean.TRUE.equals(callback.getReturnValue())
				&& EPMClientHooks.shouldFinishVaultEarlyForCloseChain((Vault) (Object) this, player)) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}
}
