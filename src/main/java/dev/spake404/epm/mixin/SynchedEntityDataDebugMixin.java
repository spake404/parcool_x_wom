package dev.spake404.epm.mixin;

import java.util.List;

import dev.spake404.epm.RotationTraceDebug;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataDebugMixin {
	@Shadow
	@Final
	private Entity entity;

	@Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("HEAD"))
	private <T> void epm$traceSetStart(EntityDataAccessor<T> accessor, T value, boolean force, CallbackInfo callback) {
		RotationTraceDebug.synchedDataSetStart(this.entity, accessor, value, force);
	}

	@Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("TAIL"))
	private <T> void epm$traceSetEnd(EntityDataAccessor<T> accessor, T value, boolean force, CallbackInfo callback) {
		RotationTraceDebug.synchedDataSetEnd(this.entity, accessor, value, force);
	}

	@Inject(method = "assignValues", at = @At("HEAD"))
	private void epm$traceAssignValuesStart(List<SynchedEntityData.DataValue<?>> values, CallbackInfo callback) {
		RotationTraceDebug.synchedDataAssignValuesStart(this.entity, values);
	}

	@Inject(method = "assignValues", at = @At("TAIL"))
	private void epm$traceAssignValuesEnd(List<SynchedEntityData.DataValue<?>> values, CallbackInfo callback) {
		RotationTraceDebug.synchedDataAssignValuesEnd(this.entity, values);
	}
}
