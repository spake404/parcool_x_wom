package dev.spake404.epm;

import java.util.WeakHashMap;

import com.alrex.parcool.client.animation.impl.FastRunningAnimator;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.HangDown;
import com.alrex.parcool.common.action.impl.Roll;
import com.alrex.parcool.common.action.impl.Tap;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import dev.spake404.epm.mixin.AnimatorControlPacketAccessor;
import dev.spake404.epm.mixin.SPAnimatorControlAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.client.input.PlayerInputState;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.network.common.AnimatorControlPacket;
import yesman.epicfight.network.server.SPAnimatorControl;

public final class EPMClientHooks {
	private static final WeakHashMap<Player, Integer> NATURAL_SPRINTER_CAT_LEAP_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntityPatch<?>, Boolean> NATURAL_SPRINTER_CAT_LEAP_PATCHES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PHANTOM_ASCENT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, PhantomAscentPrimeSource> PHANTOM_ASCENT_SOURCES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PHANTOM_ASCENT_USED_AIRBORNE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PHANTOM_ASCENT_STARTED_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, DelayedAnimatorControl> DELAYED_PHANTOM_ASCENT_AIR_ATTACKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PENDING_FORCED_PHANTOM_ASCENT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_HOLD_FAST_RUN = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_FAST_RUN_GRACE_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> WALL_JUMP_AUTO_SPRINT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_SHOOT_FAST_RUN_RESTORE_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> TACZ_SHOOT_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, AssetAccessor<? extends StaticAnimation>> PENDING_FAST_RUN_DASHES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> NATURAL_SPRINTER_BREAKFALL_START_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, AssetAccessor<? extends StaticAnimation>> NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, SkillContainer> PHANTOM_ASCENT_CONTAINERS = new WeakHashMap<>();
	private static final ResourceLocation HF_MURASAMA = ResourceLocation.fromNamespaceAndPath("efn", "hf_murasama");
	private static final int PHANTOM_ASCENT_AIR_ATTACK_DELAY_TICKS = 10;
	private static final int PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_DURATION_TICKS = 12;
	private static final int WALL_JUMP_AUTO_SPRINT_DURATION_TICKS = 12;
	private static final int TACZ_WALL_JUMP_SHOOT_CANCEL_DURATION_TICKS = 40;
	private static final int TACZ_SHOOT_FAST_RUN_SUPPRESS_DURATION_TICKS = 3;
	private static final int TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS = 12;
	private static final int TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS = 30;
	private static final int TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS = 20;
	private static final int NATURAL_SPRINTER_BREAKFALL_DASH_STARTUP_GRACE_TICKS = 3;
	private static final int NATURAL_SPRINTER_BREAKFALL_DASH_MAX_DELAY_TICKS = 40;
	private static volatile boolean jumpSpeedModifierInstalled;
	private static boolean phantomJumpWasDown;

	private EPMClientHooks() {
	}

	public static void startNaturalSprinterCatLeap(Player player) {
		if (player == null || !player.isLocalPlayer() || !EPMConfig.naturalSprinterAnimations() || !hasNaturalSprinter(player)) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		installJumpSpeedModifier();
		NATURAL_SPRINTER_CAT_LEAP_TICKS.put(player, Integer.valueOf(0));
		NATURAL_SPRINTER_CAT_LEAP_PATCHES.put(playerPatch, Boolean.TRUE);
	}

