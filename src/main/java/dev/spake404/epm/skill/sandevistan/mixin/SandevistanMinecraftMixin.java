package dev.spake404.epm.skill.sandevistan.mixin;

import dev.spake404.epm.skill.sandevistan.client.SandevistanRenderTimeContext;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class SandevistanMinecraftMixin {
	@Inject(method = "getFrameTime", at = @At("HEAD"), cancellable = true, require = 0)
	private void epm$useSandevistanEntityRenderTime(CallbackInfoReturnable<Float> callback) {
		Float partialTick = SandevistanRenderTimeContext.currentPartialTick();
		if (partialTick != null) {
			callback.setReturnValue(partialTick);
		}
	}
}
