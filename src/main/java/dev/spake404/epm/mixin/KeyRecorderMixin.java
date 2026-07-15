package dev.spake404.epm.mixin;

import com.alrex.parcool.client.input.KeyRecorder;
import dev.spake404.epm.EPMClientHooks;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyRecorder.class, remap = false)
public abstract class KeyRecorderMixin {
	@Inject(method = "onClientTick", at = @At("HEAD"), require = 0)
	private static void parcoolxwom$logKeyRecorderHead(MovementInputUpdateEvent event, CallbackInfo callback) {
		EPMClientHooks.logParCoolKeyRecorderOrder(event, "key_recorder_head");
	}

	@Inject(method = "onClientTick", at = @At("RETURN"), require = 0)
	private static void parcoolxwom$logKeyRecorderReturn(MovementInputUpdateEvent event, CallbackInfo callback) {
		EPMClientHooks.logParCoolKeyRecorderOrder(event, "key_recorder_return");
	}
}
