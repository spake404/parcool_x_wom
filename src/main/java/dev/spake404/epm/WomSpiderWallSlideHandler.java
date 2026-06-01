package dev.spake404.epm;

import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
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
	private static final int STALE_STATE_PROBE_INTERVAL_TICKS = 5;
	private static final WeakHashMap<Player, WallSlideState> ACTIVE_WALL_SLIDES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_STALE_STATE_CLEAR_LOG_TICK = new WeakHashMap<>();

	private WomSpiderWallSlideHandler() {
	}

	public static void tick(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!WomSpiderWallRunReplacementGate.canUseReplacement(player, playerPatch)) {
			clearStaleWallState(player, playerPatch, "replacement_disabled", false);
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

		if (!isWallSlideControlDown() || !canSlideNow(player)) {
			clearStaleWallState(player, playerPatch, "slide_input_inactive", false);
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
				&& wallDirection(player) != null;
	}

	private static boolean canSlideNow(Player player) {
		return player instanceof LocalPlayer
				&& !player.onGround()
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& player.getVehicle() == null;
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
		WallSlide wallSlide = wallSlideAction(player);
		Vec3 wallDirection = wallSlide == null ? null : wallSlide.getLeanedWallDirection();
		if (wallDirection != null) {
			return wallDirection;
		}

		try {
			return WorldUtil.getWall(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
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
		playWallGlideAnimation(playerPatch);
		clearParCoolAnimator(player);
		applyWomGlideMotion(player, slowGlide);

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

	private static void clearParCoolAnimator(Player player) {
		Animation animation = Animation.get(player);
		if (animation != null && animation.hasAnimator()) {
			animation.removeAnimator();
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static float wallFacingYaw(Vec3 wallDirection) {
		return (float) Math.toDegrees(Math.atan2(-wallDirection.x(), wallDirection.z()));
	}

	private static void triggerWallBackflip(Player player, LocalPlayerPatch playerPatch, boolean slowGlide) {
		player.stopFallFlying();
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
		return WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallBackflip());
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
		if (animation == null) {
			return;
		}

		try {
			playerPatch.stopPlaying(animation);
		} catch (RuntimeException | LinkageError ignored) {
		}
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
		return WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallGlide());
	}

	private static boolean shouldProbePassiveWallState(Player player) {
		return player != null && player.tickCount % STALE_STATE_PROBE_INTERVAL_TICKS == 0;
	}

	private static void logWallStateClear(Player player, PlayerPatch<?> playerPatch, String reason, boolean wasActive, boolean wallMovementData, boolean wallGlideAnimation) {
		if (wasActive) {
			return;
		}

		Integer lastTick = LAST_STALE_STATE_CLEAR_LOG_TICK.get(player);
		if (lastTick != null && player.tickCount - lastTick.intValue() < 20) {
			return;
		}

		LAST_STALE_STATE_CLEAR_LOG_TICK.put(player, Integer.valueOf(player.tickCount));
		EPM.LOGGER.info("[WomSpiderWallSlide] clearWallState reason={} wasActive={} dataWallState={} animationWallGlide={} onGround={} wallRunKeyDown={} wallSlideKeyDown={} forwardDown={} shiftDown={} delta={} state={}",
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
}
