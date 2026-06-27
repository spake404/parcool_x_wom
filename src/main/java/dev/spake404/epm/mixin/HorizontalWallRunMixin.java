package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.wom.spider.WomSpiderWallRunModeGate;
import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.HorizontalWallRun;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HorizontalWallRun.class, remap = false)
public abstract class HorizontalWallRunMixin {
	@Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$replaceWithWomSpiderWallRun(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo, CallbackInfoReturnable<Boolean> cir) {
		if (WomSpiderWallRunModeGate.shouldDisableParCoolHorizontalWallRun(player)) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
