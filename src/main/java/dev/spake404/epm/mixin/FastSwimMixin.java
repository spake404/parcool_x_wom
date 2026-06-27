package dev.spake404.epm.mixin;

import dev.spake404.epm.aqua.AquaManeuvreFastSwimHandler;
import dev.spake404.epm.EPM;
import com.alrex.parcool.common.action.impl.FastSwim;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(value = FastSwim.class, remap = false)
public abstract class FastSwimMixin {
	@Inject(method = "canStart", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$disableParCoolFastSwimForAqua(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo, CallbackInfoReturnable<Boolean> callback) {
		if (AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}

	@Inject(method = "canContinue", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$stopParCoolFastSwimForAqua(Player player, Parkourability parkourability, IStamina stamina, CallbackInfoReturnable<Boolean> callback) {
		if (AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}
}
