package dev.spake404.epm.wom.spider;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.WomAnimationRefs;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunHandler;
import dev.spake404.epm.parcool.ParCoolClimbUpState;
import dev.spake404.epm.parcool.ParCoolRightClickActionPriority;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallSlideHandler {
	private static final float WALL_BACKFLIP_STAMINA_COST = 0.5F;
	private static final double WOM_GLIDE_FALL_SPEED = -0.2D;
	private static final double WOM_SLOW_GLIDE_FALL_SPEED = -0.01D;
	private static final double WOM_GLIDE_PARTICLE_HEIGHT = 1.7000000476837158D;
	private static final int STALE_STATE_PROBE_INTERVAL_TICKS = 5;
	private static final WeakHashMap<Player, WallSlideState> ACTIVE_WALL_SLIDES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_STALE_STATE_CLEAR_LOG_TICK = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_RIGHT_CLICK_PRIORITY_SKIP_LOG_TICK = new WeakHashMap<>();

	private WomSpiderWallSlideHandler() {
	}

	public static void tick(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunModeGate.canUseParCoolReplacement(player, playerPatch)) {
			clearOwnedWallSlide(player, playerPatch, "replacement_disabled");
			return;
		}

		if (isWomWallBackflipAnimation(playerPatch)) {
			handoffToWallBackflip(player, playerPatch);
			return;
		}

		if (WomSpiderWallRunHandler.isWallRunActive(player)) {
			handoffToWallRun(player, playerPatch);
			return;
		}

		if (ParCoolClimbUpState.isActiveOrAnimating(player, playerPatch)) {
			yieldToParCoolClimbUp(player, playerPatch);
			return;
		}

		if (!isWallSlideControlDown() || !canSlideNow(player)) {
			clearStaleWallState(player, playerPatch, "slide_input_inactive", false);
			return;
		}

		String blockingAction = ParCoolRightClickActionPriority.blockingActionForSpiderWallSlide(player);
		if (blockingAction != null) {
			logRightClickPrioritySkip(player, blockingAction);
			clearStaleWallState(player, playerPatch, "slide_right_click_priority_" + blockingAction, false);
			return;
		}

		Vec3 wallDirection = wallDirection(player);
		if (wallDirection == null) {
			clearStaleWallState(player, playerPatch, "slide_no_wall", false);
			return;
		}

		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch)) {
			clearStaleWallState(player, playerPatch, "missing_local_patch", false);
			return;
		}

		boolean slowGlide = isSlowGlideKeyDown(player);
		boolean jumpDown = isJumpKeyDown();
		WallSlideState previous = ACTIVE_WALL_SLIDES.get(player);
		if (jumpDown && previous != null && previous.jumpKeyUp() && localPlayerPatch.hasStamina(WALL_BACKFLIP_STAMINA_COST)) {
			if (!EPMClientHooks.claimWomWallJump(player, "wom_wallslide_backflip")) {
				return;
			}
			triggerWallBackflip(player, localPlayerPatch, slowGlide);
			return;
		}

		boolean jumpKeyUp = !jumpDown;
		applyWomWallSlide(player, localPlayerPatch, wallDirection, previous, slowGlide, jumpKeyUp);
		ACTIVE_WALL_SLIDES.put(player, new WallSlideState(slowGlide, jumpKeyUp));
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
	}

	public static void clearStaleWallState(Player player, PlayerPatch<?> playerPatch, String reason) {
		clearStaleWallState(player, playerPatch, reason, true);
	}

	private static void clearStaleWallState(Player player, PlayerPatch<?> playerPatch, String reason, boolean preserveActiveSlide) {
		if (player == null) {
			return;
		}

		if ((preserveActiveSlide && shouldOwnWallState(player)) || WomSpiderWallRunHandler.isWallRunActive(player) || isWomWallBackflipAnimation(playerPatch)) {
			return;
		}

		clearWallState(player, playerPatch, reason);
	}

	public static boolean shouldOwnWallState(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& isWallSlideControlDown()
				&& canSlideNow(player)
				&& !isParCoolClimbUpActiveOrAnimating(player)
				&& ParCoolRightClickActionPriority.blockingActionForSpiderWallSlide(player) == null
				&& wallDirection(player) != null;
	}

	public static boolean isWallSlideActive(Player player) {
		return player != null && ACTIVE_WALL_SLIDES.containsKey(player);
	}

	static net.minecraft.core.Direction activeWallDirection(Player player) {
		if (!isWallSlideActive(player) && !shouldOwnWallState(player)) {
			return null;
		}
		return WomSpiderWallContactResolver.detectAdjacentWallDirection(player);
	}

	private static boolean canSlideNow(Player player) {
		return player instanceof LocalPlayer
				&& !player.onGround()
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& player.getVehicle() == null;
	}

	private static boolean isParCoolClimbUpActiveOrAnimating(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return ParCoolClimbUpState.isActiveOrAnimating(player, playerPatch);
	}

	private static WallSlide wallSlideAction(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			return parkourability == null ? null : parkourability.get(WallSlide.class);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Vec3 wallDirection(Player player) {
		Vec3 wallDirection = leanedWallDirection(wallSlideAction(player));
		if (wallDirection != null) {
			return wallDirection;
		}

		try {
			return WorldUtil.getWall(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Vec3 leanedWallDirection(WallSlide wallSlide) {
		if (wallSlide == null || !wallSlide.isDoing()) {
			return null;
		}

		try {
			return wallSlide.getLeanedWallDirection();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static void logRightClickPrioritySkip(Player player, String blockingAction) {
		if (player == null || !EPMConfig.debugSpiderWallRunState() || !EPM.LOGGER.isDebugEnabled()) {
			return;
		}

		Integer lastTick = LAST_RIGHT_CLICK_PRIORITY_SKIP_LOG_TICK.get(player);
		if (lastTick != null && player.tickCount - lastTick.intValue() < 10) {
			return;
		}

		LAST_RIGHT_CLICK_PRIORITY_SKIP_LOG_TICK.put(player, Integer.valueOf(player.tickCount));
		EPM.LOGGER.debug(
				"[WomSpiderWallSlide] yield action={} tick={} wallSlideKeyDown={} wallRunKeyDown={} forwardDown={} delta={} onGround={}",
				blockingAction,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(isParCoolWallSlideKeyDown()),
				Boolean.valueOf(isWallRunKeyDown()),
				Boolean.valueOf(isForwardKeyDown()),
				player.getDeltaMovement(),
				Boolean.valueOf(player.onGround()));
	}

	private static boolean isWallSlideControlDown() {
		boolean wallRunKeyDown = isWallRunKeyDown();
		boolean forwardDown = isForwardKeyDown();
		if (wallRunKeyDown && forwardDown) {
			return false;
		}
		return wallRunKeyDown || isParCoolWallSlideKeyDown();
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

	private static boolean isForwardKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyUp.isDown();
	}

	private static boolean isSlowGlideKeyDown(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.options != null) {
			return minecraft.options.keyShift.isDown();
		}
		return player != null && player.isShiftKeyDown();
	}

	private static boolean isJumpKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
	}

	private static void applyWomWallSlide(Player player, LocalPlayerPatch playerPatch, Vec3 wallDirection, WallSlideState previous, boolean slowGlide, boolean jumpKeyUp) {
		player.stopFallFlying();
		EPMClientHooks.forceStopGliderForWallRun(player);
		playWallGlideAnimation(playerPatch);
		clearParCoolAnimator(player);
		applyWomGlideMotion(player, slowGlide);
		spawnWallGlideParticle(player, wallDirection);

		playerPatch.setModelYRot(wallFacingYaw(wallDirection), true);

		if (previous == null || previous.slowGlide() != slowGlide || previous.jumpKeyUp() != jumpKeyUp) {
			WomCompatBridge.instance().setSpiderWallGlideState(playerPatch, previous == null, slowGlide, player.getViewYRot(1.0F), jumpKeyUp);
		}
	}

	private static void playWallGlideAnimation(LocalPlayerPatch playerPatch) {
		AssetAccessor<? extends StaticAnimation> animation = WomAnimationRefs.wallGlide();
		if (animation == null || WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), animation)) {
			return;
		}

		try {
			playerPatch.playAnimationInClientSide(animation, EPMConfig.parCoolWallRunAnimationTransition());
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void applyWomGlideMotion(Player player, boolean slowGlide) {
		Vec3 movement = player.getDeltaMovement();
		double fallSpeed = slowGlide ? WOM_SLOW_GLIDE_FALL_SPEED : WOM_GLIDE_FALL_SPEED;
		player.setDeltaMovement(movement.x(), fallSpeed, movement.z());
		player.fallDistance = 0.0F;
	}

	private static void spawnWallGlideParticle(Player player, Vec3 wallDirection) {
		if (player == null || wallDirection == null) {
			return;
		}

		Vec3 particleWallDirection = cardinalWallDirection(wallDirection);
		if (particleWallDirection == null) {
			return;
		}

		WallParticleContact contact = wallParticleContact(player, particleWallDirection);
		if (contact == null) {
			particleWallDirection = particleWallDirection.reverse();
			contact = wallParticleContact(player, particleWallDirection);
			if (contact == null) {
				return;
			}
		}

		Level level = player.level();
		RandomSource random = player.getRandom();
		level.addParticle(
				new BlockParticleOption(ParticleTypes.BLOCK, contact.blockState()),
				player.getX(),
				player.getY() + WOM_GLIDE_PARTICLE_HEIGHT,
				player.getZ(),
				(random.nextFloat() - 0.5F) * 0.005D,
				random.nextFloat() * -0.02D,
				(random.nextFloat() - 0.5F) * 0.005D);
	}

	private static WallParticleContact wallParticleContact(Player player, Vec3 wallDirection) {
		WallParticleContact contact = wallParticleContact(player, wallDirection, 0.3D);
		if (contact != null) {
			return contact;
		}

		contact = wallParticleContact(player, wallDirection, 1.0D);
		if (contact != null) {
			return contact;
		}

		return wallParticleContact(player, wallDirection, 1.6D);
	}

	private static WallParticleContact wallParticleContact(Player player, Vec3 wallDirection, double yOffset) {
		Level level = player.level();
		BlockPos blockPos = wallParticleBlockPos(player, wallDirection, yOffset);
		BlockState blockState = level.getBlockState(blockPos);
		if (!isParticleWallBlock(blockState, blockPos, level)) {
			return null;
		}
		return new WallParticleContact(blockState);
	}

	private static BlockPos wallParticleBlockPos(Player player, Vec3 wallDirection, double yOffset) {
		AABB box = player.getBoundingBox();
		if (Math.abs(wallDirection.x()) > Math.abs(wallDirection.z())) {
			double x = wallDirection.x() > 0.0D ? box.maxX + 0.35D : box.minX - 0.35D;
			return BlockPos.containing(x, player.getY() + yOffset, player.getZ());
		}

		double z = wallDirection.z() > 0.0D ? box.maxZ + 0.35D : box.minZ - 0.35D;
		return BlockPos.containing(player.getX(), player.getY() + yOffset, z);
	}

	private static boolean isParticleWallBlock(BlockState blockState, BlockPos blockPos, Level level) {
		return !blockState.isAir() && !blockState.getCollisionShape(level, blockPos).isEmpty();
	}

	private static Vec3 cardinalWallDirection(Vec3 wallDirection) {
		double absX = Math.abs(wallDirection.x());
		double absZ = Math.abs(wallDirection.z());
		if (absX < 1.0E-6D && absZ < 1.0E-6D) {
			return null;
		}
		if (absX > absZ) {
			return new Vec3(Math.signum(wallDirection.x()), 0.0D, 0.0D);
		}
		return new Vec3(0.0D, 0.0D, Math.signum(wallDirection.z()));
	}

	private static void clearParCoolAnimator(Player player) {
		Animation animation = Animation.get(player);
		if (animation != null && animation.hasAnimator()) {
			animation.removeAnimator();
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		return AnimationQuery.currentAnimation(playerPatch);
	}

	private static float wallFacingYaw(Vec3 wallDirection) {
		return (float) Math.toDegrees(Math.atan2(-wallDirection.x(), wallDirection.z()));
	}

	private static void triggerWallBackflip(Player player, LocalPlayerPatch playerPatch, boolean slowGlide) {
		player.stopFallFlying();
		EPMClientHooks.markWomWallJumpForPhantomAscent(player);
		ACTIVE_WALL_SLIDES.remove(player);
		stopPlaying(playerPatch, WomAnimationRefs.wallGlide());

		AssetAccessor<? extends StaticAnimation> animation = WomAnimationRefs.wallBackflip();
		if (animation != null) {
			try {
				playerPatch.playAnimationInClientSide(animation, 0.0F);
			} catch (RuntimeException | LinkageError ignored) {
			}
		}

		WomCompatBridge.instance().triggerSpiderWallBackflipState(playerPatch, slowGlide ? 0.0F : -20.0F, player.getViewYRot(1.0F));
		NaturalSprinterFastRunHandler.cancelManualFastRunStepKey(player);
	}

	private static boolean isWomWallBackflipAnimation(PlayerPatch<?> playerPatch) {
		return WomSpiderWallMovementState.isWallBackflipAnimation(playerPatch);
	}

	private static void clearWallState(Player player, PlayerPatch<?> playerPatch, String reason) {
		if (player == null) {
			return;
		}

		boolean wasActive = ACTIVE_WALL_SLIDES.remove(player) != null;
		boolean wallGlideAnimation = isWomWallGlideAnimation(playerPatch);
		boolean wallMovementData = (wasActive || wallGlideAnimation || shouldProbePassiveWallState(player))
				&& WomCompatBridge.instance().isSpiderWallMovementActive(playerPatch);
		if (!wasActive && !wallGlideAnimation && !wallMovementData) {
			return;
		}

		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopWallSlideAnimation(localPlayerPatch);
		}
		logWallStateClear(player, playerPatch, reason, wasActive, wallMovementData, wallGlideAnimation);
	}

	private static void clearOwnedWallSlide(Player player, PlayerPatch<?> playerPatch, String reason) {
		if (player == null || ACTIVE_WALL_SLIDES.remove(player) == null) {
			return;
		}

		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopWallSlideAnimation(localPlayerPatch);
		}
		logWallStateClear(player, playerPatch, reason, true, false, false);
	}

	private static void yieldToParCoolClimbUp(Player player, PlayerPatch<?> playerPatch) {
		if (player == null) {
			return;
		}

		boolean wasActive = ACTIVE_WALL_SLIDES.remove(player) != null;
		boolean wallGlideAnimation = isWomWallGlideAnimation(playerPatch);
		boolean wallMovementData = (wasActive || wallGlideAnimation || shouldProbePassiveWallState(player))
				&& WomCompatBridge.instance().isSpiderWallMovementActive(playerPatch);
		if (!wasActive && !wallGlideAnimation && !wallMovementData) {
			return;
		}

		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopPlaying(localPlayerPatch, WomAnimationRefs.wallGlide());
		}
		logWallStateClear(player, playerPatch, "parcool_climbup", wasActive, wallMovementData, wallGlideAnimation);
	}

	private static void handoffToWallBackflip(Player player, PlayerPatch<?> playerPatch) {
		if (player == null || ACTIVE_WALL_SLIDES.remove(player) == null) {
			return;
		}

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopPlaying(localPlayerPatch, WomAnimationRefs.wallGlide());
		}
	}

	private static void handoffToWallRun(Player player, PlayerPatch<?> playerPatch) {
		if (player == null || ACTIVE_WALL_SLIDES.remove(player) == null) {
			return;
		}

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopPlaying(localPlayerPatch, WomAnimationRefs.wallGlide());
		}
	}

	private static void stopPlaying(LocalPlayerPatch playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		WomSpiderWallMovementState.stopPlaying(playerPatch, animation);
	}

	private static void stopWallSlideAnimation(LocalPlayerPatch playerPatch) {
		stopPlaying(playerPatch, WomAnimationRefs.wallGlide());
		try {
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static boolean isWomWallGlideAnimation(PlayerPatch<?> playerPatch) {
		return WomSpiderWallMovementState.isWallGlideAnimation(playerPatch);
	}

	private static boolean shouldProbePassiveWallState(Player player) {
		return player != null && player.tickCount % STALE_STATE_PROBE_INTERVAL_TICKS == 0;
	}

	private static void logWallStateClear(Player player, PlayerPatch<?> playerPatch, String reason, boolean wasActive, boolean wallMovementData, boolean wallGlideAnimation) {
		if (wasActive || !EPMConfig.debugSpiderWallRunState() || !EPM.LOGGER.isDebugEnabled()) {
			return;
		}

		Integer lastTick = LAST_STALE_STATE_CLEAR_LOG_TICK.get(player);
		if (lastTick != null && player.tickCount - lastTick.intValue() < 20) {
			return;
		}

		LAST_STALE_STATE_CLEAR_LOG_TICK.put(player, Integer.valueOf(player.tickCount));
		EPM.LOGGER.debug("[WomSpiderWallSlide] clearWallState reason={} wasActive={} dataWallState={} animationWallGlide={} onGround={} wallRunKeyDown={} wallSlideKeyDown={} forwardDown={} shiftDown={} delta={} state={}",
				reason,
				Boolean.valueOf(wasActive),
				Boolean.valueOf(wallMovementData),
				Boolean.valueOf(wallGlideAnimation),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(isWallRunKeyDown()),
				Boolean.valueOf(isParCoolWallSlideKeyDown()),
				Boolean.valueOf(isForwardKeyDown()),
				Boolean.valueOf(isSlowGlideKeyDown(player)),
				player.getDeltaMovement(),
				WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch));
	}

	private record WallSlideState(boolean slowGlide, boolean jumpKeyUp) {
	}

	private record WallParticleContact(BlockState blockState) {
	}
}