	public static void markCatLeapForPhantomAscent(Player player) {
		if (player != null && player.isLocalPlayer() && EPMConfig.catLeapPrimesPhantomAscent()) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.CAT_LEAP);
		}
	}

	public static void markWallJumpForPhantomAscent(Player player) {
		if (player != null && player.isLocalPlayer() && EPMConfig.wallJumpPrimesPhantomAscent()) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.WALL_JUMP);
		}
	}

	public static void reduceNaturalSprinterCatLeapMotion(Player player) {
		if (player == null || !player.isLocalPlayer() || !hasNaturalSprinter(player)) {
			return;
		}

		Vec3 movement = player.getDeltaMovement();
		player.setDeltaMovement(movement.x() * 0.5D, movement.y() * 1.1D, movement.z() * 0.5D);
	}

	public static void compensateEpicParCoolClimbUp(Player player) {
		if (player == null || !player.isLocalPlayer() || !player.level().isClientSide()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !playerPatch.isEpicFightMode()) {
			return;
		}

		Vec3 movement = player.getDeltaMovement();
		double configuredVelocity = EPMConfig.epicParCoolClimbUpVerticalVelocity();
		if (movement.y() < configuredVelocity) {
			player.setDeltaMovement(movement.x(), configuredVelocity, movement.z());
		}

		startEpicParCoolClimbUpAirControl(player);
	}

	public static boolean shouldAllowClimbUpFromEpicParCoolClingMove(Player player) {
		if (player == null || !player.isLocalPlayer() || !player.level().isClientSide()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !playerPatch.isEpicFightMode()) {
			return false;
		}

		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) {
			return false;
		}

		ClingToCliff cling = parkourability.get(ClingToCliff.class);
		return cling != null
				&& cling.isDoing()
				&& cling.getDoingTick() > 2
				&& cling.getFacingDirection() == ClingToCliff.FacingDirection.ToWall
				&& isParCoolJumpPressed()
				&& isEpicParCoolClingMoveAnimation(currentBaseAnimation(playerPatch));
	}

	public static void markClimbUpFromEpicParCoolClingMove(Player player) {
		if (player != null && player.isLocalPlayer()) {
			EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.put(player, Integer.valueOf(player.tickCount));
		}
	}

	public static void restoreClingMoveClimbUpVelocity(Player player) {
		restoreClingMoveClimbUpVelocity(player, false);
	}

	public static void queueNaturalSprinterFastRunDash(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		if (playerPatch == null || animation == null || !EPMConfig.naturalSprinterAnimations()) {
			return;
		}

		Player player = playerPatch.getOriginal();
		if (shouldDelayNaturalSprinterDashForBreakfall(player)) {
			NATURAL_SPRINTER_BREAKFALL_START_TICKS.putIfAbsent(player, Integer.valueOf(player.tickCount));
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.put(player, animation);
			return;
		}

		PENDING_FAST_RUN_DASHES.put(playerPatch, animation);
	}

	public static void markBreakfallStarted(Player player) {
		if (player == null || !player.isLocalPlayer() || !ModCompat.isWomLoaded() || !EPMConfig.naturalSprinterAnimations()) {
			return;
		}

		NATURAL_SPRINTER_BREAKFALL_START_TICKS.put(player, Integer.valueOf(player.tickCount));
		delayPendingNaturalSprinterDashForBreakfall(player);
	}

	public static boolean isFastRunKeyRecentlyPressed() {
		KeyRecorder.KeyState keyState = fastRunKeyState();
		if (keyState == null) {
			return false;
		}

		int ticksDown = keyState.getTickKeyDown();
		return keyState.isPressed() || (ticksDown > 0 && ticksDown <= 4);
	}

	public static boolean isFastRunKeyDown() {
		KeyRecorder.KeyState keyState = fastRunKeyState();
		return keyState != null && keyState.getTickKeyDown() > 0;
	}

	public static boolean isFastRunPressKeyControl() {
		return ParCoolConfig.Client.FastRunControl.get() == FastRun.ControlType.PressKey;
	}

	public static boolean isFastRunControlKeyDown() {
		return KeyBindings.getKeyFastRunning().isDown();
	}

	public static void markVaultStartedFromFastRun(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) {
			return;
		}

		FastRun fastRun = parkourability.get(FastRun.class);
		if (fastRun != null && fastRun.isDoing()) {
			VAULT_HOLD_FAST_RUN.put(player, Boolean.TRUE);
			player.setSprinting(true);
		}
	}

	public static void clearVaultFastRunHold(Player player) {
		if (Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.remove(player))) {
			VAULT_FAST_RUN_GRACE_TICKS.put(player, Integer.valueOf(40));
		}
	}

	public static boolean wasHoldingFastRunDuringVault(Player player) {
		return Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player)) || VAULT_FAST_RUN_GRACE_TICKS.containsKey(player);
	}

	public static boolean shouldPreserveFastRunToggleDuringVault(Player player) {
		if (!wasHoldingFastRunDuringVault(player)) {
			return false;
		}

		return !isFastRunPressKeyControl() || isFastRunControlKeyDown();
	}

	public static boolean shouldKeepFastRunDuringVault(Player player) {
		if (!Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player))) {
			return false;
		}

		if (isFastRunPressKeyControl() && !isFastRunControlKeyDown()) {
			return false;
		}

		if (hasHardVaultFastRunBlocker(player)) {
			clearVaultFastRunHold(player);
			return false;
		}

		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null || !parkourability.get(Vault.class).isDoing()) {
			clearVaultFastRunHold(player);
			return false;
		}

		player.setSprinting(true);
		return true;
	}

	public static void markAutoSprintAfterWallJump(Player player) {
		if (player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.autoSprintAfterWallJump()
				|| hasHardVaultFastRunBlocker(player)) {
			return;
		}

		WALL_JUMP_AUTO_SPRINT_TICKS.put(player, Integer.valueOf(WALL_JUMP_AUTO_SPRINT_DURATION_TICKS));
		player.setSprinting(true);
		ensureFastRunAnimator(player);
	}

	public static void markWallJumpForTaczShootCancel(Player player) {
		if (player != null && player.isLocalPlayer() && EPMConfig.taczShootDuringWallJump()) {
			TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.put(player, Integer.valueOf(TACZ_WALL_JUMP_SHOOT_CANCEL_DURATION_TICKS));
		}
	}

	public static boolean shouldPreserveFastRunToggleAfterWallJump(Player player, IStamina stamina) {
		return shouldKeepFastRunAfterWallJump(player, stamina);
	}

	public static boolean shouldKeepFastRunAfterWallJump(Player player, IStamina stamina) {
		if (!WALL_JUMP_AUTO_SPRINT_TICKS.containsKey(player)) {
			return false;
		}

		if (stamina != null && stamina.isExhausted()) {
			cancelAutoSprintAfterWallJump(player);
			return false;
		}

		if (!EPMConfig.autoSprintAfterWallJump() || hasHardVaultFastRunBlocker(player)) {
			cancelAutoSprintAfterWallJump(player);
			return false;
		}

		player.setSprinting(true);
		ensureFastRunAnimator(player);
		return true;
	}

	public static void suppressFastRunForTaczShoot(Player player, boolean restoreFastRunAfterShoot) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_SUPPRESS_DURATION_TICKS));
		if (restoreFastRunAfterShoot) {
			rememberFastRunBeforeTaczShoot(player);
		} else {
			TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.remove(player);
		}
		clearVaultFastRunHold(player);
		cancelAutoSprintAfterWallJump(player);
		stopLocalSprintAndFastRunAnimation(player);
	}

	public static void cancelWallJumpForTaczShoot(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			WallJump wallJump = parkourability == null ? null : parkourability.get(WallJump.class);
			if (wallJump != null && wallJump.isDoing()) {
				wallJump.finish(player);
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		clearVaultFastRunHold(player);
		cancelAutoSprintAfterWallJump(player);
		cancelTaczWallJumpShootCancel(player);
		clearParCoolAnimator(player);
		player.setSprinting(false);

		Vec3 movement = player.getDeltaMovement();
		if (movement.y() > 0.0D) {
			player.setDeltaMovement(movement.x(), 0.0D, movement.z());
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			PENDING_FAST_RUN_DASHES.remove(localPlayerPatch);
			stopPlaying(localPlayerPatch,
					WomAnimationRefs.epicParCoolWallJumpLeftStart(),
					WomAnimationRefs.epicParCoolWallJumpRightStart(),
					WomAnimationRefs.epicParCoolWallJumpLeft(),
					WomAnimationRefs.epicParCoolWallJumpRight());
			try {
				localPlayerPatch.getClientAnimator().resetCompositeMotion();
				localPlayerPatch.setModelYRot(player.getYRot(), true);
			} catch (RuntimeException | LinkageError ignored) {
			}
		}
	}

	public static boolean isWallJumpActiveForTaczShoot(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return false;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			WallJump wallJump = parkourability == null ? null : parkourability.get(WallJump.class);
			if (wallJump != null && wallJump.isDoing()) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		return TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.containsKey(player) || MomentumAirAttackWindowState.isInWallJumpWindow(player);
	}

	public static void rememberFastRunBeforeTaczShoot(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS));
	}

	public static boolean shouldStopFastRunForTaczShoot(Player player) {
		return player != null && TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(player);
	}

	public static boolean shouldPreserveFastRunAfterTaczShoot(Player player, IStamina stamina) {
		return shouldRestoreFastRunAfterTaczShoot(player, stamina);
	}

	public static boolean shouldRestoreFastRunAfterTaczShoot(Player player, IStamina stamina) {
		if (!TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.containsKey(player)) {
			return false;
		}

		if (shouldKeepTaczShootFastRunSuppression(player)) {
			return false;
		}

		if ((stamina != null && stamina.isExhausted()) || hasHardVaultFastRunBlocker(player) || !isHoldingTaczGun(player)) {
			cancelTaczShootFastRunRestore(player);
			return false;
		}

		ensureFastRunAnimator(player);
		return true;
	}

	public static boolean shouldSuppressAutoFastRunDashForTacz(PlayerPatch<?> playerPatch) {
		return playerPatch != null
				&& (TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(playerPatch.getOriginal())
				|| TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(playerPatch.getOriginal()));
	}

	public static void markTaczShootActive(Player player) {
		if (player != null && player.isLocalPlayer() && isHoldingTaczGun(player)) {
			TACZ_SHOOT_ACTIVE.put(player, Boolean.TRUE);
		}
	}

	public static void suppressAutoFastRunDashForTaczReload(Player player) {
		if (player != null
				&& player.isLocalPlayer()
				&& isHoldingTaczGun(player)) {
			TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.remove(player);
			TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.put(player, Integer.valueOf(TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS));
		}
	}

	public static boolean isHoldingTaczGunItem(Player player) {
		return ModCompat.isTaczLoaded() && player != null && isHoldingTaczGun(player);
	}

	public static boolean cancelWallJumpForTaczAttackInput(Player player) {
		if (player == null || !player.isLocalPlayer() || !EPMConfig.taczShootDuringWallJump() || !isHoldingTaczGun(player)) {
			return false;
		}

		if (!isWallJumpActiveForTaczShoot(player)) {
			return false;
		}

		cancelWallJumpForTaczShoot(player);
		MomentumAirAttackWindowState.clearWallJumpWindow(player);
		return true;
	}

	public static boolean cancelWallJumpForHeldTaczAttack(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null || !minecraft.options.keyAttack.isDown()) {
			return false;
		}

		return cancelWallJumpForTaczAttackInput(player);
	}

	public static void markPhantomAscentAirAttackWindow(Player player) {
		if (player == null || !player.isLocalPlayer() || isHoldingPhantomAscentBlockedWeapon(player) || Boolean.TRUE.equals(PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT.get(player))) {
			return;
		}

		PhantomAscentAirAttackState.mark(player);
		PHANTOM_ASCENT_STARTED_TICKS.put(player, Integer.valueOf(player.tickCount));
		PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT.put(player, Boolean.TRUE);
		EPMNetwork.sendPhantomAscentAirAttackWindow();
	}

	public static boolean delayPhantomAscentAirAttackAnimation(PlayerPatch<?> playerPatch, AnimatorControlPacketAccessor packet, SPAnimatorControlAccessor serverPacket) {
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch)) {
			return false;
		}

		Player player = localPlayerPatch.getOriginal();
		boolean hasClientSignal = PhantomAscentAirAttackState.hasAirAttackSignal(player);
		boolean hasProtectNextFall = PhantomAscentAirAttackState.hasProtectNextFall(playerPatch);
		if ((!hasClientSignal && !hasProtectNextFall) || player.isSpectator() || player.isInWater()) {
			return false;
		}

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (!WomAnimationRefs.isAny(currentAnimation, WomAnimationRefs.bipedPhantomAscentForward(), WomAnimationRefs.bipedPhantomAscentBackward())) {
			return false;
		}

		AssetAccessor<? extends StaticAnimation> incomingAnimation = AnimationManager.byId(packet.parcoolxwom$animationId());
		if (!isIncomingPhantomAscentAirAttackFollowup(playerPatch, incomingAnimation)) {
			return false;
		}

		int elapsedTicks = phantomAscentElapsedTicks(player);
		if (elapsedTicks < PHANTOM_ASCENT_AIR_ATTACK_DELAY_TICKS) {
			DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.put(player, new DelayedAnimatorControl(
					packet.parcoolxwom$action(),
					packet.parcoolxwom$animationId(),
					packet.parcoolxwom$transitionTimeModifier(),
					packet.parcoolxwom$pause(),
					serverPacket.parcoolxwom$layer(),
					serverPacket.parcoolxwom$priority()));
			return true;
		}

		cancelPhantomAscentForAirAttack(localPlayerPatch);
		return false;
	}

	private static void tickDelayedPhantomAscentAirAttack(Player player) {
		DelayedAnimatorControl delayedAttack = DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.get(player);
		if (delayedAttack == null) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch) || player.onGround() || player.isDeadOrDying() || player.isInWater()) {
			DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.remove(player);
			return;
		}

		if (phantomAscentElapsedTicks(player) < PHANTOM_ASCENT_AIR_ATTACK_DELAY_TICKS) {
			return;
		}

		DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.remove(player);
		cancelPhantomAscentForAirAttack(localPlayerPatch);
		playDelayedAnimatorControl(localPlayerPatch, delayedAttack);
	}

	public static void tickLocalPlayer(TickEvent.PlayerTickEvent event) {
		if (!event.player.isLocalPlayer()) {
			return;
		}

		ClingToCliffDebug.logClingInputTick(event.player);
		NaturalSprinterFastRunHandler.tickManualFastRunStepKey(event.player);
		restoreClingMoveClimbUpVelocity(event.player, true);
		tickEpicParCoolClimbUpAirControl(event.player);

		cancelWallJumpForHeldTaczAttack(event.player);
		if (TACZ_SHOOT_ACTIVE.containsKey(event.player) || TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(event.player)) {
			tickTaczShootStopFastRunDashSuppression(event.player);
		}
		if (TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(event.player)) {
			tickTaczReloadFastRunDashSuppression(event.player);
		}

		boolean hasTickWork = hasClientTickWork(event.player);
		boolean shouldProbeSpiderWallJump = shouldProbeSpiderWallJump(event.player);
		if (!hasTickWork && !shouldProbeSpiderWallJump) {
			return;
		}

		clearAirbornePhantomAscentLockIfLanded(event.player);
		if (PENDING_FORCED_PHANTOM_ASCENT_TICKS.containsKey(event.player)) {
			tickPendingForcedPhantomAscent(event.player);
		}
		if (NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(event.player)) {
			tickNaturalSprinterCatLeap(event.player);
		}
		if (shouldProbeSpiderWallJump) {
			markSpiderWallJumpForPhantomAscent(event.player);
		}
		if (PHANTOM_ASCENT_TICKS.containsKey(event.player)) {
			tickPhantomAscent(event.player);
		}
		if (DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.containsKey(event.player)) {
			tickDelayedPhantomAscentAirAttack(event.player);
		}
		if (PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.containsKey(event.player)) {
			tickPhantomAscentAirAttackSprintSuppression(event.player);
		}
		if (VAULT_FAST_RUN_GRACE_TICKS.containsKey(event.player)) {
			tickVaultFastRunGraceWindow(event.player);
		}
		if (WALL_JUMP_AUTO_SPRINT_TICKS.containsKey(event.player)) {
			tickAutoSprintAfterWallJump(event.player);
		}
		if (TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.containsKey(event.player)) {
			tickTaczWallJumpShootCancel(event.player);
		}
		if (TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(event.player)) {
			tickTaczShootFastRunSuppression(event.player);
		}
		if (TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.containsKey(event.player)) {
			tickTaczShootFastRunRestore(event.player);
		}
		if (NATURAL_SPRINTER_BREAKFALL_START_TICKS.containsKey(event.player)
				|| NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.containsKey(event.player)) {
			tickBreakfallDelayedNaturalSprinterDash(event.player);
		}
		if (!PENDING_FAST_RUN_DASHES.isEmpty()) {
			playPendingFastRunDash(event.player);
		}
	}

	private static boolean hasClientTickWork(Player player) {
		return PHANTOM_ASCENT_TICKS.containsKey(player)
				|| PHANTOM_ASCENT_USED_AIRBORNE.containsKey(player)
				|| PHANTOM_ASCENT_STARTED_TICKS.containsKey(player)
				|| DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.containsKey(player)
				|| PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.containsKey(player)
				|| PENDING_FORCED_PHANTOM_ASCENT_TICKS.containsKey(player)
				|| PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT.containsKey(player)
				|| NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(player)
				|| VAULT_HOLD_FAST_RUN.containsKey(player)
				|| VAULT_FAST_RUN_GRACE_TICKS.containsKey(player)
				|| WALL_JUMP_AUTO_SPRINT_TICKS.containsKey(player)
				|| TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.containsKey(player)
				|| TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(player)
				|| TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.containsKey(player)
				|| TACZ_SHOOT_ACTIVE.containsKey(player)
				|| TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(player)
				|| TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_BREAKFALL_START_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.containsKey(player)
				|| !PENDING_FAST_RUN_DASHES.isEmpty();
	}

	private static boolean shouldProbeSpiderWallJump(Player player) {
		return ModCompat.isWomLoaded()
				&& EPMConfig.spiderWallJumpPrimesPhantomAscent()
				&& !PHANTOM_ASCENT_TICKS.containsKey(player)
				&& !Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player))
				&& !player.onGround()
				&& !player.isInWater();
	}

	private static void playPendingFastRunDash(Player player) {
		if (!EPMConfig.naturalSprinterAnimations()) {
			PENDING_FAST_RUN_DASHES.clear();
			NATURAL_SPRINTER_BREAKFALL_START_TICKS.remove(player);
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.remove(player);
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (isPhantomAscentAirborneLocked(player)) {
			if (playerPatch != null) {
				PENDING_FAST_RUN_DASHES.remove(playerPatch);
			}
			return;
		}

		if (playerPatch == null || !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = PENDING_FAST_RUN_DASHES.remove(playerPatch);
		if (animation != null) {
			playerPatch.playAnimationInClientSide(animation, 0.0F);
		}
	}

	private static void tickBreakfallDelayedNaturalSprinterDash(Player player) {
		if (player == null || !player.isLocalPlayer() || !EPMConfig.naturalSprinterAnimations()) {
			NATURAL_SPRINTER_BREAKFALL_START_TICKS.remove(player);
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.remove(player);
			return;
		}

		Integer startTick = NATURAL_SPRINTER_BREAKFALL_START_TICKS.get(player);
		if (startTick == null) {
			startTick = Integer.valueOf(player.tickCount);
			NATURAL_SPRINTER_BREAKFALL_START_TICKS.put(player, startTick);
		}

		int elapsedTicks = player.tickCount - startTick.intValue();
		if (elapsedTicks < 1) {
			return;
		}

		boolean actionStillDoing = isBreakfallFollowupDoing(player);
		if (actionStillDoing && elapsedTicks < NATURAL_SPRINTER_BREAKFALL_DASH_MAX_DELAY_TICKS) {
			delayPendingNaturalSprinterDashForBreakfall(player);
			return;
		}
		if (!actionStillDoing
				&& elapsedTicks <= NATURAL_SPRINTER_BREAKFALL_DASH_STARTUP_GRACE_TICKS
				&& NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.containsKey(player)) {
			return;
		}

		NATURAL_SPRINTER_BREAKFALL_START_TICKS.remove(player);
		AssetAccessor<? extends StaticAnimation> delayedDash = NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.remove(player);
		if (delayedDash == null) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch) && !isPhantomAscentAirborneLocked(player)) {
			PENDING_FAST_RUN_DASHES.put(playerPatch, delayedDash);
		}
	}

	private static boolean shouldDelayNaturalSprinterDashForBreakfall(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& (NATURAL_SPRINTER_BREAKFALL_START_TICKS.containsKey(player) || isBreakfallFollowupDoing(player));
	}

	private static void delayPendingNaturalSprinterDashForBreakfall(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> pendingDash = PENDING_FAST_RUN_DASHES.remove(playerPatch);
		if (pendingDash != null) {
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.put(player, pendingDash);
		}
	}

	private static boolean isBreakfallFollowupDoing(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			return parkourability != null
					&& (parkourability.get(Roll.class).isDoing() || parkourability.get(Tap.class).isDoing());
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void markForPhantomAscent(Player player, PhantomAscentPrimeSource source) {
		if (Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player)) || isHoldingPhantomAscentBlockedWeapon(player) || isParCoolHanging(player)) {
			return;
		}

		PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(0));
		PHANTOM_ASCENT_SOURCES.put(player, source);
		phantomJumpWasDown = isPhysicalJumpKeyDown();
	}

	private static void tickPhantomAscent(Player player) {
		Integer ticks = PHANTOM_ASCENT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		int nextTick = ticks.intValue() + 1;
		if (!isPhantomAscentPrimeEnabled(player) || isHoldingPhantomAscentBlockedWeapon(player) || isParCoolHanging(player) || nextTick > 80) {
			clearPhantomAscent(player);
			return;
		}

		if (isJumpKeyRecentlyPressed()) {
			primePhantomAscentForNextInput(player);
			clearPhantomAscent(player);
			return;
		}

		PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(nextTick));
	}

	private static void clearPhantomAscent(Player player) {
		PHANTOM_ASCENT_TICKS.remove(player);
		PHANTOM_ASCENT_SOURCES.remove(player);
		phantomJumpWasDown = false;
	}

	private static void markSpiderWallJumpForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch != null && WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallBackflip())) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
		}
	}

	private static void primePhantomAscentForNextInput(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		if (isHoldingPhantomAscentBlockedWeapon(player)) {
			clearPhantomAscent(player);
			return;
		}

		SkillContainer phantomAscent = findPhantomAscent(playerPatch);
		if (phantomAscent != null && phantomAscent.getDataManager() != null) {
			phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.TRUE);
			phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_COUNT.get(), Integer.valueOf(999));
			phantomAscent.setResource(0.0F);
		}

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			cancelCurrentActionBeforeNativePhantomAscent(player, localPlayerPatch);
			PENDING_FORCED_PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(1));
		}
	}

	private static void tickPendingForcedPhantomAscent(Player player) {
		Integer ticks = PENDING_FORCED_PHANTOM_ASCENT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() > 3) {
			PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
			return;
		}

		PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			triggerNativePhantomAscent(player, localPlayerPatch);
		}
	}

	private static void cancelCurrentActionBeforeNativePhantomAscent(Player player, LocalPlayerPatch playerPatch) {
		PHANTOM_ASCENT_USED_AIRBORNE.put(player, Boolean.TRUE);

		stopPlaying(playerPatch,
				WomAnimationRefs.bipedSprintJump(),
				WomAnimationRefs.wallBackflip(),
				WomAnimationRefs.epicParCoolCatLeap(),
				WomAnimationRefs.epicParCoolCatLeapPreparation(),
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight());

		NATURAL_SPRINTER_CAT_LEAP_TICKS.remove(player);
		NATURAL_SPRINTER_CAT_LEAP_PATCHES.remove(playerPatch);
		NaturalSprinterState.suppress(playerPatch);
		clearParCoolAnimator(player);

		try {
			playerPatch.getClientAnimator().offAllLayers();
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
			playerPatch.setModelYRot(player.getYRot(), true);
		} catch (RuntimeException | LinkageError ignored) {
		}

	}

	private static void triggerNativePhantomAscent(Player player, LocalPlayerPatch playerPatch) {
		SkillContainer phantomAscent = findPhantomAscent(playerPatch);
		if (phantomAscent == null || phantomAscent.getDataManager() == null || !(player instanceof LocalPlayer localPlayer)) {
			return;
		}

		phantomAscent.setResource(0.0F);
		if (phantomAscent.getStack() < 1) {
			phantomAscent.setStack(1);
		}
		phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.FALSE);
		phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_COUNT.get(), Integer.valueOf(1));

		try {
			PlayerInputState inputState = PlayerInputState.fromVanillaInput(localPlayer.input).withJumping(true);
			MovementInputEvent movementInputEvent = new MovementInputEvent(playerPatch, inputState);
			playerPatch.getEventListener().triggerEvents(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, movementInputEvent);
			markPhantomAscentAirAttackWindow(player);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void clearAirbornePhantomAscentLockIfLanded(Player player) {
		if (!player.onGround()) {
			return;
		}

		PHANTOM_ASCENT_USED_AIRBORNE.remove(player);
		PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
		PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT.remove(player);
		PHANTOM_ASCENT_STARTED_TICKS.remove(player);
		DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.remove(player);
		PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.remove(player);
	}

	private static void cancelPhantomAscentForAirAttack(LocalPlayerPatch playerPatch) {
		beginPhantomAscentAirAttackSprintSuppression(playerPatch);
		stopPlaying(playerPatch, WomAnimationRefs.bipedPhantomAscentForward(), WomAnimationRefs.bipedPhantomAscentBackward());
		try {
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void beginPhantomAscentAirAttackSprintSuppression(LocalPlayerPatch playerPatch) {
		Player player = playerPatch.getOriginal();
		PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.put(player, Integer.valueOf(PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_DURATION_TICKS));
		NaturalSprinterState.suppress(playerPatch);
		PENDING_FAST_RUN_DASHES.remove(playerPatch);
		clearVaultFastRunHold(player);
		cancelAutoSprintAfterWallJump(player);
		clearParCoolAnimator(player);
	}

	private static void tickPhantomAscentAirAttackSprintSuppression(Player player) {
		Integer ticks = PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (ticks.intValue() <= 0 || player.onGround() || player.isDeadOrDying() || player.isInWater()) {
			PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.remove(player);
			if (playerPatch != null) {
				PENDING_FAST_RUN_DASHES.remove(playerPatch);
			}
			return;
		}

		if (playerPatch != null) {
			NaturalSprinterState.suppress(playerPatch);
			PENDING_FAST_RUN_DASHES.remove(playerPatch);
		}
		clearVaultFastRunHold(player);
		cancelAutoSprintAfterWallJump(player);
		clearParCoolAnimator(player);
		PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static int phantomAscentElapsedTicks(Player player) {
		Integer startTick = PHANTOM_ASCENT_STARTED_TICKS.get(player);
		return startTick == null ? PHANTOM_ASCENT_AIR_ATTACK_DELAY_TICKS : player.tickCount - startTick.intValue();
	}

	private static boolean isIncomingPhantomAscentAirAttackFollowup(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> incomingAnimation) {
		return isIncomingBasicAirAttack(playerPatch, incomingAnimation) || isInvincibleStyleComboAttack(incomingAnimation);
	}

	private static boolean isIncomingBasicAirAttack(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> incomingAnimation) {
		if (incomingAnimation == null) {
			return false;
		}

		CapabilityItem mainHand = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		if (mainHand == null) {
			return false;
		}

		java.util.List<? extends AssetAccessor<?>> autoAttackMotions = mainHand.getAutoAttackMotion(playerPatch);
		return autoAttackMotions != null
				&& !autoAttackMotions.isEmpty()
				&& isSameAnimation(incomingAnimation, autoAttackMotions.get(autoAttackMotions.size() - 1));
	}

	private static boolean isInvincibleStyleComboAttack(AssetAccessor<?> incomingAnimation) {
		ResourceLocation registryName = safeRegistryName(incomingAnimation);
		if (registryName == null) {
			return false;
		}

		String namespace = registryName.getNamespace();
		return "invincible".equals(namespace) || "efn".equals(namespace);
	}

	private static boolean isSameAnimation(AssetAccessor<?> first, AssetAccessor<?> second) {
		if (first == null || second == null) {
			return false;
		}

		if (first.equals(second)) {
			return true;
		}

		ResourceLocation firstRegistryName = safeRegistryName(first);
		ResourceLocation secondRegistryName = safeRegistryName(second);
		if (firstRegistryName != null && firstRegistryName.equals(secondRegistryName)) {
			return true;
		}

		Object firstAnimation = safeAsset(first);
		Object secondAnimation = safeAsset(second);
		if (firstAnimation == null || secondAnimation == null) {
			return false;
		}

		if (firstAnimation == secondAnimation || firstAnimation.equals(secondAnimation)) {
			return true;
		}

		if (firstAnimation instanceof StaticAnimation firstStaticAnimation && secondAnimation instanceof StaticAnimation secondStaticAnimation) {
			ResourceLocation firstLocation = firstStaticAnimation.getRegistryName();
			ResourceLocation secondLocation = secondStaticAnimation.getRegistryName();
			return firstLocation != null && firstLocation.equals(secondLocation);
		}

		return false;
	}

	private static ResourceLocation safeRegistryName(AssetAccessor<?> animation) {
		try {
			return animation.registryName();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Object safeAsset(AssetAccessor<?> animation) {
		try {
			return animation.get();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static void playDelayedAnimatorControl(LocalPlayerPatch playerPatch, DelayedAnimatorControl delayedAttack) {
		if (delayedAttack.action() == AnimatorControlPacket.Action.PLAY_CLIENT
				&& delayedAttack.layer() != AnimatorControlPacket.Layer.ANIMATION
				&& delayedAttack.priority() != AnimatorControlPacket.Priority.ANIMATION) {
			playerPatch.getClientAnimator().playAnimationAt(AnimationManager.byId(delayedAttack.animationId()),
					delayedAttack.transitionTimeModifier(),
					delayedAttack.layer(),
					delayedAttack.priority());
			return;
		}

		new SPAnimatorControl(delayedAttack.action(),
				delayedAttack.animationId(),
				playerPatch.getOriginal().getId(),
				delayedAttack.transitionTimeModifier(),
				delayedAttack.pause(),
				delayedAttack.layer(),
				delayedAttack.priority()).process(playerPatch);
	}

	private static SkillContainer findPhantomAscent(PlayerPatch<?> playerPatch) {
		if (playerPatch.getSkillCapability() == null) {
			return null;
		}

		SkillContainer cached = PHANTOM_ASCENT_CONTAINERS.get(playerPatch);
		if (isPhantomAscentContainer(cached)) {
			return cached;
		}

		try (var containers = playerPatch.getSkillCapability().listSkillContainers()) {
			SkillContainer found = containers
					.filter(EPMClientHooks::isPhantomAscentContainer)
					.findFirst()
					.orElse(null);
			if (found != null) {
				PHANTOM_ASCENT_CONTAINERS.put(playerPatch, found);
			}
			return found;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isPhantomAscentContainer(SkillContainer container) {
		if (container == null) {
			return false;
		}

		Skill skill = container.getSkill();
		return skill instanceof PhantomAscentSkill
				|| skill != null && "epicfight:phantom_ascent".equals(String.valueOf(skill.getRegistryName()));
	}

	@SafeVarargs
	private static void stopPlaying(LocalPlayerPatch playerPatch, AssetAccessor<? extends StaticAnimation>... animations) {
		for (AssetAccessor<? extends StaticAnimation> animation : animations) {
			if (animation == null) {
				continue;
			}

			try {
				playerPatch.stopPlaying(animation);
			} catch (RuntimeException | LinkageError ignored) {
			}
		}
	}

	private static void tickNaturalSprinterCatLeap(Player player) {
		Integer ticks = NATURAL_SPRINTER_CAT_LEAP_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		int nextTick = ticks.intValue() + 1;
		if ((nextTick > 2 && player.onGround()) || nextTick > 60) {
			NATURAL_SPRINTER_CAT_LEAP_TICKS.remove(player);
			resetBaseAnimation(player);
			return;
		}

		NATURAL_SPRINTER_CAT_LEAP_TICKS.put(player, Integer.valueOf(nextTick));
	}

	private static void installJumpSpeedModifier() {
		if (jumpSpeedModifierInstalled) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> sprintJump = WomAnimationRefs.bipedSprintJump();
		if (sprintJump == null) {
			return;
		}

		StaticAnimation animation = sprintJump.get();
		PlaybackSpeedModifier originalModifier = animation.getProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER).orElse(null);
		animation.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
			float modifiedSpeed = originalModifier == null ? speed : originalModifier.modify(self, entitypatch, speed, prevElapsedTime, elapsedTime);
			return isNaturalSprinterCatLeapPatch(entitypatch) ? modifiedSpeed * 0.5F : modifiedSpeed;
		});
		jumpSpeedModifierInstalled = true;
	}

	private static boolean isNaturalSprinterCatLeapPatch(LivingEntityPatch<?> entityPatch) {
		return entityPatch != null && Boolean.TRUE.equals(NATURAL_SPRINTER_CAT_LEAP_PATCHES.get(entityPatch));
	}

	private static void resetBaseAnimation(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		NATURAL_SPRINTER_CAT_LEAP_PATCHES.remove(playerPatch);
		NaturalSprinterState.suppress(playerPatch);
		player.setSprinting(false);
		clearParCoolAnimator(player);

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopPlaying(localPlayerPatch, WomAnimationRefs.bipedSprintJump());
		}

		try {
			AssetAccessor<? extends StaticAnimation> idle = WomAnimationRefs.bipedIdle();
			if (idle != null) {
				playerPatch.playAnimationInClientSide(idle, 0.0F);
			} else {
				playerPatch.getClientAnimator().resetMotion(true);
			}
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void clearParCoolAnimator(Player player) {
		Animation animation = Animation.get(player);
		if (animation != null) {
			animation.removeAnimator();
		}
	}

	private static void tickVaultFastRunGraceWindow(Player player) {
		Integer ticks = VAULT_FAST_RUN_GRACE_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() <= 0) {
			VAULT_FAST_RUN_GRACE_TICKS.remove(player);
			return;
		}

		VAULT_FAST_RUN_GRACE_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void tickAutoSprintAfterWallJump(Player player) {
		Integer ticks = WALL_JUMP_AUTO_SPRINT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() <= 0 || !EPMConfig.autoSprintAfterWallJump() || hasHardVaultFastRunBlocker(player)) {
			cancelAutoSprintAfterWallJump(player);
			return;
		}

		player.setSprinting(true);
		ensureFastRunAnimator(player);
		WALL_JUMP_AUTO_SPRINT_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void tickTaczWallJumpShootCancel(Player player) {
		Integer ticks = TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() <= 0 || player.onGround() || player.isSpectator() || player.isInWater()) {
			cancelTaczWallJumpShootCancel(player);
			return;
		}

		TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void tickTaczShootFastRunSuppression(Player player) {
		Integer ticks = TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (shouldKeepTaczShootFastRunSuppression(player)) {
			stopLocalSprintAndFastRunAnimation(player);
			TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_SUPPRESS_DURATION_TICKS));
			return;
		}

		TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.remove(player);
		if (TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.containsKey(player)) {
			startTaczShootFastRunRestore(player);
		}
	}

	private static void tickTaczShootFastRunRestore(Player player) {
		Integer ticks = TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (shouldKeepTaczShootFastRunSuppression(player)) {
			TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS));
			return;
		}

		if (ticks.intValue() <= 0) {
			cancelTaczShootFastRunRestore(player);
			return;
		}

		if (!isHoldingTaczGun(player) || hasHardVaultFastRunBlocker(player)) {
			cancelTaczShootFastRunRestore(player);
			return;
		}

		player.setSprinting(true);
		ensureFastRunAnimator(player);
		TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void tickTaczShootStopFastRunDashSuppression(Player player) {
		if (player == null || !player.isLocalPlayer() || !isHoldingTaczGun(player)) {
			TACZ_SHOOT_ACTIVE.remove(player);
			TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.remove(player);
			return;
		}

		if (TACZ_SHOOT_ACTIVE.containsKey(player)) {
			Minecraft minecraft = Minecraft.getInstance();
			boolean attackDown = minecraft != null && minecraft.options != null && minecraft.options.keyAttack.isDown();
			if (attackDown) {
				return;
			}

			TACZ_SHOOT_ACTIVE.remove(player);
			TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS));
			return;
		}

		Integer ticks = TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() <= 0) {
			TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.remove(player);
			return;
		}

		TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void tickTaczReloadFastRunDashSuppression(Player player) {
		Integer ticks = TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() <= 0 || player == null || !player.isLocalPlayer() || !isHoldingTaczGun(player)) {
			TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.remove(player);
			return;
		}

		TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static boolean shouldKeepTaczShootFastRunSuppression(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		return player != null
				&& player.isLocalPlayer()
				&& minecraft != null
				&& minecraft.options != null
				&& minecraft.options.keyAttack.isDown()
				&& isHoldingTaczGun(player);
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

	private static void stopLocalSprintAndFastRunAnimation(Player player) {
		player.setSprinting(false);
		clearParCoolAnimator(player);
	}

	private static void startTaczShootFastRunRestore(Player player) {
		if (!isHoldingTaczGun(player) || hasHardVaultFastRunBlocker(player)) {
			cancelTaczShootFastRunRestore(player);
			return;
		}

		TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS));
		player.setSprinting(true);
		ensureFastRunAnimator(player);
	}

	private static void cancelTaczShootFastRunRestore(Player player) {
		TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.remove(player);
	}


	private static void cancelAutoSprintAfterWallJump(Player player) {
		WALL_JUMP_AUTO_SPRINT_TICKS.remove(player);
	}

	private static void cancelTaczWallJumpShootCancel(Player player) {
		TACZ_WALL_JUMP_SHOOT_CANCEL_TICKS.remove(player);
	}

	private static void restoreClingMoveClimbUpVelocity(Player player, boolean consume) {
		Integer startTick = EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.get(player);
		if (startTick == null || startTick.intValue() != player.tickCount || player.onGround()) {
			if (consume) {
				EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.remove(player);
			}
			return;
		}

		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null || !parkourability.get(ClimbUp.class).isDoing()) {
			if (consume) {
				EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.remove(player);
			}
			return;
		}

		Vec3 movement = player.getDeltaMovement();
		double configuredVelocity = EPMConfig.epicParCoolClimbUpVerticalVelocity();
		if (movement.y() < configuredVelocity) {
			player.setDeltaMovement(movement.x(), configuredVelocity, movement.z());
		}

		if (consume) {
			EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.remove(player);
		}
	}

	private static void startEpicParCoolClimbUpAirControl(Player player) {
		int ticks = EPMConfig.epicParCoolClimbUpLateralAirControlTicks();
		double velocity = EPMConfig.epicParCoolClimbUpLateralAirControlVelocity();
		if (ticks <= 0 || velocity <= 0.0D || player == null || !player.isLocalPlayer()) {
			EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.remove(player);
			return;
		}

		EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.put(player, Integer.valueOf(player.tickCount));
	}

	private static void tickEpicParCoolClimbUpAirControl(Player player) {
		if (!hasEpicParCoolClimbUpAirControlWindow(player)) {
			return;
		}

		int direction = lateralInputDirection();
		if (direction != 0) {
			applyClimbUpLateralAirControl(player, direction);
		}
	}

	private static boolean hasEpicParCoolClimbUpAirControlWindow(Player player) {
		Integer startTick = EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.get(player);
		if (startTick == null) {
			return false;
		}

		int durationTicks = EPMConfig.epicParCoolClimbUpLateralAirControlTicks();
		double velocity = EPMConfig.epicParCoolClimbUpLateralAirControlVelocity();
		if (durationTicks <= 0 || velocity <= 0.0D || player == null || !player.isLocalPlayer() || player.onGround()) {
			EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.remove(player);
			return false;
		}

		int elapsedTicks = player.tickCount - startTick.intValue();
		if (elapsedTicks < 0 || elapsedTicks >= durationTicks) {
			EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.remove(player);
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !playerPatch.isEpicFightMode()) {
			EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.remove(player);
			return false;
		}

		return true;
	}

	private static void applyClimbUpLateralAirControl(Player player, int direction) {
		double velocity = EPMConfig.epicParCoolClimbUpLateralAirControlVelocity();
		if (velocity <= 0.0D) {
			return;
		}

		Vec3 lateralDirection = leftDirection(player);
		if (direction < 0) {
			lateralDirection = lateralDirection.reverse();
		}

		Vec3 movement = player.getDeltaMovement();
		Vec3 compensation = lateralDirection.scale(velocity);
		player.setDeltaMovement(movement.x() + compensation.x(), movement.y(), movement.z() + compensation.z());
	}

	private static int lateralInputDirection() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null) {
			return 0;
		}

		boolean left = minecraft.options.keyLeft.isDown();
		boolean right = minecraft.options.keyRight.isDown();
		if (left == right) {
			return 0;
		}
		return left ? 1 : -1;
	}

	private static Vec3 leftDirection(Player player) {
		double radians = Math.toRadians(player.getYRot() - 90.0F);
		return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians)).normalize();
	}

	private static boolean hasHardVaultFastRunBlocker(Player player) {
		return player == null
				|| !player.isLocalPlayer()
				|| isPhantomAscentAirborneLocked(player)
				|| player.isShiftKeyDown()
				|| player.isInWaterOrBubble()
				|| player.isFallFlying()
				|| player.getVehicle() != null;
	}

	private static boolean isParCoolHanging(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return false;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			return parkourability != null && parkourability.get(HangDown.class).isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void ensureFastRunAnimator(Player player) {
		Animation animation = Animation.get(player);
		if (animation != null && !animation.hasAnimator()) {
			animation.setAnimator(new FastRunningAnimator());
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isEpicParCoolClingMoveAnimation(AssetAccessor<?> animation) {
		ResourceLocation registryName = safeRegistryName(animation);
		return registryName != null
				&& "epicparcool".equals(registryName.getNamespace())
				&& registryName.getPath().startsWith("biped/cling_move_");
	}

	private static boolean isEpicParCoolClimbUpNoActionAnimation(AssetAccessor<?> animation) {
		ResourceLocation registryName = safeRegistryName(animation);
		return registryName != null
				&& "epicparcool".equals(registryName.getNamespace())
				&& "biped/climb_up_no_action".equals(registryName.getPath());
	}

	private static boolean isParCoolJumpPressed() {
		try {
			return KeyRecorder.keyJumpState.isPressed();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasNaturalSprinter(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch);
	}

	public static boolean isHoldingPhantomAscentBlockedWeapon(Player player) {
		return player != null && (isPhantomAscentBlockedWeapon(player.getMainHandItem()) || isPhantomAscentBlockedWeapon(player.getOffhandItem()));
	}

	private static boolean isPhantomAscentBlockedWeapon(net.minecraft.world.item.ItemStack stack) {
		return !stack.isEmpty() && HF_MURASAMA.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
	}

	private static boolean isPhantomAscentAirborneLocked(Player player) {
		return player != null && Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player)) && !player.onGround();
	}

	private static boolean isPhantomAscentPrimeEnabled(Player player) {
		PhantomAscentPrimeSource source = PHANTOM_ASCENT_SOURCES.get(player);
		return source != null && source.enabled();
	}

	private static boolean isJumpKeyRecentlyPressed() {
		boolean down = isPhysicalJumpKeyDown();
		boolean pressed = down && !phantomJumpWasDown;
		phantomJumpWasDown = down;
		return pressed;
	}

	private static boolean isPhysicalJumpKeyDown() {
		try {
			if (InputManager.isActionActive(MinecraftInputAction.JUMP)) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
	}

	private static KeyRecorder.KeyState fastRunKeyState() {
		try {
			return KeyRecorder.keyFastRunning;
		} catch (LinkageError ignored) {
			return null;
		}
	}

	private enum PhantomAscentPrimeSource {
		CAT_LEAP {
			@Override
			boolean enabled() {
				return EPMConfig.catLeapPrimesPhantomAscent();
			}
		},
		WALL_JUMP {
			@Override
			boolean enabled() {
				return EPMConfig.wallJumpPrimesPhantomAscent();
			}
		},
		SPIDER_WALL_JUMP {
			@Override
			boolean enabled() {
				return EPMConfig.spiderWallJumpPrimesPhantomAscent();
			}
		};

		abstract boolean enabled();
	}

	private record DelayedAnimatorControl(
			AnimatorControlPacket.Action action,
			int animationId,
			float transitionTimeModifier,
			boolean pause,
			AnimatorControlPacket.Layer layer,
			AnimatorControlPacket.Priority priority) {
	}
}
