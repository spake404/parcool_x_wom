package dev.spake404.epm.skill.sandevistan.mixin;

import dev.spake404.epm.skill.sandevistan.client.SandevistanClientState;
import dev.spake404.epm.skill.sandevistan.client.SandevistanClientTickClock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class SandevistanClientLevelMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void epm$sandevistanSlowClientEntity(Entity entity, CallbackInfo callback) {
		int interval = SandevistanClientState.tickIntervalFor(entity);
		if (!SandevistanClientTickClock.beginTick(entity, interval)) {
			callback.cancel();
		}
	}

	@Inject(method = "tickNonPassenger", at = @At("TAIL"))
	private void epm$sandevistanFinishClientEntityTick(Entity entity, CallbackInfo callback) {
		SandevistanClientTickClock.endTick(entity);
	}
}
