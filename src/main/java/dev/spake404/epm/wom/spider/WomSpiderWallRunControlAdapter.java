package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.parcool.ParCoolClimbUpState;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

public final class WomSpiderWallRunControlAdapter {
	private static final double VERTICAL_FACING_DOT = 0.93D;
	private static final double HORIZONTAL_SIDE_DOT = 0.35D;
	private static final int LOG_INTERVAL_TICKS = 5;
	private static final WeakHashMap<Player, Integer> LAST_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, InputSnapshot> INPUT_SNAPSHOTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, WallRunInputDecision> WALL_RUN_DECISIONS = new WeakHashMap<>();

	private WomSpiderWallRunControlAdapter() {
	}

	public static boolean shouldUseOriginalWomWallRunControls(Player player, PlayerPatch<?> playerPatch) {
		return WomSpiderWallRunModeGate.canUseParCoolOriginalAdapter(player, playerPatch);
	}

	public static boolean shouldTriggerOriginalWomWallRunSprintInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!shouldUseOriginalWomWallRunControls(player, playerPatch)) {
			return false;
		}

		WallRunInputDecision decision = player == null ? null : WALL_RUN_DECISIONS.get(player);
		boolean wallRunKeyDown = isWallRunKeyDown();
		boolean wallSlideKeyDown = isParCoolWallSlideKeyDown();
		boolean forwardDown = isForwardDown(event.getMovementInput());
		boolean backflipActive = WomSpiderWallMovementState.isWallBackflipActive(playerPatch);
		boolean allow = wallRunKeyDown && forwardDown && !backflipActive && isAppliedDecision(decision);
		String reason = allow ? "wallrun_input" : blockedWallRunReason(wallRunKeyDown, forwardDown, backflipActive, decision);
		logAdapterInput(player, "wallrun", allow, reason, wallRunKeyDown, wallSlideKeyDown, forwardDown, decision);
		return allow;
	}

	public static boolean shouldTriggerOriginalWomWallGlideSprintInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!shouldUseOriginalWomWallRunControls(player, playerPatch)) {
			return false;
		}

		boolean wallRunKeyDown = isWallRunKeyDown();
		boolean wallSlideKeyDown = isParCoolWallSlideKeyDown();
		boolean forwardDown = isForwardDown(event.getMovementInput());
		boolean glideControlDown = wallSlideKeyDown || wallRunKeyDown && !forwardDown;
		boolean backflipActive = WomSpiderWallMovementState.isWallBackflipActive(playerPatch);
		boolean climbUpActive = ParCoolClimbUpState.isActiveOrAnimating(player, playerPatch);
		Direction wallDirection = glideControlDown && player != null ? WomSpiderWallContactResolver.detectAdjacentWallDirection(player) : null;
		boolean allow = glideControlDown
				&& !forwardDown
				&& !backflipActive
				&& !climbUpActive
				&& player != null
				&& !player.onGround()
				&& wallDirection != null;
		String reason = allow ? "wallglide_input" : blockedWallGlideReason(glideControlDown, forwardDown, backflipActive, climbUpActive, player, wallDirection);
		logAdapterInput(player, "wallglide", allow, reason, wallRunKeyDown, wallSlideKeyDown, forwardDown,
				new WallRunInputDecision(allow ? WallRunInputMode.GLIDE : WallRunInputMode.NONE, wallDirection, 0.0D, 0.0D, 0));
		return allow;
	}

	public static void beforeOriginalWomInput(MovementInputEvent event) {
		if (event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		Input input = event.getMovementInput();
		if (player == null || input == null) {
			return;
		}

		restoreInput(player);
		if (!shouldUseOriginalWomWallRunControls(player, playerPatch) || !isWallRunKeyDown() || !isForwardDown(input)
				|| WomSpiderWallMovementState.isWallBackflipActive(playerPatch)) {
			return;
		}

		InputSnapshot snapshot = InputSnapshot.capture(input);
		WallRunInputDecision decision = resolveMouseWallRunInput(player);
		if (decision.mode() == WallRunInputMode.NONE) {
			logMouseInput(player, decision, input, false);
			return;
		}

		INPUT_SNAPSHOTS.put(player, snapshot);
		WALL_RUN_DECISIONS.put(player, decision);
		applyDecision(input, decision);
		logMouseInput(player, decision, input, true);
	}

	public static void afterOriginalWomInput(MovementInputEvent event) {
		if (event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		restoreInput(player);
	}

	public static boolean shouldOwnOriginalWomSprintInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		return shouldUseOriginalWomWallRunControls(player, playerPatch);
	}

	public static boolean isWallRunRequestActive(Player player) {
		PlayerPatch<?> playerPatch = player == null ? null : EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return shouldUseOriginalWomWallRunControls(player, playerPatch)
				&& isWallRunKeyDown()
				&& isForwardDown(null);
	}

	private static boolean isWallRunKeyDown() {
		try {
			return KeyBindings.getKeyHorizontalWallRun().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolWallSlideKeyDown() {
		try {
			return KeyBindings.getKeyWallSlide().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isForwardDown(Input input) {
		if (input != null && input.up) {
			return true;
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyUp.isDown();
	}

	private static WallRunInputDecision resolveMouseWallRunInput(Player player) {
		Direction wallDirection = WomOriginalSpiderWallRunDirectionFix.activeWallDirection(player);
		if (wallDirection == null || !WomSpiderWallContactResolver.hasAdjacentWallDirection(player, wallDirection)) {
			wallDirection = WomSpiderWallContactResolver.detectAdjacentWallDirection(player, player.getViewYRot(1.0F));
		}
		if (wallDirection == null) {
			return new WallRunInputDecision(WallRunInputMode.NONE, null, 0.0D, 0.0D, 0);
		}

		Vec3 lookDirection = horizontalLook(player);
		if (lookDirection == null) {
			return new WallRunInputDecision(WallRunInputMode.NONE, wallDirection, 0.0D, 0.0D, 0);
		}

		Vec3 wallNormal = WomSpiderWallContactResolver.wallNormalDirection(wallDirection);
		double facingScore = wallNormal.dot(lookDirection);
		double sideScore = -wallNormal.x() * lookDirection.z() + wallNormal.z() * lookDirection.x();
		if (facingScore >= VERTICAL_FACING_DOT) {
			return new WallRunInputDecision(WallRunInputMode.VERTICAL, wallDirection, facingScore, sideScore, 0);
		}
		if (Math.abs(sideScore) >= HORIZONTAL_SIDE_DOT) {
			return new WallRunInputDecision(WallRunInputMode.HORIZONTAL, wallDirection, facingScore, sideScore, sideScore > 0.0D ? 1 : -1);
		}
		return new WallRunInputDecision(WallRunInputMode.NONE, wallDirection, facingScore, sideScore, 0);
	}

	private static Vec3 horizontalLook(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 horizontalLook = new Vec3(look.x(), 0.0D, look.z());
		if (horizontalLook.lengthSqr() < 1.0E-6D) {
			Vec3 yawLook = Vec3.directionFromRotation(0.0F, player.getViewYRot(1.0F));
			horizontalLook = new Vec3(yawLook.x(), 0.0D, yawLook.z());
		}
		return horizontalLook.lengthSqr() < 1.0E-6D ? null : horizontalLook.normalize();
	}

	private static void applyDecision(Input input, WallRunInputDecision decision) {
		input.down = false;
		if (decision.mode() == WallRunInputMode.VERTICAL) {
			input.up = true;
			input.left = false;
			input.right = false;
			return;
		}

		input.up = false;
		input.left = decision.sideInput() > 0;
		input.right = decision.sideInput() < 0;
	}

	private static void restoreInput(Player player) {
		if (player == null) {
			return;
		}

		InputSnapshot snapshot = INPUT_SNAPSHOTS.remove(player);
		WALL_RUN_DECISIONS.remove(player);
		if (snapshot != null) {
			snapshot.restore();
		}
	}

	private static boolean isAppliedDecision(WallRunInputDecision decision) {
		return decision != null && decision.mode() != WallRunInputMode.NONE && decision.mode() != WallRunInputMode.GLIDE;
	}

	private static String blockedWallRunReason(boolean wallRunKeyDown, boolean forwardDown, boolean backflipActive, WallRunInputDecision decision) {
		if (!wallRunKeyDown) {
			return "wallrun_key_up";
		}
		if (!forwardDown) {
			return "forward_up";
		}
		if (backflipActive) {
			return "wom_backflip_active";
		}
		if (!isAppliedDecision(decision)) {
			return "no_wallrun_mouse_decision";
		}
		return "blocked";
	}

	private static String blockedWallGlideReason(boolean glideControlDown, boolean forwardDown, boolean backflipActive, boolean climbUpActive, Player player, Direction wallDirection) {
		if (!glideControlDown) {
			return "glide_key_up";
		}
		if (forwardDown) {
			return "forward_down_wallrun_branch";
		}
		if (backflipActive) {
			return "wom_backflip_active";
		}
		if (climbUpActive) {
			return "parcool_climbup_active";
		}
		if (player == null) {
			return "missing_player";
		}
		if (player.onGround()) {
			return "on_ground";
		}
		if (wallDirection == null) {
			return "no_wall";
		}
		return "blocked";
	}

	private static void logAdapterInput(Player player, String branch, boolean allow, String reason, boolean wallRunKeyDown, boolean wallSlideKeyDown, boolean forwardDown, WallRunInputDecision decision) {
		if (!EPMConfig.debugSpiderWallRunState() || player == null || !player.isLocalPlayer()) {
			return;
		}
		Integer previousTick = LAST_LOG_TICKS.get(player);
		if (previousTick != null && player.tickCount - previousTick.intValue() < LOG_INTERVAL_TICKS) {
			return;
		}

		LAST_LOG_TICKS.put(player, Integer.valueOf(player.tickCount));
		EPM.LOGGER.info(
				"[EPM/WomWallRunControlAdapter] branch={} phase=sprint_input tick={} allow={} reason={} mode={} wall={} wallRunKeyDown={} wallSlideKeyDown={} forwardDown={} onGround={} delta={} yRot={} yHeadRot={} yBodyRot={} state={}",
				branch,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(allow),
				reason,
				decision == null ? WallRunInputMode.NONE : decision.mode(),
				decision == null ? null : decision.wallDirection(),
				Boolean.valueOf(wallRunKeyDown),
				Boolean.valueOf(wallSlideKeyDown),
				Boolean.valueOf(forwardDown),
				Boolean.valueOf(player.onGround()),
				player.getDeltaMovement(),
				Float.valueOf(player.getViewYRot(1.0F)),
				Float.valueOf(player.yHeadRot),
				Float.valueOf(player.yBodyRot),
				WomCompatBridge.instance().describeSpiderTechniquesState(EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class)));
	}

	private static void logMouseInput(Player player, WallRunInputDecision decision, Input input, boolean applied) {
		if (!EPMConfig.debugSpiderWallRunState() || player == null || !player.isLocalPlayer()) {
			return;
		}

		Integer previousTick = LAST_LOG_TICKS.get(player);
		if (previousTick != null && player.tickCount - previousTick.intValue() < LOG_INTERVAL_TICKS) {
			return;
		}

		LAST_LOG_TICKS.put(player, Integer.valueOf(player.tickCount));
		EPM.LOGGER.info(
				"[EPM/WomWallRunControlAdapter] phase=mouse_input tick={} applied={} mode={} wall={} facingScore={} sideScore={} sideInput={} up={} left={} right={} down={} viewYaw={} delta={}",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(applied),
				decision.mode(),
				decision.wallDirection(),
				Double.valueOf(decision.facingScore()),
				Double.valueOf(decision.sideScore()),
				Integer.valueOf(decision.sideInput()),
				Boolean.valueOf(input != null && input.up),
				Boolean.valueOf(input != null && input.left),
				Boolean.valueOf(input != null && input.right),
				Boolean.valueOf(input != null && input.down),
				Float.valueOf(player.getViewYRot(1.0F)),
				player.getDeltaMovement());
	}

	private enum WallRunInputMode {
		NONE,
		VERTICAL,
		HORIZONTAL,
		GLIDE
	}

	private record WallRunInputDecision(WallRunInputMode mode, Direction wallDirection, double facingScore, double sideScore, int sideInput) {
	}

	private record InputSnapshot(Input input, boolean up, boolean down, boolean left, boolean right) {
		static InputSnapshot capture(Input input) {
			return new InputSnapshot(input, input.up, input.down, input.left, input.right);
		}

		void restore() {
			input.up = up;
			input.down = down;
			input.left = left;
			input.right = right;
		}
	}
}
