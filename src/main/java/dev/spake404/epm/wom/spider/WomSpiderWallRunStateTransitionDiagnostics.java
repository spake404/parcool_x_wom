package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import reascer.wom.skill.WOMSkillDataKeys;
import reascer.wom.skill.mover.SpiderTechniquesSkill;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallRunStateTransitionDiagnostics {
	private static final WeakHashMap<Player, Snapshot> LAST_SNAPSHOTS = new WeakHashMap<>();
	private static final int HEARTBEAT_INTERVAL_TICKS = 10;

	private WomSpiderWallRunStateTransitionDiagnostics() {
	}

	public static void tick(Player player, String phase) {
		if (!EPMConfig.debugSpiderWallRunState() || player == null || !player.level().isClientSide()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canStabilizeOriginalWomWallRun(player, playerPatch)) {
			LAST_SNAPSHOTS.remove(player);
			return;
		}

		SkillContainer spiderTechniques = spiderTechniques(playerPatch);
		if (spiderTechniques == null) {
			LAST_SNAPSHOTS.remove(player);
			return;
		}

		Snapshot previous = LAST_SNAPSHOTS.get(player);
		Snapshot current = Snapshot.capture(phase, player, playerPatch, spiderTechniques);
		LAST_SNAPSHOTS.put(player, current);

		if (previous == null) {
			if (isWallMovementState(current.wallRunning) || current.wallGlide) {
				log("initial_wall_state", previous, current, playerPatch);
			}
			return;
		}

		if (previous.wallRunning != current.wallRunning || previous.wallGlide != current.wallGlide) {
			log(reason(previous, current), previous, current, playerPatch);
			return;
		}

		if ((isSideWallRunState(current.wallRunning) || isWallRunAnimation(current.animation))
				&& current.tick - previous.lastLoggedTick >= HEARTBEAT_INTERVAL_TICKS) {
			current.lastLoggedTick = current.tick;
			log("wallrun_heartbeat", previous, current, playerPatch);
		} else {
			current.lastLoggedTick = previous.lastLoggedTick;
		}
	}

	private static String reason(Snapshot previous, Snapshot current) {
		if (isSideWallRunState(previous.wallRunning) && current.wallRunning == -2) {
			return "side_wallrun_lost";
		}
		if (previous.wallRunning > -2 && current.wallRunning == -2) {
			return "wallrun_lost";
		}
		if (current.wallGlide != previous.wallGlide) {
			return "wall_glide_changed";
		}
		return "wall_running_changed";
	}

	private static void log(String reason, Snapshot previous, Snapshot current, PlayerPatch<?> playerPatch) {
		EPM.LOGGER.info(
				"[EPM/WomSpiderWallRunState] reason={} phase={} tick={} prevWallRunning={} currWallRunning={} prevWallGlide={} currWallGlide={} prevTimer={} currTimer={} prevTimerRefresh={} currTimerRefresh={} prevJumpKeyUp={} currJumpKeyUp={} animationPrev={} animationCurr={} elapsedPrev={} elapsedCurr={} motionPrev={} motionCurr={} keys=(forward={}, back={}, left={}, right={}, jump={}, shift={}, sprint={}) input=(leftImpulse={}, forwardImpulse={}) state=(onGround={}, sprinting={}, crouching={}, fallDistance={}) rotPrev=(view={}, x={}, head={}, body={}, model={}) rotCurr=(view={}, x={}, head={}, body={}, model={}) wall=(activePrev={}, activeCurr={}, lookPrev={}, lookCurr={}, viewPrev={}, viewCurr={}, bodyPrev={}, bodyCurr={}, modelPrev={}, modelCurr={}, activeContactPrev={}, activeContactCurr={}) deltaPrev=({}, {}, {}) deltaCurr=({}, {}, {}) posCurr=({}, {}, {}) wallSpeedCurr=(normal={}, right={}) womState={} camera={} mods=(shouldersurfing={}, ssrcamerafixes={}, cameraoverhaul={}, seramicx_smooth_f5={}, smooth_steps={}, betterlockon={}, efn={})",
				reason,
				current.phase,
				Integer.valueOf(current.tick),
				previous == null ? null : Integer.valueOf(previous.wallRunning),
				Integer.valueOf(current.wallRunning),
				previous == null ? null : Boolean.valueOf(previous.wallGlide),
				Boolean.valueOf(current.wallGlide),
				previous == null ? null : current.timer,
				current.timer,
				previous == null ? null : current.timerRefresh,
				current.timerRefresh,
				previous == null ? null : current.jumpKeyUp,
				current.jumpKeyUp,
				previous == null ? null : previous.animation,
				current.animation,
				previous == null ? null : Float.valueOf(previous.elapsed),
				Float.valueOf(current.elapsed),
				previous == null ? null : previous.motion,
				current.motion,
				Boolean.valueOf(current.keyForward),
				Boolean.valueOf(current.keyBack),
				Boolean.valueOf(current.keyLeft),
				Boolean.valueOf(current.keyRight),
				Boolean.valueOf(current.keyJump),
				Boolean.valueOf(current.keyShift),
				Boolean.valueOf(current.keySprint),
				Float.valueOf(current.leftImpulse),
				Float.valueOf(current.forwardImpulse),
				Boolean.valueOf(current.onGround),
				Boolean.valueOf(current.sprinting),
				Boolean.valueOf(current.crouching),
				Float.valueOf(current.fallDistance),
				previous == null ? null : Float.valueOf(previous.viewYaw),
				previous == null ? null : Float.valueOf(previous.xRot),
				previous == null ? null : Float.valueOf(previous.headYaw),
				previous == null ? null : Float.valueOf(previous.bodyYaw),
				previous == null ? null : Float.valueOf(previous.modelYaw),
				Float.valueOf(current.viewYaw),
				Float.valueOf(current.xRot),
				Float.valueOf(current.headYaw),
				Float.valueOf(current.bodyYaw),
				Float.valueOf(current.modelYaw),
				previous == null ? null : previous.activeWall,
				current.activeWall,
				previous == null ? null : previous.lookWall,
				current.lookWall,
				previous == null ? null : previous.viewWall,
				current.viewWall,
				previous == null ? null : previous.bodyWall,
				current.bodyWall,
				previous == null ? null : previous.modelWall,
				current.modelWall,
				previous == null ? null : Boolean.valueOf(previous.activeWallContact),
				Boolean.valueOf(current.activeWallContact),
				previous == null ? null : Double.valueOf(previous.delta.x()),
				previous == null ? null : Double.valueOf(previous.delta.y()),
				previous == null ? null : Double.valueOf(previous.delta.z()),
				Double.valueOf(current.delta.x()),
				Double.valueOf(current.delta.y()),
				Double.valueOf(current.delta.z()),
				Double.valueOf(current.x),
				Double.valueOf(current.y),
				Double.valueOf(current.z),
				Double.valueOf(current.wallNormalSpeed),
				Double.valueOf(current.wallRightSpeed),
				WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch),
				current.cameraType,
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

	private static boolean isWallMovementState(int wallRunning) {
		return wallRunning > -2;
	}

	private static boolean isWallRunAnimation(String animation) {
		return animation != null && animation.contains("wall_run");
	}

	private static SkillContainer spiderTechniques(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getSkillCapability() == null) {
			return null;
		}

		try (Stream<SkillContainer> containers = playerPatch.getSkillCapability().listSkillContainers()) {
			return containers
					.filter(WomSpiderWallRunStateTransitionDiagnostics::isSpiderTechniquesContainer)
					.findFirst()
					.orElse(null);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isSpiderTechniquesContainer(SkillContainer container) {
		try {
			return container != null && container.getSkill() instanceof SpiderTechniquesSkill;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static int wallRunningState(SkillContainer container) {
		Integer value = integerData(container, WOMSkillDataKeys.WALL_RUNNING.get());
		return value == null ? -2 : value.intValue();
	}

	private static boolean wallGlide(SkillContainer container) {
		return Boolean.TRUE.equals(booleanData(container, WOMSkillDataKeys.WALL_GLIDE.get()));
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

	private static String motionName(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return "null";
		}

		try {
			return String.valueOf(playerPatch.currentLivingMotion);
		} catch (RuntimeException | LinkageError ignored) {
			return "unknown";
		}
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

	private static final class Snapshot {
		private final String phase;
		private final int tick;
		private int lastLoggedTick;
		private final int wallRunning;
		private final boolean wallGlide;
		private final Integer timer;
		private final Integer timerRefresh;
		private final Boolean jumpKeyUp;
		private final String animation;
		private final float elapsed;
		private final String motion;
		private final boolean keyForward;
		private final boolean keyBack;
		private final boolean keyLeft;
		private final boolean keyRight;
		private final boolean keyJump;
		private final boolean keyShift;
		private final boolean keySprint;
		private final float leftImpulse;
		private final float forwardImpulse;
		private final boolean onGround;
		private final boolean sprinting;
		private final boolean crouching;
		private final float fallDistance;
		private final float viewYaw;
		private final float xRot;
		private final float headYaw;
		private final float bodyYaw;
		private final float modelYaw;
		private final Direction activeWall;
		private final Direction lookWall;
		private final Direction viewWall;
		private final Direction bodyWall;
		private final Direction modelWall;
		private final boolean activeWallContact;
		private final Vec3 delta;
		private final double x;
		private final double y;
		private final double z;
		private final double wallNormalSpeed;
		private final double wallRightSpeed;
		private final String cameraType;

		private Snapshot(String phase, int tick, int wallRunning, boolean wallGlide, Integer timer, Integer timerRefresh,
				Boolean jumpKeyUp, String animation, float elapsed, String motion, boolean keyForward, boolean keyBack,
				boolean keyLeft, boolean keyRight, boolean keyJump, boolean keyShift, boolean keySprint,
				float leftImpulse, float forwardImpulse, boolean onGround, boolean sprinting, boolean crouching,
				float fallDistance, float viewYaw, float xRot, float headYaw, float bodyYaw, float modelYaw,
				Direction activeWall, Direction lookWall, Direction viewWall, Direction bodyWall, Direction modelWall,
				boolean activeWallContact, Vec3 delta, double x, double y, double z, double wallNormalSpeed,
				double wallRightSpeed, String cameraType) {
			this.phase = phase;
			this.tick = tick;
			this.lastLoggedTick = tick;
			this.wallRunning = wallRunning;
			this.wallGlide = wallGlide;
			this.timer = timer;
			this.timerRefresh = timerRefresh;
			this.jumpKeyUp = jumpKeyUp;
			this.animation = animation;
			this.elapsed = elapsed;
			this.motion = motion;
			this.keyForward = keyForward;
			this.keyBack = keyBack;
			this.keyLeft = keyLeft;
			this.keyRight = keyRight;
			this.keyJump = keyJump;
			this.keyShift = keyShift;
			this.keySprint = keySprint;
			this.leftImpulse = leftImpulse;
			this.forwardImpulse = forwardImpulse;
			this.onGround = onGround;
			this.sprinting = sprinting;
			this.crouching = crouching;
			this.fallDistance = fallDistance;
			this.viewYaw = viewYaw;
			this.xRot = xRot;
			this.headYaw = headYaw;
			this.bodyYaw = bodyYaw;
			this.modelYaw = modelYaw;
			this.activeWall = activeWall;
			this.lookWall = lookWall;
			this.viewWall = viewWall;
			this.bodyWall = bodyWall;
			this.modelWall = modelWall;
			this.activeWallContact = activeWallContact;
			this.delta = delta;
			this.x = x;
			this.y = y;
			this.z = z;
			this.wallNormalSpeed = wallNormalSpeed;
			this.wallRightSpeed = wallRightSpeed;
			this.cameraType = cameraType;
		}

		static Snapshot capture(String phase, Player player, PlayerPatch<?> playerPatch, SkillContainer spiderTechniques) {
			float modelYaw = WomSpiderWallRunStateTransitionDiagnostics.modelYaw(playerPatch, player);
			Direction activeWall = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
			Direction speedWall = activeWall == null ? WomSpiderWallContactResolver.detectAdjacentWallDirection(player) : activeWall;
			Vec3 delta = player == null ? Vec3.ZERO : player.getDeltaMovement();
			Vec3 horizontalDelta = new Vec3(delta.x(), 0.0D, delta.z());
			Vec3 wallNormal = speedWall == null ? Vec3.ZERO : WomSpiderWallContactResolver.wallNormalDirection(speedWall);
			Vec3 wallRight = speedWall == null ? Vec3.ZERO : WomSpiderWallContactResolver.rightSideDirection(speedWall);
			LocalPlayer localPlayer = player instanceof LocalPlayer local ? local : null;
			return new Snapshot(
					phase,
					player == null ? -1 : player.tickCount,
					wallRunningState(spiderTechniques),
					wallGlide(spiderTechniques),
					timer(spiderTechniques),
					timerRefresh(spiderTechniques),
					jumpKeyUp(spiderTechniques),
					animationName(playerPatch),
					AnimationQuery.currentElapsedTime(playerPatch),
					motionName(playerPatch),
					keyDown(minecraft -> minecraft.options.keyUp.isDown()),
					keyDown(minecraft -> minecraft.options.keyDown.isDown()),
					keyDown(minecraft -> minecraft.options.keyLeft.isDown()),
					keyDown(minecraft -> minecraft.options.keyRight.isDown()),
					keyDown(minecraft -> minecraft.options.keyJump.isDown()),
					keyDown(minecraft -> minecraft.options.keyShift.isDown()),
					keyDown(minecraft -> minecraft.options.keySprint.isDown()),
					localPlayer == null ? 0.0F : localPlayer.input.leftImpulse,
					localPlayer == null ? 0.0F : localPlayer.input.forwardImpulse,
					player != null && player.onGround(),
					player != null && player.isSprinting(),
					player != null && player.isCrouching(),
					player == null ? 0.0F : player.fallDistance,
					player == null ? 0.0F : player.getViewYRot(1.0F),
					player == null ? 0.0F : player.getXRot(),
					player == null ? 0.0F : player.yHeadRot,
					player == null ? 0.0F : player.yBodyRot,
					modelYaw,
					activeWall,
					WomSpiderWallContactResolver.detectAdjacentWallDirection(player),
					player == null ? null : WomSpiderWallContactResolver.detectAdjacentWallDirection(player, player.getViewYRot(1.0F)),
					player == null ? null : WomSpiderWallContactResolver.detectAdjacentWallDirection(player, player.yBodyRot),
					player == null ? null : WomSpiderWallContactResolver.detectAdjacentWallDirection(player, modelYaw),
					WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall),
					delta,
					player == null ? 0.0D : player.getX(),
					player == null ? 0.0D : player.getY(),
					player == null ? 0.0D : player.getZ(),
					horizontalDelta.dot(wallNormal),
					horizontalDelta.dot(wallRight),
					cameraType());
		}
	}

	@FunctionalInterface
	private interface KeyGetter {
		boolean isDown(Minecraft minecraft);
	}
}
