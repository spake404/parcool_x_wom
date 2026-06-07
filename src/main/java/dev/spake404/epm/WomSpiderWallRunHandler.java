package dev.spake404.epm;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

/**
 * PARCOOL 模式下的 WOM Spider Techniques 跑墙接管逻辑。
 *
 * 只有 {@link WomSpiderWallRunModeGate#canUseParCoolReplacement} 通过时，这个类才会接管移动：
 * WOM 已安装、玩家已学习 Spider Techniques，并且 spiderTechniquesWallRunMode 为 PARCOOL。
 */
public final class WomSpiderWallRunHandler {
	private static final float WALL_RUN_STAMINA_COST = 0.5F;
	private static final double VERTICAL_FACING_DOT = 0.93D;
	private static final double HORIZONTAL_SIDE_DOT = 0.9D;
	private static final double TRANSITION_DOT = 0.25D;
	private static final double WALL_RUN_HORIZONTAL_SPEED = 0.1D;
	private static final double WALL_RUN_SIDE_HORIZONTAL_SPEED = 0.075D;
	private static final double WALL_RUN_SIDE_MOMENTUM = 0.72D;
	private static final double WALL_RUN_SIDE_MAX_HORIZONTAL_SPEED = 0.26D;
	private static final double WALL_RUN_VERTICAL_SPEED = 0.3D;
	private static final double WALL_RUN_SIDE_VERTICAL_SPEED = -0.02D;
	private static final int HORIZONTAL_WALL_CONTACT_GRACE_TICKS = 2;
	private static final int WALL_RUN_MODE_STICK_TICKS = 5;
	private static final int WALL_RUN_MODE_SWITCH_CONFIRM_TICKS = 3;
	private static final int PARCOOL_CORNER_TRANSFER_COOLDOWN_TICKS = 6;
	private static final int STALE_STATE_PROBE_INTERVAL_TICKS = 5;
	private static final double PARCOOL_CAMERA_SIDE_SWITCH_DOT = 0.35D;
	private static final double PARCOOL_CAMERA_SIDE_SWITCH_MARGIN = 0.1D;
	private static final WeakHashMap<Player, WallRunState> ACTIVE_WALL_RUNS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> WALL_RUN_KEY_RELEASE_REQUIRED = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_MOVEMENT_INPUT_TICK = new WeakHashMap<>();

	private WomSpiderWallRunHandler() {
	}

