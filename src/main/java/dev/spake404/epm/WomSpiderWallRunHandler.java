package dev.spake404.epm;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

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
	private static final WeakHashMap<Player, WallRunState> ACTIVE_WALL_RUNS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> WALL_RUN_KEY_RELEASE_REQUIRED = new WeakHashMap<>();

	private WomSpiderWallRunHandler() {
	}

	public static void tick(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		boolean wallRunKeyDown = isWallRunKeyDown();
		clearRestartGateIfKeyReleased(player, wallRunKeyDown);
		if (!WomSpiderWallRunReplacementGate.canUseReplacement(player, playerPatch)) {
			stop(player, playerPatch, wallRunKeyDown);
			return;
		}

		if (isWomWallBackflipAnimation(playerPatch)) {
			removeActiveWallRun(player, wallRunKeyDown);
			NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
			return;
		}

		if (isParCoolWallJumpActive(player) || isEpicParCoolWallJumpAnimation(playerPatch)) {
			suspendForParCoolWallJump(player, playerPatch, wallRunKeyDown);
			return;
		}

		if (!wallRunKeyDown || shouldLetTaczReloadUseWallRunKey(player) || !canRunNow(player, playerPatch)) {
			stop(player, playerPatch, wallRunKeyDown);
			return;
		}

		if (shouldWaitForWallRunKeyRelease(player)) {
			return;
		}

		DecisionResult decisionResult = resolveWallRunDecision(player);
		if (decisionResult == null) {
			stop(player, playerPatch, wallRunKeyDown);
			return;
		}

		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch)) {
			stop(player, playerPatch, wallRunKeyDown);
			return;
		}

		WallRunState previous = ACTIVE_WALL_RUNS.get(player);
		boolean jumpHeld = previous == null ? isJumpKeyDown() : previous.jumpHeld();
		applyWallRun(player, localPlayerPatch, decisionResult.decision(), jumpHeld);
		ACTIVE_WALL_RUNS.put(player, nextWallRunState(player, previous, decisionResult, jumpHeld));
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
	}

	public static boolean handleMovementInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		boolean wallRunKeyDown = isWallRunKeyDown();
		clearRestartGateIfKeyReleased(player, wallRunKeyDown);
		if (!WomSpiderWallRunReplacementGate.canUseReplacement(player, playerPatch)) {
			return false;
		}

		if (isWomWallBackflipAnimation(playerPatch)) {
			removeActiveWallRun(player, wallRunKeyDown);
			NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
			return true;
		}

		if (isParCoolWallJumpActive(player) || isEpicParCoolWallJumpAnimation(playerPatch)) {
			suspendForParCoolWallJump(player, playerPatch, wallRunKeyDown);
			return true;
		}

		if (!wallRunKeyDown || shouldLetTaczReloadUseWallRunKey(player) || !canRunNow(player, playerPatch)) {
			stop(player, playerPatch, wallRunKeyDown);
			return true;
		}

		if (shouldWaitForWallRunKeyRelease(player)) {
			return true;
		}

		DecisionResult decisionResult = resolveWallRunDecision(player);
		if (decisionResult == null) {
			stop(player, playerPatch, wallRunKeyDown);
			return true;
		}

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			WallRunState previous = ACTIVE_WALL_RUNS.get(player);
			boolean jumping = event.getMovementInput() != null && event.getMovementInput().jumping;
			if (jumping && previous != null && !previous.jumpHeld()) {
				triggerWallBackflip(player, localPlayerPatch, decisionResult.decision());
				return true;
			}

			applyWallRun(player, localPlayerPatch, decisionResult.decision(), jumping);
			previous = ACTIVE_WALL_RUNS.put(player, nextWallRunState(player, previous, decisionResult, jumping));
			logDecisionChange(player, previous == null ? null : previous.decision(), decisionResult.decision());
			NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
		}
		return true;
	}

	public static boolean shouldReplaceParCoolHorizontalWallRun(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return WomSpiderWallRunReplacementGate.canUseReplacement(player, playerPatch);
	}

	public static boolean shouldDisableOriginalWomSprintTrigger(PlayerPatch<?> playerPatch) {
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		return WomSpiderWallRunReplacementGate.canUseReplacement(player, playerPatch);
	}

	private static boolean canRunNow(Player player, PlayerPatch<?> playerPatch) {
		return player instanceof LocalPlayer
				&& !player.onGround()
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& player.getVehicle() == null
				&& playerPatch instanceof LocalPlayerPatch localPlayerPatch
				&& localPlayerPatch.hasStamina(WALL_RUN_STAMINA_COST);
	}

	private static boolean isWallRunKeyDown() {
		try {
			return KeyBindings.getKeyHorizontalWallRun().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean shouldLetTaczReloadUseWallRunKey(Player player) {
		return isTaczItem(player.getMainHandItem()) || isTaczItem(player.getOffhandItem());
	}

	private static boolean isTaczItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return itemId != null && ModCompat.TACZ.equals(itemId.getNamespace());
	}

	private static DecisionResult resolveWallRunDecision(Player player) {
		WallRunState active = ACTIVE_WALL_RUNS.get(player);
		Vec3 preferredWallDirection = active == null ? null : active.decision().wallDirection();
		WallRunCandidates candidates = findWallRunCandidates(player, preferredWallDirection);
		WallRunSelection detected = selectWallRunDecision(player, active, candidates);
		if (detected != null) {
			return new DecisionResult(detected.decision(), 0, detected.pendingMode(), detected.pendingModeTicks());
		}

		if (active != null
				&& active.decision().mode() == WallRunMode.HORIZONTAL
				&& active.missedContactTicks() < HORIZONTAL_WALL_CONTACT_GRACE_TICKS) {
			return new DecisionResult(active.decision(), active.missedContactTicks() + 1, null, 0);
		}

		return null;
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

	private static WallRunCandidates findWallRunCandidates(Player player, Vec3 preferredWallDirection) {
		List<WallContact> contacts = detectWallContacts(player);
		if (contacts.isEmpty()) {
			return null;
		}

		if (preferredWallDirection != null) {
			List<WallContact> preferredContacts = new ArrayList<>(contacts.size());
			for (WallContact contact : contacts) {
				if (sameWallDirection(contact.wallDirection(), preferredWallDirection)) {
					preferredContacts.add(contact);
				}
			}

			if (!preferredContacts.isEmpty()) {
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
			WallRunDecision horizontalCandidate = new WallRunDecision(WallRunMode.HORIZONTAL, womSide, wallDirection, runDirection.normalize(), contact.blockState(), contact.blockPos());
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
		return new WallRunState(
				decisionResult.decision(),
				decisionResult.missedContactTicks(),
				jumpHeld,
				modeTicks,
				decisionResult.pendingMode(),
				decisionResult.pendingModeTicks(),
				player.tickCount);
	}

	private static int tickIncrement(Player player, WallRunState active) {
		return active.lastUpdateTick() == player.tickCount ? 0 : 1;
	}

	private static List<WallContact> detectWallContacts(Player player) {
		List<WallContact> contacts = new ArrayList<>(7);
		Level level = player.level();
		Vec3 forward = horizontalLook(player);
		Vec3 right = rightDirection(forward);

		BlockPos center = BlockPos.containing(player.getX(), player.getY(), player.getZ());
		BlockPos lower = BlockPos.containing(player.getX(), player.getY() - 0.5D, player.getZ());
		if (!isFreeForWomWallRun(level.getBlockState(center), center, level, false)
				|| !isFreeForWomWallRun(level.getBlockState(lower), lower, level, player.isInWater())) {
			return contacts;
		}

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
		return look.normalize();
	}

	private static Vec3 rightDirection(Vec3 forward) {
		return new Vec3(forward.z(), 0.0D, -forward.x()).normalize();
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

	private static boolean isEpicParCoolWallJumpAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(
				currentBaseAnimation(playerPatch),
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight());
	}

	private static boolean isParCoolWallJumpActive(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			WallJump wallJump = parkourability == null ? null : parkourability.get(WallJump.class);
			return wallJump != null && wallJump.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
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

	private static void logDecisionChange(Player player, WallRunDecision previous, WallRunDecision decision) {
		if (decision.equals(previous)) {
			return;
		}

		EPM.LOGGER.info("[WomSpiderWallRun] mode={} womSide={} wallDirection={} runDirection={} yRot={} delta={} block={} pos={}",
				decision.mode(),
				Integer.valueOf(decision.womSide()),
				decision.wallDirection(),
				decision.runDirection(),
				Float.valueOf(player.getYRot()),
				player.getDeltaMovement(),
				decision.blockPos(),
				player.position());
	}

	private enum WallRunMode {
		VERTICAL,
		HORIZONTAL
	}

	private record WallContact(Vec3 wallDirection, BlockState blockState, BlockPos blockPos) {
	}

	private record WallRunState(WallRunDecision decision, int missedContactTicks, boolean jumpHeld, int modeTicks, WallRunMode pendingMode, int pendingModeTicks, int lastUpdateTick) {
	}

	private record DecisionResult(WallRunDecision decision, int missedContactTicks, WallRunMode pendingMode, int pendingModeTicks) {
	}

	private record WallRunSelection(WallRunDecision decision, WallRunMode pendingMode, int pendingModeTicks) {
	}

	private record WallRunCandidates(WallRunDecision vertical, WallRunDecision horizontal, WallRunDecision verticalFallback, WallRunDecision horizontalFallback) {
	}

	private record WallRunDecision(WallRunMode mode, int womSide, Vec3 wallDirection, Vec3 runDirection, BlockState blockState, BlockPos blockPos) {
	}
}
