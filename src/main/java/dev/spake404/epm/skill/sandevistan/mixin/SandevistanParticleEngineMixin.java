package dev.spake404.epm.skill.sandevistan.mixin;

import dev.spake404.epm.skill.sandevistan.client.SandevistanClientState;
import dev.spake404.epm.skill.sandevistan.client.SandevistanParticleTickClock;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class SandevistanParticleEngineMixin {
	@Shadow
	@Nullable
	private ClientLevel level;

	@Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true, require = 0)
	private void epm$beginParticleTick(Particle particle, CallbackInfo callbackInfo) {
		if (!epm$shouldTick(particle)) {
			callbackInfo.cancel();
		}
	}

	@Inject(method = "tickParticle", at = @At("RETURN"), require = 0)
	private void epm$endParticleTick(Particle particle, CallbackInfo callbackInfo) {
		SandevistanParticleTickClock.endTick(particle, this.level);
	}

	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/particle/TrackingEmitter;tick()V"),
			require = 0)
	private void epm$tickTrackingEmitter(TrackingEmitter emitter) {
		if (epm$shouldTick(emitter)) {
			emitter.tick();
			SandevistanParticleTickClock.endTick(emitter, this.level);
		}
	}

	@Inject(method = "setLevel", at = @At("HEAD"), require = 0)
	private void epm$clearParticleClocks(ClientLevel level, CallbackInfo callbackInfo) {
		SandevistanParticleTickClock.clear();
	}

	private boolean epm$shouldTick(Particle particle) {
		if (SandevistanClientState.isSandevistanAfterimage(particle) || this.level == null) {
			return true;
		}

		Vec3 position = particle.getPos();
		double timeScale = SandevistanClientState.timeScaleAt(
				this.level,
				position.x,
				position.y,
				position.z);
		return SandevistanParticleTickClock.beginTick(particle, timeScale);
	}
}
