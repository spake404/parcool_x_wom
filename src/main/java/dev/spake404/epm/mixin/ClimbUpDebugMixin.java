package dev.spake404.epm.mixin;

import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.ClingToCliffDebug;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClimbUp.class, remap = false)
public abstract class ClimbUpDebugMixin {
	@Inject(method = "canStart", at = @At("RETURN"), require = 0)
	private void epm$logCanStart(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer buffer, CallbackInfoReturnable<Boolean> callback) {
		boolean result = Boolean.TRUE.equals(callback.getReturnValue());
		ClingToCliffDebug.logClimbUpCanStart(player, parkourability, stamina, result);
	}
}
