package dev.spake404.epm.debug;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.naturalsprinter.NaturalSprinterProceduralStepPulse;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.Flipping;
import com.alrex.parcool.common.action.impl.HangDown;
import com.alrex.parcool.common.action.impl.HorizontalWallRun;
import com.alrex.parcool.common.action.impl.Roll;
import com.alrex.parcool.common.action.impl.Slide;
import com.alrex.parcool.common.action.impl.Tap;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.VerticalWallRun;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.capability.Parkourability;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.fml.ModList;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class CameraEventDebug {
	private static final WeakHashMap<LocalPlayer, CameraSample> ENTRY_SAMPLES = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, CameraSample> LAST_FINAL_SAMPLES = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, Integer> LAST_HEARTBEAT_TICKS = new WeakHashMap<>();
	private static final double EVENT_DELTA_LOG_THRESHOLD = 0.01D;
	private static final double FRAME_DELTA_LOG_THRESHOLD = 4.0D;
	private static final double CAMERA_POSITION_DELTA_LOG_THRESHOLD = 0.01D;
	private static final double PLAYER_POSITION_DELTA_LOG_THRESHOLD = 0.01D;
	private static final int HEARTBEAT_TICKS = 20;

	private CameraEventDebug() {
	}

	public static void captureStart(ViewportEvent.ComputeCameraAngles event) {
		if (!EPMConfig.debugCameraEventState()) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		ENTRY_SAMPLES.put(player, sample(event));
	}

	public static void logEnd(ViewportEvent.ComputeCameraAngles event) {
		if (!EPMConfig.debugCameraEventState()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		CameraSample before = ENTRY_SAMPLES.remove(player);
		CameraSample after = sample(event);
		CameraSample previous = LAST_FINAL_SAMPLES.put(player, after);
		double eventPitchDelta = before == null ? 0.0D : after.pitch - before.pitch;
		double eventYawDelta = before == null ? 0.0D : angleDelta(after.yaw, before.yaw);
		double eventRollDelta = before == null ? 0.0D : after.roll - before.roll;
		double framePitchDelta = previous == null ? 0.0D : after.pitch - previous.pitch;
		double frameYawDelta = previous == null ? 0.0D : angleDelta(after.yaw, previous.yaw);
		double frameRollDelta = previous == null ? 0.0D : after.roll - previous.roll;
		double cameraPositionDelta = previous == null ? 0.0D : distance(after.cameraPosition, previous.cameraPosition);
		double playerPositionDelta = previous == null ? 0.0D : distance(after.playerPosition, previous.playerPosition);
		Vec3 interpolatedPlayerPosition = interpolatedPosition(player, after.partialTick);
		Vec3 interpolatedPlayerPositionDelta = interpolatedPlayerPosition.subtract(after.cameraPosition);
		Vec3 eyePosition = player.getEyePosition((float)after.partialTick);
		Vec3 eyePositionDelta = eyePosition.subtract(after.cameraPosition);

		Parkourability parkourability = safeParkourability(player);
		String parcoolActions = parcoolActions(parkourability);
		String proceduralStep = NaturalSprinterProceduralStepPulse.debugState(player);
		boolean activeParCoolAction = !"none".equals(parcoolActions);
		boolean activeProceduralStep = proceduralStep.startsWith("active");
		boolean eventChanged = exceeds(EVENT_DELTA_LOG_THRESHOLD, eventPitchDelta, eventYawDelta, eventRollDelta);
		boolean frameJumped = exceeds(FRAME_DELTA_LOG_THRESHOLD, framePitchDelta, frameYawDelta, frameRollDelta);
		boolean cameraPositionJumped = cameraPositionDelta > CAMERA_POSITION_DELTA_LOG_THRESHOLD;
		boolean playerPositionJumped = playerPositionDelta > PLAYER_POSITION_DELTA_LOG_THRESHOLD;
		boolean heartbeat = shouldHeartbeat(player);
		if (!eventChanged && !frameJumped && !cameraPositionJumped && !playerPositionJumped && !activeParCoolAction && !activeProceduralStep && !heartbeat && Math.abs(after.roll) <= EVENT_DELTA_LOG_THRESHOLD) {
			return;
		}

		PlayerPatch<?> playerPatch = safePlayerPatch(player);
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		Vec3 movement = player.getDeltaMovement();
		Pose pose = safePose(player);
		EPM.LOGGER.info(
				"[EPM/CameraDebug] tick={} partial={} reason={} cameraType={} cameraEntity={} pre=({}, {}, {}) post=({}, {}, {}) eventDelta=({}, {}, {}) frameDelta=({}, {}, {}) cameraPos={} cameraPosPrev={} cameraPosDelta={} playerPos={} playerPrev=({}, {}, {}) playerPosDelta={} interpPlayerPos={} interpMinusCamera={} eyePos={} eyeMinusCamera={} pose=(current={}, eyeHeight={}, bbHeight={}, crouching={}, shiftKeyDown={}, discrete={}, forcedPose={}) playerRot=(x={}, y={}, xOld={}, yOld={}, body={}, bodyOld={}, head={}, headOld={}, model={}) input=(forward={}, back={}, left={}, right={}, jump={}, shift={}, fastRun={}) state=(sprinting={}, onGround={}, swimming={}, passenger={}, fallFlying={}) delta=({}, {}, {}) speed={} parcoolActions={} efMotion={} efAnimation={} efElapsed={} proceduralStep={} mods=(epicArsenal={}, nightfall={}, ssrCameraFixes={}, shoulderSurfing={})",
				Integer.valueOf(player.tickCount),
				Double.valueOf(after.partialTick),
				reason(eventChanged, frameJumped, cameraPositionJumped, playerPositionJumped, activeParCoolAction, activeProceduralStep, heartbeat, after.roll),
				cameraType(minecraft),
				cameraEntityName(after.cameraEntity),
				Double.valueOf(before == null ? Double.NaN : before.pitch),
				Double.valueOf(before == null ? Double.NaN : before.yaw),
				Double.valueOf(before == null ? Double.NaN : before.roll),
				Double.valueOf(after.pitch),
				Double.valueOf(after.yaw),
				Double.valueOf(after.roll),
				Double.valueOf(eventPitchDelta),
				Double.valueOf(eventYawDelta),
				Double.valueOf(eventRollDelta),
				Double.valueOf(framePitchDelta),
				Double.valueOf(frameYawDelta),
				Double.valueOf(frameRollDelta),
				formatVec(after.cameraPosition),
				formatVec(previous == null ? null : previous.cameraPosition),
				Double.valueOf(cameraPositionDelta),
				formatVec(after.playerPosition),
				Double.valueOf(player.xo),
				Double.valueOf(player.yo),
				Double.valueOf(player.zo),
				Double.valueOf(playerPositionDelta),
				formatVec(interpolatedPlayerPosition),
				formatVec(interpolatedPlayerPositionDelta),
				formatVec(eyePosition),
				formatVec(eyePositionDelta),
				poseName(pose),
				Float.valueOf(safeEyeHeight(player)),
				Float.valueOf(safeBbHeight(player)),
				Boolean.valueOf(safeIsCrouching(player)),
				Boolean.valueOf(safeIsShiftKeyDown(player)),
				Boolean.valueOf(safeHasPoseFlag(player, pose)),
				poseName(safeForcedPose(player)),
				Float.valueOf(player.getXRot()),
				Float.valueOf(player.getYRot()),
				Float.valueOf(player.xRotO),
				Float.valueOf(player.yRotO),
				Float.valueOf(player.yBodyRot),
				Float.valueOf(player.yBodyRotO),
				Float.valueOf(player.yHeadRot),
				Float.valueOf(player.yHeadRotO),
				Float.valueOf(safeModelYRot(playerPatch)),
				Boolean.valueOf(keyDown(minecraft, "forward")),
				Boolean.valueOf(keyDown(minecraft, "back")),
				Boolean.valueOf(keyDown(minecraft, "left")),
				Boolean.valueOf(keyDown(minecraft, "right")),
				Boolean.valueOf(keyDown(minecraft, "jump")),
				Boolean.valueOf(keyDown(minecraft, "shift")),
				Boolean.valueOf(fastRunKeyDown()),
				Boolean.valueOf(player.isSprinting()),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(player.isSwimming()),
				Boolean.valueOf(player.isPassenger()),
				Boolean.valueOf(player.isFallFlying()),
				Double.valueOf(movement.x()),
				Double.valueOf(movement.y()),
				Double.valueOf(movement.z()),
				Double.valueOf(horizontalSpeed(movement)),
				parcoolActions,
				safeLivingMotion(playerPatch),
				animationId == null ? String.valueOf(animation) : animationId.toString(),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				proceduralStep,
				Boolean.valueOf(ModCompat.isEpicArsenalLoaded()),
				Boolean.valueOf(ModCompat.isNightfallLoaded()),
				Boolean.valueOf(ModCompat.isSsrCameraFixesLoaded()),
				Boolean.valueOf(isModLoaded("shouldersurfing")));
	}

	private static CameraSample sample(ViewportEvent.ComputeCameraAngles event) {
		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer == null ? null : minecraft.gameRenderer.getMainCamera();
		LocalPlayer player = minecraft.player;
		Vec3 cameraPosition = camera == null ? Vec3.ZERO : camera.getPosition();
		Vec3 playerPosition = player == null ? Vec3.ZERO : player.position();
		Entity cameraEntity = camera == null ? null : camera.getEntity();
		return new CameraSample(event.getPitch(), event.getYaw(), event.getRoll(), event.getPartialTick(), cameraPosition, playerPosition, cameraEntity);
	}

	private static boolean shouldHeartbeat(LocalPlayer player) {
		int tick = player.tickCount;
		Integer lastTick = LAST_HEARTBEAT_TICKS.get(player);
		if (lastTick == null || tick - lastTick.intValue() >= HEARTBEAT_TICKS) {
			LAST_HEARTBEAT_TICKS.put(player, Integer.valueOf(tick));
			return true;
		}
		return false;
	}

	private static String reason(boolean eventChanged, boolean frameJumped, boolean cameraPositionJumped,
			boolean playerPositionJumped, boolean activeParCoolAction,
			boolean activeProceduralStep, boolean heartbeat, double roll) {
		StringBuilder builder = new StringBuilder();
		appendReason(builder, eventChanged, "event_delta");
		appendReason(builder, frameJumped, "frame_delta");
		appendReason(builder, cameraPositionJumped, "camera_pos_delta");
		appendReason(builder, playerPositionJumped, "player_pos_delta");
		appendReason(builder, activeParCoolAction, "parcool_action");
		appendReason(builder, activeProceduralStep, "procedural_step");
		appendReason(builder, Math.abs(roll) > EVENT_DELTA_LOG_THRESHOLD, "roll");
		appendReason(builder, heartbeat, "heartbeat");
		return builder.length() == 0 ? "unknown" : builder.toString();
	}

	private static void appendReason(StringBuilder builder, boolean active, String reason) {
		if (!active) {
			return;
		}
		if (builder.length() > 0) {
			builder.append('+');
		}
		builder.append(reason);
	}

	private static boolean exceeds(double threshold, double... values) {
		for (double value : values) {
			if (Math.abs(value) > threshold) {
				return true;
			}
		}
		return false;
	}

	private static double angleDelta(double current, double previous) {
		double delta = current - previous;
		while (delta > 180.0D) {
			delta -= 360.0D;
		}
		while (delta < -180.0D) {
			delta += 360.0D;
		}
		return delta;
	}

	private static String cameraType(Minecraft minecraft) {
		try {
			return minecraft == null || minecraft.options == null ? "unknown" : String.valueOf(minecraft.options.getCameraType());
		} catch (RuntimeException | LinkageError ignored) {
			return "unavailable";
		}
	}

	private static boolean keyDown(Minecraft minecraft, String key) {
		try {
			if (minecraft == null || minecraft.options == null) {
				return false;
			}
			return switch (key) {
				case "forward" -> minecraft.options.keyUp.isDown();
				case "back" -> minecraft.options.keyDown.isDown();
				case "left" -> minecraft.options.keyLeft.isDown();
				case "right" -> minecraft.options.keyRight.isDown();
				case "jump" -> minecraft.options.keyJump.isDown();
				case "shift" -> minecraft.options.keyShift.isDown();
				default -> false;
			};
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean fastRunKeyDown() {
		try {
			return KeyBindings.getKeyFastRunning() != null && KeyBindings.getKeyFastRunning().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static Parkourability safeParkourability(LocalPlayer player) {
		try {
			return player == null ? null : Parkourability.get(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static PlayerPatch<?> safePlayerPatch(LocalPlayer player) {
		try {
			return player == null ? null : EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Object safeLivingMotion(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch == null ? null : playerPatch.getCurrentLivingMotion();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static float safeModelYRot(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch instanceof LocalPlayerPatch localPlayerPatch
					? localPlayerPatch.getModelYRot()
					: Float.NaN;
		} catch (RuntimeException | LinkageError ignored) {
			return Float.NaN;
		}
	}

	private static String parcoolActions(Parkourability parkourability) {
		if (parkourability == null) {
			return "none";
		}

		StringBuilder builder = new StringBuilder();
		appendAction(builder, parkourability, CatLeap.class);
		appendAction(builder, parkourability, ClimbUp.class);
		appendAction(builder, parkourability, ClingToCliff.class);
		appendAction(builder, parkourability, Crawl.class);
		appendAction(builder, parkourability, Dodge.class);
		appendAction(builder, parkourability, FastRun.class);
		appendAction(builder, parkourability, Flipping.class);
		appendAction(builder, parkourability, HangDown.class);
		appendAction(builder, parkourability, HorizontalWallRun.class);
		appendAction(builder, parkourability, Roll.class);
		appendAction(builder, parkourability, Slide.class);
		appendAction(builder, parkourability, Tap.class);
		appendAction(builder, parkourability, Vault.class);
		appendAction(builder, parkourability, VerticalWallRun.class);
		appendAction(builder, parkourability, WallJump.class);
		appendAction(builder, parkourability, WallSlide.class);
		return builder.length() == 0 ? "none" : builder.toString();
	}

	private static <T extends Action> void appendAction(StringBuilder builder, Parkourability parkourability, Class<T> actionClass) {
		try {
			T action = parkourability.get(actionClass);
			if (action == null || !action.isDoing()) {
				return;
			}
			if (builder.length() > 0) {
				builder.append(',');
			}
			builder.append(actionClass.getSimpleName()).append('#').append(action.getDoingTick());
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static double horizontalSpeed(Vec3 movement) {
		return movement == null ? Double.NaN : Math.sqrt(movement.x() * movement.x() + movement.z() * movement.z());
	}

	private static Vec3 interpolatedPosition(LocalPlayer player, double partialTick) {
		double clampedPartial = Math.max(0.0D, Math.min(1.0D, partialTick));
		return new Vec3(
				player.xo + (player.getX() - player.xo) * clampedPartial,
				player.yo + (player.getY() - player.yo) * clampedPartial,
				player.zo + (player.getZ() - player.zo) * clampedPartial);
	}

	private static double distance(Vec3 current, Vec3 previous) {
		if (current == null || previous == null) {
			return 0.0D;
		}
		return current.distanceTo(previous);
	}

	private static String formatVec(Vec3 vector) {
		if (vector == null) {
			return "null";
		}
		return "(" + vector.x() + ", " + vector.y() + ", " + vector.z() + ")";
	}

	private static String cameraEntityName(Entity entity) {
		if (entity == null) {
			return "null";
		}
		return entity.getType() + "#" + entity.getId();
	}

	private static Pose safePose(LocalPlayer player) {
		try {
			return player == null ? null : player.getPose();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Pose safeForcedPose(LocalPlayer player) {
		try {
			return player == null ? null : player.getForcedPose();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static float safeEyeHeight(LocalPlayer player) {
		try {
			return player == null ? Float.NaN : player.getEyeHeight();
		} catch (RuntimeException | LinkageError ignored) {
			return Float.NaN;
		}
	}

	private static float safeBbHeight(LocalPlayer player) {
		try {
			return player == null ? Float.NaN : player.getBbHeight();
		} catch (RuntimeException | LinkageError ignored) {
			return Float.NaN;
		}
	}

	private static boolean safeIsCrouching(LocalPlayer player) {
		try {
			return player != null && player.isCrouching();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean safeIsShiftKeyDown(LocalPlayer player) {
		try {
			return player != null && player.isShiftKeyDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean safeHasPoseFlag(LocalPlayer player, Pose pose) {
		try {
			return player != null && pose != null && player.hasPose(pose);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static String poseName(Pose pose) {
		return pose == null ? "null" : pose.name();
	}

	private static boolean isModLoaded(String modId) {
		try {
			return ModList.get().isLoaded(modId);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private record CameraSample(
			double pitch,
			double yaw,
			double roll,
			double partialTick,
			Vec3 cameraPosition,
			Vec3 playerPosition,
			Entity cameraEntity) {
	}
}
