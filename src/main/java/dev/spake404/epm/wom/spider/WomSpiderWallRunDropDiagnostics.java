package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import reascer.wom.skill.WOMSkillDataKeys;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

public final class WomSpiderWallRunDropDiagnostics {
	private static final WeakHashMap<Player, Snapshot> BEFORE_ORIGINAL = new WeakHashMap<>();

	private WomSpiderWallRunDropDiagnostics() {
	}

	public static void beforeOriginalInput(SkillContainer container, MovementInputEvent event) {
		if (!EPMConfig.debugSpiderWallRunState() || container == null || event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			clear(player);
			return;
		}

		BEFORE_ORIGINAL.put(player, Snapshot.capture("before_original", container, event, player, playerPatch));
	}

	public static void afterOriginalInput(SkillContainer container, MovementInputEvent event, String phase) {
		if (!EPMConfig.debugSpiderWallRunState() || container == null || event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			clear(player);
			return;
		}

		Snapshot before = BEFORE_ORIGINAL.remove(player);
		Snapshot after = Snapshot.capture(phase, container, event, player, playerPatch);
		if (before == null || !isSideWallRunState(before.wallRunning) || isSideWallRunState(after.wallRunning)) {
			return;
		}

		logDrop(playerPatch, before, after);
	}

	private static void clear(Player player) {
		if (player != null) {
			BEFORE_ORIGINAL.remove(player);
		}
	}

