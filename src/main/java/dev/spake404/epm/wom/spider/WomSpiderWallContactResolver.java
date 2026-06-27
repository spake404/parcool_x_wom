package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WomSpiderWallContactResolver {
	private WomSpiderWallContactResolver() {
	}

	public static Direction detectAdjacentWallDirection(Player player) {
		if (player == null) {
			return null;
		}

		return detectAdjacentWallDirection(player, horizontalLook(player));
	}

	public static Direction detectAdjacentWallDirection(Player player, float yaw) {
		if (player == null) {
			return null;
		}

		Vec3 facing = Vec3.directionFromRotation(0.0F, yaw);
		Vec3 horizontalFacing = new Vec3(facing.x(), 0.0D, facing.z());
		if (horizontalFacing.lengthSqr() < 1.0E-6D) {
			return detectAdjacentWallDirection(player);
		}
		return detectAdjacentWallDirection(player, horizontalFacing.normalize());
	}

	private static Direction detectAdjacentWallDirection(Player player, Vec3 preferredNormal) {
		Direction bestDirection = null;
		double bestScore = Double.MAX_VALUE;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			AABB probeBox = probeBox(player.getBoundingBox(), direction);
			boolean collided = !player.level().noCollision(player, probeBox);
			if (!collided) {
				continue;
			}

			Vec3 normal = wallNormal(direction);
			double score = 1.0D - preferredNormal.dot(normal);
			if (score < bestScore) {
				bestScore = score;
				bestDirection = direction;
			}
		}

		return bestDirection;
	}

	public static boolean hasAdjacentWallDirection(Player player, Direction direction) {
		if (player == null || direction == null) {
			return false;
		}

		return !player.level().noCollision(player, probeBox(player.getBoundingBox(), direction));
	}

	public static float wallFacingYaw(Player player, Direction wallDirection) {
		if (wallDirection == null) {
			return player == null ? 0.0F : player.yBodyRot;
		}

		Vec3 normal = wallNormal(wallDirection);
		return (float) Math.toDegrees(Math.atan2(-normal.x(), normal.z()));
	}

	public static Vec3 rightSideDirection(Direction wallDirection) {
		Vec3 normal = wallNormal(wallDirection);
		return new Vec3(-normal.z(), 0.0D, normal.x());
	}

	public static Vec3 wallNormalDirection(Direction wallDirection) {
		return wallNormal(wallDirection);
	}

	private static Vec3 horizontalLook(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
		if (horizontalLook.lengthSqr() < 1.0E-6D) {
			horizontalLook = Vec3.directionFromRotation(0.0F, player.getYRot());
			horizontalLook = new Vec3(horizontalLook.x(), 0.0D, horizontalLook.z());
		}
		return horizontalLook.normalize();
	}

	private static Vec3 wallNormal(Direction wallDirection) {
		return Vec3.atLowerCornerOf(wallDirection.getNormal());
	}

	private static AABB probeBox(AABB box, Direction direction) {
		double reach = 0.62D;
		double thickness = 0.06D;
		double minY = box.minY + 0.05D;
		double maxY = box.maxY - 0.05D;
		return switch (direction) {
			case NORTH -> new AABB(box.minX, minY, box.minZ - reach, box.maxX, maxY, box.minZ + thickness);
			case SOUTH -> new AABB(box.minX, minY, box.maxZ - thickness, box.maxX, maxY, box.maxZ + reach);
			case WEST -> new AABB(box.minX - reach, minY, box.minZ, box.minX + thickness, maxY, box.maxZ);
			case EAST -> new AABB(box.maxX - thickness, minY, box.minZ, box.maxX + reach, maxY, box.maxZ);
			default -> box;
		};
	}
}
