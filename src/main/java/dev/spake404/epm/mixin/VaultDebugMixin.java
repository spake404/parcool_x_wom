package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.vault.VaultDebug;
import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Vault.class, remap = false)
public abstract class VaultDebugMixin {
	@Inject(method = "canStart", at = @At("RETURN"), require = 0)
	private void epm$logCanStart(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo,
			CallbackInfoReturnable<Boolean> callback) {
		VaultDebug.logCanStart(player, parkourability, stamina, Boolean.TRUE.equals(callback.getReturnValue()));
	}

	@Inject(method = "canContinue", at = @At("RETURN"), require = 0)
	private void epm$logCanContinue(Player player, Parkourability parkourability, IStamina stamina,
			CallbackInfoReturnable<Boolean> callback) {
		VaultDebug.logCanContinue((Vault) (Object) this, player, parkourability, stamina, Boolean.TRUE.equals(callback.getReturnValue()));
	}

	@Inject(method = "onStartInLocalClient", at = @At("TAIL"), require = 0)
	private void epm$logStart(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startData,
			CallbackInfo callback) {
		VaultDebug.logStart("start_local", (Vault) (Object) this, player, parkourability, stamina);
	}

	@Inject(method = "onWorkingTickInLocalClient", at = @At("TAIL"), require = 0)
	private void epm$logWorkingTick(Player player, Parkourability parkourability, IStamina stamina, CallbackInfo callback) {
		VaultDebug.logWorkingTick((Vault) (Object) this, player, parkourability, stamina);
	}

	@Inject(method = "onStopInLocalClient", at = @At("TAIL"), require = 0)
	private void epm$logStop(Player player, CallbackInfo callback) {
		VaultDebug.logStop((Vault) (Object) this, player);
	}
}
