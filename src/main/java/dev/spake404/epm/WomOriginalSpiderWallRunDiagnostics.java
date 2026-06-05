package dev.spake404.epm;

import java.util.WeakHashMap;

import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import reascer.wom.skill.WOMSkillDataKeys;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

public final class WomOriginalSpiderWallRunDiagnostics {
	private static final int LOG_INTERVAL_TICKS = 5;
	private static final WeakHashMap<Player, Integer> LAST_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_REDIRECT_LOG_TICKS = new WeakHashMap<>();

	private WomOriginalSpiderWallRunDiagnostics() {
	}

	public static void logAfterOriginalInput(SkillContainer container, MovementInputEvent event) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		if (container == null || event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			return;
		}

		int wallRunning = wallRunningState(container);
		if (wallRunning != -1 && wallRunning != 1) {
			return;
		}
		if (!shouldLog(player, LAST_LOG_TICKS)) {
			return;
		}

		Input input = event.getMovementInput();
		Direction wallDirection = WomSpiderWallContactResolver.detectAdjacentWallDirection(player);
		Direction activeWall = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
		Vec3 delta = player.getDeltaMovement();
		Vec3 wallRight = wallDirection == null ? Vec3.ZERO : WomSpiderWallContactResolver.rightSideDirection(wallDirection);
		double wallRightSpeed = wallDirection == null ? 0.0D : new Vec3(delta.x(), 0.0D, delta.z()).dot(wallRight);
		float modelYaw = modelYaw(playerPatch, player);
		EPM.LOGGER.debug("[WomOriginalWallRun] wallRunning={} inputUp={} inputDown={} inputLeft={} inputRight={} jumping={} jumpKeyUp={} timerRefresh={} sideJumpReleased={} crouchControl={} restartBlocked={} wallDirection={} activeWall={} wallRightSpeed={} viewYRot={} yHeadRot={} yBodyRot={} modelYRot={} delta=({}, {}, {}) taczGun={} epicArsenalLoaded={}",
				Integer.valueOf(wallRunning),
				Boolean.valueOf(input != null && input.up),
				Boolean.valueOf(input != null && input.down),
				Boolean.valueOf(input != null && input.left),
				Boolean.valueOf(input != null && input.right),
				Boolean.valueOf(input != null && input.jumping),
				jumpKeyUp(container),
				timerRefresh(container),
				Boolean.valueOf(WomOriginalSpiderWallRunDirectionFix.isSideJumpReleased(player)),
				Boolean.valueOf(WomOriginalSpiderWallRunDirectionFix.isCrouchControlDown(player)),
				Boolean.valueOf(WomOriginalSpiderWallRunDirectionFix.isRestartReleaseRequired(player)),
				wallDirection,
				activeWall,
				Double.valueOf(wallRightSpeed),
				Float.valueOf(player.getViewYRot(1.0F)),
				Float.valueOf(player.yHeadRot),
				Float.valueOf(player.yBodyRot),
				Float.valueOf(modelYaw),
				Double.valueOf(delta.x()),
				Double.valueOf(delta.y()),
				Double.valueOf(delta.z()),
				Boolean.valueOf(isHoldingTaczGun(player)),
				Boolean.valueOf(ModCompat.isEpicArsenalLoaded()));
	}

	public static void logViewYawRedirect(Player player, float vanillaViewYaw, float redirectedYaw) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		if (player == null) {
			return;
		}
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			return;
		}
		if (!shouldLog(player, LAST_REDIRECT_LOG_TICKS)) {
			return;
		}

		EPM.LOGGER.debug("[WomOriginalWallRun] getViewYRot redirected vanillaViewYaw={} redirectedYaw={} yHeadRot={} yBodyRot={} taczGun={} epicArsenalLoaded={}",
				Float.valueOf(vanillaViewYaw),
				Float.valueOf(redirectedYaw),
				Float.valueOf(player.yHeadRot),
				Float.valueOf(player.yBodyRot),
				Boolean.valueOf(isHoldingTaczGun(player)),
				Boolean.valueOf(ModCompat.isEpicArsenalLoaded()));
	}

	private static boolean shouldLog(Player player, WeakHashMap<Player, Integer> ticks) {
		if (player == null) {
			return false;
		}

		Integer previousTick = ticks.get(player);
		if (previousTick != null && player.tickCount - previousTick.intValue() < LOG_INTERVAL_TICKS) {
			return false;
		}

		ticks.put(player, Integer.valueOf(player.tickCount));
		return true;
	}

	private static int wallRunningState(SkillContainer container) {
		try {
			Object value = container.getDataManager().getDataValue(WOMSkillDataKeys.WALL_RUNNING.get());
			return value instanceof Integer wallRunning ? wallRunning.intValue() : 0;
		} catch (RuntimeException | LinkageError ignored) {
			return 0;
		}
	}

	private static Boolean jumpKeyUp(SkillContainer container) {
		return booleanData(container, WOMSkillDataKeys.JUMP_KEY_UP.get());
	}

	private static Integer timerRefresh(SkillContainer container) {
		return integerData(container, WOMSkillDataKeys.TIMER_REFRESH.get());
	}

	private static Boolean booleanData(SkillContainer container, SkillDataKey<?> key) {
		try {
			Object value = container.getDataManager().getDataValue(key);
			return value instanceof Boolean bool ? bool : null;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Integer integerData(SkillContainer container, SkillDataKey<?> key) {
		try {
			Object value = container.getDataManager().getDataValue(key);
			return value instanceof Integer integer ? integer : null;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static float modelYaw(PlayerPatch<?> playerPatch, Player player) {
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			return localPlayerPatch.getModelYRot();
		}
		return player == null ? 0.0F : player.yBodyRot;
	}

	private static boolean isHoldingTaczGun(Player player) {
		return player != null && isTaczItem(player.getMainHandItem());
	}

	private static boolean isTaczItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return itemId != null && ModCompat.TACZ.equals(itemId.getNamespace());
	}
}
