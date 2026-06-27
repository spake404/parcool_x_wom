package dev.spake404.epm.mixin;

import dev.spake404.epm.debug.RotationTraceDebug;
import dev.spake404.epm.EPM;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerRotationDebugMixin {
	@Inject(method = "onMove", at = @At("HEAD"))
	private void epm$traceMouseMoveStart(long window, double xpos, double ypos, CallbackInfo callback) {
		RotationTraceDebug.mouseMoveStart((MouseHandler) (Object) this, window, xpos, ypos);
	}

	@Inject(method = "onMove", at = @At("TAIL"))
	private void epm$traceMouseMoveEnd(long window, double xpos, double ypos, CallbackInfo callback) {
		RotationTraceDebug.mouseMoveEnd((MouseHandler) (Object) this, window, xpos, ypos);
	}

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void epm$traceMouseTurnStart(CallbackInfo callback) {
		RotationTraceDebug.mouseTurnStart((MouseHandler) (Object) this);
	}

	@Inject(method = "turnPlayer", at = @At("TAIL"))
	private void epm$traceMouseTurnEnd(CallbackInfo callback) {
		RotationTraceDebug.mouseTurnEnd((MouseHandler) (Object) this);
	}
}
