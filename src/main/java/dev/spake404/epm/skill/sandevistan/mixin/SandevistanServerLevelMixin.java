package dev.spake404.epm.skill.sandevistan.mixin;

import dev.spake404.epm.skill.sandevistan.SandevistanManager;
import dev.spake404.epm.skill.sandevistan.SandevistanEntityTickClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class SandevistanServerLevelMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void epm$sandevistanSlowServerEntity(Entity entity, CallbackInfo callback) {
		double timeScale = SandevistanManager.timeScaleFor(entity);
		if (!SandevistanEntityTickClock.shouldTick(entity, timeScale)) {
			SandevistanEntityTickClock.advanceUnscaledDamageTimers(entity);
			callback.cancel();
		}
	}
}
