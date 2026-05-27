package dev.spake404.epm.mixin;

import java.nio.ByteBuffer;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.ClingToCliffDebug;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClingToCliff.class, priority = 500, remap = false)
public abstract class ClingToCliffDebugMixin {
	@Inject(method = "canStart", at = @At("RETURN"), require = 0)
	private void epm$logCanStart(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer buffer, CallbackInfoReturnable<Boolean> callback) {
		ClingToCliffDebug.logCanStart(player, parkourability, stamina, epm$isGrabWallDown(), Boolean.TRUE.equals(callback.getReturnValue()));
	}

	@Inject(method = "canContinue", at = @At("RETURN"), require = 0)
	private void epm$logCanContinue(Player player, Parkourability parkourability, IStamina stamina, CallbackInfoReturnable<Boolean> callback) {
		ClingToCliffDebug.logCanContinue(player, parkourability, stamina, epm$isGrabWallDown(), Boolean.TRUE.equals(callback.getReturnValue()));
	}

	@Inject(method = "onWorkingTickInLocalClient", at = @At("TAIL"), require = 0)
	private void epm$restoreClingMoveClimbUpVelocity(Player player, Parkourability parkourability, IStamina stamina, CallbackInfo callback) {
		EPMClientHooks.restoreClingMoveClimbUpVelocity(player);
	}

	private static boolean epm$isGrabWallDown() {
		try {
			return KeyBindings.getKeyGrabWall().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
