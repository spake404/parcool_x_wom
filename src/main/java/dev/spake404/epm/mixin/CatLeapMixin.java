package dev.spake404.epm.mixin;

import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.DemolitionLeapCatJumpHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CatLeap.class, remap = false)
public abstract class CatLeapMixin {
	@Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
	private void epm$demolitionLeapReplacesCatLeap(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer buffer, CallbackInfoReturnable<Boolean> callback) {
		if (DemolitionLeapCatJumpHandler.shouldSuppressParCool(player)) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}
}
