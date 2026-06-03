package dev.spake404.epm;

import java.util.WeakHashMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallYawLock {
	private static final WeakHashMap<Player, Float> BODY_YAWS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Direction> BODY_WALLS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> LOGGED = new WeakHashMap<>();

	private WomSpiderWallYawLock() {
	}

	public static void tick(Player player) {
		if (!ModCompat.isWomLoaded() || ModCompat.isSsrCameraFixesLoaded()) {
			clear(player);
			return;
		}

		if (player == null || !player.isLocalPlayer() || player.isDeadOrDying()) {
			clear(player);
			return;
		}

		if (player.onGround() && !BODY_YAWS.containsKey(player)) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> currentAnimation = playerPatch == null ? null : currentBaseAnimation(playerPatch);
		if (playerPatch == null || !isWomSpiderWallAnimation(currentAnimation)) {
			clear(player);
			return;
		}

		Direction wallDirection = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
		if (wallDirection == null) {
			wallDirection = WomSpiderWallRunHandler.activeWallDirection(player);
		}
		Float lockedYaw = BODY_YAWS.get(player);
		Direction lockedWall = BODY_WALLS.get(player);
		if (lockedYaw == null || wallDirection != null && wallDirection != lockedWall) {
			if (wallDirection == null) {
				wallDirection = WomSpiderWallContactResolver.detectAdjacentWallDirection(player);
			}
			lockedYaw = Float.valueOf(lockToWall(player, playerPatch, wallDirection, "tick"));
			logLock(player, currentAnimation, wallDirection, lockedYaw.floatValue());
		}

		float yaw = lockedYaw.floatValue();
		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			localPlayerPatch.setModelYRot(yaw, true);
		}
	}

	static float lockToWall(Player player, PlayerPatch<?> playerPatch, Direction wallDirection, String reason) {
		if (!ModCompat.isWomLoaded() || ModCompat.isSsrCameraFixesLoaded() || player == null) {
			return player == null ? 0.0F : player.yBodyRot;
		}

		float yaw = WomSpiderWallContactResolver.wallFacingYaw(player, wallDirection);
		BODY_YAWS.put(player, Float.valueOf(yaw));
		if (wallDirection == null) {
			BODY_WALLS.remove(player);
		} else {
			BODY_WALLS.put(player, wallDirection);
		}

		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			localPlayerPatch.setModelYRot(yaw, true);
		}

		if (!EPM.LOGGER.isDebugEnabled()) {
			return yaw;
		}
		EPM.LOGGER.debug("[WomSpiderWallYaw] refresh reason={} wallDirection={} yaw={} playerYRot={} yHeadRot={}",
				reason,
				wallDirection,
				Float.valueOf(yaw),
				Float.valueOf(player.getYRot()),
				Float.valueOf(player.yHeadRot));
		return yaw;
	}

	private static void clear(Player player) {
		BODY_YAWS.remove(player);
		BODY_WALLS.remove(player);
		LOGGED.remove(player);
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isWomSpiderWallAnimation(AssetAccessor<?> animation) {
		ResourceLocation registryName = safeRegistryName(animation);
		if (registryName == null || !"wom".equals(registryName.getNamespace())) {
			return false;
		}

		return switch (registryName.getPath()) {
			case "biped/living/wall_run",
					"biped/living/wall_glide",
					"biped/living/wall_run_left_side",
					"biped/living/wall_run_right_side" -> true;
			default -> false;
		};
	}

	private static void logLock(Player player, AssetAccessor<?> currentAnimation, Direction wallDirection, float lockedYaw) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		if (Boolean.TRUE.equals(LOGGED.get(player))) {
			return;
		}

		LOGGED.put(player, Boolean.TRUE);
		ResourceLocation animationId = safeRegistryName(currentAnimation);
		EPM.LOGGER.debug("[WomSpiderWallYaw] lock animation={} wallDirection={} lockedYaw={} playerYRot={} yBodyRot={} yHeadRot={} pos=({}, {}, {}) delta=({}, {}, {}) onGround={} ssrCameraFixesLoaded={}",
				animationId,
				wallDirection,
				Float.valueOf(lockedYaw),
				Float.valueOf(player.getYRot()),
				Float.valueOf(player.yBodyRot),
				Float.valueOf(player.yHeadRot),
				Double.valueOf(player.getX()),
				Double.valueOf(player.getY()),
				Double.valueOf(player.getZ()),
				Double.valueOf(player.getDeltaMovement().x()),
				Double.valueOf(player.getDeltaMovement().y()),
				Double.valueOf(player.getDeltaMovement().z()),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(ModCompat.isSsrCameraFixesLoaded()));
	}

	private static ResourceLocation safeRegistryName(AssetAccessor<?> animation) {
		try {
			return animation.registryName();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}
}
