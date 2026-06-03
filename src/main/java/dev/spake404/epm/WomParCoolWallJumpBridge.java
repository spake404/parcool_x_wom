package dev.spake404.epm;

import java.nio.ByteBuffer;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.RideZipline;
import com.alrex.parcool.common.action.impl.VerticalWallRun;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomParCoolWallJumpBridge {
	private static final int LOG_INTERVAL_TICKS = 5;
	private static final WeakHashMap<Player, Integer> LAST_LOG_TICKS = new WeakHashMap<>();

	private WomParCoolWallJumpBridge() {
	}

	public static boolean writeFallbackStartInfo(WallJump wallJump, Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)
				|| wallJump == null
				|| parkourability == null
				|| stamina == null
				|| startInfo == null) {
			return false;
		}
		if (!isWallJumpInputDone()) {
			return false;
		}
		if (!isWomSideWallRunState(player, playerPatch)) {
			return false;
		}
		if (!canUseParCoolWallJump(player, parkourability, stamina, wallJump)) {
			return false;
		}

		Direction wall = wallJumpWallDirection(player, playerPatch);
		if (wall == null || !WomSpiderWallContactResolver.hasAdjacentWallDirection(player, wall)) {
			return false;
		}

		Vec3 wallDirection = WomSpiderWallContactResolver.wallNormalDirection(wall).normalize();
		Vec3 jumpDirection = jumpDirection(player, wallDirection);
		if (jumpDirection == null || startInfo.remaining() < 41) {
			return false;
		}

		startInfo.putDouble(jumpDirection.x())
				.putDouble(jumpDirection.y())
				.putDouble(jumpDirection.z())
				.putDouble(wallDirection.x())
				.putDouble(wallDirection.z())
				.put(wallJumpAnimationType(wallDirection, jumpDirection));
		logFallback(player, wall, wallDirection, jumpDirection);
		return true;
	}

	public static void onWallJumpStarted(Player player, ByteBuffer startData) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			return;
		}
		if (!isWomSideWallRunState(player, playerPatch)) {
			return;
		}

		byte animationType = wallJumpAnimationType(startData);
		Direction activeWall = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
		WomOriginalSpiderWallRunDirectionFix.releaseSideWallForParCoolWallJump(player, playerPatch);
		playEpicParCoolWallJumpAnimation(playerPatch, animationType);
		logStarted(player, activeWall, animationType);
	}

	private static boolean isWomSideWallRunState(Player player, PlayerPatch<?> playerPatch) {
		if (player == null || playerPatch == null || WomCompatBridge.instance().isSpiderWallGlideActive(playerPatch)) {
			return false;
		}

		Direction activeWall = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
		if (activeWall != null && WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall)
				&& WomCompatBridge.instance().isSpiderWallMovementActive(playerPatch)) {
			return true;
		}
		return WomAnimationRefs.isAny(
				currentBaseAnimation(playerPatch),
				WomAnimationRefs.wallRunLeftSide(),
				WomAnimationRefs.wallRunRightSide());
	}

	private static Direction wallJumpWallDirection(Player player, PlayerPatch<?> playerPatch) {
		Direction activeWall = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
		if (activeWall != null && WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall)) {
			return activeWall;
		}
		return WomSpiderWallContactResolver.detectAdjacentWallDirection(player, modelYaw(player, playerPatch));
	}

	private static Vec3 jumpDirection(Player player, Vec3 wallDirection) {
		Vec3 look = player.getLookAngle();
		Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
		Vec3 awayFromWall = wallDirection.reverse();
		if (horizontalLook.lengthSqr() < 1.0E-6D) {
			horizontalLook = awayFromWall;
		} else {
			horizontalLook = horizontalLook.normalize();
			double intoWall = Math.max(0.0D, horizontalLook.dot(wallDirection));
			if (intoWall > 0.0D) {
				horizontalLook = horizontalLook.subtract(wallDirection.scale(intoWall));
				horizontalLook = horizontalLook.lengthSqr() < 1.0E-6D ? awayFromWall : horizontalLook.normalize();
			}
		}

		Vec3 horizontalJump = horizontalLook.add(awayFromWall.scale(0.7D));
		if (horizontalJump.lengthSqr() < 1.0E-6D) {
			horizontalJump = awayFromWall;
		}

		double lookY = look.normalize().y();
		double yBoost = lookY > 0.5D ? lookY * 2.0D : 1.0D;
		return horizontalJump.normalize().add(0.0D, yBoost, 0.0D).normalize();
	}

	private static boolean canUseParCoolWallJump(Player player, Parkourability parkourability, IStamina stamina, WallJump wallJump) {
		ClingToCliff cling = parkourability.get(ClingToCliff.class);
		return !stamina.isExhausted()
				&& !player.onGround()
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& !player.getAbilities().flying
				&& parkourability.getAdditionalProperties().getNotCreativeFlyingTick() > 10
				&& (!cling.isDoing() && cling.getNotDoingTick() > 3 || cling.isDoing() && cling.getFacingDirection() != ClingToCliff.FacingDirection.ToWall)
				&& !parkourability.get(Crawl.class).isDoing()
				&& !parkourability.get(VerticalWallRun.class).isDoing()
				&& !parkourability.get(RideZipline.class).isDoing()
				&& parkourability.getAdditionalProperties().getNotLandingTick() > 4
				&& !isWallJumpInCooldown(wallJump, parkourability);
	}

	private static boolean isWallJumpInCooldown(WallJump wallJump, Parkourability parkourability) {
		return (parkourability.getClientInfo().get(ParCoolConfig.Client.Booleans.EnableWallJumpCooldown)
				|| !parkourability.getServerLimitation().get(ParCoolConfig.Server.Booleans.AllowDisableWallJumpCooldown))
				&& wallJump.getNotDoingTick() <= 8;
	}

	private static boolean isWallJumpInputDone() {
		try {
			WallJump.ControlType control = (WallJump.ControlType) ParCoolConfig.Client.WallJumpControl.get();
			return control == WallJump.ControlType.PressKey && KeyRecorder.keyWallJump.isPressed()
					|| control == WallJump.ControlType.ReleaseKey && KeyRecorder.keyWallJump.isReleased();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static byte wallJumpAnimationType(Vec3 wallDirection, Vec3 jumpDirection) {
		Vec3 horizontalJump = new Vec3(jumpDirection.x(), 0.0D, jumpDirection.z());
		if (horizontalJump.lengthSqr() < 1.0E-6D) {
			return 2;
		}
		horizontalJump = horizontalJump.normalize();
		Vec3 divided = new Vec3(
				wallDirection.x() * horizontalJump.x() + wallDirection.z() * horizontalJump.z(),
				0.0D,
				-wallDirection.x() * horizontalJump.z() + wallDirection.z() * horizontalJump.x()).normalize();
		return (byte) (divided.z() > 0.0D ? 1 : 2);
	}

	private static byte wallJumpAnimationType(ByteBuffer startData) {
		if (startData == null) {
			return 2;
		}

		try {
			ByteBuffer copy = startData.asReadOnlyBuffer();
			copy.getDouble();
			copy.getDouble();
			copy.getDouble();
			copy.getDouble();
			copy.getDouble();
			byte animationType = copy.get();
			return animationType == 1 || animationType == 2 ? animationType : 2;
		} catch (RuntimeException ignored) {
			return 2;
		}
	}

	private static void playEpicParCoolWallJumpAnimation(PlayerPatch<?> playerPatch, byte animationType) {
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch) || !playerPatch.isLogicalClient() || !playerPatch.isEpicFightMode()) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = animationType == 1
				? WomAnimationRefs.epicParCoolWallJumpLeft()
				: WomAnimationRefs.epicParCoolWallJumpRight();
		if (animation == null) {
			return;
		}

		try {
			localPlayerPatch.playAnimationInClientSide(animation, 0.0F);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static float modelYaw(Player player, PlayerPatch<?> playerPatch) {
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			return localPlayerPatch.getModelYRot();
		}
		return player == null ? 0.0F : player.yBodyRot;
	}

	private static void logFallback(Player player, Direction wall, Vec3 wallDirection, Vec3 jumpDirection) {
		if (!EPM.LOGGER.isDebugEnabled() || !shouldLog(player)) {
			return;
		}
		EPM.LOGGER.debug("[WomParCoolWallJump] fallback wall={} wallDirection={} jumpDirection={} delta={} state={}",
				wall,
				wallDirection,
				jumpDirection,
				player.getDeltaMovement(),
				WomCompatBridge.instance().describeSpiderTechniquesState(EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class)));
	}

	private static void logStarted(Player player, Direction activeWall, byte animationType) {
		if (!EPM.LOGGER.isDebugEnabled() || !shouldLog(player)) {
			return;
		}
		EPM.LOGGER.debug("[WomParCoolWallJump] started animationType={} activeWall={} delta={}",
				Byte.valueOf(animationType),
				activeWall,
				player.getDeltaMovement());
	}

	private static boolean shouldLog(Player player) {
		if (player == null) {
			return false;
		}

		Integer previousTick = LAST_LOG_TICKS.get(player);
		if (previousTick != null && player.tickCount - previousTick.intValue() < LOG_INTERVAL_TICKS) {
			return false;
		}

		LAST_LOG_TICKS.put(player, Integer.valueOf(player.tickCount));
		return true;
	}
}