	public static void tick(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		boolean wallRunKeyDown = isWallRunControlDown();
		clearRestartGateIfKeyReleased(player, wallRunKeyDown);
		// 非 PARCOOL 模式下，这个 handler 不能持有 WOM 跑墙状态。
		if (!WomSpiderWallRunModeGate.canUseParCoolReplacement(player, playerPatch)) {
			stopOwnedWallRun(player, playerPatch, wallRunKeyDown);
			return;
		}

		if (isWomWallBackflipAnimation(playerPatch)) {
			logWallRunState("tick_stop", "wall_backflip_animation", player, playerPatch, ACTIVE_WALL_RUNS.get(player), wallRunKeyDown);
			removeActiveWallRun(player, wallRunKeyDown);
			NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
			return;
		}

		if (isParCoolWallJumpActive(player) || isEpicParCoolWallJumpAnimation(playerPatch)) {
			logWallRunState("tick_stop", "parcool_walljump", player, playerPatch, ACTIVE_WALL_RUNS.get(player), wallRunKeyDown);
			// ParCool 蹬墙跳优先级高于替换跑墙；这里清掉本类状态，避免跑墙动画残留。
			suspendForParCoolWallJump(player, playerPatch, wallRunKeyDown);
			return;
		}

		if (isParCoolClimbUpActive(player) || isEpicParCoolClimbUpAnimation(playerPatch)) {
			logWallRunState("tick_stop", "parcool_climbup", player, playerPatch, ACTIVE_WALL_RUNS.get(player), wallRunKeyDown);
			// ParCool ClimbUp/翻越动画可能在 isDoing() 变 false 后继续播放，所以动作状态和动画都要检查。
			suspendForParCoolClimbUp(player, playerPatch, wallRunKeyDown);
			return;
		}

		WallRunState previous = ACTIVE_WALL_RUNS.get(player);
		logWallRunState("tick_head", "head", player, playerPatch, previous, wallRunKeyDown);
		if (shouldStopBecauseLanded(player, previous)) {
			logWallRunState("tick_stop", "landed", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_tick_landed");
			return;
		}
		// PARCOOL 替换模式使用 ParCool 跑墙键 + 前进键作为跑墙持续输入。
		if (!wallRunKeyDown || !canAttemptWallRun(player, playerPatch)) {
			logWallRunState("tick_stop", "inactive", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_tick_inactive");
			return;
		}

		if (handledMovementInputThisTick(player)) {
			// MovementInputEvent 是主路径；tick 路径只补没有收到输入事件的帧。
			return;
		}

		if (shouldWaitForWallRunKeyRelease(player)) {
			return;
		}

		DecisionResult decisionResult = resolveWallRunDecision(player);
		if (decisionResult == null || !canUseDecisionFromInput(player, decisionResult.decision())) {
			logWallRunState("tick_stop", decisionResult == null ? "no_wall" : "input_rejected", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_tick_no_wall");
			return;
		}

		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch)) {
			logWallRunState("tick_stop", "missing_local_patch", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_tick_missing_local_patch");
			return;
		}

		boolean jumpHeld = previous == null ? isJumpKeyDown() : previous.jumpHeld();
		applyWallRun(player, localPlayerPatch, decisionResult.decision(), jumpHeld);
		WallRunState next = nextWallRunState(player, previous, decisionResult, jumpHeld);
		ACTIVE_WALL_RUNS.put(player, next);
		logWallRunState("tick_apply", "apply", player, playerPatch, next, wallRunKeyDown);
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
	}

	public static boolean handleMovementInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		boolean wallRunKeyDown = isWallRunControlDown();
		clearRestartGateIfKeyReleased(player, wallRunKeyDown);
		// 返回 false 表示 DEFAULT/WOM 模式不由本类接管，保持 WOM/ParCool 原逻辑。
		if (!WomSpiderWallRunModeGate.canUseParCoolReplacement(player, playerPatch)) {
			return false;
		}
		markMovementInputHandled(player);

		if (isWomWallBackflipAnimation(playerPatch)) {
			logWallRunState("input_stop", "wall_backflip_animation", player, playerPatch, ACTIVE_WALL_RUNS.get(player), wallRunKeyDown);
			removeActiveWallRun(player, wallRunKeyDown);
			NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
			return true;
		}

		if (isParCoolWallJumpActive(player) || isEpicParCoolWallJumpAnimation(playerPatch)) {
			logWallRunState("input_stop", "parcool_walljump", player, playerPatch, ACTIVE_WALL_RUNS.get(player), wallRunKeyDown);
			// 让 ParCool 自己完成蹬墙跳，本帧不要重新写入 WOM Spider 跑墙状态。
			suspendForParCoolWallJump(player, playerPatch, wallRunKeyDown);
			return true;
		}

		if (isParCoolClimbUpActive(player) || isEpicParCoolClimbUpAnimation(playerPatch)) {
			logWallRunState("input_stop", "parcool_climbup", player, playerPatch, ACTIVE_WALL_RUNS.get(player), wallRunKeyDown);
			// ClimbUp 对替换跑墙是硬中断；否则翻越动画后可能接上旧的跑墙状态。
			suspendForParCoolClimbUp(player, playerPatch, wallRunKeyDown);
			return true;
		}

		WallRunState previous = ACTIVE_WALL_RUNS.get(player);
		logWallRunState("input_head", "head", player, playerPatch, previous, wallRunKeyDown);
		if (shouldStopBecauseLanded(player, previous)) {
			logWallRunState("input_stop", "landed", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_input_landed");
			return true;
		}

		if (!wallRunKeyDown || !canAttemptWallRun(player, playerPatch)) {
			logWallRunState("input_stop", "inactive", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_input_inactive");
			return true;
		}

		if (shouldWaitForWallRunKeyRelease(player)) {
			logWallRunState("input_wait", "wait_key_release", player, playerPatch, previous, wallRunKeyDown);
			return true;
		}

		DecisionResult decisionResult = resolveWallRunDecision(player);
		if (decisionResult == null || !canUseDecisionFromInput(player, decisionResult.decision())) {
			logWallRunState("input_stop", decisionResult == null ? "no_wall" : "input_rejected", player, playerPatch, previous, wallRunKeyDown);
			stop(player, playerPatch, wallRunKeyDown);
			WomSpiderWallSlideHandler.clearStaleWallState(player, playerPatch, "wallrun_input_no_wall");
			return true;
		}

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			boolean jumping = event.getMovementInput() != null && event.getMovementInput().jumping;
			if (jumping && previous != null && !previous.jumpHeld()) {
				logWallRunState("input_stop", "wall_backflip", player, playerPatch, previous, wallRunKeyDown);
				triggerWallBackflip(player, localPlayerPatch, decisionResult.decision());
				return true;
			}

			applyWallRun(player, localPlayerPatch, decisionResult.decision(), jumping);
			WallRunState next = nextWallRunState(player, previous, decisionResult, jumping);
			previous = ACTIVE_WALL_RUNS.put(player, next);
			logDecisionChange(player, previous == null ? null : previous.decision(), decisionResult.decision());
			logWallRunState("input_apply", "apply", player, playerPatch, next, wallRunKeyDown);
			NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
		}
		return true;
	}

	public static boolean isHorizontalWallRunActive(Player player) {
		WallRunState state = player == null ? null : ACTIVE_WALL_RUNS.get(player);
		return state != null && state.decision().mode() == WallRunMode.HORIZONTAL;
	}

	public static boolean isWallRunActive(Player player) {
		return player != null && ACTIVE_WALL_RUNS.containsKey(player);
	}

	public static boolean shouldBlockParCoolClimbUp(Player player) {
		if (player == null || !player.isLocalPlayer() || player.onGround()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canUseParCoolReplacement(player, playerPatch)) {
			return false;
		}

		WallRunState active = ACTIVE_WALL_RUNS.get(player);
		if (active != null && active.decision().mode() == WallRunMode.VERTICAL) {
			return true;
		}

		return isWomVerticalWallRunAnimation(playerPatch);
	}

	static Direction activeWallDirection(Player player) {
		WallRunState state = player == null ? null : ACTIVE_WALL_RUNS.get(player);
		if (state == null) {
			return null;
		}
		return state.lockedWall() == null ? wallDirectionForDecision(state.decision()) : state.lockedWall();
	}

	private static boolean canAttemptWallRun(Player player, PlayerPatch<?> playerPatch) {
		return player instanceof LocalPlayer
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& !player.onGround()
				&& player.getVehicle() == null
				&& playerPatch instanceof LocalPlayerPatch localPlayerPatch
				&& localPlayerPatch.hasStamina(WALL_RUN_STAMINA_COST);
	}

	private static boolean canUseDecisionFromInput(Player player, WallRunDecision decision) {
		if (decision == null) {
			return false;
		}
		if (decision.mode() == WallRunMode.VERTICAL) {
			return true;
		}
		return !player.onGround() && isForwardKeyDown();
	}

	private static boolean shouldStopBecauseLanded(Player player, WallRunState previous) {
		return previous != null
				&& (player.onGround() || hasGroundSupport(player));
	}

	private static boolean hasGroundSupport(Player player) {
		return detectGroundSupport(player).present();
	}

	private static GroundSupport detectGroundSupport(Player player) {
		Level level = player.level();
		AABB supportBox = player.getBoundingBox()
				.deflate(0.08D, 0.0D, 0.08D)
				.move(0.0D, -0.08D, 0.0D);
		int minX = (int) Math.floor(supportBox.minX);
		int minY = (int) Math.floor(supportBox.minY);
		int minZ = (int) Math.floor(supportBox.minZ);
		int maxX = (int) Math.floor(supportBox.maxX);
		int maxY = (int) Math.floor(supportBox.maxY);
		int maxZ = (int) Math.floor(supportBox.maxZ);

		for (BlockPos blockPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			BlockState blockState = level.getBlockState(blockPos);
			if (blockState.isAir()) {
				continue;
			}

			VoxelShape collisionShape = blockState.getCollisionShape(level, blockPos);
			if (collisionShape.isEmpty()) {
				continue;
			}

			for (AABB collisionBox : collisionShape.toAabbs()) {
				if (collisionBox.move(blockPos.getX(), blockPos.getY(), blockPos.getZ()).intersects(supportBox)) {
					return new GroundSupport(true, blockPos.immutable(), String.valueOf(blockState.getBlock()));
				}
			}
		}
		return new GroundSupport(false, null, "none");
	}

	private static void markMovementInputHandled(Player player) {
		if (player != null) {
			LAST_MOVEMENT_INPUT_TICK.put(player, Integer.valueOf(player.tickCount));
		}
	}

	private static boolean handledMovementInputThisTick(Player player) {
		Integer tick = player == null ? null : LAST_MOVEMENT_INPUT_TICK.get(player);
		return tick != null && tick.intValue() == player.tickCount;
	}

	private static boolean isWallRunControlDown() {
		return isWallRunKeyDown() && isForwardKeyDown();
	}

	private static boolean isWallRunKeyDown() {
		try {
			return KeyBindings.getKeyHorizontalWallRun().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isForwardKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyUp.isDown();
	}

	private static DecisionResult resolveWallRunDecision(Player player) {
		WallRunState active = ACTIVE_WALL_RUNS.get(player);
		if (active != null && active.decision().mode() == WallRunMode.HORIZONTAL) {
			return resolveForwardCornerHorizontalDecision(player, active);
		}

		List<WallContact> contacts = detectWallContacts(player);
		Vec3 preferredWallDirection = active == null ? null : active.decision().wallDirection();
		WallRunCandidates candidates = findWallRunCandidates(player, preferredWallDirection, contacts);
		WallRunSelection detected = selectWallRunDecision(player, active, candidates);
		if (detected != null) {
			return new DecisionResult(detected.decision(), 0, detected.pendingMode(), detected.pendingModeTicks(), wallDirectionForDecision(detected.decision()), null, false);
		}

		return null;
	}

	private static DecisionResult resolveForwardCornerHorizontalDecision(Player player, WallRunState active) {
		Direction lockedWall = active.lockedWall() == null ? wallDirectionForDecision(active.decision()) : active.lockedWall();
		if (lockedWall == null || active.decision().womSide() == 0) {
			return null;
		}

		WallContact lockedContact = adjacentWallContact(player, lockedWall);
		DecisionResult verticalSwitch = resolveLockedHorizontalVerticalSwitch(player, active, lockedWall, lockedContact);
		if (verticalSwitch != null) {
			return verticalSwitch;
		}

		int womSide = cameraControlledWomSide(player, lockedWall, active.decision().womSide());
		if (womSide != active.decision().womSide()) {
			logParCoolCameraSideSwitch(player, lockedWall, active.decision().womSide(), womSide);
		}

		Vec3 forwardRunDirection = WomSpiderWallCornerTransfer.targetRunDirection(lockedWall, womSide);
		int cooldownTicks = Math.max(0, active.cornerCooldownTicks() - tickIncrement(player, active));
		ForwardCornerContact cornerContact = findForwardAdjacentCornerContact(player, lockedWall, forwardRunDirection, active.previousWall(), cooldownTicks <= 0);
		if (cornerContact != null) {
			WallRunDecision decision = horizontalDecisionForLockedWall(cornerContact.wallDirection(), womSide, cornerContact.contact());
			logParCoolForwardCornerTransfer(player, lockedWall, cornerContact.wallDirection(), womSide, forwardRunDirection, decision);
			return new DecisionResult(decision, 0, null, 0, cornerContact.wallDirection(), lockedWall, true);
		}

		if (lockedContact == null) {
			logParCoolLockedWallLost(player, lockedWall, forwardRunDirection);
			return null;
		}

		WallRunDecision decision = horizontalDecisionForLockedWall(lockedWall, womSide, lockedContact);
		return new DecisionResult(decision, 0, null, 0, lockedWall, active.previousWall(), false);
	}

	private static DecisionResult resolveLockedHorizontalVerticalSwitch(Player player, WallRunState active, Direction lockedWall, WallContact lockedContact) {
		WallRunDecision verticalDecision = verticalDecisionForLockedWall(player, lockedWall, lockedContact);
		if (verticalDecision == null) {
			return null;
		}

		int pendingTicks = active.pendingMode() == WallRunMode.VERTICAL ? active.pendingModeTicks() + tickIncrement(player, active) : 1;
		if (active.modeTicks() < WALL_RUN_MODE_STICK_TICKS || pendingTicks < WALL_RUN_MODE_SWITCH_CONFIRM_TICKS) {
			WallRunDecision horizontalDecision = horizontalDecisionForLockedWall(lockedWall, active.decision().womSide(), lockedContact);
			if (pendingTicks == 1) {
				logParCoolVerticalSwitchPending(player, lockedWall, pendingTicks);
			}
			return new DecisionResult(horizontalDecision, 0, WallRunMode.VERTICAL, pendingTicks, lockedWall, active.previousWall(), false);
		}

		logParCoolVerticalSwitch(player, lockedWall, verticalDecision);
		return new DecisionResult(verticalDecision, 0, null, 0, null, null, false);
	}

	private static WallRunDecision verticalDecisionForLockedWall(Player player, Direction lockedWall, WallContact lockedContact) {
		if (lockedContact == null) {
			return null;
		}

		Vec3 lookDirection = horizontalLook(player);
		Vec3 wallDirection = WomSpiderWallCornerTransfer.wallVector(lockedWall);
		if (wallDirection.dot(lookDirection) < VERTICAL_FACING_DOT) {
			return null;
		}

		return new WallRunDecision(WallRunMode.VERTICAL, 0, wallDirection, lookDirection, lockedContact.blockState(), lockedContact.blockPos());
	}

	private static int cameraControlledWomSide(Player player, Direction lockedWall, int currentSide) {
		if (currentSide != -1 && currentSide != 1) {
			return currentSide;
		}

		Vec3 lookDirection = horizontalLook(player);
		Vec3 currentDirection = WomSpiderWallCornerTransfer.targetRunDirection(lockedWall, currentSide);
		int oppositeSide = -currentSide;
		Vec3 oppositeDirection = WomSpiderWallCornerTransfer.targetRunDirection(lockedWall, oppositeSide);
		double currentScore = currentDirection.dot(lookDirection);
		double oppositeScore = oppositeDirection.dot(lookDirection);
		if (oppositeScore > PARCOOL_CAMERA_SIDE_SWITCH_DOT
				&& oppositeScore > currentScore + PARCOOL_CAMERA_SIDE_SWITCH_MARGIN) {
			return oppositeSide;
		}
		return currentSide;
	}

	private static ForwardCornerContact findForwardAdjacentCornerContact(Player player, Direction activeWall, Vec3 runDirection, Direction previousWall, boolean allowAnyCorner) {
		Direction bestDirection = null;
		WallContact bestContact = null;
		double bestScore = 0.65D;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (!allowAnyCorner && direction != previousWall) {
				continue;
			}
			if (!WomSpiderWallCornerTransfer.canTransferTo(activeWall, runDirection, direction)) {
				continue;
			}
			WallContact contact = adjacentWallContact(player, direction);
			if (contact == null) {
				continue;
			}

			double score = WomSpiderWallContactResolver.wallNormalDirection(direction).dot(runDirection);
			if (score > bestScore) {
				bestScore = score;
				bestDirection = direction;
				bestContact = contact;
			}
		}
		return bestDirection == null ? null : new ForwardCornerContact(bestDirection, bestContact);
	}

	private static WallRunDecision horizontalDecisionForLockedWall(Direction wallDirection, int womSide, WallContact contact) {
		Vec3 wallVector = WomSpiderWallCornerTransfer.wallVector(wallDirection);
		Vec3 runDirection = WomSpiderWallCornerTransfer.targetRunDirection(wallDirection, womSide);
		return new WallRunDecision(WallRunMode.HORIZONTAL, womSide, wallVector, runDirection, contact.blockState(), contact.blockPos());
	}

	private static WallContact adjacentWallContact(Player player, Direction direction) {
		if (player == null || direction == null || !WomSpiderWallContactResolver.hasAdjacentWallDirection(player, direction)) {
			return null;
		}

		Level level = player.level();
		BlockPos blockPos = adjacentWallBlockPos(player, direction);
		BlockState blockState = level.getBlockState(blockPos);
		if (!isValidWomWallBlock(blockState, blockPos, level)) {
			return null;
		}
		return new WallContact(Vec3.atLowerCornerOf(direction.getNormal()), blockState, blockPos);
	}

	private static WallRunSelection selectWallRunDecision(Player player, WallRunState active, WallRunCandidates candidates) {
		if (candidates == null) {
			return null;
		}

		WallRunDecision decision;
		if (active != null && active.decision().mode() == WallRunMode.HORIZONTAL) {
			if (candidates.horizontal() != null) {
				decision = candidates.horizontal();
			} else if (candidates.horizontalFallback() != null) {
				decision = candidates.horizontalFallback();
			} else if (candidates.vertical() != null && active.missedContactTicks() >= HORIZONTAL_WALL_CONTACT_GRACE_TICKS) {
				decision = candidates.vertical();
			} else {
				return null;
			}
		} else if (active != null && active.decision().mode() == WallRunMode.VERTICAL) {
			if (candidates.vertical() != null) {
				decision = candidates.vertical();
			} else if (candidates.horizontal() != null) {
				decision = candidates.horizontal();
			} else {
				decision = candidates.verticalFallback() != null ? candidates.verticalFallback() : candidates.horizontalFallback();
			}
		} else {
			decision = candidates.vertical() != null ? candidates.vertical() : candidates.horizontal();
		}

		if (decision == null) {
			return null;
		}
		return applyModeSwitchWeight(player, active, candidates, decision);
	}

	private static WallRunCandidates findWallRunCandidates(Player player, Vec3 preferredWallDirection, List<WallContact> contacts) {
		if (contacts.isEmpty()) {
			return null;
		}

		if (preferredWallDirection != null) {
			List<WallContact> preferredContacts = null;
			for (WallContact contact : contacts) {
				if (sameWallDirection(contact.wallDirection(), preferredWallDirection)) {
					if (preferredContacts == null) {
						preferredContacts = new ArrayList<>(contacts.size());
					}
					preferredContacts.add(contact);
				}
			}

			if (preferredContacts != null) {
				WallRunCandidates preferredCandidates = buildWallRunCandidates(player, preferredContacts);
				if (hasAnyCandidate(preferredCandidates)) {
					return preferredCandidates;
				}
			}
		}

		return buildWallRunCandidates(player, contacts);
	}

	private static WallRunCandidates buildWallRunCandidates(Player player, List<WallContact> contacts) {
		Vec3 lookDirection = horizontalLook(player);
		WallRunDecision vertical = null;
		WallRunDecision horizontal = null;
		WallRunDecision verticalFallback = null;
		WallRunDecision horizontalFallback = null;
		double bestVerticalScore = VERTICAL_FACING_DOT;
		double bestHorizontalScore = HORIZONTAL_SIDE_DOT;
		double bestVerticalFallbackScore = TRANSITION_DOT;
		double bestHorizontalFallbackScore = TRANSITION_DOT;

		for (WallContact contact : contacts) {
			Vec3 wallDirection = contact.wallDirection();
			double facingScore = wallDirection.dot(lookDirection);
			WallRunDecision verticalCandidate = new WallRunDecision(WallRunMode.VERTICAL, 0, wallDirection, lookDirection, contact.blockState(), contact.blockPos());
			if (facingScore > bestVerticalScore) {
				bestVerticalScore = facingScore;
				vertical = verticalCandidate;
			}
			if (facingScore > bestVerticalFallbackScore) {
				bestVerticalFallbackScore = facingScore;
				verticalFallback = verticalCandidate;
			}

			double sideScore = parCoolSideScore(wallDirection, lookDirection);
			double absSideScore = Math.abs(sideScore);
			Vec3 runDirection = wallDirection.yRot((float) (Math.PI / 2.0D));
			if (runDirection.dot(lookDirection) < 0.0D) {
				runDirection = runDirection.reverse();
			}
			int womSide = sideScore > 0.0D ? 1 : -1;
			WallRunDecision horizontalCandidate = new WallRunDecision(WallRunMode.HORIZONTAL, womSide, wallDirection, runDirection, contact.blockState(), contact.blockPos());
			if (absSideScore > bestHorizontalScore) {
				bestHorizontalScore = absSideScore;
				horizontal = horizontalCandidate;
			}
			if (absSideScore > bestHorizontalFallbackScore) {
				bestHorizontalFallbackScore = absSideScore;
				horizontalFallback = horizontalCandidate;
			}
		}

		return new WallRunCandidates(vertical, horizontal, verticalFallback, horizontalFallback);
	}

	private static boolean hasAnyCandidate(WallRunCandidates candidates) {
		return candidates != null
				&& (candidates.vertical() != null
				|| candidates.horizontal() != null
				|| candidates.verticalFallback() != null
				|| candidates.horizontalFallback() != null);
	}

	private static boolean sameWallDirection(Vec3 left, Vec3 right) {
		return left.distanceToSqr(right) < 1.0E-6D;
	}

	private static Direction wallDirectionForDecision(WallRunDecision decision) {
		return decision == null || decision.mode() != WallRunMode.HORIZONTAL
				? null
				: WomSpiderWallCornerTransfer.directionFromWallVector(decision.wallDirection());
	}

	private static WallRunSelection applyModeSwitchWeight(Player player, WallRunState active, WallRunCandidates candidates, WallRunDecision decision) {
		if (active == null || active.decision().mode() == decision.mode()) {
			return new WallRunSelection(decision, null, 0);
		}

		WallRunDecision currentModeDecision = bestCandidateForMode(candidates, active.decision().mode());
		if (currentModeDecision == null) {
			return new WallRunSelection(decision, null, 0);
		}

		int pendingTicks = active.pendingMode() == decision.mode() ? active.pendingModeTicks() + tickIncrement(player, active) : 1;
		if (active.modeTicks() < WALL_RUN_MODE_STICK_TICKS || pendingTicks < WALL_RUN_MODE_SWITCH_CONFIRM_TICKS) {
			return new WallRunSelection(currentModeDecision, decision.mode(), pendingTicks);
		}

		return new WallRunSelection(decision, null, 0);
	}

	private static WallRunDecision bestCandidateForMode(WallRunCandidates candidates, WallRunMode mode) {
		if (mode == WallRunMode.VERTICAL) {
			return candidates.vertical() != null ? candidates.vertical() : candidates.verticalFallback();
		}
		return candidates.horizontal() != null ? candidates.horizontal() : candidates.horizontalFallback();
	}

	private static WallRunState nextWallRunState(Player player, WallRunState previous, DecisionResult decisionResult, boolean jumpHeld) {
		int tickIncrement = previous == null || previous.lastUpdateTick() != player.tickCount ? 1 : 0;
		int modeTicks = previous != null && previous.decision().mode() == decisionResult.decision().mode()
				? previous.modeTicks() + tickIncrement
				: 1;
		Direction lockedWall = decisionResult.decision().mode() == WallRunMode.HORIZONTAL ? decisionResult.lockedWall() : null;
		Direction previousWall = decisionResult.decision().mode() == WallRunMode.HORIZONTAL ? decisionResult.previousWall() : null;
		int cornerCooldownTicks = decisionResult.cornerTransfer()
				? PARCOOL_CORNER_TRANSFER_COOLDOWN_TICKS
				: previous != null && decisionResult.decision().mode() == WallRunMode.HORIZONTAL
				? Math.max(0, previous.cornerCooldownTicks() - tickIncrement)
				: 0;
		return new WallRunState(
				decisionResult.decision(),
				decisionResult.missedContactTicks(),
				jumpHeld,
				modeTicks,
				decisionResult.pendingMode(),
				decisionResult.pendingModeTicks(),
				lockedWall,
				previousWall,
				cornerCooldownTicks,
				player.tickCount);
	}

	private static int tickIncrement(Player player, WallRunState active) {
		return active.lastUpdateTick() == player.tickCount ? 0 : 1;
	}

	private static List<WallContact> detectWallContacts(Player player) {
		Level level = player.level();
		Vec3 forward = horizontalLook(player);
		Vec3 right = rightDirection(forward);

		BlockPos center = BlockPos.containing(player.getX(), player.getY(), player.getZ());
		BlockPos lower = BlockPos.containing(player.getX(), player.getY() - 0.5D, player.getZ());
		if (!isFreeForWomWallRun(level.getBlockState(center), center, level, false)
				|| !isFreeForWomWallRun(level.getBlockState(lower), lower, level, player.isInWater())) {
			return List.of();
		}

		List<WallContact> contacts = new ArrayList<>(7);
		addContact(player, contacts, forward.scale(0.7D));
		addContact(player, contacts, forward.scale(0.6D).subtract(right.scale(0.3D)));
		addContact(player, contacts, forward.scale(0.6D).add(right.scale(0.3D)));
		addAdjacentWallContacts(player, contacts);
		return contacts;
	}

	private static void addContact(Player player, List<WallContact> contacts, Vec3 offset) {
		BlockPos blockPos = BlockPos.containing(player.getX() + offset.x(), player.getY() + 0.3D, player.getZ() + offset.z());
		Level level = player.level();
		BlockState blockState = level.getBlockState(blockPos);
		if (!isValidWomWallBlock(blockState, blockPos, level)) {
			return;
		}

		Vec3 wallDirection = cardinalDirectionTo(player, blockPos);
		if (wallDirection.lengthSqr() < 1.0E-6D) {
			return;
		}

		addContactIfAbsent(contacts, new WallContact(wallDirection, blockState, blockPos));
	}

	private static void addAdjacentWallContacts(Player player, List<WallContact> contacts) {
		Level level = player.level();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			AABB probeBox = wallProbeBox(player.getBoundingBox(), direction);
			if (level.noCollision(player, probeBox)) {
				continue;
			}

			BlockPos blockPos = adjacentWallBlockPos(player, direction);
			BlockState blockState = level.getBlockState(blockPos);
			if (isValidWomWallBlock(blockState, blockPos, level)) {
				addContactIfAbsent(contacts, new WallContact(Vec3.atLowerCornerOf(direction.getNormal()), blockState, blockPos));
			}
		}
	}

	private static AABB wallProbeBox(AABB box, Direction direction) {
		double reach = 0.5D;
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

	private static BlockPos adjacentWallBlockPos(Player player, Direction direction) {
		AABB box = player.getBoundingBox();
		return switch (direction) {
			case NORTH -> BlockPos.containing(player.getX(), player.getY() + 0.3D, box.minZ - 0.35D);
			case SOUTH -> BlockPos.containing(player.getX(), player.getY() + 0.3D, box.maxZ + 0.35D);
			case WEST -> BlockPos.containing(box.minX - 0.35D, player.getY() + 0.3D, player.getZ());
			case EAST -> BlockPos.containing(box.maxX + 0.35D, player.getY() + 0.3D, player.getZ());
			default -> player.blockPosition();
		};
	}

	private static void addContactIfAbsent(List<WallContact> contacts, WallContact contact) {
		for (WallContact existing : contacts) {
			if (existing.blockPos().equals(contact.blockPos()) && existing.wallDirection().equals(contact.wallDirection())) {
				return;
			}
		}

		contacts.add(contact);
	}

	private static boolean isValidWomWallBlock(BlockState blockState, BlockPos blockPos, Level level) {
		if (blockState.isAir() || blockState.is(Blocks.WATER) || blockState.is(BlockTags.ICE) || blockState.is(Blocks.BARRIER)) {
			return false;
		}

		VoxelShape collisionShape = blockState.getCollisionShape(level, blockPos);
		if (collisionShape.isEmpty()) {
			return false;
		}

		return blockState.isCollisionShapeFullBlock(level, blockPos) || blockState.is(BlockTags.SLABS) || blockState.is(BlockTags.STAIRS);
	}

	private static boolean isFreeForWomWallRun(BlockState blockState, BlockPos blockPos, Level level, boolean allowWater) {
		if (blockState.isAir() || blockState.getBlock() instanceof BushBlock || blockState.is(Blocks.SNOW)) {
			return true;
		}
		if (allowWater && blockState.is(Blocks.WATER)) {
			return true;
		}
		if (blockState.getBlock() instanceof CarpetBlock) {
			return false;
		}
		return blockState.getCollisionShape(level, blockPos).isEmpty();
	}

	private static Vec3 cardinalDirectionTo(Player player, BlockPos blockPos) {
		double dx = blockPos.getX() + 0.5D - player.getX();
		double dz = blockPos.getZ() + 0.5D - player.getZ();
		if (Math.abs(dx) > Math.abs(dz)) {
			return new Vec3(Math.signum(dx), 0.0D, 0.0D);
		}
		return new Vec3(0.0D, 0.0D, Math.signum(dz));
	}

	private static Vec3 horizontalLook(Player player) {
		float yaw = player.getViewYRot(1.0F);
		double radians = Math.toRadians(yaw);
		Vec3 look = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
		if (look.lengthSqr() < 1.0E-6D) {
			return new Vec3(0.0D, 0.0D, 1.0D);
		}
		return look;
	}

	private static Vec3 rightDirection(Vec3 forward) {
		return new Vec3(forward.z(), 0.0D, -forward.x());
	}

	private static double parCoolSideScore(Vec3 wallDirection, Vec3 lookDirection) {
		return -wallDirection.x() * lookDirection.z() + wallDirection.z() * lookDirection.x();
	}

	private static void applyWallRun(Player player, LocalPlayerPatch playerPatch, WallRunDecision decision, boolean jumpHeld) {
		player.stopFallFlying();
		AssetAccessor<? extends StaticAnimation> animation = animationFor(decision);
		if (animation != null) {
			AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
			if (!animation.equals(currentAnimation)) {
				playerPatch.playAnimationInClientSide(animation, EPMConfig.parCoolWallRunAnimationTransition());
			}
		}

		if (decision.mode() == WallRunMode.VERTICAL) {
			Vec3 horizontalMotion = decision.runDirection().scale(WALL_RUN_HORIZONTAL_SPEED);
			player.setDeltaMovement(horizontalMotion.x(), WALL_RUN_VERTICAL_SPEED, horizontalMotion.z());
		} else {
			Vec3 desiredMotion = decision.runDirection().scale(WALL_RUN_SIDE_HORIZONTAL_SPEED);
			Vec3 previousMotion = player.getDeltaMovement();
			Vec3 horizontalMotion = limitHorizontalSpeed(
					desiredMotion.x() + previousMotion.x() * WALL_RUN_SIDE_MOMENTUM,
					desiredMotion.z() + previousMotion.z() * WALL_RUN_SIDE_MOMENTUM,
					WALL_RUN_SIDE_MAX_HORIZONTAL_SPEED);
			player.setDeltaMovement(
					horizontalMotion.x(),
					WALL_RUN_SIDE_VERTICAL_SPEED,
					horizontalMotion.z());
		}

		playerPatch.setModelYRot(wallFacingYaw(decision.wallDirection()), true);
		WomCompatBridge.instance().setSpiderWallRunState(playerPatch, decision.womSide(), false, 3, jumpHeld);
	}

	private static void triggerWallBackflip(Player player, LocalPlayerPatch playerPatch, WallRunDecision decision) {
		player.stopFallFlying();
		stopWallRunAnimationsOnly(playerPatch);

		AssetAccessor<? extends StaticAnimation> animation = WomAnimationRefs.wallBackflip();
		if (animation != null) {
			playerPatch.playAnimationInClientSide(animation, 0.0F);
		}

		float xRot = decision.mode() == WallRunMode.VERTICAL ? 20.0F : 0.0F;
		WomCompatBridge.instance().triggerSpiderWallBackflipState(playerPatch, xRot, player.getViewYRot(1.0F));
		removeActiveWallRun(player, isWallRunKeyDown());
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
	}

	private static Vec3 limitHorizontalSpeed(double x, double z, double maxSpeed) {
		double speedSqr = x * x + z * z;
		double maxSpeedSqr = maxSpeed * maxSpeed;
		if (speedSqr <= maxSpeedSqr) {
			return new Vec3(x, 0.0D, z);
		}

		double scale = maxSpeed / Math.sqrt(speedSqr);
		return new Vec3(x * scale, 0.0D, z * scale);
	}

	private static AssetAccessor<? extends StaticAnimation> animationFor(WallRunDecision decision) {
		if (decision.mode() == WallRunMode.VERTICAL) {
			return WomAnimationRefs.wallRunning();
		}
		return decision.womSide() < 0 ? WomAnimationRefs.wallRunRightSide() : WomAnimationRefs.wallRunLeftSide();
	}

	private static float wallFacingYaw(Vec3 wallDirection) {
		return (float) Math.toDegrees(Math.atan2(-wallDirection.x(), wallDirection.z()));
	}

	private static boolean isJumpKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static void stop(Player player, PlayerPatch<?> playerPatch, boolean wallRunKeyDown) {
		if (player == null) {
			return;
		}

		WallRunState removed = removeActiveWallRun(player, wallRunKeyDown);
		if (removed == null && WomSpiderWallSlideHandler.shouldOwnWallState(player)) {
			return;
		}

		boolean wallRunAnimation = isWomWallRunAnimation(playerPatch);
		boolean wallMovementData = (removed != null || wallRunAnimation || shouldProbePassiveWallState(player))
				&& WomCompatBridge.instance().isSpiderWallMovementActive(playerPatch);
		if (removed == null && !wallRunAnimation && !wallMovementData) {
			return;
		}

		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		stopWallRunAnimation(playerPatch);
	}

	private static void stopOwnedWallRun(Player player, PlayerPatch<?> playerPatch, boolean wallRunKeyDown) {
		if (player == null) {
			return;
		}

		WallRunState removed = removeActiveWallRun(player, wallRunKeyDown);
		if (removed == null) {
			return;
		}

		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		stopWallRunAnimation(playerPatch);
	}

	private static WallRunState removeActiveWallRun(Player player, boolean wallRunKeyDown) {
		if (player == null) {
			return null;
		}

		WallRunState removed = ACTIVE_WALL_RUNS.remove(player);
		if (removed != null) {
			markRestartGate(player, wallRunKeyDown);
		}
		return removed;
	}

	private static void markRestartGate(Player player, boolean wallRunKeyDown) {
		if (player == null) {
			return;
		}

		// 跑墙被我们主动结束时，如果玩家还按着跑墙键，需要松开后才能重新进入。
		if (wallRunKeyDown) {
			WALL_RUN_KEY_RELEASE_REQUIRED.put(player, Boolean.TRUE);
		} else {
			WALL_RUN_KEY_RELEASE_REQUIRED.remove(player);
		}
	}

	private static void clearRestartGateIfKeyReleased(Player player, boolean wallRunKeyDown) {
		if (player != null && !wallRunKeyDown) {
			WALL_RUN_KEY_RELEASE_REQUIRED.remove(player);
		}
	}

	private static boolean shouldWaitForWallRunKeyRelease(Player player) {
		return player != null
				&& !ACTIVE_WALL_RUNS.containsKey(player)
				&& WALL_RUN_KEY_RELEASE_REQUIRED.containsKey(player);
	}

	private static boolean isWomWallBackflipAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallBackflip());
	}

