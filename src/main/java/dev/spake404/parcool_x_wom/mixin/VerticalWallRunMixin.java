package dev.spake404.parcool_x_wom.mixin;

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alrex.parcool.common.action.impl.VerticalWallRun;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.parcool_x_wom.ParcoolXWomConfig;
import dev.spake404.parcool_x_wom.SpiderTechniquesState;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = VerticalWallRun.class, remap = false)
public abstract class VerticalWallRunMixin {
	@Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disableWhenSpiderTechniquesKnown(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer buffer, CallbackInfoReturnable<Boolean> cir) {
		if (!ParcoolXWomConfig.disableVerticalWallRunWithSpiderTechniques()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (SpiderTechniquesState.hasSpiderTechniques(playerPatch)) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
