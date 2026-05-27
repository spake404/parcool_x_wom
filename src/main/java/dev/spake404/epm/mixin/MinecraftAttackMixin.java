package dev.spake404.epm.mixin;

import dev.spake404.epm.EPMClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
	@Shadow
	public LocalPlayer player;

	@Inject(method = "startAttack", at = @At("HEAD"), require = 0)
	private void parcoolxwom$cancelWallJumpBeforeTaczAttack(CallbackInfoReturnable<Boolean> callback) {
		EPMClientHooks.cancelWallJumpForTaczAttackInput(this.player);
	}
}