	private static void logDrop(PlayerPatch<?> playerPatch, Snapshot before, Snapshot after) {
		EPM.LOGGER.info(
				"[EPM/WomSpiderWallRunDrop] reason=original_wom_side_wallrun_lost tick={} beforeWallRunning={} afterWallRunning={} beforeWallGlide={} afterWallGlide={} beforeTimer={} afterTimer={} beforeTimerRefresh={} afterTimerRefresh={} beforeJumpKeyUp={} afterJumpKeyUp={} input=(up={}, down={}, left={}, right={}, jumping={}, shiftKeyDown={}) keys=(forward={}, back={}, left={}, right={}, jump={}, shift={}, sprint={}) state=(onGround={}, sprinting={}, crouching={}, fallDistance={}) rotBefore=(view={}, x={}, head={}, body={}, model={}) rotAfter=(view={}, x={}, head={}, body={}, model={}) wall=(activeBefore={}, activeAfter={}, lookBefore={}, lookAfter={}, viewBefore={}, viewAfter={}, bodyBefore={}, bodyAfter={}, activeContactBefore={}, activeContactAfter={}) deltaBefore=({}, {}, {}) deltaAfter=({}, {}, {}) animationBefore={} animationAfter={} elapsedBefore={} elapsedAfter={} womState={} camera={} mods=(shouldersurfing={}, ssrcamerafixes={}, cameraoverhaul={}, seramicx_smooth_f5={}, smooth_steps={}, betterlockon={}, efn={})",
				Integer.valueOf(after.tick),
				Integer.valueOf(before.wallRunning),
				Integer.valueOf(after.wallRunning),
				before.wallGlide,
				after.wallGlide,
				before.timer,
				after.timer,
				before.timerRefresh,
				after.timerRefresh,
				before.jumpKeyUp,
				after.jumpKeyUp,
				Boolean.valueOf(after.inputUp),
				Boolean.valueOf(after.inputDown),
				Boolean.valueOf(after.inputLeft),
				Boolean.valueOf(after.inputRight),
				Boolean.valueOf(after.inputJumping),
				Boolean.valueOf(after.shiftKeyDown),
				Boolean.valueOf(after.keyForward),
				Boolean.valueOf(after.keyBack),
				Boolean.valueOf(after.keyLeft),
				Boolean.valueOf(after.keyRight),
				Boolean.valueOf(after.keyJump),
				Boolean.valueOf(after.keyShift),
				Boolean.valueOf(after.keySprint),
				Boolean.valueOf(after.onGround),
				Boolean.valueOf(after.sprinting),
				Boolean.valueOf(after.crouching),
				Float.valueOf(after.fallDistance),
				Float.valueOf(before.viewYaw),
				Float.valueOf(before.xRot),
				Float.valueOf(before.headYaw),
				Float.valueOf(before.bodyYaw),
				Float.valueOf(before.modelYaw),
				Float.valueOf(after.viewYaw),
				Float.valueOf(after.xRot),
				Float.valueOf(after.headYaw),
				Float.valueOf(after.bodyYaw),
				Float.valueOf(after.modelYaw),
				before.activeWall,
				after.activeWall,
				before.lookWall,
				after.lookWall,
				before.viewWall,
				after.viewWall,
				before.bodyWall,
				after.bodyWall,
				Boolean.valueOf(before.activeWallContact),
				Boolean.valueOf(after.activeWallContact),
				Double.valueOf(before.delta.x()),
				Double.valueOf(before.delta.y()),
				Double.valueOf(before.delta.z()),
				Double.valueOf(after.delta.x()),
				Double.valueOf(after.delta.y()),
				Double.valueOf(after.delta.z()),
				before.animation,
				after.animation,
				Float.valueOf(before.elapsed),
				Float.valueOf(after.elapsed),
				WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch),
				after.cameraType,
				Boolean.valueOf(isLoaded("shouldersurfing")),
				Boolean.valueOf(isLoaded("ssrcamerafixes")),
				Boolean.valueOf(isLoaded("cameraoverhaul")),
				Boolean.valueOf(isLoaded("seramicx_smooth_f5")),
				Boolean.valueOf(isLoaded("smooth_steps")),
				Boolean.valueOf(isLoaded("betterlockon")),
				Boolean.valueOf(isLoaded("efn")));
	}

	private static boolean isSideWallRunState(int wallRunning) {
		return wallRunning == -1 || wallRunning == 1;
	}

	private static int wallRunningState(SkillContainer container) {
		Integer value = integerData(container, WOMSkillDataKeys.WALL_RUNNING.get());
		return value == null ? 0 : value.intValue();
	}

	private static Boolean wallGlide(SkillContainer container) {
		return booleanData(container, WOMSkillDataKeys.WALL_GLIDE.get());
	}

	private static Integer timer(SkillContainer container) {
		return integerData(container, WOMSkillDataKeys.TIMER.get());
	}

	private static Integer timerRefresh(SkillContainer container) {
		return integerData(container, WOMSkillDataKeys.TIMER_REFRESH.get());
	}

	private static Boolean jumpKeyUp(SkillContainer container) {
		return booleanData(container, WOMSkillDataKeys.JUMP_KEY_UP.get());
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

	private static String animationName(PlayerPatch<?> playerPatch) {
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		return registryName == null ? "null" : registryName.toString();
	}

	private static String cameraType() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null) {
			return "unknown";
		}
		return String.valueOf(minecraft.options.getCameraType());
	}

	private static boolean keyDown(KeyGetter getter) {
		try {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft != null && minecraft.options != null && getter.isDown(minecraft);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isLoaded(String modId) {
		try {
			return ModList.get().isLoaded(modId);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private record Snapshot(
			String phase,
			int tick,
			int wallRunning,
			Boolean wallGlide,
			Integer timer,
			Integer timerRefresh,
			Boolean jumpKeyUp,
			boolean inputUp,
			boolean inputDown,
			boolean inputLeft,
			boolean inputRight,
			boolean inputJumping,
			boolean shiftKeyDown,
			boolean keyForward,
			boolean keyBack,
			boolean keyLeft,
			boolean keyRight,
			boolean keyJump,
			boolean keyShift,
			boolean keySprint,
			boolean onGround,
			boolean sprinting,
			boolean crouching,
			float fallDistance,
			float viewYaw,
			float xRot,
			float headYaw,
			float bodyYaw,
			float modelYaw,
			Direction activeWall,
			Direction lookWall,
			Direction viewWall,
			Direction bodyWall,
			boolean activeWallContact,
			Vec3 delta,
			String animation,
			float elapsed,
			String cameraType) {
		static Snapshot capture(String phase, SkillContainer container, MovementInputEvent event, Player player, PlayerPatch<?> playerPatch) {
			Input input = event.getMovementInput();
			Direction activeWall = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
			return new Snapshot(
					phase,
					player == null ? -1 : player.tickCount,
					wallRunningState(container),
					WomSpiderWallRunDropDiagnostics.wallGlide(container),
					WomSpiderWallRunDropDiagnostics.timer(container),
					WomSpiderWallRunDropDiagnostics.timerRefresh(container),
					WomSpiderWallRunDropDiagnostics.jumpKeyUp(container),
					input != null && input.up,
					input != null && input.down,
					input != null && input.left,
					input != null && input.right,
					input != null && input.jumping,
					player != null && player.isShiftKeyDown(),
					keyDown(minecraft -> minecraft.options.keyUp.isDown()),
					keyDown(minecraft -> minecraft.options.keyDown.isDown()),
					keyDown(minecraft -> minecraft.options.keyLeft.isDown()),
					keyDown(minecraft -> minecraft.options.keyRight.isDown()),
					keyDown(minecraft -> minecraft.options.keyJump.isDown()),
					keyDown(minecraft -> minecraft.options.keyShift.isDown()),
					keyDown(minecraft -> minecraft.options.keySprint.isDown()),
					player != null && player.onGround(),
					player != null && player.isSprinting(),
					player != null && player.isCrouching(),
					player == null ? 0.0F : player.fallDistance,
					player == null ? 0.0F : player.getViewYRot(1.0F),
					player == null ? 0.0F : player.getXRot(),
					player == null ? 0.0F : player.yHeadRot,
					player == null ? 0.0F : player.yBodyRot,
					WomSpiderWallRunDropDiagnostics.modelYaw(playerPatch, player),
					activeWall,
					WomSpiderWallContactResolver.detectAdjacentWallDirection(player),
					player == null ? null : WomSpiderWallContactResolver.detectAdjacentWallDirection(player, player.getViewYRot(1.0F)),
					player == null ? null : WomSpiderWallContactResolver.detectAdjacentWallDirection(player, player.yBodyRot),
					WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall),
					player == null ? Vec3.ZERO : player.getDeltaMovement(),
					animationName(playerPatch),
					AnimationQuery.currentElapsedTime(playerPatch),
					WomSpiderWallRunDropDiagnostics.cameraType());
		}
	}

	@FunctionalInterface
	private interface KeyGetter {
		boolean isDown(Minecraft minecraft);
	}
}
