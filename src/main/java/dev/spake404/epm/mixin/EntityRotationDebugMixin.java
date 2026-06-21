package dev.spake404.epm.mixin;

import dev.spake404.epm.RotationTraceDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRotationDebugMixin {
	@Inject(method = "turn", at = @At("HEAD"))
	private void epm$traceTurnStart(double yawDeltaInput, double pitchDeltaInput, CallbackInfo callback) {
		RotationTraceDebug.entityTurnStart((Entity) (Object) this, yawDeltaInput, pitchDeltaInput);
	}

	@Inject(method = "turn", at = @At("TAIL"))
	private void epm$traceTurnEnd(double yawDeltaInput, double pitchDeltaInput, CallbackInfo callback) {
		RotationTraceDebug.entityTurnEnd((Entity) (Object) this, yawDeltaInput, pitchDeltaInput);
	}

	@Inject(method = "setYRot", at = @At("HEAD"))
	private void epm$traceSetYRotStart(float targetYaw, CallbackInfo callback) {
		RotationTraceDebug.setYRotStart((Entity) (Object) this, targetYaw);
	}

	@Inject(method = "setYRot", at = @At("TAIL"))
	private void epm$traceSetYRotEnd(float targetYaw, CallbackInfo callback) {
		RotationTraceDebug.setYRotEnd((Entity) (Object) this, targetYaw);
	}

	@Inject(method = "setXRot", at = @At("HEAD"))
	private void epm$traceSetXRotStart(float targetPitch, CallbackInfo callback) {
		RotationTraceDebug.setXRotStart((Entity) (Object) this, targetPitch);
	}

	@Inject(method = "setXRot", at = @At("TAIL"))
	private void epm$traceSetXRotEnd(float targetPitch, CallbackInfo callback) {
		RotationTraceDebug.setXRotEnd((Entity) (Object) this, targetPitch);
	}

	@Inject(method = "absMoveTo(DDDFF)V", at = @At("HEAD"))
	private void epm$traceAbsMoveToStart(double x, double y, double z, float yaw, float pitch, CallbackInfo callback) {
		RotationTraceDebug.absMoveToStart((Entity) (Object) this, x, y, z, yaw, pitch);
	}

	@Inject(method = "absMoveTo(DDDFF)V", at = @At("TAIL"))
	private void epm$traceAbsMoveToEnd(double x, double y, double z, float yaw, float pitch, CallbackInfo callback) {
		RotationTraceDebug.absMoveToEnd((Entity) (Object) this, x, y, z, yaw, pitch);
	}

	@Inject(method = "moveTo(DDDFF)V", at = @At("HEAD"))
	private void epm$traceMoveToStart(double x, double y, double z, float yaw, float pitch, CallbackInfo callback) {
		RotationTraceDebug.moveToStart((Entity) (Object) this, x, y, z, yaw, pitch);
	}

	@Inject(method = "moveTo(DDDFF)V", at = @At("TAIL"))
	private void epm$traceMoveToEnd(double x, double y, double z, float yaw, float pitch, CallbackInfo callback) {
		RotationTraceDebug.moveToEnd((Entity) (Object) this, x, y, z, yaw, pitch);
	}

	@Inject(method = "setPose", at = @At("HEAD"))
	private void epm$traceSetPoseStart(Pose pose, CallbackInfo callback) {
		RotationTraceDebug.setPoseStart((Entity) (Object) this, pose);
	}

	@Inject(method = "setPose", at = @At("TAIL"))
	private void epm$traceSetPoseEnd(Pose pose, CallbackInfo callback) {
		RotationTraceDebug.setPoseEnd((Entity) (Object) this, pose);
	}
}
