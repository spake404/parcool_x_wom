package dev.spake404.epm.mixin;

import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMConfig;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.EPMParCoolGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public abstract class LocalPlayerShootMixin {
	@Shadow
	private LocalPlayer player;
	private static boolean parcoolxwom$retryingAfterSprintStop;
	private static boolean parcoolxwom$wallJumpCanceledForShoot;

	@Inject(method = "shoot", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$handleParCoolShootState(CallbackInfoReturnable<ShootResult> callback) {
		if (parcoolxwom$retryingAfterSprintStop) {
			return;
		}

		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		parcoolxwom$logShootState("head", null, false);

		if (parcoolxwom$isParCoolActionDoing(CatLeap.class)) {
			callback.setReturnValue(ShootResult.UNKNOWN_FAIL);
			parcoolxwom$logShootState("head_cat_leap_cancel", ShootResult.UNKNOWN_FAIL, false);
			return;
		}

		if (!EPMConfig.taczShootDuringWallJump()) {
			return;
		}

		if (EPMClientHooks.cancelWallJumpForTaczAttackInput(this.player)) {
			parcoolxwom$wallJumpCanceledForShoot = true;
			clearTaczSprintState(this.player, this.player == null ? null : IGunOperator.fromLivingEntity(this.player));
		}
	}

	@Inject(method = "shoot", at = @At("HEAD"), require = 0)
	private void parcoolxwom$rememberFastRunBeforeShoot(CallbackInfoReturnable<ShootResult> callback) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| parcoolxwom$retryingAfterSprintStop
				|| this.player == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null || !minecraft.options.keyAttack.isDown()) {
			return;
		}

		Parkourability parkourability = Parkourability.get(this.player);
		FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
		if (fastRun != null && fastRun.isDoing()) {
			EPMClientHooks.rememberFastRunBeforeTaczShoot(this.player);
		}
	}

	@Inject(method = "shoot", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$retryShootAfterParcoolSprintStop(CallbackInfoReturnable<ShootResult> callback) {
		ShootResult result = callback.getReturnValue();
		parcoolxwom$logShootState("return", result, false);
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| result != ShootResult.IS_SPRINTING
				|| parcoolxwom$retryingAfterSprintStop) {
			if (!parcoolxwom$retryingAfterSprintStop) {
				parcoolxwom$wallJumpCanceledForShoot = false;
			}
			return;
		}

		IGunOperator gunOperator = this.player == null ? null : IGunOperator.fromLivingEntity(this.player);
		float taczSprintTime = gunOperator == null ? -1.0F : gunOperator.getSynSprintTime();
		Parkourability parkourability = this.player == null ? null : Parkourability.get(this.player);
		FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
		boolean fastRunDoing = fastRun != null && fastRun.isDoing();
		boolean wallJumpCanceledForShoot = parcoolxwom$wallJumpCanceledForShoot;
		boolean vanillaSprinting = this.player != null && this.player.isSprinting();
		boolean taczFastRunHandoffActive = EPMClientHooks.isTaczShootFastRunHandoffActive(this.player);
		Minecraft minecraft = Minecraft.getInstance();
		boolean attackDown = minecraft != null && minecraft.options != null && minecraft.options.keyAttack.isDown();

		if (!attackDown || (!fastRunDoing && !vanillaSprinting && !wallJumpCanceledForShoot && !taczFastRunHandoffActive)) {
			parcoolxwom$logShootState("return_no_retry_context", result, false);
			parcoolxwom$wallJumpCanceledForShoot = false;
			return;
		}

		if (fastRunDoing || vanillaSprinting) {
			EPMClientHooks.suppressFastRunForTaczShoot(this.player, fastRunDoing);
		}
		clearTaczSprintState(this.player, gunOperator);
		parcoolxwom$logShootState("retry_before", result, true);

		parcoolxwom$retryingAfterSprintStop = true;
		try {
			ShootResult retryResult = ((LocalPlayerShoot) (Object) this).shoot();
			callback.setReturnValue(retryResult);
			parcoolxwom$logShootState("retry_after", retryResult, true);
		} finally {
			parcoolxwom$retryingAfterSprintStop = false;
			parcoolxwom$wallJumpCanceledForShoot = false;
		}
	}

	@Inject(method = "shoot", at = @At("RETURN"), require = 0)
	private void parcoolxwom$markRecentSuccessfulShoot(CallbackInfoReturnable<ShootResult> callback) {
		if (EPMParCoolGate.allowCrossModSkillCompat() && callback.getReturnValue() == ShootResult.SUCCESS) {
			EPMClientHooks.markTaczShootActive(this.player);
		}
	}

	private boolean parcoolxwom$isParCoolActionDoing(Class<? extends Action> actionClass) {
		Action action = parcoolxwom$getParCoolAction(actionClass);
		return action != null && action.isDoing();
	}

	private void parcoolxwom$logShootState(String phase, ShootResult result, boolean retryPhase) {
		if (!EPMConfig.debugGliderState() || this.player == null || !this.player.isLocalPlayer()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		boolean attackDown = minecraft != null && minecraft.options != null && minecraft.options.keyAttack.isDown();
		Parkourability parkourability = Parkourability.get(this.player);
		FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
		boolean fastRunDoing = fastRun != null && fastRun.isDoing();
		boolean vanillaSprinting = this.player.isSprinting();
		boolean taczFastRunHandoffActive = EPMClientHooks.isTaczShootFastRunHandoffActive(this.player);
		IGunOperator gunOperator = IGunOperator.fromLivingEntity(this.player);
		float taczSprintTime = gunOperator == null ? -1.0F : gunOperator.getSynSprintTime();
		if (!attackDown
				&& !fastRunDoing
				&& !vanillaSprinting
				&& !parcoolxwom$wallJumpCanceledForShoot
				&& !taczFastRunHandoffActive
				&& result != ShootResult.IS_SPRINTING
				&& result != ShootResult.SUCCESS) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/TaCZShoot] phase={} result={} retrying={} retryPhase={} wallJumpCanceled={} attackDown={} fastRunDoing={} vanillaSprinting={} handoffActive={} taczSprintTime={} playerTick={} pos=({}, {}, {}) delta=({}, {}, {})",
				phase,
				result,
				Boolean.valueOf(parcoolxwom$retryingAfterSprintStop),
				Boolean.valueOf(retryPhase),
				Boolean.valueOf(parcoolxwom$wallJumpCanceledForShoot),
				Boolean.valueOf(attackDown),
				Boolean.valueOf(fastRunDoing),
				Boolean.valueOf(vanillaSprinting),
				Boolean.valueOf(taczFastRunHandoffActive),
				Float.valueOf(taczSprintTime),
				Integer.valueOf(this.player.tickCount),
				Double.valueOf(this.player.getX()),
				Double.valueOf(this.player.getY()),
				Double.valueOf(this.player.getZ()),
				Double.valueOf(this.player.getDeltaMovement().x),
				Double.valueOf(this.player.getDeltaMovement().y),
				Double.valueOf(this.player.getDeltaMovement().z));
	}

	private <T extends Action> T parcoolxwom$getParCoolAction(Class<T> actionClass) {
		if (this.player == null) {
			return null;
		}

		try {
			Parkourability parkourability = Parkourability.get(this.player);
			return parkourability == null ? null : parkourability.get(actionClass);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static void clearTaczSprintState(LocalPlayer player, IGunOperator gunOperator) {
		if (gunOperator != null) {
			ShooterDataHolder dataHolder = gunOperator.getDataHolder();
			if (dataHolder != null) {
				dataHolder.sprintTimeS = 0.0F;
			}
		}

		if (player != null) {
			ModSyncedEntityData.SPRINT_TIME_KEY.setValue(player, Float.valueOf(0.0F));
		}
	}
}
