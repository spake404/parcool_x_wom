package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WomSpiderWallJumpPriority {
	private WomSpiderWallJumpPriority() {
	}

	public static Decision resolve(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return Decision.unavailable("no_local_player");
		}

		Direction wallDirection = activeWallDirection(player);
		if (wallDirection == null) {
			wallDirection = WomSpiderWallContactResolver.detectAdjacentWallDirection(player);
		}
		if (wallDirection == null) {
			return Decision.unavailable("no_wall");
		}

		Vec3 look = horizontalLook(player);
		Vec3 wall = WomSpiderWallContactResolver.wallNormalDirection(wallDirection);
		if (look == null || wall.lengthSqr() < 1.0E-6D) {
			return Decision.unavailable("no_direction");
		}

		double dot = clamp(look.normalize().dot(wall.normalize()), -1.0D, 1.0D);
		double angle = Math.toDegrees(Math.acos(dot));
		double threshold = EPMConfig.spiderWallJumpWomFrontAngle();
		boolean frontFacing = angle <= threshold;
		Decision decision = new Decision(true, frontFacing, wallDirection, dot, angle, threshold, "resolved");
		log(player, decision);
		return decision;
	}

	public static boolean shouldPreferWom(Player player) {
		return resolve(player).preferWom();
	}

	private static Direction activeWallDirection(Player player) {
		Direction wallRunDirection = WomSpiderWallRunHandler.activeWallDirection(player);
		if (wallRunDirection != null) {
			return wallRunDirection;
		}
		return WomSpiderWallSlideHandler.activeWallDirection(player);
	}

	private static Vec3 horizontalLook(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 horizontal = new Vec3(look.x(), 0.0D, look.z());
		if (horizontal.lengthSqr() < 1.0E-6D) {
			horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
			horizontal = new Vec3(horizontal.x(), 0.0D, horizontal.z());
		}
		if (horizontal.lengthSqr() < 1.0E-6D) {
			return null;
		}
		return horizontal.normalize();
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static void log(Player player, Decision decision) {
		if (!EPMConfig.debugActionArbitrationState()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/WomWallJumpPriority] tick={} preferWom={} wallDirection={} lookDot={} angle={} threshold={} reason={} onGround={} delta={}",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(decision.preferWom()),
				decision.wallDirection() == null ? "none" : decision.wallDirection().getSerializedName(),
				Double.valueOf(decision.lookDot()),
				Double.valueOf(decision.angleDegrees()),
				Double.valueOf(decision.thresholdDegrees()),
				decision.reason(),
				Boolean.valueOf(player.onGround()),
				player.getDeltaMovement());
	}

	public record Decision(
			boolean available,
			boolean preferWom,
			Direction wallDirection,
			double lookDot,
			double angleDegrees,
			double thresholdDegrees,
			String reason) {
		private static Decision unavailable(String reason) {
			return new Decision(false, false, null, 0.0D, 180.0D, EPMConfig.spiderWallJumpWomFrontAngle(), reason);
		}
	}
}
