package dev.spake404.parcool_x_wom.mixin;

import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import dev.spake404.parcool_x_wom.MomentumAirAttackWindowState;
import dev.spake404.parcool_x_wom.ParcoolXWom;
import dev.spake404.parcool_x_wom.ParcoolXWomConfig;
import dev.spake404.parcool_x_wom.ParcoolXWomClientHooks;
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

		if (parcoolxwom$isParCoolActionDoing(CatLeap.class)) {
			callback.setReturnValue(ShootResult.UNKNOWN_FAIL);
			return;
		}

		if (!ParcoolXWomConfig.taczShootDuringWallJump()) {
			return;
		}

		if (ParcoolXWomClientHooks.isWallJumpActiveForTaczShoot(this.player)) {
			ParcoolXWomClientHooks.cancelWallJumpForTaczShoot(this.player);
			MomentumAirAttackWindowState.clearWallJumpWindow(this.player);
			parcoolxwom$wallJumpCanceledForShoot = true;
			clearTaczSprintTime(this.player == null ? null : IGunOperator.fromLivingEntity(this.player));
		}
	}

	@Inject(method = "shoot", at = @At("HEAD"), require = 0)
	private void parcoolxwom$rememberFastRunBeforeShoot(CallbackInfoReturnable<ShootResult> callback) {
		if (parcoolxwom$retryingAfterSprintStop || this.player == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null || !minecraft.options.keyAttack.isDown()) {
			return;
		}

		Parkourability parkourability = Parkourability.get(this.player);
		FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
		if (fastRun != null && fastRun.isDoing()) {
			ParcoolXWomClientHooks.rememberFastRunBeforeTaczShoot(this.player);
		}
	}

	@Inject(method = "shoot", at = @At("RETURN"), cancellable = true, require = 0)
	private void parcoolxwom$retryShootAfterParcoolSprintStop(CallbackInfoReturnable<ShootResult> callback) {
		ShootResult result = callback.getReturnValue();
		if (result != ShootResult.IS_SPRINTING || parcoolxwom$retryingAfterSprintStop) {
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
		Minecraft minecraft = Minecraft.getInstance();
		boolean attackDown = minecraft != null && minecraft.options != null && minecraft.options.keyAttack.isDown();

		if (!attackDown || (!fastRunDoing && !vanillaSprinting && !wallJumpCanceledForShoot)) {
			parcoolxwom$wallJumpCanceledForShoot = false;
			return;
		}

		if (fastRunDoing || vanillaSprinting) {
			ParcoolXWomClientHooks.suppressFastRunForTaczShoot(this.player, fastRunDoing);
		}
		clearTaczSprintTime(gunOperator);

		parcoolxwom$retryingAfterSprintStop = true;
		try {
			ShootResult retryResult = ((LocalPlayerShoot) (Object) this).shoot();
			callback.setReturnValue(retryResult);
			ParcoolXWom.LOGGER.debug(
					"[TaCZSprintShootFix] original={}, retry={}, taczSprintTime={}, parcoolFastRunDoing={}, vanillaSprinting={}",
					result,
					retryResult,
					Float.valueOf(taczSprintTime),
					Boolean.valueOf(fastRunDoing),
					Boolean.valueOf(vanillaSprinting || wallJumpCanceledForShoot));
		} finally {
			parcoolxwom$retryingAfterSprintStop = false;
			parcoolxwom$wallJumpCanceledForShoot = false;
		}
	}

	@Inject(method = "shoot", at = @At("RETURN"), require = 0)
	private void parcoolxwom$markRecentSuccessfulShoot(CallbackInfoReturnable<ShootResult> callback) {
		if (callback.getReturnValue() == ShootResult.SUCCESS) {
			ParcoolXWomClientHooks.markTaczShootActive(this.player);
		}
	}

	private boolean parcoolxwom$isParCoolActionDoing(Class<? extends Action> actionClass) {
		Action action = parcoolxwom$getParCoolAction(actionClass);
		return action != null && action.isDoing();
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

	private static void clearTaczSprintTime(IGunOperator gunOperator) {
		if (gunOperator == null || gunOperator.getDataHolder() == null) {
			return;
		}

		Object dataHolder = gunOperator.getDataHolder();
		setField(dataHolder, "sprintTimeS", Float.valueOf(0.0F));
	}

	private static void setField(Object target, String name, Object value) {
		try {
			var field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
}
