package dev.spake404.epm.debug;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.mixin.EntityAccessor;
import dev.spake404.epm.mixin.EntityRotationDebugMixin;
import dev.spake404.epm.mixin.MouseHandlerAccessor;
import dev.spake404.epm.mixin.MouseHandlerRotationDebugMixin;
import dev.spake404.epm.mixin.SynchedEntityDataDebugMixin;
import dev.spake404.epm.naturalsprinter.NaturalSprinterProceduralStepPulse;
import java.util.WeakHashMap;
import java.util.List;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class RotationTraceDebug {
	private static final WeakHashMap<LocalPlayer, RotationSample> MOUSE_TURN_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, RotationSample> ENTITY_TURN_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, RotationSample> SET_Y_ROT_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, RotationSample> SET_X_ROT_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, RotationSample> ABS_MOVE_TO_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, RotationSample> MOVE_TO_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, RotationSample> LAST_PHASE_SAMPLES = new WeakHashMap<>();
	private static final WeakHashMap<LocalPlayer, PoseSample> POSE_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<MouseHandler, MouseMoveSample> MOUSE_MOVE_STARTS = new WeakHashMap<>();
	private static final WeakHashMap<MouseHandler, MouseAccumulatorSample> MOUSE_TURN_ACCUMULATORS = new WeakHashMap<>();
	private static final double ROTATION_DELTA_LOG_THRESHOLD = 0.25D;
	private static final double ROTATION_JUMP_STACK_THRESHOLD = 4.0D;

	private RotationTraceDebug() {
	}

	public static void logClientTick(TickEvent.ClientTickEvent event, String priority) {
		if (event == null) {
			return;
		}
		logPhase("client_tick_" + priority, event.phase, Float.NaN);
	}

	public static void logRenderTick(TickEvent.RenderTickEvent event, String priority) {
		if (event == null) {
			return;
		}
		logPhase("render_tick_" + priority, event.phase, event.renderTickTime);
	}

	public static void mouseMoveStart(MouseHandler handler, long window, double xpos, double ypos) {
		if (!enabled() || handler == null) {
			return;
		}

		MouseAccumulatorSample mouse = mouse(handler);
		MOUSE_MOVE_STARTS.put(handler, new MouseMoveSample(mouse, handler.xpos(), handler.ypos(), xpos, ypos));
		logMouse("mouse_move_start", handler, window, xpos, ypos, null, null, true);
	}

	public static void mouseMoveEnd(MouseHandler handler, long window, double xpos, double ypos) {
		if (!enabled() || handler == null) {
			return;
		}

		MouseMoveSample before = MOUSE_MOVE_STARTS.remove(handler);
		logMouse("mouse_move_end", handler, window, xpos, ypos, before, null, true);
	}

	public static void mouseTurnStart(MouseHandler handler) {
		if (!enabled() || handler == null) {
			return;
		}

		LocalPlayer player = localPlayer();
		if (player == null) {
			return;
		}

		MOUSE_TURN_STARTS.put(player, sample(player));
		MOUSE_TURN_ACCUMULATORS.put(handler, mouse(handler));
		logMouse("mouse_turn_start", handler, 0L, Double.NaN, Double.NaN, null, player, true);
	}

	public static void mouseTurnEnd(MouseHandler handler) {
		if (!enabled() || handler == null) {
			return;
		}

		LocalPlayer player = localPlayer();
		if (player == null) {
			return;
		}

		RotationSample before = MOUSE_TURN_STARTS.remove(player);
		MouseAccumulatorSample mouseBefore = MOUSE_TURN_ACCUMULATORS.remove(handler);
		RotationSample after = sample(player);
		if (!shouldLog(player, before, after, true)) {
			return;
		}

		logRotation("mouse_turn_end", "turnPlayer", player, before, after,
				"mouseBefore=" + formatMouse(mouseBefore) + " mouseAfter=" + formatMouse(mouse(handler)),
				true);
	}

	public static void entityTurnStart(Entity entity, double yawDeltaInput, double pitchDeltaInput) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		ENTITY_TURN_STARTS.put(player, sample(player));
	}

	public static void entityTurnEnd(Entity entity, double yawDeltaInput, double pitchDeltaInput) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		RotationSample before = ENTITY_TURN_STARTS.remove(player);
		RotationSample after = sample(player);
		if (!shouldLog(player, before, after, false)) {
			return;
		}

		logRotation("entity_turn_end", "Entity.turn",
				player,
				before,
				after,
				"inputYawDelta=" + yawDeltaInput + " inputPitchDelta=" + pitchDeltaInput,
				true);
	}

	public static void setYRotStart(Entity entity, float targetYaw) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		SET_Y_ROT_STARTS.put(player, sample(player));
	}

	public static void setYRotEnd(Entity entity, float targetYaw) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		RotationSample before = SET_Y_ROT_STARTS.remove(player);
		RotationSample after = sample(player);
		if (!shouldLog(player, before, after, false)) {
			return;
		}

		logRotation("entity_set_y_rot_end", "Entity.setYRot",
				player,
				before,
				after,
				"targetYaw=" + targetYaw,
				largeJump(before, after));
	}

	public static void setXRotStart(Entity entity, float targetPitch) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		SET_X_ROT_STARTS.put(player, sample(player));
	}

	public static void setXRotEnd(Entity entity, float targetPitch) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		RotationSample before = SET_X_ROT_STARTS.remove(player);
		RotationSample after = sample(player);
		if (!shouldLog(player, before, after, false)) {
			return;
		}

		logRotation("entity_set_x_rot_end", "Entity.setXRot",
				player,
				before,
				after,
				"targetPitch=" + targetPitch,
				largeJump(before, after));
	}

	public static void absMoveToStart(Entity entity, double x, double y, double z, float yaw, float pitch) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		ABS_MOVE_TO_STARTS.put(player, sample(player));
	}

	public static void absMoveToEnd(Entity entity, double x, double y, double z, float yaw, float pitch) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		RotationSample before = ABS_MOVE_TO_STARTS.remove(player);
		RotationSample after = sample(player);
		if (!shouldLog(player, before, after, false)) {
			return;
		}

		logRotation("entity_abs_move_to_end", "Entity.absMoveTo",
				player,
				before,
				after,
				"targetPos=(" + x + ", " + y + ", " + z + ") targetYaw=" + yaw + " targetPitch=" + pitch,
				true);
	}

	public static void moveToStart(Entity entity, double x, double y, double z, float yaw, float pitch) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		MOVE_TO_STARTS.put(player, sample(player));
	}

	public static void moveToEnd(Entity entity, double x, double y, double z, float yaw, float pitch) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		RotationSample before = MOVE_TO_STARTS.remove(player);
		RotationSample after = sample(player);
		if (!shouldLog(player, before, after, false)) {
			return;
		}

		logRotation("entity_move_to_end", "Entity.moveTo",
				player,
				before,
				after,
				"targetPos=(" + x + ", " + y + ", " + z + ") targetYaw=" + yaw + " targetPitch=" + pitch,
				true);
	}

	public static void setPoseStart(Entity entity, Pose targetPose) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		POSE_STARTS.put(player, poseSample(player));
	}

	public static void setPoseEnd(Entity entity, Pose targetPose) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled()) {
			return;
		}

		PoseSample before = POSE_STARTS.remove(player);
		PoseSample after = poseSample(player);
		if (before != null && after != null && before.pose == after.pose && targetPose == after.pose) {
			return;
		}

		logPose("entity_set_pose_end", "Entity.setPose", player, before, after, targetPose);
	}

	public static <T> void synchedDataSetStart(Entity entity, EntityDataAccessor<T> accessor, T value, boolean force) {
		logPoseData("data_set_start", "SynchedEntityData.set", entity, accessor, value, force, null);
	}

	public static <T> void synchedDataSetEnd(Entity entity, EntityDataAccessor<T> accessor, T value, boolean force) {
		logPoseData("data_set_end", "SynchedEntityData.set", entity, accessor, value, force, null);
	}

	public static void synchedDataAssignValuesStart(Entity entity, List<SynchedEntityData.DataValue<?>> values) {
		logPoseDataValues("assign_values_start", entity, values);
	}

	public static void synchedDataAssignValuesEnd(Entity entity, List<SynchedEntityData.DataValue<?>> values) {
		logPoseDataValues("assign_values_end", entity, values);
	}

	private static void logPhase(String hook, TickEvent.Phase phase, float renderTickTime) {
		if (!enabled()) {
			return;
		}

		LocalPlayer player = localPlayer();
		if (player == null) {
			return;
		}

		RotationSample previous = LAST_PHASE_SAMPLES.put(player, sample(player));
		RotationSample current = sample(player);
		if (!shouldLog(player, previous, current, false)) {
			return;
		}

		logRotation(hook,
				"phase=" + phase + " renderTickTime=" + renderTickTime,
				player,
				previous,
				current,
				"snapshot",
				false);
	}

	private static void logMouse(String hook, MouseHandler handler, long window, double eventX, double eventY,
			MouseMoveSample before, LocalPlayer playerOverride, boolean includeStack) {
		LocalPlayer player = playerOverride == null ? localPlayer() : playerOverride;
		if (player == null) {
			return;
		}

		RotationSample current = sample(player);
		boolean rawMoved = before == null
				? !Double.isNaN(eventX) && (Math.abs(eventX - handler.xpos()) > 0.01D || Math.abs(eventY - handler.ypos()) > 0.01D)
				: Math.abs(before.eventX - before.oldX) > 0.01D || Math.abs(before.eventY - before.oldY) > 0.01D;
		if (!rawMoved && !interesting(player)) {
			return;
		}

		String details = "window=" + window
				+ " eventPos=(" + eventX + ", " + eventY + ")"
				+ " storedPos=(" + handler.xpos() + ", " + handler.ypos() + ")"
				+ " mouse=" + formatMouse(mouse(handler));
		if (before != null) {
			details += " beforeMove=(old=(" + before.oldX + ", " + before.oldY + ") event=(" + before.eventX + ", " + before.eventY + ") mouse=" + formatMouse(before.mouse) + ")";
		}

		logRotation(hook, "MouseHandler", player, null, current, details, includeStack);
	}

	private static void logRotation(String hook, String source, LocalPlayer player, RotationSample before,
			RotationSample after, String details, boolean includeStack) {
		PlayerPatch<?> playerPatch = safePlayerPatch(player);
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		Vec3 movement = safeDeltaMovement(player);
		Minecraft minecraft = Minecraft.getInstance();
		EPM.LOGGER.info(
				"[EPM/RotationTrace] hook={} source={} tick={} nano={} thread={} screen={} camera={} before={} after={} delta={} details={} keys=(forward={}, back={}, left={}, right={}, jump={}, shift={}, sprint={}, fastRun={}) state=(sprinting={}, onGround={}, swimming={}, passenger={}, fallFlying={}) deltaMove=({}, {}, {}) speed={} parcoolActions={} efMotion={} efAnimation={} efElapsed={} proceduralStep={} stack={}",
				hook,
				source,
				Integer.valueOf(player.tickCount),
				Long.valueOf(System.nanoTime()),
				Thread.currentThread().getName(),
				screen(minecraft),
				cameraType(minecraft),
				formatSample(before),
				formatSample(after),
				formatDelta(before, after),
				details,
				Boolean.valueOf(keyDown(minecraft, "forward")),
				Boolean.valueOf(keyDown(minecraft, "back")),
				Boolean.valueOf(keyDown(minecraft, "left")),
				Boolean.valueOf(keyDown(minecraft, "right")),
				Boolean.valueOf(keyDown(minecraft, "jump")),
				Boolean.valueOf(keyDown(minecraft, "shift")),
				Boolean.valueOf(keyDown(minecraft, "sprint")),
				Boolean.valueOf(fastRunKeyDown()),
				Boolean.valueOf(safeBoolean(player, "sprinting")),
				Boolean.valueOf(safeBoolean(player, "onGround")),
				Boolean.valueOf(safeBoolean(player, "swimming")),
				Boolean.valueOf(safeBoolean(player, "passenger")),
				Boolean.valueOf(safeBoolean(player, "fallFlying")),
				Double.valueOf(movement.x()),
				Double.valueOf(movement.y()),
				Double.valueOf(movement.z()),
				Double.valueOf(horizontalSpeed(movement)),
				parcoolActions(safeParkourability(player)),
				safeLivingMotion(playerPatch),
				animationId == null ? String.valueOf(animation) : animationId.toString(),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				NaturalSprinterProceduralStepPulse.debugState(player),
				includeStack ? stackSummary() : "none");
	}

	private static void logPose(String hook, String source, LocalPlayer player, PoseSample before,
			PoseSample after, Pose targetPose) {
		PlayerPatch<?> playerPatch = safePlayerPatch(player);
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		Vec3 movement = safeDeltaMovement(player);
		Minecraft minecraft = Minecraft.getInstance();
		EPM.LOGGER.info(
				"[EPM/PoseTrace] hook={} source={} tick={} nano={} thread={} screen={} camera={} before={} target={} after={} keys=(forward={}, back={}, left={}, right={}, jump={}, shift={}, sprint={}, fastRun={}) state=(sprinting={}, onGround={}, swimming={}, passenger={}, fallFlying={}, crouching={}, shiftKeyDown={}) deltaMove=({}, {}, {}) speed={} parcoolActions={} efMotion={} efAnimation={} efElapsed={} proceduralStep={} stack={}",
				hook,
				source,
				Integer.valueOf(player.tickCount),
				Long.valueOf(System.nanoTime()),
				Thread.currentThread().getName(),
				screen(minecraft),
				cameraType(minecraft),
				formatPoseSample(before),
				poseName(targetPose),
				formatPoseSample(after),
				Boolean.valueOf(keyDown(minecraft, "forward")),
				Boolean.valueOf(keyDown(minecraft, "back")),
				Boolean.valueOf(keyDown(minecraft, "left")),
				Boolean.valueOf(keyDown(minecraft, "right")),
				Boolean.valueOf(keyDown(minecraft, "jump")),
				Boolean.valueOf(keyDown(minecraft, "shift")),
				Boolean.valueOf(keyDown(minecraft, "sprint")),
				Boolean.valueOf(fastRunKeyDown()),
				Boolean.valueOf(safeBoolean(player, "sprinting")),
				Boolean.valueOf(safeBoolean(player, "onGround")),
				Boolean.valueOf(safeBoolean(player, "swimming")),
				Boolean.valueOf(safeBoolean(player, "passenger")),
				Boolean.valueOf(safeBoolean(player, "fallFlying")),
				Boolean.valueOf(safeIsCrouching(player)),
				Boolean.valueOf(safeIsShiftKeyDown(player)),
				Double.valueOf(movement.x()),
				Double.valueOf(movement.y()),
				Double.valueOf(movement.z()),
				Double.valueOf(horizontalSpeed(movement)),
				parcoolActions(safeParkourability(player)),
				safeLivingMotion(playerPatch),
				animationId == null ? String.valueOf(animation) : animationId.toString(),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				NaturalSprinterProceduralStepPulse.debugState(player),
				stackSummary());
	}

	private static void logPoseData(String hook, String source, Entity entity, EntityDataAccessor<?> accessor,
			Object value, boolean force, String values) {
		LocalPlayer player = localPlayer(entity);
		if (player == null || !enabled() || !isPoseData(accessor, value)) {
			return;
		}

		PoseSample current = poseSample(player);
		PlayerPatch<?> playerPatch = safePlayerPatch(player);
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		Vec3 movement = safeDeltaMovement(player);
		Minecraft minecraft = Minecraft.getInstance();
		EntityDataAccessor<Pose> poseAccessor = poseAccessor();
		EPM.LOGGER.info(
				"[EPM/PoseDataTrace] hook={} source={} tick={} nano={} thread={} screen={} camera={} entity={} accessorId={} poseAccessorId={} force={} targetValue={} values={} current={} keys=(forward={}, back={}, left={}, right={}, jump={}, shift={}, sprint={}, fastRun={}) state=(sprinting={}, onGround={}, swimming={}, passenger={}, fallFlying={}, crouching={}, shiftKeyDown={}) deltaMove=({}, {}, {}) speed={} parcoolActions={} efMotion={} efAnimation={} efElapsed={} proceduralStep={} stack={}",
				hook,
				source,
				Integer.valueOf(player.tickCount),
				Long.valueOf(System.nanoTime()),
				Thread.currentThread().getName(),
				screen(minecraft),
				cameraType(minecraft),
				entityName(entity),
				Integer.valueOf(accessor == null ? -1 : accessor.getId()),
				Integer.valueOf(poseAccessor == null ? -1 : poseAccessor.getId()),
				Boolean.valueOf(force),
				value,
				values == null ? "none" : values,
				formatPoseSample(current),
				Boolean.valueOf(keyDown(minecraft, "forward")),
				Boolean.valueOf(keyDown(minecraft, "back")),
				Boolean.valueOf(keyDown(minecraft, "left")),
				Boolean.valueOf(keyDown(minecraft, "right")),
				Boolean.valueOf(keyDown(minecraft, "jump")),
				Boolean.valueOf(keyDown(minecraft, "shift")),
				Boolean.valueOf(keyDown(minecraft, "sprint")),
				Boolean.valueOf(fastRunKeyDown()),
				Boolean.valueOf(safeBoolean(player, "sprinting")),
				Boolean.valueOf(safeBoolean(player, "onGround")),
				Boolean.valueOf(safeBoolean(player, "swimming")),
				Boolean.valueOf(safeBoolean(player, "passenger")),
				Boolean.valueOf(safeBoolean(player, "fallFlying")),
				Boolean.valueOf(safeIsCrouching(player)),
				Boolean.valueOf(safeIsShiftKeyDown(player)),
				Double.valueOf(movement.x()),
				Double.valueOf(movement.y()),
				Double.valueOf(movement.z()),
				Double.valueOf(horizontalSpeed(movement)),
				parcoolActions(safeParkourability(player)),
				safeLivingMotion(playerPatch),
				animationId == null ? String.valueOf(animation) : animationId.toString(),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				NaturalSprinterProceduralStepPulse.debugState(player),
				stackSummary());
	}

	private static void logPoseDataValues(String hook, Entity entity, List<SynchedEntityData.DataValue<?>> values) {
		if (!enabled() || values == null || values.isEmpty()) {
			return;
		}

		EntityDataAccessor<Pose> poseAccessor = poseAccessor();
		if (poseAccessor == null) {
			return;
		}

		for (SynchedEntityData.DataValue<?> value : values) {
			if (value != null && value.id() == poseAccessor.getId()) {
				logPoseData(hook, "SynchedEntityData.assignValues", entity, poseAccessor, value.value(), false, formatDataValues(values));
				return;
			}
		}
	}

	private static boolean enabled() {
		return EPMConfig.debugCameraEventState();
	}

	private static boolean shouldLog(LocalPlayer player, RotationSample before, RotationSample after, boolean forceWhileMouse) {
		if (player == null || after == null) {
			return false;
		}
		if (forceWhileMouse && interesting(player)) {
			return true;
		}
		if (interesting(player)) {
			return true;
		}
		if (before == null) {
			return true;
		}
		return Math.abs(angleDelta(after.yaw, before.yaw)) > ROTATION_DELTA_LOG_THRESHOLD
				|| Math.abs(after.pitch - before.pitch) > ROTATION_DELTA_LOG_THRESHOLD
				|| Math.abs(angleDelta(after.bodyYaw, before.bodyYaw)) > ROTATION_DELTA_LOG_THRESHOLD
				|| Math.abs(angleDelta(after.headYaw, before.headYaw)) > ROTATION_DELTA_LOG_THRESHOLD;
	}

	private static boolean largeJump(RotationSample before, RotationSample after) {
		if (before == null || after == null) {
			return true;
		}
		return Math.abs(angleDelta(after.yaw, before.yaw)) > ROTATION_JUMP_STACK_THRESHOLD
				|| Math.abs(after.pitch - before.pitch) > ROTATION_JUMP_STACK_THRESHOLD;
	}

	private static boolean interesting(LocalPlayer player) {
		if (player == null) {
			return false;
		}
		if (safeBoolean(player, "sprinting")) {
			return true;
		}
		return !"none".equals(parcoolActions(safeParkourability(player)));
	}

	private static RotationSample sample(LocalPlayer player) {
		if (player == null) {
			return null;
		}
		PlayerPatch<?> playerPatch = safePlayerPatch(player);
		return new RotationSample(
				player.getXRot(),
				player.getYRot(),
				player.xRotO,
				player.yRotO,
				player.yBodyRot,
				player.yBodyRotO,
				player.yHeadRot,
				player.yHeadRotO,
				safeModelYRot(playerPatch));
	}

	private static PoseSample poseSample(LocalPlayer player) {
		if (player == null) {
			return null;
		}
		Pose pose = safePose(player);
		EntityDimensions dimensions = safeDimensions(player, pose);
		return new PoseSample(
				pose,
				safeEyeHeight(player),
				safeBbHeight(player),
				dimensions == null ? Float.NaN : dimensions.width,
				dimensions == null ? Float.NaN : dimensions.height,
				safeIsCrouching(player),
				safeIsShiftKeyDown(player));
	}

	private static MouseAccumulatorSample mouse(MouseHandler handler) {
		if (handler == null) {
			return null;
		}
		try {
			MouseHandlerAccessor accessor = (MouseHandlerAccessor) handler;
			return new MouseAccumulatorSample(
					accessor.epm$getAccumulatedDX(),
					accessor.epm$getAccumulatedDY(),
					accessor.epm$isMouseGrabbed(),
					accessor.epm$isIgnoreFirstMove(),
					accessor.epm$getLastMouseEventTime());
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static LocalPlayer localPlayer() {
		try {
			return Minecraft.getInstance().player;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static LocalPlayer localPlayer(Entity entity) {
		if (!(entity instanceof LocalPlayer player) || !player.isLocalPlayer()) {
			return null;
		}

		LocalPlayer activePlayer = localPlayer();
		if (activePlayer == player) {
			return player;
		}
		return null;
	}

	private static boolean safeBoolean(LocalPlayer player, String state) {
		try {
			if (player == null) {
				return false;
			}
			return switch (state) {
				case "sprinting" -> player.isSprinting();
				case "onGround" -> player.onGround();
				case "swimming" -> player.isSwimming();
				case "passenger" -> player.isPassenger();
				case "fallFlying" -> player.isFallFlying();
				default -> false;
			};
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static Vec3 safeDeltaMovement(LocalPlayer player) {
		try {
			return player == null ? Vec3.ZERO : player.getDeltaMovement();
		} catch (RuntimeException | LinkageError ignored) {
			return Vec3.ZERO;
		}
	}

	private static Pose safePose(LocalPlayer player) {
		try {
			return player == null ? null : player.getPose();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static EntityDataAccessor<Pose> poseAccessor() {
		try {
			return EntityAccessor.epm$getDataPose();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isPoseData(EntityDataAccessor<?> accessor, Object value) {
		EntityDataAccessor<Pose> poseAccessor = poseAccessor();
		if (poseAccessor == null || accessor == null) {
			return value instanceof Pose;
		}
		return accessor.getId() == poseAccessor.getId();
	}

	private static EntityDimensions safeDimensions(LocalPlayer player, Pose pose) {
		try {
			return player == null || pose == null ? null : player.getDimensions(pose);
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

	private static String formatSample(RotationSample sample) {
		if (sample == null) {
			return "null";
		}
		return "(x=" + sample.pitch
				+ ", y=" + sample.yaw
				+ ", xOld=" + sample.oldPitch
				+ ", yOld=" + sample.oldYaw
				+ ", body=" + sample.bodyYaw
				+ ", bodyOld=" + sample.oldBodyYaw
				+ ", head=" + sample.headYaw
				+ ", headOld=" + sample.oldHeadYaw
				+ ", model=" + sample.modelYaw
				+ ")";
	}

	private static String formatPoseSample(PoseSample sample) {
		if (sample == null) {
			return "null";
		}
		return "(pose=" + poseName(sample.pose)
				+ ", eyeHeight=" + sample.eyeHeight
				+ ", bbHeight=" + sample.bbHeight
				+ ", dimWidth=" + sample.dimWidth
				+ ", dimHeight=" + sample.dimHeight
				+ ", crouching=" + sample.crouching
				+ ", shiftKeyDown=" + sample.shiftKeyDown
				+ ")";
	}

	private static String poseName(Pose pose) {
		return pose == null ? "null" : pose.name();
	}

	private static String formatDataValues(List<SynchedEntityData.DataValue<?>> values) {
		if (values == null || values.isEmpty()) {
			return "[]";
		}
		StringBuilder builder = new StringBuilder("[");
		int count = 0;
		for (SynchedEntityData.DataValue<?> value : values) {
			if (value == null) {
				continue;
			}
			if (count > 0) {
				builder.append(", ");
			}
			builder.append("{id=")
					.append(value.id())
					.append(", value=")
					.append(value.value())
					.append('}');
			count++;
			if (count >= 12) {
				builder.append(", ...");
				break;
			}
		}
		return builder.append(']').toString();
	}

	private static String formatDelta(RotationSample before, RotationSample after) {
		if (before == null || after == null) {
			return "null";
		}
		return "(x=" + (after.pitch - before.pitch)
				+ ", y=" + angleDelta(after.yaw, before.yaw)
				+ ", xOld=" + (after.oldPitch - before.oldPitch)
				+ ", yOld=" + angleDelta(after.oldYaw, before.oldYaw)
				+ ", body=" + angleDelta(after.bodyYaw, before.bodyYaw)
				+ ", bodyOld=" + angleDelta(after.oldBodyYaw, before.oldBodyYaw)
				+ ", head=" + angleDelta(after.headYaw, before.headYaw)
				+ ", headOld=" + angleDelta(after.oldHeadYaw, before.oldHeadYaw)
				+ ", model=" + angleDelta(after.modelYaw, before.modelYaw)
				+ ")";
	}

	private static String formatMouse(MouseAccumulatorSample mouse) {
		if (mouse == null) {
			return "null";
		}
		return "(accumulatedDX=" + mouse.accumulatedDX
				+ ", accumulatedDY=" + mouse.accumulatedDY
				+ ", grabbed=" + mouse.grabbed
				+ ", ignoreFirstMove=" + mouse.ignoreFirstMove
				+ ", lastEventTime=" + mouse.lastEventTime
				+ ")";
	}

	private static String stackSummary() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for (StackTraceElement frame : stack) {
			String className = frame.getClassName();
			if (shouldSkipStackFrame(className)) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(" <- ");
			}
			builder.append(className)
					.append('#')
					.append(frame.getMethodName())
					.append(':')
					.append(frame.getLineNumber());
			count++;
			if (count >= 10) {
				break;
			}
		}
		return builder.length() == 0 ? "none" : builder.toString();
	}

	private static boolean shouldSkipStackFrame(String className) {
		return className == null
				|| className.equals(Thread.class.getName())
				|| className.equals(RotationTraceDebug.class.getName())
				|| className.startsWith("dev.spake404.epm.mixin.MouseHandlerRotationDebugMixin")
				|| className.startsWith("dev.spake404.epm.mixin.EntityRotationDebugMixin")
				|| className.startsWith("dev.spake404.epm.mixin.SynchedEntityDataDebugMixin")
				|| className.startsWith("org.spongepowered.asm.mixin.injection.callback");
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

	private static String screen(Minecraft minecraft) {
		try {
			return minecraft == null || minecraft.screen == null ? "none" : minecraft.screen.getClass().getName();
		} catch (RuntimeException | LinkageError ignored) {
			return "unavailable";
		}
	}

	private static String entityName(Entity entity) {
		try {
			return entity == null ? "null" : entity.getEncodeId() + "#" + entity.getId();
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
				case "sprint" -> minecraft.options.keySprint.isDown();
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

	private record RotationSample(double pitch, double yaw, double oldPitch, double oldYaw, double bodyYaw,
			double oldBodyYaw, double headYaw, double oldHeadYaw, double modelYaw) {
	}

	private record PoseSample(Pose pose, float eyeHeight, float bbHeight, float dimWidth, float dimHeight,
			boolean crouching, boolean shiftKeyDown) {
	}

	private record MouseAccumulatorSample(double accumulatedDX, double accumulatedDY, boolean grabbed,
			boolean ignoreFirstMove, double lastEventTime) {
	}

	private record MouseMoveSample(MouseAccumulatorSample mouse, double oldX, double oldY, double eventX, double eventY) {
	}
}
