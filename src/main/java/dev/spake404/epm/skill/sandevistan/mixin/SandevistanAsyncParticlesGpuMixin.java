package dev.spake404.epm.skill.sandevistan.mixin;

import dev.spake404.epm.skill.sandevistan.client.compat.asyncparticles.SandevistanAsyncParticlesGpuCompat;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextureSheetParticle.class, priority = 900)
public abstract class SandevistanAsyncParticlesGpuMixin {
	@Dynamic("Added by AsyncParticles")
	@Inject(
			method = "asyncparticles$postTick(J)V",
			at = @At("RETURN"),
			require = 0,
			remap = false)
	private void epm$warpAsyncParticlesGpuBuffer(long address, CallbackInfo callbackInfo) {
		SandevistanAsyncParticlesGpuCompat.warpParticleBuffer(
				(TextureSheetParticle)(Object)this,
				address);
	}
}
