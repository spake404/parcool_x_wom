package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WomSpiderWallCornerTransfer {
	private static final double CORNER_TRANSFER_DOT = 0.65D;

	private WomSpiderWallCornerTransfer() {
	}

	public static Direction findAdjacentCornerWall(Player player, Direction activeWall, int womSide) {
		if (player == null || activeWall == null || womSide == 0) {
			return null;
		}

		Vec3 runDirection = targetRunDirection(activeWall, womSide);
		Direction bestDirection = null;
		double bestScore = CORNER_TRANSFER_DOT;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (!WomSpiderWallContactResolver.hasAdjacentWallDirection(player, direction)) {
				continue;
			}
			if (!canTransferTo(activeWall, runDirection, direction)) {
				continue;
			}

			double score = WomSpiderWallContactResolver.wallNormalDirection(direction).dot(runDirection);
			if (score > bestScore) {
				bestScore = score;
				bestDirection = direction;
			}
		}
		return bestDirection;
	}

	public static boolean canTransferTo(Direction activeWall, Vec3 runDirection, Direction candidateWall) {
		if (activeWall == null || runDirection == null || candidateWall == null) {
			return false;
		}
		if (candidateWall == activeWall || candidateWall == activeWall.getOpposite()) {
			return false;
		}

		return WomSpiderWallContactResolver.wallNormalDirection(candidateWall).dot(runDirection) > CORNER_TRANSFER_DOT;
	}

	public static Vec3 targetRunDirection(Direction wallDirection, int womSide) {
		Vec3 wallRight = WomSpiderWallContactResolver.rightSideDirection(wallDirection);
		return womSide < 0 ? wallRight : wallRight.reverse();
	}

	public static Direction directionFromWallVector(Vec3 wallDirection) {
		if (wallDirection == null) {
			return null;
		}

		double absX = Math.abs(wallDirection.x());
		double absZ = Math.abs(wallDirection.z());
		if (absX < 1.0E-6D && absZ < 1.0E-6D) {
			return null;
		}
		if (absX > absZ) {
			return wallDirection.x() > 0.0D ? Direction.EAST : Direction.WEST;
		}
		return wallDirection.z() > 0.0D ? Direction.SOUTH : Direction.NORTH;
	}

	public static Vec3 wallVector(Direction direction) {
		return Vec3.atLowerCornerOf(direction.getNormal());
	}
}
