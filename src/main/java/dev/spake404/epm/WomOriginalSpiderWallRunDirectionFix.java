package dev.spake404.epm;

import java.util.WeakHashMap;

import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import reascer.wom.skill.WOMSkillDataKeys;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

public final class WomOriginalSpiderWallRunDirectionFix {
	private static final WeakHashMap<Player, Direction> ACTIVE_SIDE_WALLS = new WeakHashMap<>();

	private WomOriginalSpiderWallRunDirectionFix() {
	}

	public static boolean beforeOriginalInput(SkillContainer container, MovementInputEvent event) {
		if (container == null || event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			clearPlayer(player);
			return false;
		}

		int wallRunning = wallRunningState(container);
		if (isParCoolWallJumpActive(player, playerPatch)) {
			releaseSideWallForParCoolWallJump(player, playerPatch);
			return false;
		}
		if (isWomWallBackflipState(wallRunning, playerPatch)) {
			clearPlayer(player);
			return false;
		}

		Input input = event.getMovementInput();
		int sideInput = sideInput(input);
		if (sideInput == 0) {
			sideInput = wallRunning;
		}
		if (!isSideWallRunState(sideInput)) {
			if (!isSideWallRunState(wallRunning)) {
				clearPlayer(player);
			}
			return false;
		}

		updateWallProbeState(player, playerPatch, sideInput, "head");
		return false;
	}

	public static void stabilizeSideWallRun(SkillContainer container, MovementInputEvent event) {
		if (container == null || event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			clearPlayer(player);
			return;
		}

		int wallRunning = wallRunningState(container);
		if (isParCoolWallJumpActive(player, playerPatch)) {
			releaseSideWallForParCoolWallJump(player, playerPatch);
			return;
		}
		if (isWomWallBackflipState(wallRunning, playerPatch)) {
			clearPlayer(player);
			return;
		}
		if (!isSideWallRunState(wallRunning)) {
			clearPlayer(player);
			return;
		}

		Input input = event.getMovementInput();
		int sideInput = sideInput(input);
		if (sideInput == 0) {
			sideInput = wallRunning;
		}
		updateWallProbeState(player, playerPatch, sideInput, "tail");
	}

	public static float wallProbeYaw(Player player) {
		if (player == null) {
			return 0.0F;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			clearPlayer(player);
			return player.yHeadRot;
		}

		Direction activeWall = ACTIVE_SIDE_WALLS.get(player);
		if (activeWall == null) {
			return player.yHeadRot;
		}
		if (!WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall)) {
			ACTIVE_SIDE_WALLS.remove(player);
			return player.yHeadRot;
		}

