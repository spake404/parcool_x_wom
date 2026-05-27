package dev.spake404.epm.mixin;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import java.nio.ByteBuffer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Dodge.class, remap = false)
public abstract class DodgeMixin {
	@Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disableDodge(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer buffer,
			CallbackInfoReturnable<Boolean> callback) {
		callback.setReturnValue(false);
	}
}
