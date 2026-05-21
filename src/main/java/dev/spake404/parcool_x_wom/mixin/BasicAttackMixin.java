package dev.spake404.parcool_x_wom.mixin;

import com.mojang.logging.LogUtils;
import dev.spake404.parcool_x_wom.PhantomAscentAirAttackState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Mixin(value = BasicAttack.class, remap = false)
public abstract class BasicAttackMixin {
	private static final Logger PARCOOLXWOM_LOGGER = LogUtils.getLogger();

	@Inject(method = "isExecutableState", at = @At("RETURN"), cancellable = true)
	private void parcoolxwom$allowPhantomAscentAirAttack(PlayerPatch<?> playerPatch, CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValueZ() && PhantomAscentAirAttackState.canUseBasicAttackAfterPhantomAscent(playerPatch)) {
			callback.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "executeOnServer", at = @At("HEAD"))
	private void parcoolxwom$logPhantomAscentAirAttackState(SkillContainer skillContainer, FriendlyByteBuf buffer, CallbackInfo callback) {
		ServerPlayerPatch serverPlayerPatch = skillContainer.getServerExecutor();
		if (serverPlayerPatch == null) {
			return;
		}

		Player player = serverPlayerPatch.getOriginal();
		Vec3 movement = player.getDeltaMovement();
		boolean hasSignal = PhantomAscentAirAttackState.hasAirAttackSignal(player);
		boolean hasProtectNextFall = PhantomAscentAirAttackState.hasProtectNextFall(serverPlayerPatch);
		boolean consumed = PhantomAscentAirAttackState.isAirAttackConsumed(player);
		boolean effectiveWindow = PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(serverPlayerPatch);
		boolean originalAirAttack = !player.onGround() && !player.isInWater() && movement.y() > -0.05D;
		double adjustedY = PhantomAscentAirAttackState.adjustAirAttackYVelocity(serverPlayerPatch, movement.y());
		boolean adjustedAirAttack = (!player.onGround() || effectiveWindow) && !player.isInWater() && adjustedY > -0.05D;

		PARCOOLXWOM_LOGGER.info("[ParCool x WOM] PhantomAscentAirAttack debug: hasSignal={}, protectNextFall={}, consumed={}, effectiveWindow={}, onGround={}, inWater={}, sprinting={}, deltaY={}, adjustedY={}, originalAirAttack={}, adjustedAirAttack={}",
				hasSignal,
				hasProtectNextFall,
				consumed,
				effectiveWindow,
				player.onGround(),
				player.isInWater(),
				player.isSprinting(),
				movement.y(),
				adjustedY,
				originalAirAttack,
				adjustedAirAttack);
	}

	@Redirect(
			method = "executeOnServer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"
			)
	)
	private boolean parcoolxwom$forcePhantomAscentAirAttackOffGround(Player player, SkillContainer skillContainer, FriendlyByteBuf buffer) {
		ServerPlayerPatch serverPlayerPatch = skillContainer.getServerExecutor();
		return !PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(serverPlayerPatch) && player.onGround();
	}

	@Redirect(
			method = "executeOnServer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/phys/Vec3;y()D"
			)
	)
	private double parcoolxwom$relaxPhantomAscentAirAttackYVelocity(Vec3 movement, SkillContainer skillContainer, FriendlyByteBuf buffer) {
		ServerPlayerPatch serverPlayerPatch = skillContainer.getServerExecutor();
		return PhantomAscentAirAttackState.adjustAirAttackYVelocity(serverPlayerPatch, movement.y());
	}

	@Inject(method = "executeOnServer", at = @At("RETURN"))
	private void parcoolxwom$consumePhantomAscentAirAttackWindow(SkillContainer skillContainer, FriendlyByteBuf buffer, CallbackInfo callback) {
		ServerPlayerPatch serverPlayerPatch = skillContainer.getServerExecutor();
		if (serverPlayerPatch != null && PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(serverPlayerPatch)) {
			PhantomAscentAirAttackState.consume(serverPlayerPatch.getOriginal());
		}
	}
}