	private static boolean isWomWallRunAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(
				currentBaseAnimation(playerPatch),
				WomAnimationRefs.wallRunning(),
				WomAnimationRefs.wallRunLeftSide(),
				WomAnimationRefs.wallRunRightSide(),
				WomAnimationRefs.wallGlide());
	}

	private static boolean isWomVerticalWallRunAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallRunning());
	}

	private static boolean shouldProbePassiveWallState(Player player) {
		return player != null && player.tickCount % STALE_STATE_PROBE_INTERVAL_TICKS == 0;
	}

	private static boolean isEpicParCoolWallJumpAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(
				currentBaseAnimation(playerPatch),
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight());
	}

	private static boolean isEpicParCoolClimbUpAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(
				currentBaseAnimation(playerPatch),
				WomAnimationRefs.epicParCoolFlipForward(),
				WomAnimationRefs.epicParCoolClimbUp(),
				WomAnimationRefs.epicParCoolClimbUpNoAction());
	}

	private static boolean isParCoolClimbUpActive(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			ClimbUp climbUp = parkourability == null ? null : parkourability.get(ClimbUp.class);
			return climbUp != null && climbUp.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolWallJumpActive(Player player) {
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
			return Class.forName(className, false, WomSpiderWallRunHandler.class.getClassLoader());
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

	private static void suspendForParCoolWallJump(Player player, PlayerPatch<?> playerPatch, boolean wallRunKeyDown) {
		removeActiveWallRun(player, wallRunKeyDown);
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopWallRunAnimationsOnly(localPlayerPatch);
		}
	}

	private static void suspendForParCoolClimbUp(Player player, PlayerPatch<?> playerPatch, boolean wallRunKeyDown) {
		removeActiveWallRun(player, wallRunKeyDown);
		// 翻越/爬墙中断时即使没有活动跑墙状态，也要等玩家松开跑墙键后才允许重新触发。
		markRestartGate(player, wallRunKeyDown);
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopWallRunAnimationsOnly(localPlayerPatch);
		}
	}

	private static void stopWallRunAnimation(PlayerPatch<?> playerPatch) {
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch)) {
			return;
		}

		stopWallRunAnimationsOnly(localPlayerPatch);
		try {
			localPlayerPatch.getClientAnimator().resetMotion(true);
			localPlayerPatch.getClientAnimator().resetCompositeMotion();
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void stopWallRunAnimationsOnly(LocalPlayerPatch playerPatch) {
		stopPlaying(playerPatch, WomAnimationRefs.wallRunning());
		stopPlaying(playerPatch, WomAnimationRefs.wallRunLeftSide());
		stopPlaying(playerPatch, WomAnimationRefs.wallRunRightSide());
		stopPlaying(playerPatch, WomAnimationRefs.wallGlide());
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

	private static void logWallRunState(String phase, String reason, Player player, PlayerPatch<?> playerPatch, WallRunState state, boolean wallRunKeyDown) {
		if (!EPMConfig.debugSpiderWallRunState() || player == null) {
			return;
		}

		boolean wallRunAnimation = isWomWallRunAnimation(playerPatch);
		if (state == null && !wallRunKeyDown && !wallRunAnimation && !shouldWaitForWallRunKeyRelease(player)) {
			return;
		}

		GroundSupport groundSupport = detectGroundSupport(player);
		EPM.LOGGER.info(
				"[EPM/SpiderWallRun] phase={} reason={} tick={} active={} mode={} modeTicks={} pending={} pendingTicks={} missed={} lockedWall={} previousWall={} wallRunKeyDown={} rawWallRunKeyDown={} forwardDown={} jumpDown={} waitRelease={} canAttempt={} onGround={} groundSupport={} supportBlock={} supportPos={} pos={} delta={} bbMinY={} bbMaxY={} animation={} womState={}",
				phase,
				reason,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(state != null),
				state == null ? "none" : state.decision().mode(),
				Integer.valueOf(state == null ? 0 : state.modeTicks()),
				state == null ? "none" : state.pendingMode(),
				Integer.valueOf(state == null ? 0 : state.pendingModeTicks()),
				Integer.valueOf(state == null ? 0 : state.missedContactTicks()),
				state == null ? "none" : state.lockedWall(),
				state == null ? "none" : state.previousWall(),
				Boolean.valueOf(wallRunKeyDown),
				Boolean.valueOf(isWallRunKeyDown()),
				Boolean.valueOf(isForwardKeyDown()),
				Boolean.valueOf(isJumpKeyDown()),
				Boolean.valueOf(shouldWaitForWallRunKeyRelease(player)),
				Boolean.valueOf(canAttemptWallRun(player, playerPatch)),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(groundSupport.present()),
				groundSupport.block(),
				groundSupport.blockPos(),
				player.position(),
				player.getDeltaMovement(),
				Double.valueOf(player.getBoundingBox().minY),
				Double.valueOf(player.getBoundingBox().maxY),
				currentBaseAnimation(playerPatch),
				describeSpiderState(playerPatch));
	}

	private static String describeSpiderState(PlayerPatch<?> playerPatch) {
		try {
			return WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch);
		} catch (RuntimeException | LinkageError ignored) {
			return "unavailable";
		}
	}

	private static void logDecisionChange(Player player, WallRunDecision previous, WallRunDecision decision) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		if (decision.equals(previous)) {
			return;
		}

		EPM.LOGGER.debug("[WomSpiderWallRun] mode={} womSide={} wallDirection={} runDirection={} yRot={} delta={} block={} pos={}",
				decision.mode(),
				Integer.valueOf(decision.womSide()),
				decision.wallDirection(),
				decision.runDirection(),
				Float.valueOf(player.getYRot()),
				player.getDeltaMovement(),
				decision.blockPos(),
				player.position());
	}

	private static void logParCoolCameraSideSwitch(Player player, Direction lockedWall, int fromSide, int toSide) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		EPM.LOGGER.debug("[WomSpiderWallRun] parCoolCameraSideSwitch wall={} fromSide={} toSide={} yRot={} pos={} delta={}",
				lockedWall,
				Integer.valueOf(fromSide),
				Integer.valueOf(toSide),
				Float.valueOf(player.getYRot()),
				player.position(),
				player.getDeltaMovement());
	}

	private static void logParCoolForwardCornerTransfer(Player player, Direction fromWall, Direction toWall, int womSide, Vec3 forwardRunDirection, WallRunDecision decision) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		EPM.LOGGER.debug("[WomSpiderWallRun] parCoolForwardCornerTransfer fromWall={} toWall={} womSide={} forwardRun={} newRun={} block={} pos={} delta={}",
				fromWall,
				toWall,
				Integer.valueOf(womSide),
				forwardRunDirection,
				decision.runDirection(),
				decision.blockPos(),
				player.position(),
				player.getDeltaMovement());
	}

	private static void logParCoolVerticalSwitchPending(Player player, Direction lockedWall, int pendingTicks) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		EPM.LOGGER.debug("[WomSpiderWallRun] parCoolVerticalSwitchPending wall={} pendingTicks={} yRot={} pos={} delta={}",
				lockedWall,
				Integer.valueOf(pendingTicks),
				Float.valueOf(player.getYRot()),
				player.position(),
				player.getDeltaMovement());
	}

	private static void logParCoolVerticalSwitch(Player player, Direction lockedWall, WallRunDecision decision) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		EPM.LOGGER.debug("[WomSpiderWallRun] parCoolVerticalSwitch wall={} runDirection={} block={} yRot={} pos={} delta={}",
				lockedWall,
				decision.runDirection(),
				decision.blockPos(),
				Float.valueOf(player.getYRot()),
				player.position(),
				player.getDeltaMovement());
	}

	private static void logParCoolLockedWallLost(Player player, Direction lockedWall, Vec3 forwardRunDirection) {
		if (!EPM.LOGGER.isDebugEnabled()) {
			return;
		}
		EPM.LOGGER.debug("[WomSpiderWallRun] parCoolLockedWallLost wall={} forwardRun={} detected={} pos={} delta={}",
				lockedWall,
				forwardRunDirection,
				WomSpiderWallContactResolver.detectAdjacentWallDirection(player),
				player.position(),
				player.getDeltaMovement());
	}

	private enum WallRunMode {
		VERTICAL,
		HORIZONTAL
	}

	private record WallContact(Vec3 wallDirection, BlockState blockState, BlockPos blockPos) {
	}

	private record ForwardCornerContact(Direction wallDirection, WallContact contact) {
	}

	private record GroundSupport(boolean present, BlockPos blockPos, String block) {
	}

	private record WallRunState(WallRunDecision decision, int missedContactTicks, boolean jumpHeld, int modeTicks, WallRunMode pendingMode, int pendingModeTicks, Direction lockedWall, Direction previousWall, int cornerCooldownTicks, int lastUpdateTick) {
	}

	private record DecisionResult(WallRunDecision decision, int missedContactTicks, WallRunMode pendingMode, int pendingModeTicks, Direction lockedWall, Direction previousWall, boolean cornerTransfer) {
	}

	private record WallRunSelection(WallRunDecision decision, WallRunMode pendingMode, int pendingModeTicks) {
	}

	private record WallRunCandidates(WallRunDecision vertical, WallRunDecision horizontal, WallRunDecision verticalFallback, WallRunDecision horizontalFallback) {
	}

	private record WallRunDecision(WallRunMode mode, int womSide, Vec3 wallDirection, Vec3 runDirection, BlockState blockState, BlockPos blockPos) {
	}
}
