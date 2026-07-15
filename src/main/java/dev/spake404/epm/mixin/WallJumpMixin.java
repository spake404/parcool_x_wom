package dev.spake404.epm.mixin;

import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.walljump.WomParCoolWallJumpBridge;
import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WallJump.class, remap = false)
public abstract class WallJumpMixin {
	@Inject(method = "checkCanStart", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$blockParCoolWallJumpAfterPhantom(Player player, Parkourability parkourability, IStamina stamina,
			ByteBuffer startInfo, CallbackInfoReturnable<Boolean> cir) {
		if (EPMParCoolGate.allowCrossModSkillCompat()
				&& WomParCoolWallJumpBridge.shouldBlockAfterPhantom(player, "wall_jump_check_can_start")) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}

	@Inject(method = "checkCanStart", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$allowParCoolWallJumpFromWomSideWallRun(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo, CallbackInfoReturnable<Boolean> cir) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (cir.getReturnValueZ()) {
			WomParCoolWallJumpBridge.markNativeStartCandidate((WallJump) (Object) this, player, parkourability, stamina, startInfo);
		} else if (WomParCoolWallJumpBridge.writeFallbackStartInfo((WallJump) (Object) this, player, parkourability, stamina, startInfo)) {
			cir.setReturnValue(Boolean.TRUE);
		}

		if (cir.getReturnValueZ()
				&& !WomParCoolWallJumpBridge.claimParCoolWallJump(player, "parcool_wall_jump_can_start")) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}

	@Inject(method = "onStartInLocalClient", at = @At("HEAD"), require = 0)
	private void parcoolxwom$releaseWomSideWallRunOnParCoolWallJump(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startData, CallbackInfo ci) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		WomParCoolWallJumpBridge.onWallJumpStarted(player, startData);
	}
}
