package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.wom.spider.WomSpiderWallRunModeGate;
import com.alrex.parcool.client.input.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	@Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disableParCoolHorizontalWallRunKeyDown(CallbackInfoReturnable<Boolean> callback) {
		if (parcoolxwom$shouldDisableParCoolKey()) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}

	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	private void parcoolxwom$disableParCoolHorizontalWallRunClick(CallbackInfoReturnable<Boolean> callback) {
		if (parcoolxwom$shouldDisableParCoolKey()) {
			callback.setReturnValue(Boolean.FALSE);
		}
	}

	private boolean parcoolxwom$shouldDisableParCoolKey() {
		boolean horizontalWallRunKey = (Object) this == KeyBindings.getKeyHorizontalWallRun();
		boolean wallSlideKey = (Object) this == KeyBindings.getKeyWallSlide();
		if (!horizontalWallRunKey && !wallSlideKey) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		if (horizontalWallRunKey && WomSpiderWallRunModeGate.shouldDisableParCoolHorizontalWallRunKey(player)) {
			return true;
		}
		return wallSlideKey && WomSpiderWallRunModeGate.shouldDisableParCoolWallSlide(player);
	}
}