		return WomSpiderWallContactResolver.wallFacingYaw(player, activeWall);
	}

	public static float wallRunMovementYaw(Player player, float partialTick, float fallbackYaw) {
		if (player == null) {
			return fallbackYaw;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			clearPlayer(player);
			return fallbackYaw;
		}

		Direction activeWall = ACTIVE_SIDE_WALLS.get(player);
		if (activeWall == null) {
			return fallbackYaw;
		}
		if (!WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall)) {
			ACTIVE_SIDE_WALLS.remove(player);
			return fallbackYaw;
		}

		return WomSpiderWallContactResolver.wallFacingYaw(player, activeWall);
	}

	static Direction activeWallDirection(Player player) {
		return player == null ? null : ACTIVE_SIDE_WALLS.get(player);
	}

	static boolean isCrouchControlDown(Player player) {
		if (player != null && player.isCrouching()) {
			return true;
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyShift.isDown();
	}

	static boolean isRestartReleaseRequired(Player player) {
		return false;
	}

	static boolean isSideJumpReleased(Player player) {
		return true;
	}

	static void releaseSideWallForParCoolWallJump(Player player, PlayerPatch<?> playerPatch) {
		clearPlayer(player);
		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopWallRunAnimationsOnly(localPlayerPatch);
		}
	}

	private static Direction updateWallProbeState(Player player, PlayerPatch<?> playerPatch, int sideInput, String phase) {
		if (player == null || !isSideWallRunState(sideInput)) {
			return null;
		}

		Direction activeWall = ACTIVE_SIDE_WALLS.get(player);
		if (activeWall != null) {
			Direction cornerWall = WomSpiderWallCornerTransfer.findAdjacentCornerWall(player, activeWall, sideInput);
			if (cornerWall != null) {
				transferToCornerWall(player, playerPatch, activeWall, cornerWall, phase);
				return cornerWall;
			}
			if (WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall)) {
				return activeWall;
			}
			ACTIVE_SIDE_WALLS.remove(player);
		}

		Direction detectedWall = detectFacingWallDirection(player, playerPatch);
		if (detectedWall != null) {
			ACTIVE_SIDE_WALLS.put(player, detectedWall);
		}
		return detectedWall;
	}

	private static void transferToCornerWall(Player player, PlayerPatch<?> playerPatch, Direction fromWall, Direction toWall, String phase) {
		ACTIVE_SIDE_WALLS.put(player, toWall);
		float yaw = WomSpiderWallYawLock.lockToWall(player, playerPatch, toWall, "corner_" + phase);
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}

		Vec3 delta = player.getDeltaMovement();
		EPM.LOGGER.debug("[WomOriginalWallRun] cornerProbe phase={} fromWall={} toWall={} yaw={} pos=({}, {}, {}) delta=({}, {}, {})",
				phase,
				fromWall,
				toWall,
				Float.valueOf(yaw),
				Double.valueOf(player.getX()),
				Double.valueOf(player.getY()),
				Double.valueOf(player.getZ()),
				Double.valueOf(delta.x()),
				Double.valueOf(delta.y()),
				Double.valueOf(delta.z()));
	}

	private static Direction detectFacingWallDirection(Player player, PlayerPatch<?> playerPatch) {
		return WomSpiderWallContactResolver.detectAdjacentWallDirection(player, modelYaw(player, playerPatch));
	}

	private static float modelYaw(Player player, PlayerPatch<?> playerPatch) {
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			return localPlayerPatch.getModelYRot();
		}
		return player == null ? 0.0F : player.yBodyRot;
	}

	private static boolean isWomWallBackflipState(int wallRunning, PlayerPatch<?> playerPatch) {
		return wallRunning == -3 || WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallBackflip());
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isParCoolWallJumpActive(Player player, PlayerPatch<?> playerPatch) {
		return isParCoolWallJumpActionActive(player) || isParCoolWallJumpAnimation(playerPatch);
	}

	private static boolean isParCoolWallJumpActionActive(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			Class<?> wallJumpClass = parCoolActionClass("com.alrex.parcool.common.action.impl.WallJump");
			Object wallJump = parkourability == null || wallJumpClass == null ? null : parCoolAction(parkourability, wallJumpClass);
			return isParCoolActionDoing(wallJump);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object parCoolAction(Parkourability parkourability, Class<?> actionClass) {
		return parkourability.get((Class) actionClass);
	}

	private static Class<?> parCoolActionClass(String className) {
		try {
			return Class.forName(className, false, WomOriginalSpiderWallRunDirectionFix.class.getClassLoader());
		} catch (ClassNotFoundException | LinkageError | RuntimeException ignored) {
			return null;
		}
	}

	private static boolean isParCoolActionDoing(Object action) {
		if (action == null) {
			return false;
		}

		try {
			Object value = action.getClass().getMethod("isDoing").invoke(action);
			return value instanceof Boolean doing && doing.booleanValue();
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolWallJumpAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(
				currentBaseAnimation(playerPatch),
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight());
	}

	private static void stopWallRunAnimationsOnly(LocalPlayerPatch localPlayerPatch) {
		stopPlaying(localPlayerPatch, WomAnimationRefs.wallRunning());
		stopPlaying(localPlayerPatch, WomAnimationRefs.wallRunLeftSide());
		stopPlaying(localPlayerPatch, WomAnimationRefs.wallRunRightSide());
		stopPlaying(localPlayerPatch, WomAnimationRefs.wallGlide());
	}

	private static void stopPlaying(LocalPlayerPatch playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		if (animation == null) {
			return;
		}

		try {
			playerPatch.stopPlaying(animation);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void clearPlayer(Player player) {
		if (player != null) {
			ACTIVE_SIDE_WALLS.remove(player);
		}
	}

	private static int sideInput(Input input) {
		if (input == null) {
			return 0;
		}
		if (input.left == input.right) {
			return 0;
		}
		return input.left ? 1 : -1;
	}

	private static boolean isSideWallRunState(int wallRunning) {
		return wallRunning == -1 || wallRunning == 1;
	}

	private static int wallRunningState(SkillContainer container) {
		try {
			Object value = container.getDataManager().getDataValue(WOMSkillDataKeys.WALL_RUNNING.get());
			return value instanceof Integer wallRunning ? wallRunning.intValue() : 0;
		} catch (RuntimeException | LinkageError ignored) {
			return 0;
		}
	}
}
