package dev.spake404.epm.mixin;

import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.WomParCoolWallJumpBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WallJump.class, remap = false)
public abstract class WallJumpMixin {
	@Inject(method = "checkCanStart", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$allowParCoolWallJumpFromWomSideWallRun(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()
				&& WomParCoolWallJumpBridge.writeFallbackStartInfo((WallJump) (Object) this, player, parkourability, stamina, startInfo)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "onStartInLocalClient", at = @At("HEAD"), require = 0)
	private void parcoolxwom$releaseWomSideWallRunOnParCoolWallJump(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startData, CallbackInfo ci) {
		WomParCoolWallJumpBridge.onWallJumpStarted(player, startData);
	}
}
