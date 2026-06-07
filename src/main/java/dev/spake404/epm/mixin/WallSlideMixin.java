package dev.spake404.epm.mixin;

import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.WomSpiderWallRunModeGate;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WallSlide.class, remap = false)
public abstract class WallSlideMixin {
	@Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disableParCoolWallSlideInSpiderReplacementMode(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo, CallbackInfoReturnable<Boolean> cir) {
		if (WomSpiderWallRunModeGate.shouldDisableParCoolWallSlideAction(player)) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}

	@Inject(method = "canContinue", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$stopParCoolWallSlideInSpiderReplacementMode(Player player, Parkourability parkourability, IStamina stamina, CallbackInfoReturnable<Boolean> cir) {
		if (WomSpiderWallRunModeGate.shouldDisableParCoolWallSlideAction(player)) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
