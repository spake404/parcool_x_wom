package dev.spake404.epm;

import java.util.WeakHashMap;

import com.alrex.parcool.client.animation.impl.FastRunningAnimator;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.HangDown;
import com.alrex.parcool.common.action.impl.HideInBlock;
import com.alrex.parcool.common.action.impl.RideZipline;
import com.alrex.parcool.common.action.impl.Roll;
import com.alrex.parcool.common.action.impl.Tap;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import dev.spake404.epm.mixin.AnimatorControlPacketAccessor;
import dev.spake404.epm.mixin.ParCoolAnimationAccessor;
import dev.spake404.epm.mixin.SPAnimatorControlAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
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
import yesman.epicfight.gameasset.Animations;
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
import net.venturecraft.gliders.client.animation.AnimatedPlayer;
import net.venturecraft.gliders.common.compat.trinket.CuriosTrinketsUtil;
import net.venturecraft.gliders.common.item.GliderItem;
import net.venturecraft.gliders.data.GliderData;
import net.venturecraft.gliders.network.MessageToggleGlide;

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
	private static final WeakHashMap<Player, PhantomAscentPrimeSource> PENDING_FORCED_PHANTOM_ASCENT_SOURCES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_HOLD_FAST_RUN = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_FAST_RUN_GRACE_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_GRACE_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_EARLY_FINISH_LOG_TICKS = new WeakHashMap<>();
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
	private static final WeakHashMap<Player, NaturalSprinterStepFastRunState> NATURAL_SPRINTER_STEP_FAST_RUN_STATES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> NATURAL_SPRINTER_BREAKFALL_START_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, AssetAccessor<? extends StaticAnimation>> NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES = new WeakHashMap<>();
	private static final WeakHashMap<Player, DeferredNaturalSprinterDodgeStep> NATURAL_SPRINTER_DODGE_DEFERRED_STEPS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, SkillContainer> PHANTOM_ASCENT_CONTAINERS = new WeakHashMap<>();
	private static final WeakHashMap<Player, ExhaustionPoseSnapshot> EXHAUSTION_POSE_SNAPSHOTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, GliderOpeningDelayState> GLIDER_OPENING_DELAY_STATES = new WeakHashMap<>();
	private static final WeakHashMap<Player, GliderFastRunSnapshot> GLIDER_FAST_RUN_SNAPSHOTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_GLIDER_FAST_RUN_ACTIVE_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> GLIDER_FAST_RUN_ANIMATOR_CLEARED = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PENDING_GLIDER_OPENING_SOUNDS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> GLIDER_RENDER_DIAGNOSTIC_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> GLIDER_DECISION_DIAGNOSTIC_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> GLIDER_DISABLE_REQUESTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> GLIDER_REPLAY_REQUESTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PENDING_GLIDER_INPUT_ARBITRATION_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, PendingGliderPreinput> PENDING_GLIDER_PREINPUTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PENDING_GLIDER_TOGGLE_AFTER_PHANTOM = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PENDING_WOM_BACKFLIP_GLIDER_TOGGLE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PARCOOL_WALL_JUMP_INPUT_DOWN = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PARCOOL_WALL_JUMP_STARTED_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PARCOOL_WALL_RUN_HANDOFF_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> WOM_BACKFLIP_PHANTOM_LOCK_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> WALL_MOVEMENT_GLIDER_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<Player, CachedBoolean> WALL_MOVEMENT_GLIDER_CACHE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> GLIDER_DISABLE_SENT_TICKS = new WeakHashMap<>();
	private static final ResourceLocation HF_MURASAMA = ResourceLocation.fromNamespaceAndPath("efn", "hf_murasama");
	private static final ResourceLocation GLIDER_OPEN_SOUND = ResourceLocation.fromNamespaceAndPath(ModCompat.VC_GLIDERS, "glider_open");
	private static final ResourceLocation SPACE_DEPLOY_SOUND = ResourceLocation.fromNamespaceAndPath(ModCompat.VC_GLIDERS, "space_deploy");
	private static final int PHANTOM_ASCENT_AIR_ATTACK_DELAY_TICKS = 10;
	private static final int PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS = 13;
	private static final int PARCOOL_WALL_JUMP_PHANTOM_LOCK_MAX_TICKS = 80;
	private static final int PARCOOL_WALL_JUMP_HANDOFF_TOGGLE_SUPPRESS_TICKS = 2;
	private static final int PARCOOL_WALL_RUN_HANDOFF_GLIDER_BLOCK_MAX_TICKS = 30;
	private static final int WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_MAX_TICKS = 30;
	private static final int WOM_BACKFLIP_PHANTOM_LOCK_DURATION_TICKS = 15;
	private static final int WOM_BACKFLIP_GLIDER_START_SUPPRESS_TICKS = 13;
	private static final int WOM_BACKFLIP_PENDING_GLIDER_TOGGLE_MAX_TICKS = 25;
	private static final int PARCOOL_WALL_JUMP_PENDING_GLIDER_TOGGLE_MAX_TICKS = 30;
	private static final int PENDING_GLIDER_TOGGLE_MAX_TICKS = 30;
	private static final int GLIDER_INPUT_ARBITRATION_DELAY_TICKS = 1;
	private static final int GLIDER_INPUT_ARBITRATION_MAX_TICKS = 3;
	private static final int GLIDER_PREINPUT_SAME_PRESS_GRACE_TICKS = 1;
	private static final int PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_DURATION_TICKS = 12;
	private static final int WALL_JUMP_AUTO_SPRINT_DURATION_TICKS = 12;
	private static final int TACZ_WALL_JUMP_SHOOT_CANCEL_DURATION_TICKS = 40;
	private static final int TACZ_SHOOT_FAST_RUN_SUPPRESS_DURATION_TICKS = 3;
	private static final int TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS = 12;
	private static final int TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS = 30;
	private static final int TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS = 20;
	private static final int VAULT_FAST_RUN_CAN_ACT_GRACE_TICKS = 10;
	private static final int NATURAL_SPRINTER_BREAKFALL_DASH_STARTUP_GRACE_TICKS = 3;
	private static final int NATURAL_SPRINTER_BREAKFALL_DASH_MAX_DELAY_TICKS = 40;
	private static final int NATURAL_SPRINTER_DODGE_STEP_CLEAR_GRACE_TICKS = 6;
	private static final int NATURAL_SPRINTER_DODGE_STEP_MAX_DELAY_TICKS = 60;
	private static final int NATURAL_SPRINTER_STEP_FAST_RUN_STARTUP_MAX_TICKS = 6;
	private static volatile boolean jumpSpeedModifierInstalled;
	private static boolean phantomJumpWasDown;
	private static final ThreadLocal<PhantomAscentPrimeSource> ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE = new ThreadLocal<>();

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

	public static boolean markDemolitionLeapForPhantomAscent(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& EPMConfig.demolitionLeapAirDoubleJump()
				&& markForPhantomAscent(player, PhantomAscentPrimeSource.DEMOLITION_LEAP);
	}

	public static boolean isForcingDemolitionPhantomAscent(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.get() == PhantomAscentPrimeSource.DEMOLITION_LEAP;
	}

	public static void logForcedDemolitionPhantomAscentBypass(Player player, String phase, boolean originalValue) {
		if (!isForcingDemolitionPhantomAscent(player)) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		EPM.LOGGER.info(
				"[EPM/DemolitionLeap] phase={} tick={} originalBlocked={} inaction={} holdingAny={} currentAnimation={} delta={}",
				phase,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(originalValue),
				Boolean.valueOf(entityStateInaction(playerPatch)),
				Boolean.valueOf(isHoldingAny(playerPatch)),
				assetName(currentBaseAnimation(playerPatch)),
				player.getDeltaMovement());
	}

	public static void markWallJumpForPhantomAscent(Player player) {
		if (player != null && player.isLocalPlayer() && EPMConfig.wallJumpPrimesPhantomAscent()) {
			markParCoolWallJumpInputBaseline(player);
			if (markForPhantomAscent(player, PhantomAscentPrimeSource.WALL_JUMP)) {
				PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.put(player, Integer.valueOf(player.tickCount));
				logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_mark", true, false, false);
			} else {
				logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_prime_skip", false, false, false);
			}
		}
	}

	public static void markWomWallRunToParCoolWallJumpStarted(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		markParCoolWallJumpInputBaseline(player);
		markWallRunToParCoolWallJumpGliderSuppress(player, "wallrun_parcool_glider_lock_start");
		Integer previousTick = PARCOOL_WALL_RUN_HANDOFF_TICKS.put(player, Integer.valueOf(player.tickCount));
		if (previousTick == null || previousTick.intValue() != player.tickCount) {
			logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_mark", true, false, false);
		}
	}

	public static void markWallRunToParCoolWallJumpGliderSuppress(Player player, String phase) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		markParCoolWallJumpInputBaseline(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
		GLIDER_REPLAY_REQUESTS.remove(player);
		PENDING_GLIDER_OPENING_SOUNDS.remove(player);
		WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.put(player, Integer.valueOf(player.tickCount));
		logGliderOpeningDelayDiagnostic(player, phase, true, false, false);
		if (GliderCompat.isGlidingWithActiveGlider(player)) {
			forceStopGliderForWallRun(player);
		}
	}

	public static boolean hasWallRunToParCoolWallJumpCandidate(Player player) {
		Integer localStart = WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		return elapsed >= 0 && elapsed <= 2;
	}

	private static void markParCoolWallJumpInputBaseline(Player player) {
		if (player != null && player.isLocalPlayer()) {
			PARCOOL_WALL_JUMP_INPUT_DOWN.put(player, Boolean.valueOf(isPhysicalJumpKeyDown()));
			PARCOOL_WALL_JUMP_STARTED_TICKS.put(player, Integer.valueOf(player.tickCount));
		}
	}

	public static void markWomWallJumpForPhantomAscent(Player player) {
		if (player != null && player.isLocalPlayer() && EPMConfig.spiderWallJumpPrimesPhantomAscent()) {
			markWomBackflipPhantomLock(player, "mark_wom_wall_jump");
			if (!markForPhantomAscent(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP)) {
				logWomBackflipGliderGuard(player, "wom_backflip_phantom_prime_skip", "unavailable_or_used", false, false);
			}
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
		if (shouldStopFastRunForGlider(player)) {
			suppressFastRunAnimationForGlider(player);
			return;
		}

		if (shouldDelayNaturalSprinterDashForBreakfall(player)) {
			NATURAL_SPRINTER_BREAKFALL_START_TICKS.putIfAbsent(player, Integer.valueOf(player.tickCount));
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.put(player, animation);
			return;
		}

		PENDING_FAST_RUN_DASHES.put(playerPatch, animation);
	}

	public static boolean requestNaturalSprinterStepFastRun(Player player, AssetAccessor<? extends StaticAnimation> stepAnimation) {
		IStamina stamina = player == null ? null : IStamina.get(player);
		if (stepAnimation == null || !canKeepNaturalSprinterStepFastRun(player, stamina)) {
			clearNaturalSprinterStepFastRun(player);
			return false;
		}

		NATURAL_SPRINTER_STEP_FAST_RUN_STATES.put(player, new NaturalSprinterStepFastRunState(player.tickCount, stepAnimation));
		setSprintingWithDiagnostic(player, true, "natural_sprinter_step_fast_run_request");
		return true;
	}

	public static boolean playPendingNaturalSprinterStepFastRun(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		NaturalSprinterStepFastRunState state = NATURAL_SPRINTER_STEP_FAST_RUN_STATES.get(player);
		AssetAccessor<? extends StaticAnimation> stepAnimation = state == null ? null : state.startupStep;
		if (stepAnimation == null) {
			return false;
		}

		if (player.tickCount - state.startTick > NATURAL_SPRINTER_STEP_FAST_RUN_STARTUP_MAX_TICKS) {
			clearNaturalSprinterStepFastRun(player);
			return true;
		}

		if (!canKeepNaturalSprinterStepFastRun(player, IStamina.get(player))
				|| !NaturalSprinterState.hasNaturalSprinter(playerPatch)
				|| !NaturalSprinterState.consumeStep(playerPatch)) {
			clearNaturalSprinterStepFastRun(player);
			return true;
		}

		state.startupStep = null;
		NaturalSprinterFastRunHandler.advanceSprintStepPublic(playerPatch);
		queueNaturalSprinterFastRunDash(playerPatch, stepAnimation);
		return true;
	}

	public static boolean shouldKeepFastRunAfterNaturalSprinterStep(Player player, IStamina stamina) {
		if (!NATURAL_SPRINTER_STEP_FAST_RUN_STATES.containsKey(player)) {
			return false;
		}

		if (!canKeepNaturalSprinterStepFastRun(player, stamina)) {
			clearNaturalSprinterStepFastRun(player);
			return false;
		}

		setSprintingWithDiagnostic(player, true, "natural_sprinter_step_fast_run_keep");
		if (isParCoolFastRunDoing(player)) {
			ensureFastRunAnimator(player);
		}
		return true;
	}

	private static void tickNaturalSprinterStepFastRun(Player player, NaturalSprinterStepFastRunState state) {
		if (state == null) {
			return;
		}

		if (state.startupStep != null
				&& player.tickCount - state.startTick > NATURAL_SPRINTER_STEP_FAST_RUN_STARTUP_MAX_TICKS) {
			clearNaturalSprinterStepFastRun(player);
			return;
		}

		IStamina stamina = player == null ? null : IStamina.get(player);
		if (!canKeepNaturalSprinterStepFastRun(player, stamina)) {
			clearNaturalSprinterStepFastRun(player);
			return;
		}

		setSprintingWithDiagnostic(player, true, "natural_sprinter_step_fast_run_tick");
		if (isParCoolFastRunDoing(player)) {
			ensureFastRunAnimator(player);
		}
	}

	private static boolean canKeepNaturalSprinterStepFastRun(Player player, IStamina stamina) {
		if (player == null
				|| !player.isLocalPlayer()
				|| !ModCompat.isWomLoaded()
				|| !EPMConfig.naturalSprinterAnimations()
				|| !EPMConfig.naturalSprinterManualStep()
				|| stamina == null
				|| player.isSpectator()
				|| player.isDeadOrDying()
				|| !player.onGround()
				|| hasHardVaultFastRunBlocker(player)
				|| shouldStopFastRunForGlider(player)
				|| !hasNaturalSprinter(player)
				|| !hasNaturalSprinterStepFastRunMovementInput()) {
			return false;
		}

		if (stamina.isExhausted()) {
			return false;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			return parkourability != null
					&& parkourability.getActionInfo().can(FastRun.class)
					&& !parkourability.get(Crawl.class).isDoing()
					&& !parkourability.get(ClingToCliff.class).isDoing()
					&& !parkourability.get(HangDown.class).isDoing()
					&& !parkourability.get(RideZipline.class).isDoing()
					&& !parkourability.get(HideInBlock.class).isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasNaturalSprinterStepFastRunMovementInput() {
		try {
			Vec3 moveVector = KeyBindings.getCurrentMoveVector();
			return moveVector != null && moveVector.lengthSqr() > 1.0E-6D;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void clearNaturalSprinterStepFastRun(Player player) {
		if (player != null) {
			NATURAL_SPRINTER_STEP_FAST_RUN_STATES.remove(player);
		}
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

		if (!EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunState(player);
			return;
		}

		boolean fastRunGrace = VAULT_FAST_RUN_GRACE_TICKS.containsKey(player);
		VAULT_HOLD_FAST_RUN.remove(player);
		VAULT_FAST_RUN_GRACE_TICKS.remove(player);

		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) {
			return;
		}

		FastRun fastRun = parkourability.get(FastRun.class);
		if (fastRun != null && (fastRun.isDoing() || fastRunGrace)) {
			VAULT_HOLD_FAST_RUN.put(player, Boolean.TRUE);
			setSprintingWithDiagnostic(player, true, "vault_start_from_fast_run");
		}
	}

	public static void clearVaultFastRunHold(Player player) {
		if (player == null) {
			return;
		}

		if (!EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunState(player);
			return;
		}

		if (Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.remove(player))) {
			VAULT_FAST_RUN_GRACE_TICKS.put(player, Integer.valueOf(VAULT_FAST_RUN_CAN_ACT_GRACE_TICKS));
		}
	}

	public static boolean shouldAllowFastRunForVaultGrace(Player player) {
		if (!EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunState(player);
			return false;
		}

		Integer ticks = VAULT_FAST_RUN_GRACE_TICKS.get(player);
		if (ticks == null || ticks.intValue() <= 0) {
			return false;
		}

		if (player == null
				|| !player.isLocalPlayer()
				|| !player.level().isClientSide()
				|| !player.onGround()
				|| isParCoolVaultDoing(player)
				|| hasHardVaultFastRunBlocker(player)
				|| !hasVaultGraceMovementInput()) {
			return false;
		}

		if (EPMConfig.debugVaultState() && shouldLogVaultTick(VAULT_GRACE_LOG_TICKS, player)) {
			Vec3 delta = player.getDeltaMovement();
			EPM.LOGGER.info(
					"[EPM/VaultDebug] phase=fast_run_can_act_grace tick={} graceTicks={} onGround={} sprinting={} pos=({}, {}, {}) delta=({}, {}, {})",
					Integer.valueOf(player.tickCount),
					ticks,
					Boolean.valueOf(player.onGround()),
					Boolean.valueOf(player.isSprinting()),
					Double.valueOf(player.getX()),
					Double.valueOf(player.getY()),
					Double.valueOf(player.getZ()),
					Double.valueOf(delta.x()),
					Double.valueOf(delta.y()),
					Double.valueOf(delta.z()));
		}
		return true;
	}

	public static void finishVaultEarlyForCloseChain(Vault vault, Player player) {
		if (!EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunState(player);
			return;
		}

		if (vault == null
				|| player == null
				|| !player.isLocalPlayer()
				|| !player.level().isClientSide()
				|| !vault.isDoing()
				|| vault.getDoingTick() < 6
				|| vault.getDoingTick() >= 11
				|| !Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player))
				|| hasHardVaultFastRunBlocker(player)
				|| !hasVaultGraceMovementInput()) {
			return;
		}

		if (EPMConfig.debugVaultState() && shouldLogVaultTick(VAULT_EARLY_FINISH_LOG_TICKS, player)) {
			Vec3 delta = player.getDeltaMovement();
			EPM.LOGGER.info(
					"[EPM/VaultDebug] phase=early_finish_for_close_chain tick={} vaultTick={} pos=({}, {}, {}) delta=({}, {}, {})",
					Integer.valueOf(player.tickCount),
					Integer.valueOf(vault.getDoingTick()),
					Double.valueOf(player.getX()),
					Double.valueOf(player.getY()),
					Double.valueOf(player.getZ()),
					Double.valueOf(delta.x()),
					Double.valueOf(delta.y()),
					Double.valueOf(delta.z()));
		}
		vault.finish(player);
	}

	public static boolean wasHoldingFastRunDuringVault(Player player) {
		return EPMConfig.fastRunVaultChainFix()
				&& (Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player)) || VAULT_FAST_RUN_GRACE_TICKS.containsKey(player));
	}

	public static boolean shouldPreserveFastRunToggleDuringVault(Player player) {
		if (!wasHoldingFastRunDuringVault(player)) {
			return false;
		}

		return !isFastRunPressKeyControl() || isFastRunControlKeyDown();
	}

	public static boolean shouldKeepFastRunDuringVault(Player player) {
		if (!EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunState(player);
			return false;
		}

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

		setSprintingWithDiagnostic(player, true, "vault_keep_fast_run");
		return true;
	}

	public static void markAutoSprintAfterWallJump(Player player) {
		if (player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.autoSprintAfterWallJump()
				|| GliderCompat.isGlidingWithActiveGlider(player)
				|| hasHardVaultFastRunBlocker(player)) {
			return;
		}

		WALL_JUMP_AUTO_SPRINT_TICKS.put(player, Integer.valueOf(WALL_JUMP_AUTO_SPRINT_DURATION_TICKS));
		setSprintingWithDiagnostic(player, true, "wall_jump_auto_sprint_mark");
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
		setSprintingWithDiagnostic(player, false, "cancel_wall_jump_for_tacz_shoot");

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
		logGliderFastRunDiagnostic(player, "tacz_restore_remember", true);
	}

	public static boolean shouldStopFastRunForTaczShoot(Player player) {
		return player != null && TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(player);
	}

	public static boolean shouldStopFastRunForGlider(Player player) {
		return player != null && player.isLocalPlayer() && GliderCompat.isGlidingWithActiveGlider(player);
	}

	public static void suppressFastRunAnimationForGlider(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		NATURAL_SPRINTER_BREAKFALL_START_TICKS.remove(player);
		NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.remove(player);
		clearNaturalSprinterStepFastRun(player);
		clearDeferredDodgeStep(player);

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch != null) {
			NaturalSprinterState.suppress(playerPatch);
			PENDING_FAST_RUN_DASHES.remove(playerPatch);
		}

		clearParCoolAnimator(player);
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

		logGliderFastRunDiagnostic(player, "tacz_restore_query", true);
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

	public static boolean shouldSuppressJumpChargingForTacz(Player player) {
		return ModCompat.isTaczLoaded() && isHoldingTaczGun(player);
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
		clearGliderOpeningDelayState(player);
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

	public static boolean shouldDelayGliderOpeningAnimation(net.minecraft.world.entity.LivingEntity entity) {
		if (!(entity instanceof Player player) || !player.isLocalPlayer()) {
			return false;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		if (!snapshot.gliderActive()) {
			clearGliderOpeningDelayState(player);
			PENDING_GLIDER_OPENING_SOUNDS.remove(player);
			return false;
		}

		ensureGliderOpeningDelayWindowFromCurrentAnimation(player);
		if (!shouldDelayGliderOpeningForPriority(player)) {
			clearGliderOpeningDelayState(player);
			return false;
		}

		GliderOpeningDelayState state = GLIDER_OPENING_DELAY_STATES.get(player);
		if (state != null && state.checkedTick == player.tickCount) {
			return state.delay;
		}

		boolean delay = computeGliderOpeningDelay(player);
		if (state == null) {
			state = new GliderOpeningDelayState();
			GLIDER_OPENING_DELAY_STATES.put(player, state);
		}

		state.checkedTick = player.tickCount;
		state.delay = delay;
		logGliderOpeningDelayDiagnostic(player, "decision", delay, false, true);
		return state.delay;
	}

	public static void delayGliderOpeningSound(PlaySoundEvent event) {
		if (event == null || (!GLIDER_OPEN_SOUND.toString().equals(event.getName()) && !SPACE_DEPLOY_SOUND.toString().equals(event.getName()))) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		if (player != null && shouldHardCancelGliderOpeningSound(player)) {
			event.setSound(null);
			return;
		}
		if (player == null || !shouldDelayGliderOpeningAnimation(player)) {
			if (player != null) {
				logGliderOpeningDelayDiagnostic(player, "sound_allow", false, false, false);
			}
			return;
		}

		PENDING_GLIDER_OPENING_SOUNDS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "sound_cancel", true, true, false);
		event.setSound(null);
	}

	public static void playDelayedGliderOpeningSound(net.minecraft.world.entity.LivingEntity entity) {
		if (!(entity instanceof Player player) || !player.isLocalPlayer()) {
			return;
		}

		if (!Boolean.TRUE.equals(PENDING_GLIDER_OPENING_SOUNDS.remove(player))) {
			return;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		if (!snapshot.gliderActive() || shouldDelayGliderOpeningAnimation(player)) {
			logGliderOpeningDelayDiagnostic(player, "sound_replay_skip", false, false, false);
			return;
		}

		SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(GLIDER_OPEN_SOUND);
		if (sound == null) {
			logGliderOpeningDelayDiagnostic(player, "sound_replay_missing", false, false, false);
			return;
		}

		logGliderOpeningDelayDiagnostic(player, "sound_replay", false, false, false);
		player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F, false);
	}

	public static boolean shouldBlockGliderToggleForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return false;
		}

		if (shouldHardBlockGliderToggleForWallRunToParCoolWallJump(player)) {
			return true;
		}

		ensureGliderOpeningDelayWindowFromCurrentAnimation(player);
		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);

		if (isWomBackflipPhantomLockActive(player)) {
			ensureWomBackflipPhantomLockFromData(player, "phantom_priority");
			if (isWomBackflipStartSuppressActive(player)) {
				boolean protectPhantom = shouldProtectWomBackflipForPhantom(player);
				if (protectPhantom) {
					dropGliderToggleReplayForWomBackflip(player, "toggle_block_wom_backflip_start_suppress");
				} else {
					cacheWomBackflipGliderPreinput(player, "phantom_priority");
				}
				logWomBackflipGliderGuard(player, "toggle_block_wom_backflip_start_suppress", "phantom_priority", true, !protectPhantom);
				return true;
			}
			if (!shouldProtectWomBackflipForPhantom(player)) {
				logWomBackflipGliderGuard(player, "toggle_allow_wom_backflip_no_phantom_after_start", "phantom_priority", false, false);
				return false;
			}
			dropGliderToggleReplayForWomBackflip(player, "toggle_block_wom_backflip_phantom_no_cache");
			logWomBackflipGliderGuard(player, "toggle_block_wom_backflip_phantom_no_cache", "phantom_priority", true, false);
			return true;
		}

		if (shouldProtectParCoolWallJumpForPhantom(player)) {
			dropGliderToggleReplayForParCoolWallJump(player, "parcool_wall_jump_phantom_handoff_block");
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_handoff_block", true, true, false);
			return true;
		}
		if (isParCoolWallJumpHandoffSuppressActive(player)) {
			dropGliderToggleReplayForParCoolWallJump(player, "parcool_wall_jump_handoff_input");
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_handoff_input_drop", true, true, false);
			return true;
		}
		if (isParCoolWallRunHandoffGliderBlockActive(player)) {
			if (cacheParCoolWallJumpGliderPreinputFromToggle(player, "parcool_wallrun_handoff_input")) {
				logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_input_cache", true, true, false);
			} else {
				dropGliderToggleReplayForParCoolWallJump(player, "parcool_wallrun_handoff_input");
				logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_input_drop", true, true, false);
			}
			return true;
		}

		boolean phantomQueued = PHANTOM_ASCENT_TICKS.containsKey(player);
		boolean realPhantomWindow = isGliderOpeningDelayWindowActive(player) || snapshot.phantomAnimation();
		boolean wallJumpPrime = snapshot.parCoolWallJumpAnimation() || (EPMConfig.spiderWallJumpPrimesPhantomAscent() && snapshot.womBackflipAnimation() && (PHANTOM_ASCENT_TICKS.containsKey(player) || canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP)));
		if (!phantomQueued && !realPhantomWindow && !wallJumpPrime) {
			return false;
		}

		logGliderOpeningDelayDiagnostic(player, "toggle_block_phantom_priority", true, true, false);
		if (realPhantomWindow
				&& (Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player)) || !isParCoolWallJumpPhantomLockActive(player))) {
			cacheUnifiedGliderPreinput(player, GliderPreinputSource.PHANTOM_ASCENT, "phantom_priority");
		}
		return true;
	}

	public static boolean shouldBlockGliderToggleForWomBackflip(Player player) {
		if (player == null || !player.isLocalPlayer() || !isWomBackflipPhantomLockActive(player)) {
			return false;
		}

		ensureWomBackflipPhantomLockFromData(player, "mixin");
		if (isWomBackflipStartSuppressActive(player)) {
			boolean protectPhantom = shouldProtectWomBackflipForPhantom(player);
			if (protectPhantom) {
				dropGliderToggleReplayForWomBackflip(player, "toggle_block_wom_backflip_start_suppress");
			} else {
				cacheWomBackflipGliderPreinput(player, "mixin");
			}
			logWomBackflipGliderGuard(player, "toggle_block_wom_backflip_start_suppress", "mixin", true, !protectPhantom);
			return true;
		}

		if (!shouldProtectWomBackflipForPhantom(player)) {
			logWomBackflipGliderGuard(player, "toggle_allow_wom_backflip_no_phantom_after_start", "mixin", false, false);
			return false;
		}

		dropGliderToggleReplayForWomBackflip(player, "toggle_block_wom_backflip_lock");
		logWomBackflipGliderGuard(player, "toggle_block_wom_backflip_lock", "mixin", true, false);
		return true;
	}

	public static boolean consumeGliderDisableRequest(Player player) {
		return player != null && GLIDER_DISABLE_REQUESTS.remove(player) != null;
	}

	public static boolean consumeGliderReplayRequest(Player player) {
		if (player == null || GLIDER_REPLAY_REQUESTS.remove(player) == null) {
			return false;
		}
		if (!GliderCompat.isGlidingWithActiveGlider(player)) {
			clearNaturalSprinterCatLeapForGlider(player, "cat_leap_preclear_for_glider_toggle");
		}
		return true;
	}

	public static boolean shouldDeferGliderToggleForInputArbitration(Player player) {
		if (player == null || !player.isLocalPlayer() || GliderCompat.isGlidingWithActiveGlider(player)) {
			return false;
		}

		if (isPhantomAscentConsumedJumpHeld(player)) {
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_phantom_consumed_jump", true, true, false);
			return true;
		}

		if (PENDING_GLIDER_PREINPUTS.containsKey(player)) {
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_existing_preinput_drop", true, true, false);
			return true;
		}

		Integer queuedTick = PENDING_GLIDER_INPUT_ARBITRATION_TICKS.get(player);
		if (queuedTick != null && queuedTick.intValue() == player.tickCount) {
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_duplicate_drop", true, true, false);
			return true;
		}

		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.put(player, Integer.valueOf(player.tickCount));
		logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_defer", true, true, false);
		return true;
	}

	public static boolean shouldHardBlockGliderToggleForWallRunToParCoolWallJump(Player player) {
		if (!isWallRunToParCoolWallJumpGliderSuppressActive(player)) {
			return false;
		}

		dropWallRunToParCoolWallJumpGliderRequests(player);
		logGliderOpeningDelayDiagnostic(player, "wallrun_parcool_glider_toggle_block", true, true, false);
		return true;
	}

	public static boolean shouldHardSuppressGliderTick(net.minecraft.world.entity.LivingEntity entity) {
		if (!(entity instanceof Player player) || !player.isLocalPlayer() || !isWallRunToParCoolWallJumpGliderSuppressActive(player)) {
			return false;
		}

		dropWallRunToParCoolWallJumpGliderRequests(player);
		forceStopGliderForWallRun(player);
		logGliderOpeningDelayDiagnostic(player, "wallrun_parcool_glider_tick_force_off", true, true, false);
		return true;
	}

	public static boolean shouldHardCancelGliderOpeningSound(Player player) {
		if (!isWallRunToParCoolWallJumpGliderSuppressActive(player)) {
			return false;
		}

		PENDING_GLIDER_OPENING_SOUNDS.remove(player);
		logGliderOpeningDelayDiagnostic(player, "wallrun_parcool_glider_sound_cancel", true, true, false);
		return true;
	}

	public static void forceStopGliderForWallRun(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		boolean gliderActive = snapshot.gliderActive();
		boolean disabledLocal = disableLocalGliderItem(player);
		if (!gliderActive && !disabledLocal) {
			return;
		}

		if (gliderActive) {
			Integer lastSentTick = GLIDER_DISABLE_SENT_TICKS.get(player);
			if (lastSentTick == null || lastSentTick.intValue() != player.tickCount) {
				GLIDER_DISABLE_SENT_TICKS.put(player, Integer.valueOf(player.tickCount));
				GLIDER_DISABLE_REQUESTS.put(player, Boolean.TRUE);
				sendGliderToggleMessage(player);
			}
		}
		stopGliderAnimations(player);
		clearGliderOpeningDelayState(player);
		PENDING_GLIDER_OPENING_SOUNDS.remove(player);
	}

	public static boolean isWallMovementAnimationActiveForGlider(Player player) {
		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		boolean active = snapshot.wallMovementAnimationActive();
		if (player != null && player.isLocalPlayer()) {
			CachedBoolean cached = WALL_MOVEMENT_GLIDER_CACHE.get(player);
			if (cached == null || cached.tick != player.tickCount || cached.value != active) {
				WALL_MOVEMENT_GLIDER_CACHE.put(player, new CachedBoolean(player.tickCount, active));
			}
			if (active) {
				LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.put(player, Integer.valueOf(player.tickCount));
			}
		}
		return active;
	}

	public static void logGliderOpeningDelayProbe(net.minecraft.world.entity.LivingEntity entity, String phase, boolean delay, boolean canceled) {
		if (!(entity instanceof Player player) || !player.isLocalPlayer()) {
			return;
		}

		logGliderOpeningDelayDiagnostic(player, phase, delay, canceled, "render_head".equals(phase));
	}

	private static void ensureGliderOpeningDelayWindowFromCurrentAnimation(Player player) {
		if (PHANTOM_ASCENT_STARTED_TICKS.containsKey(player)) {
			return;
		}

		if (GliderFrameState.snapshot(player).phantomAnimation()) {
			PHANTOM_ASCENT_STARTED_TICKS.put(player, Integer.valueOf(player.tickCount));
			logGliderOpeningDelayDiagnostic(player, "window_from_current_animation", true, false, false);
		}
	}

	private static boolean isCurrentPhantomAscentAnimation(Player player) {
		return GliderFrameState.snapshot(player).phantomAnimation();
	}

	private static void logGliderOpeningDelayDiagnostic(Player player, String phase, boolean delay, boolean canceled, boolean throttlePerTick) {
		if (!EPMConfig.debugGliderState() || player == null || !player.isLocalPlayer()) {
			return;
		}

		WeakHashMap<Player, Integer> throttle = "render_head".equals(phase) ? GLIDER_RENDER_DIAGNOSTIC_TICKS : GLIDER_DECISION_DIAGNOSTIC_TICKS;
		Integer lastTick = throttle.get(player);
		if (throttlePerTick && lastTick != null && lastTick.intValue() == player.tickCount) {
			return;
		}
		throttle.put(player, Integer.valueOf(player.tickCount));

		GliderFrameState.Snapshot gliderState = GliderFrameState.snapshot(player);
		PlayerPatch<?> playerPatch = gliderState.playerPatch();
		Integer startTick = PHANTOM_ASCENT_STARTED_TICKS.get(player);
		int elapsed = startTick == null ? -1 : player.tickCount - startTick.intValue();
		boolean windowActive = startTick != null && elapsed >= 0 && elapsed < PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS;
		boolean phantomAnimation = gliderState.phantomAnimation();
		Integer womBackflipLockStart = WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
		int womBackflipLockElapsed = womBackflipLockStart == null ? -1 : player.tickCount - womBackflipLockStart.intValue();
		boolean womBackflipLocalLock = isWomBackflipLocalLockActive(player);
		boolean womBackflipData = isWomSpiderBackflipDataActive(playerPatch);
		boolean womBackflipStartSuppress = isWomBackflipStartSuppressActive(player);
		Integer pendingWomToggleTick = PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.get(player);
		int pendingWomToggleElapsed = pendingWomToggleTick == null ? -1 : player.tickCount - pendingWomToggleTick.intValue();
		PendingGliderPreinput pendingPreinput = PENDING_GLIDER_PREINPUTS.get(player);
		int pendingPreinputElapsed = pendingPreinput == null ? -1 : player.tickCount - pendingPreinput.queuedTick();
		boolean phantomPrimeAvailable = canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
		boolean phantomUsed = Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player));
		JumpInputConsumptionState.Snapshot jumpInput = JumpInputConsumptionState.snapshot(player);
		boolean protectPhantom = shouldProtectWomBackflipForPhantom(player);
		Integer parcoolWallJumpLockStart = PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.get(player);
		int parcoolWallJumpLockElapsed = parcoolWallJumpLockStart == null ? -1 : player.tickCount - parcoolWallJumpLockStart.intValue();
		boolean parcoolWallJumpLock = isParCoolWallJumpPhantomLockActive(player);
		boolean parcoolWallJumpAnimation = isCurrentParCoolWallJumpAnimation(player);
		boolean parcoolWallJumpProtect = shouldProtectParCoolWallJumpForPhantom(player);
		Integer parcoolHandoffSuppressStart = PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.get(player);
		int parcoolHandoffSuppressElapsed = parcoolHandoffSuppressStart == null ? -1 : player.tickCount - parcoolHandoffSuppressStart.intValue();
		boolean parcoolHandoffSuppress = isParCoolWallJumpHandoffSuppressActive(player);
		Integer parcoolWallRunHandoffStart = PARCOOL_WALL_RUN_HANDOFF_TICKS.get(player);
		int parcoolWallRunHandoffElapsed = parcoolWallRunHandoffStart == null ? -1 : player.tickCount - parcoolWallRunHandoffStart.intValue();
		boolean parcoolWallRunHandoffBlock = isParCoolWallRunHandoffGliderBlockActive(player);
		Integer wallRunParCoolSuppressStart = WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.get(player);
		int wallRunParCoolSuppressElapsed = wallRunParCoolSuppressStart == null ? -1 : player.tickCount - wallRunParCoolSuppressStart.intValue();
		boolean wallRunParCoolSuppress = isWallRunToParCoolWallJumpGliderSuppressActive(player);
		Integer lastWallMovementTick = LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.get(player);
		int lastWallMovementElapsed = lastWallMovementTick == null ? -1 : player.tickCount - lastWallMovementTick.intValue();

		EPM.LOGGER.info(
				"[EPM/GliderDelay] phase={} tick={} delay={} canceled={} gliderActive={} window={} startTick={} elapsed={} phantomAnim={} animation={} pendingSound={} pendingToggle={} pendingWomToggle={} pendingWomElapsed={} pendingPreinput={} pendingPreinputSource={} pendingPreinputElapsed={} pendingPreinputMinDelay={} pendingPreinputMaxAge={} replayRequest={} parcoolWallJumpLock={} parcoolWallJumpProtect={} parcoolWallJumpAnim={} parcoolWallJumpStart={} parcoolWallJumpElapsed={} parcoolHandoffSuppress={} parcoolHandoffSuppressStart={} parcoolHandoffSuppressElapsed={} parcoolWallRunHandoffBlock={} parcoolWallRunHandoffStart={} parcoolWallRunHandoffElapsed={} wallrunParcoolSuppress={} wallrunParcoolSuppressStart={} wallrunParcoolSuppressElapsed={} lastWallMovementTick={} lastWallMovementElapsed={} womBackflipLock={} womBackflipLocal={} womBackflipData={} womBackflipStartSuppress={} phantomPrimeAvailable={} phantomUsed={} jumpConsumedHeld={} jumpConsumedActive={} jumpConsumedBy={} jumpConsumedReason={} jumpPressId={} jumpPressStart={} jumpPressElapsed={} jumpConsumedTick={} jumpConsumedElapsed={} protectPhantom={} womBackflipStart={} womBackflipElapsed={} womState={} onGround={} inWater={} jumpDown={} pos=({}, {}, {}) delta=({}, {}, {})",
				phase,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(delay),
				Boolean.valueOf(canceled),
				Boolean.valueOf(gliderState.gliderActive()),
				Boolean.valueOf(windowActive),
				startTick,
				Integer.valueOf(elapsed),
				Boolean.valueOf(phantomAnimation),
				gliderState.animationName(),
				Boolean.valueOf(PENDING_GLIDER_OPENING_SOUNDS.containsKey(player)),
				Boolean.valueOf(PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(player)),
				Boolean.valueOf(pendingWomToggleTick != null),
				Integer.valueOf(pendingWomToggleElapsed),
				Boolean.valueOf(pendingPreinput != null),
				pendingPreinput == null ? "none" : pendingPreinput.source().name(),
				Integer.valueOf(pendingPreinputElapsed),
				Integer.valueOf(pendingPreinput == null ? -1 : pendingPreinput.minDelayTicks()),
				Integer.valueOf(pendingPreinput == null ? -1 : pendingPreinput.maxAgeTicks()),
				Boolean.valueOf(GLIDER_REPLAY_REQUESTS.containsKey(player)),
				Boolean.valueOf(parcoolWallJumpLock),
				Boolean.valueOf(parcoolWallJumpProtect),
				Boolean.valueOf(parcoolWallJumpAnimation),
				parcoolWallJumpLockStart,
				Integer.valueOf(parcoolWallJumpLockElapsed),
				Boolean.valueOf(parcoolHandoffSuppress),
				parcoolHandoffSuppressStart,
				Integer.valueOf(parcoolHandoffSuppressElapsed),
				Boolean.valueOf(parcoolWallRunHandoffBlock),
				parcoolWallRunHandoffStart,
				Integer.valueOf(parcoolWallRunHandoffElapsed),
				Boolean.valueOf(wallRunParCoolSuppress),
				wallRunParCoolSuppressStart,
				Integer.valueOf(wallRunParCoolSuppressElapsed),
				lastWallMovementTick,
				Integer.valueOf(lastWallMovementElapsed),
				Boolean.valueOf(womBackflipLocalLock || womBackflipData),
				Boolean.valueOf(womBackflipLocalLock),
				Boolean.valueOf(womBackflipData),
				Boolean.valueOf(womBackflipStartSuppress),
				Boolean.valueOf(phantomPrimeAvailable),
				Boolean.valueOf(phantomUsed),
				Boolean.valueOf(jumpInput.consumedHeld()),
				Boolean.valueOf(jumpInput.consumedActive()),
				jumpInput.consumer().name(),
				jumpInput.reason(),
				Integer.valueOf(jumpInput.currentPressId()),
				Integer.valueOf(jumpInput.currentPressStartTick()),
				Integer.valueOf(jumpInput.currentPressElapsed()),
				Integer.valueOf(jumpInput.consumedTick()),
				Integer.valueOf(jumpInput.consumedElapsed()),
				Boolean.valueOf(protectPhantom),
				womBackflipLockStart,
				Integer.valueOf(womBackflipLockElapsed),
				gliderState.womState(),
				Boolean.valueOf(gliderState.onGround()),
				Boolean.valueOf(gliderState.inWater()),
				Boolean.valueOf(gliderState.jumpDown()),
				Double.valueOf(player.getX()),
				Double.valueOf(player.getY()),
				Double.valueOf(player.getZ()),
				Double.valueOf(player.getDeltaMovement().x()),
				Double.valueOf(player.getDeltaMovement().y()),
				Double.valueOf(player.getDeltaMovement().z())
		);
	}

	private static void tickParCoolWallJumpPhantomLock(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		Integer localStart = PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.get(player);
		if (localStart == null) {
			return;
		}

		if (Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player))) {
			PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_complete", false, false, false);
			return;
		}

		int elapsed = player.tickCount - localStart.intValue();
		boolean invalid = elapsed < 0
				|| elapsed > PARCOOL_WALL_JUMP_PHANTOM_LOCK_MAX_TICKS
				|| player.onGround()
				|| player.isInWater()
				|| !PHANTOM_ASCENT_TICKS.containsKey(player) && !isCurrentParCoolWallJumpAnimation(player);
		if (invalid) {
			PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_clear", false, false, false);
			return;
		}

		if (GliderCompat.isGlidingWithActiveGlider(player)) {
			forceStopGliderForWallRun(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_glider_forced_off", true, true, false);
		}
	}

	private static boolean isParCoolWallJumpPhantomLockActive(Player player) {
		Integer localStart = PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		return elapsed >= 0 && elapsed <= PARCOOL_WALL_JUMP_PHANTOM_LOCK_MAX_TICKS;
	}

	private static boolean isParCoolWallJumpHandoffSuppressActive(Player player) {
		Integer localStart = PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		return elapsed >= 0 && elapsed <= PARCOOL_WALL_JUMP_HANDOFF_TOGGLE_SUPPRESS_TICKS;
	}

	private static boolean isWallRunToParCoolWallJumpGliderSuppressActive(Player player) {
		Integer localStart = WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		if (elapsed < 0 || elapsed > WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_MAX_TICKS) {
			return false;
		}

		return isCurrentParCoolWallJumpAnimation(player) || elapsed <= 2;
	}

	private static void tickWallRunToParCoolWallJumpGliderSuppress(Player player) {
		if (player == null || !player.isLocalPlayer() || !WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.containsKey(player)) {
			return;
		}

		if (isWallRunToParCoolWallJumpGliderSuppressActive(player)) {
			if (GliderCompat.isGlidingWithActiveGlider(player)) {
				shouldHardSuppressGliderTick(player);
			}
			return;
		}

		WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.remove(player);
		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
		GLIDER_REPLAY_REQUESTS.remove(player);
		PENDING_GLIDER_OPENING_SOUNDS.remove(player);
		logGliderOpeningDelayDiagnostic(player, "wallrun_parcool_glider_lock_clear", false, false, false);
	}

	private static void dropWallRunToParCoolWallJumpGliderRequests(Player player) {
		if (player == null) {
			return;
		}
		PENDING_GLIDER_PREINPUTS.remove(player);
		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
		GLIDER_REPLAY_REQUESTS.remove(player);
		PENDING_GLIDER_OPENING_SOUNDS.remove(player);
	}

	private static boolean isParCoolWallRunHandoffGliderBlockActive(Player player) {
		Integer localStart = PARCOOL_WALL_RUN_HANDOFF_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		if (elapsed < 0 || elapsed > PARCOOL_WALL_RUN_HANDOFF_GLIDER_BLOCK_MAX_TICKS) {
			return false;
		}

		return isCurrentParCoolWallJumpAnimation(player);
	}

	private static void tickParCoolWallJumpHandoffSuppress(Player player) {
		if (player == null || !player.isLocalPlayer() || !PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.containsKey(player)) {
			return;
		}
		if (!isParCoolWallJumpHandoffSuppressActive(player)) {
			PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_handoff_input_suppress_clear", false, false, false);
		}
	}

	private static void tickParCoolWallRunHandoff(Player player) {
		if (player == null || !player.isLocalPlayer() || !PARCOOL_WALL_RUN_HANDOFF_TICKS.containsKey(player)) {
			return;
		}
		if (GliderCompat.isGlidingWithActiveGlider(player)) {
			if (cacheParCoolWallJumpGliderPreinputFromCurrentInput(player, "parcool_wallrun_handoff_forced_off")) {
				logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_forced_off_cache", true, true, false);
			} else {
				logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_forced_off_drop", true, true, false);
			}
			forceStopGliderForWallRun(player);
		}
		if (!isParCoolWallRunHandoffGliderBlockActive(player)) {
			PARCOOL_WALL_RUN_HANDOFF_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_clear", false, false, false);
		}
	}

	private static void tickParCoolWallJumpGliderInput(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		boolean down = isPhysicalJumpKeyDown();
		if (isPhantomAscentConsumedJumpHeld(player)) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			if (!down) {
				PARCOOL_WALL_JUMP_INPUT_DOWN.remove(player);
			} else {
				PARCOOL_WALL_JUMP_INPUT_DOWN.put(player, Boolean.TRUE);
			}
			return;
		}
		if (GliderCompat.isGlidingWithActiveGlider(player)) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			if (!down) {
				PARCOOL_WALL_JUMP_INPUT_DOWN.remove(player);
			} else {
				PARCOOL_WALL_JUMP_INPUT_DOWN.put(player, Boolean.TRUE);
			}
			return;
		}

		boolean wallJumpAnimation = isCurrentParCoolWallJumpAnimation(player);
		if (wallJumpAnimation && !player.onGround() && !player.isInWater()) {
			PARCOOL_WALL_JUMP_STARTED_TICKS.putIfAbsent(player, Integer.valueOf(player.tickCount));
			Boolean previousDown = PARCOOL_WALL_JUMP_INPUT_DOWN.get(player);
			if (previousDown == null) {
				previousDown = Boolean.valueOf(down);
			}
			if (down && !previousDown.booleanValue() && cacheParCoolWallJumpGliderPreinputFromCurrentInput(player, "parcool_wall_jump_extra_press")) {
				logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_extra_press_cache", true, true, false);
			}
			PARCOOL_WALL_JUMP_INPUT_DOWN.put(player, Boolean.valueOf(down));
		} else if (!down) {
			PARCOOL_WALL_JUMP_INPUT_DOWN.remove(player);
		}

		tickPendingParCoolWallJumpGliderToggle(player);
	}

	private static boolean cacheParCoolWallJumpGliderPreinputFromToggle(Player player, String reason) {
		if (!isAdditionalParCoolWallJumpGliderPress(player)) {
			return false;
		}
		return cacheParCoolWallJumpGliderPreinput(player, reason);
	}

	private static boolean cacheParCoolWallJumpGliderPreinputFromCurrentInput(Player player, String reason) {
		if (!isPhysicalJumpKeyDown()) {
			return false;
		}
		return cacheParCoolWallJumpGliderPreinput(player, reason);
	}

	private static boolean cacheParCoolWallJumpGliderPreinput(Player player, String reason) {
		if (player == null
				|| !player.isLocalPlayer()
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)
				|| isWallRunToParCoolWallJumpGliderSuppressActive(player)
				|| PHANTOM_ASCENT_TICKS.containsKey(player)
				|| isCurrentWallJumpPhantomAscentPrime(player)
				|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
				|| isWomBackflipPhantomLockActive(player)
				|| PENDING_GLIDER_PREINPUTS.containsKey(player)) {
			return false;
		}

		PARCOOL_WALL_JUMP_INPUT_DOWN.put(player, Boolean.TRUE);
		return cacheUnifiedGliderPreinput(player, GliderPreinputSource.PARCOOL_WALL_JUMP, "parcool_" + reason);
	}

	private static boolean isAdditionalParCoolWallJumpGliderPress(Player player) {
		if (player == null || !player.isLocalPlayer() || !isPhysicalJumpKeyDown()) {
			return false;
		}
		return !Boolean.TRUE.equals(PARCOOL_WALL_JUMP_INPUT_DOWN.get(player));
	}

	private static void tickPendingParCoolWallJumpGliderToggle(Player player) {
		Integer queuedTick = PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.get(player);
		if (queuedTick == null) {
			return;
		}

		if (isPhantomAscentConsumedJumpPendingDrop(player, queuedTick.intValue())) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_drop_consumed_jump", true, false, false);
			return;
		}

		if (GliderCompat.isGlidingWithActiveGlider(player)) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_drop_active_glider", false, false, false);
			return;
		}

		int elapsed = player.tickCount - queuedTick.intValue();
		if (elapsed < 0
				|| elapsed > PARCOOL_WALL_JUMP_PENDING_GLIDER_TOGGLE_MAX_TICKS
				|| player.onGround()
				|| player.isInWater()) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_drop", true, false, false);
			return;
		}

		if (PHANTOM_ASCENT_TICKS.containsKey(player)
				|| isCurrentWallJumpPhantomAscentPrime(player)
				|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
				|| isWallRunToParCoolWallJumpGliderSuppressActive(player)) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_drop_blocked", true, false, false);
			return;
		}

		if (isCurrentParCoolWallJumpAnimation(player)
				|| isParCoolWallRunHandoffGliderBlockActive(player)
				|| isWallMovementAnimationActiveForGlider(player)
				|| GliderCompat.isGlidingWithActiveGlider(player)) {
			return;
		}

		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_replay", false, false, false);
		sendGliderToggleMessage(player);
	}

	private static boolean shouldProtectParCoolWallJumpForPhantom(Player player) {
		if (player == null || !player.isLocalPlayer() || !isParCoolWallJumpPhantomLockActive(player)) {
			return false;
		}
		if (Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player))) {
			return false;
		}

		return PHANTOM_ASCENT_TICKS.containsKey(player)
				|| isCurrentParCoolWallJumpAnimation(player);
	}

	private static void dropGliderToggleReplayForParCoolWallJump(Player player, String phase) {
		boolean unifiedPendingRemoved = player != null && PENDING_GLIDER_PREINPUTS.remove(player) != null;
		boolean arbitrationRemoved = player != null && PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player) != null;
		boolean pendingRemoved = player != null && PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player) != null;
		boolean parcoolPendingRemoved = player != null && PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player) != null;
		boolean replayRemoved = player != null && GLIDER_REPLAY_REQUESTS.remove(player) != null;
		if (unifiedPendingRemoved || arbitrationRemoved || pendingRemoved || parcoolPendingRemoved || replayRemoved) {
			logGliderOpeningDelayDiagnostic(player, phase + "_drop", true, false, false);
		}
	}

	private static void markWomBackflipPhantomLock(Player player, String reason) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		if (isWomBackflipLocalLockActive(player)) {
			return;
		}

		WOM_BACKFLIP_PHANTOM_LOCK_TICKS.put(player, Integer.valueOf(player.tickCount));
		logWomBackflipGliderGuard(player, "wom_backflip_lock_mark", reason, true, false);
	}

	private static void tickWomBackflipPhantomLock(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		boolean dataActive = isWomSpiderBackflipDataActive(player);
		Integer localStart = WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
		if (localStart == null && dataActive) {
			WOM_BACKFLIP_PHANTOM_LOCK_TICKS.put(player, Integer.valueOf(player.tickCount));
			logWomBackflipGliderGuard(player, "wom_backflip_lock_adopt_data", "wom_data_active", true, false);
			return;
		}

		if (localStart == null) {
			return;
		}

		int elapsed = player.tickCount - localStart.intValue();
		boolean localActive = elapsed >= 0
				&& elapsed <= WOM_BACKFLIP_PHANTOM_LOCK_DURATION_TICKS
				&& !player.onGround()
				&& !player.isInWater();
		if (!localActive && !dataActive) {
			WOM_BACKFLIP_PHANTOM_LOCK_TICKS.remove(player);
			logWomBackflipGliderGuard(player, "wom_backflip_lock_clear", "expired_or_grounded", false, false);
			return;
		}

		logWomBackflipGliderGuard(player, "wom_backflip_lock_tick", dataActive ? "local_or_wom_data" : "local", true, false);
	}

	public static boolean isWomBackflipPhantomLockActive(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return false;
		}

		return isWomBackflipLocalLockActive(player) || isWomSpiderBackflipDataActive(player);
	}

	private static boolean shouldProtectWomBackflipForPhantom(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return false;
		}

		return PHANTOM_ASCENT_TICKS.containsKey(player)
				|| isGliderOpeningDelayWindowActive(player)
				|| isCurrentPhantomAscentAnimation(player)
				|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
	}

	private static void ensureWomBackflipPhantomLockFromData(Player player, String reason) {
		if (player == null || !player.isLocalPlayer() || WOM_BACKFLIP_PHANTOM_LOCK_TICKS.containsKey(player)) {
			return;
		}
		if (!isWomSpiderBackflipDataActive(player)) {
			return;
		}

		WOM_BACKFLIP_PHANTOM_LOCK_TICKS.put(player, Integer.valueOf(player.tickCount));
		logWomBackflipGliderGuard(player, "wom_backflip_lock_adopt_guard", reason, true, false);
	}

	private static boolean isWomBackflipStartSuppressActive(Player player) {
		Integer localStart = WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		return elapsed >= 0 && elapsed < WOM_BACKFLIP_GLIDER_START_SUPPRESS_TICKS;
	}

	private static boolean isWomBackflipLocalLockActive(Player player) {
		Integer localStart = WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
		if (localStart == null || player == null || player.onGround() || player.isInWater()) {
			return false;
		}

		int elapsed = player.tickCount - localStart.intValue();
		return elapsed >= 0 && elapsed <= WOM_BACKFLIP_PHANTOM_LOCK_DURATION_TICKS;
	}

	private static boolean isWomSpiderBackflipDataActive(Player player) {
		if (player == null) {
			return false;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		if (!snapshot.womBackflipAnimation()) {
			return false;
		}

		PlayerPatch<?> playerPatch = snapshot.playerPatch();
		return isWomSpiderBackflipDataActive(playerPatch);
	}

	private static boolean isWomSpiderBackflipDataActive(PlayerPatch<?> playerPatch) {
		try {
			return EPMConfig.spiderWallJumpPrimesPhantomAscent()
					&& WomCompatBridge.instance().isSpiderWallBackflipActive(playerPatch);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean canStartPhantomAscentPrime(Player player, PhantomAscentPrimeSource source) {
		if (player == null || !player.isLocalPlayer() || source == null || !source.enabled()) {
			return false;
		}
		if (Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player)) || isHoldingPhantomAscentBlockedWeapon(player) || isParCoolHanging(player)) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		SkillContainer phantomAscent = findPhantomAscent(playerPatch);
		return phantomAscent != null && phantomAscent.getDataManager() != null;
	}

	private static void cacheWomBackflipGliderPreinput(Player player, String reason) {
		if (cacheUnifiedGliderPreinput(player, GliderPreinputSource.WOM_BACKFLIP, reason)) {
			logWomBackflipGliderGuard(player, "wom_backflip_glider_preinput_cache", reason, true, true);
		}
	}

	private static void dropGliderToggleReplayForWomBackflip(Player player, String phase) {
		boolean unifiedPendingRemoved = player != null && PENDING_GLIDER_PREINPUTS.remove(player) != null;
		boolean arbitrationRemoved = player != null && PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player) != null;
		boolean pendingRemoved = player != null && PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player) != null;
		boolean womPendingRemoved = player != null && PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player) != null;
		boolean replayRemoved = player != null && GLIDER_REPLAY_REQUESTS.remove(player) != null;
		if (unifiedPendingRemoved || arbitrationRemoved || pendingRemoved || womPendingRemoved || replayRemoved) {
			logWomBackflipGliderGuard(player, phase + "_drop", "unifiedPending=" + unifiedPendingRemoved + ", arbitration=" + arbitrationRemoved + ", pending=" + pendingRemoved + ", womPending=" + womPendingRemoved + ", replay=" + replayRemoved, true, false);
		}
	}

	private static void logWomBackflipGliderGuard(Player player, String phase, String reason, boolean canceled, boolean cacheAllowed) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> animation = playerPatch == null ? null : currentBaseAnimation(playerPatch);
		ResourceLocation animationId = safeRegistryName(animation);
		Integer localStart = WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
		int localElapsed = localStart == null ? -1 : player.tickCount - localStart.intValue();
		boolean localActive = isWomBackflipLocalLockActive(player);
		boolean dataActive = isWomSpiderBackflipDataActive(playerPatch);
		boolean startSuppress = isWomBackflipStartSuppressActive(player);
		Integer pendingWomToggleTick = PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.get(player);
		int pendingWomToggleElapsed = pendingWomToggleTick == null ? -1 : player.tickCount - pendingWomToggleTick.intValue();
		PendingGliderPreinput pendingPreinput = PENDING_GLIDER_PREINPUTS.get(player);
		int pendingPreinputElapsed = pendingPreinput == null ? -1 : player.tickCount - pendingPreinput.queuedTick();
		Integer phantomStart = PHANTOM_ASCENT_STARTED_TICKS.get(player);
		int phantomElapsed = phantomStart == null ? -1 : player.tickCount - phantomStart.intValue();
		boolean phantomWindow = phantomStart != null && phantomElapsed >= 0 && phantomElapsed < PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS;
		boolean phantomAnimation = playerPatch != null
				&& WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedPhantomAscentForward(), WomAnimationRefs.bipedPhantomAscentBackward());
		boolean phantomPrimeAvailable = canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
		boolean phantomUsed = Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player));
		boolean protectPhantom = shouldProtectWomBackflipForPhantom(player);

		EPM.LOGGER.info(
				"[EPM/WomBackflipGliderGuard] phase={} reason={} tick={} canceled={} cacheAllowed={} active={} localActive={} dataActive={} startSuppress={} startSuppressTicks={} protectPhantom={} phantomPrimeAvailable={} phantomUsed={} localStart={} localElapsed={} pendingToggle={} pendingWomToggle={} pendingWomElapsed={} pendingPreinput={} pendingPreinputSource={} pendingPreinputElapsed={} replayRequest={} phantomQueued={} phantomWindow={} phantomStart={} phantomElapsed={} phantomAnim={} gliderActive={} wallMovementAnim={} onGround={} inWater={} jumpDown={} animation={} womState={} pos=({}, {}, {}) delta=({}, {}, {})",
				phase,
				reason,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(canceled),
				Boolean.valueOf(cacheAllowed),
				Boolean.valueOf(localActive || dataActive),
				Boolean.valueOf(localActive),
				Boolean.valueOf(dataActive),
				Boolean.valueOf(startSuppress),
				Integer.valueOf(WOM_BACKFLIP_GLIDER_START_SUPPRESS_TICKS),
				Boolean.valueOf(protectPhantom),
				Boolean.valueOf(phantomPrimeAvailable),
				Boolean.valueOf(phantomUsed),
				localStart,
				Integer.valueOf(localElapsed),
				Boolean.valueOf(PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(player)),
				Boolean.valueOf(pendingWomToggleTick != null),
				Integer.valueOf(pendingWomToggleElapsed),
				Boolean.valueOf(pendingPreinput != null),
				pendingPreinput == null ? "none" : pendingPreinput.source().name(),
				Integer.valueOf(pendingPreinputElapsed),
				Boolean.valueOf(GLIDER_REPLAY_REQUESTS.containsKey(player)),
				Boolean.valueOf(PHANTOM_ASCENT_TICKS.containsKey(player)),
				Boolean.valueOf(phantomWindow),
				phantomStart,
				Integer.valueOf(phantomElapsed),
				Boolean.valueOf(phantomAnimation),
				Boolean.valueOf(GliderCompat.isGlidingWithActiveGlider(player)),
				Boolean.valueOf(isWallMovementAnimationActiveForGlider(player)),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(player.isInWater()),
				Boolean.valueOf(isPhysicalJumpKeyDown()),
				animationId == null ? String.valueOf(animation) : animationId.toString(),
				WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch),
				Double.valueOf(player.getX()),
				Double.valueOf(player.getY()),
				Double.valueOf(player.getZ()),
				Double.valueOf(player.getDeltaMovement().x()),
				Double.valueOf(player.getDeltaMovement().y()),
				Double.valueOf(player.getDeltaMovement().z())
		);
	}

	private static boolean computeGliderOpeningDelay(Player player) {
		return shouldDelayGliderOpeningForPriority(player);
	}

	private static boolean shouldDelayGliderOpeningForPriority(Player player) {
		return isGliderOpeningDelayWindowActive(player)
				|| shouldProtectParCoolWallJumpForPhantom(player)
				|| isParCoolWallJumpHandoffSuppressActive(player)
				|| isWallRunToParCoolWallJumpGliderSuppressActive(player)
				|| isParCoolWallRunHandoffGliderBlockActive(player);
	}

	private static boolean isGliderOpeningDelayWindowActive(Player player) {
		Integer startTick = PHANTOM_ASCENT_STARTED_TICKS.get(player);
		if (startTick == null) {
			return false;
		}

		int elapsed = player.tickCount - startTick.intValue();
		return elapsed >= 0 && elapsed < PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS;
	}

	private static void clearGliderOpeningDelayState(Player player) {
		GLIDER_OPENING_DELAY_STATES.remove(player);
	}

	private static void stopGliderForWallMovement(Player player) {
		if (!isWallMovementAnimationActiveForGlider(player)) {
			WALL_MOVEMENT_GLIDER_ACTIVE.remove(player);
			return;
		}

		boolean enteredWallMovement = !Boolean.TRUE.equals(WALL_MOVEMENT_GLIDER_ACTIVE.put(player, Boolean.TRUE));
		if (enteredWallMovement || GliderCompat.isGlidingWithActiveGlider(player)) {
			forceStopGliderForWallRun(player);
		}
	}

	private static void tickPendingGliderInputArbitration(Player player) {
		Integer queuedTick = PENDING_GLIDER_INPUT_ARBITRATION_TICKS.get(player);
		if (queuedTick == null) {
			return;
		}

		if (isPhantomAscentConsumedJumpPendingDrop(player, queuedTick.intValue())) {
			PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_phantom_consumed_jump", true, false, false);
			return;
		}

		int elapsed = player.tickCount - queuedTick.intValue();
		if (elapsed < GLIDER_INPUT_ARBITRATION_DELAY_TICKS) {
			return;
		}
		if (elapsed < 0
				|| elapsed > GLIDER_INPUT_ARBITRATION_MAX_TICKS
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)) {
			PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_invalid", true, false, false);
			return;
		}

		GliderInputArbitrationDecision decision = resolveGliderInputArbitration(player, queuedTick.intValue());
		if (decision.action() == GliderInputArbitrationAction.DROP) {
			PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_" + decision.reason(), true, false, false);
			return;
		}
		if (decision.action() == GliderInputArbitrationAction.CACHE) {
			PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
			cacheUnifiedGliderPreinput(player, decision.source(), "arbiter_" + decision.reason());
			return;
		}

		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_replay", false, false, false);
		sendGliderToggleMessage(player);
	}

	private static GliderInputArbitrationDecision resolveGliderInputArbitration(Player player, int queuedTick) {
		ensureGliderOpeningDelayWindowFromCurrentAnimation(player);
		GliderPreinputSource source = detectGliderPreinputSource(player);
		if (source == null) {
			return GliderInputArbitrationDecision.none();
		}

		if (source == GliderPreinputSource.WALL_MOVEMENT) {
			return GliderInputArbitrationDecision.drop(source, "wall_movement");
		}

		if (shouldDropGliderPreinputForPriority(player, source)) {
			return GliderInputArbitrationDecision.drop(source, "priority_action");
		}

		Integer sourceStart = gliderPreinputSourceStartTick(player, source);
		if (sourceStart == null) {
			return GliderInputArbitrationDecision.drop(source, "missing_source_start");
		}

		int sourceElapsedAtQueue = queuedTick - sourceStart.intValue();
		if (sourceElapsedAtQueue >= 0 && sourceElapsedAtQueue <= GLIDER_PREINPUT_SAME_PRESS_GRACE_TICKS) {
			return GliderInputArbitrationDecision.drop(source, "same_press_" + source.name().toLowerCase());
		}

		return GliderInputArbitrationDecision.cache(source, "extra_press_" + source.name().toLowerCase());
	}

	private static GliderPreinputSource detectGliderPreinputSource(Player player) {
		if (isWallRunToParCoolWallJumpGliderSuppressActive(player) || isParCoolWallRunHandoffGliderBlockActive(player)) {
			return GliderPreinputSource.WALLRUN_TO_PARCOOL_WALL_JUMP;
		}
		if (isCurrentParCoolWallJumpAnimation(player)
				|| shouldProtectParCoolWallJumpForPhantom(player)
				|| isParCoolWallJumpHandoffSuppressActive(player)) {
			return GliderPreinputSource.PARCOOL_WALL_JUMP;
		}
		if (isWomBackflipPhantomLockActive(player)) {
			return GliderPreinputSource.WOM_BACKFLIP;
		}
		if (PHANTOM_ASCENT_TICKS.containsKey(player)
				|| isCurrentWallJumpPhantomAscentPrime(player)
				|| isGliderOpeningDelayWindowActive(player)
				|| isCurrentPhantomAscentAnimation(player)) {
			return GliderPreinputSource.PHANTOM_ASCENT;
		}
		if (isWallMovementAnimationActiveForGlider(player)) {
			return GliderPreinputSource.WALL_MOVEMENT;
		}
		return null;
	}

	private static Integer gliderPreinputSourceStartTick(Player player, GliderPreinputSource source) {
		return switch (source) {
			case PHANTOM_ASCENT -> PHANTOM_ASCENT_STARTED_TICKS.get(player);
			case WOM_BACKFLIP -> WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
			case PARCOOL_WALL_JUMP -> PARCOOL_WALL_JUMP_STARTED_TICKS.get(player);
			case WALLRUN_TO_PARCOOL_WALL_JUMP -> {
				Integer wallRunStart = WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.get(player);
				Integer parcoolStart = PARCOOL_WALL_JUMP_STARTED_TICKS.get(player);
				yield wallRunStart != null ? wallRunStart : parcoolStart;
			}
			case WALL_MOVEMENT -> LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.get(player);
		};
	}

	private static boolean shouldDropGliderPreinputForPriority(Player player, GliderPreinputSource source) {
		return switch (source) {
			case PHANTOM_ASCENT -> false;
			case WOM_BACKFLIP -> shouldProtectWomBackflipForPhantom(player);
			case PARCOOL_WALL_JUMP, WALLRUN_TO_PARCOOL_WALL_JUMP -> PHANTOM_ASCENT_TICKS.containsKey(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP);
			case WALL_MOVEMENT -> true;
		};
	}

	private static boolean cacheUnifiedGliderPreinput(Player player, GliderPreinputSource source, String reason) {
		if (player == null
				|| !player.isLocalPlayer()
				|| source == null
				|| source == GliderPreinputSource.WALL_MOVEMENT
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)
				|| isPhantomAscentConsumedJumpHeld(player)
				|| shouldDropGliderPreinputForPriority(player, source)) {
			logGliderOpeningDelayDiagnostic(player, "glider_preinput_drop_cache_" + reason, true, false, false);
			return false;
		}

		PendingGliderPreinput existing = PENDING_GLIDER_PREINPUTS.get(player);
		if (existing != null) {
			logGliderOpeningDelayDiagnostic(player, "glider_preinput_keep_existing_" + existing.source().name().toLowerCase() + "_" + reason, true, true, false);
			return true;
		}

		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
		PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
		PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		GLIDER_REPLAY_REQUESTS.remove(player);
		PENDING_GLIDER_PREINPUTS.put(player, new PendingGliderPreinput(player.tickCount, source, gliderPreinputMinDelayTicks(source), gliderPreinputMaxAgeTicks(source)));
		logGliderOpeningDelayDiagnostic(player, "glider_preinput_cache_" + source.name().toLowerCase() + "_" + reason, true, true, false);
		return true;
	}

	private static int gliderPreinputMinDelayTicks(GliderPreinputSource source) {
		return switch (source) {
			case PHANTOM_ASCENT, WOM_BACKFLIP -> 0;
			case PARCOOL_WALL_JUMP, WALLRUN_TO_PARCOOL_WALL_JUMP -> 0;
			case WALL_MOVEMENT -> PENDING_GLIDER_TOGGLE_MAX_TICKS;
		};
	}

	private static int gliderPreinputMaxAgeTicks(GliderPreinputSource source) {
		return switch (source) {
			case WOM_BACKFLIP -> WOM_BACKFLIP_PENDING_GLIDER_TOGGLE_MAX_TICKS;
			case PARCOOL_WALL_JUMP, WALLRUN_TO_PARCOOL_WALL_JUMP -> PARCOOL_WALL_JUMP_PENDING_GLIDER_TOGGLE_MAX_TICKS;
			case PHANTOM_ASCENT, WALL_MOVEMENT -> PENDING_GLIDER_TOGGLE_MAX_TICKS;
		};
	}

	private static void tickPendingUnifiedGliderPreinput(Player player) {
		PendingGliderPreinput pending = PENDING_GLIDER_PREINPUTS.get(player);
		if (pending == null) {
			return;
		}

		if (isPhantomAscentConsumedJumpPendingDrop(player, pending.queuedTick())) {
			PENDING_GLIDER_PREINPUTS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "glider_preinput_drop_consumed_jump_" + pending.source().name().toLowerCase(), true, false, false);
			return;
		}

		int elapsed = player.tickCount - pending.queuedTick();
		if (elapsed < 0
				|| elapsed > pending.maxAgeTicks()
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)) {
			PENDING_GLIDER_PREINPUTS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "glider_preinput_drop_invalid_" + pending.source().name().toLowerCase(), true, false, false);
			return;
		}

		if (elapsed < pending.minDelayTicks() || shouldHoldUnifiedGliderPreinput(player, pending.source())) {
			return;
		}

		PENDING_GLIDER_PREINPUTS.remove(player);
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "glider_preinput_replay_" + pending.source().name().toLowerCase(), false, false, false);
		sendGliderToggleMessage(player);
	}

	private static boolean shouldHoldUnifiedGliderPreinput(Player player, GliderPreinputSource source) {
		ensureGliderOpeningDelayWindowFromCurrentAnimation(player);
		return switch (source) {
			case PHANTOM_ASCENT -> PHANTOM_ASCENT_TICKS.containsKey(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| isGliderOpeningDelayWindowActive(player)
					|| isWomBackflipStartSuppressActive(player)
					|| isWallMovementAnimationActiveForGlider(player);
			case WOM_BACKFLIP -> isWomBackflipStartSuppressActive(player)
					|| shouldProtectWomBackflipForPhantom(player)
					|| isWallMovementAnimationActiveForGlider(player);
			case PARCOOL_WALL_JUMP -> PHANTOM_ASCENT_TICKS.containsKey(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
					|| isCurrentParCoolWallJumpAnimation(player)
					|| isParCoolWallJumpHandoffSuppressActive(player)
					|| isParCoolWallRunHandoffGliderBlockActive(player)
					|| isWallRunToParCoolWallJumpGliderSuppressActive(player)
					|| isWallMovementAnimationActiveForGlider(player);
			case WALLRUN_TO_PARCOOL_WALL_JUMP -> PHANTOM_ASCENT_TICKS.containsKey(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
					|| isWallRunToParCoolWallJumpGliderSuppressActive(player)
					|| isParCoolWallRunHandoffGliderBlockActive(player)
					|| isCurrentParCoolWallJumpAnimation(player)
					|| isWallMovementAnimationActiveForGlider(player);
			case WALL_MOVEMENT -> true;
		};
	}

	private static void tickPendingGliderToggleAfterPhantom(Player player) {
		Integer queuedTick = PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.get(player);
		if (queuedTick == null) {
			return;
		}

		if (isPhantomAscentConsumedJumpPendingDrop(player, queuedTick.intValue())) {
			PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
			logGliderOpeningDelayDiagnostic(player, "toggle_after_phantom_drop_consumed_jump", true, false, false);
			return;
		}

		if (isWomBackflipPhantomLockActive(player)) {
			ensureWomBackflipPhantomLockFromData(player, "pending_tick");
		}
		if (isWomBackflipStartSuppressActive(player)) {
			dropGliderToggleReplayForWomBackflip(player, "pending_drop_wom_backflip_start_suppress");
			logWomBackflipGliderGuard(player, "pending_drop_wom_backflip_start_suppress", "pending_tick", true, false);
			return;
		}

		if (isWomBackflipPhantomLockActive(player) && shouldProtectWomBackflipForPhantom(player)) {
			dropGliderToggleReplayForWomBackflip(player, "pending_drop_wom_backflip_lock");
			logWomBackflipGliderGuard(player, "pending_drop_wom_backflip_lock", "pending_tick", true, false);
			return;
		}
		if (shouldProtectParCoolWallJumpForPhantom(player)) {
			PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
			logGliderOpeningDelayDiagnostic(player, "pending_drop_parcool_wall_jump_phantom_lock", true, false, false);
			return;
		}

		int elapsed = player.tickCount - queuedTick.intValue();
		if (elapsed < 0 || elapsed > PENDING_GLIDER_TOGGLE_MAX_TICKS || player.onGround() || player.isInWater()) {
			PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
			return;
		}

		ensureGliderOpeningDelayWindowFromCurrentAnimation(player);
		Integer startTick = PHANTOM_ASCENT_STARTED_TICKS.get(player);
		if (startTick == null || player.tickCount - startTick.intValue() < PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS
				|| PHANTOM_ASCENT_TICKS.containsKey(player)
				|| isCurrentWallJumpPhantomAscentPrime(player)
				|| isWomBackflipStartSuppressActive(player)
				|| isWomBackflipPhantomLockActive(player) && shouldProtectWomBackflipForPhantom(player)
				|| isWallMovementAnimationActiveForGlider(player)
				|| GliderCompat.isGlidingWithActiveGlider(player)) {
			return;
		}

		PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "toggle_replay_after_phantom", false, false, false);
		sendGliderToggleMessage(player);
	}

	private static void tickPendingWomBackflipGliderToggle(Player player) {
		Integer queuedTick = PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.get(player);
		if (queuedTick == null) {
			return;
		}

		if (isPhantomAscentConsumedJumpPendingDrop(player, queuedTick.intValue())) {
			PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
			logWomBackflipGliderGuard(player, "wom_backflip_glider_preinput_drop", "phantom_consumed_jump", true, false);
			return;
		}

		int elapsed = player.tickCount - queuedTick.intValue();
		if (elapsed < 0 || elapsed > WOM_BACKFLIP_PENDING_GLIDER_TOGGLE_MAX_TICKS
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)
				|| isWallMovementAnimationActiveForGlider(player)) {
			PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
			logWomBackflipGliderGuard(player, "wom_backflip_glider_preinput_drop", "invalid_or_expired", true, false);
			return;
		}

		if (isWomBackflipPhantomLockActive(player)) {
			ensureWomBackflipPhantomLockFromData(player, "wom_preinput_tick");
		}
		if (isWomBackflipStartSuppressActive(player)) {
			return;
		}
		if (shouldProtectWomBackflipForPhantom(player)) {
			PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
			logWomBackflipGliderGuard(player, "wom_backflip_glider_preinput_drop", "phantom_protect_restored", true, false);
			return;
		}

		PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logWomBackflipGliderGuard(player, "wom_backflip_glider_preinput_replay", "start_suppress_elapsed", false, true);
		sendGliderToggleMessage(player);
	}

	private static boolean disableLocalGliderItem(Player player) {
		try {
			ItemStack glider = CuriosTrinketsUtil.getInstance().getFirstFoundGlider(player);
			if (!glider.isEmpty() && GliderItem.isGlidingEnabled(glider)) {
				GliderItem.setGlide(glider, false);
				GliderFrameState.invalidate(player);
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return false;
	}

	private static void sendGliderToggleMessage(Player player) {
		try {
			new MessageToggleGlide().send();
		} catch (RuntimeException | LinkageError ignored) {
		} finally {
			GliderFrameState.invalidate(player);
		}
	}

	private static void stopGliderAnimations(Player player) {
		try {
			GliderData.get(player).ifPresent(data -> {
				data.glideAnimation.stop();
				data.fallingAnimation.stop();
				data.gliderOpeningAnimation.stop();
			});
		} catch (RuntimeException | LinkageError ignored) {
		}

		try {
			if (player instanceof AnimatedPlayer animatedPlayer) {
				var modifierLayer = animatedPlayer.gliders_getModifierLayer();
				if (modifierLayer != null) {
					modifierLayer.setAnimation(null);
				}
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
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
		WomSpiderWallHooks.tickMovement(event.player);
		stopGliderForWallMovement(event.player);
		if (shouldStopFastRunForGlider(event.player)) {
			suppressFastRunAnimationForGlider(event.player);
		}
		tickParCoolWallJumpGliderInput(event.player);
		NaturalSprinterFastRunHandler.tickManualFastRunStepKey(event.player);
		restoreClingMoveClimbUpVelocity(event.player, true);
		tickEpicParCoolClimbUpAirControl(event.player);
		WomSpiderWallHooks.tickYawLock(event.player);
		logExhaustionPose(event.player);
		VaultDebug.tickPostVaultTrace(event.player);

		cancelWallJumpForHeldTaczAttack(event.player);
		if (TACZ_SHOOT_ACTIVE.containsKey(event.player) || TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(event.player)) {
			tickTaczShootStopFastRunDashSuppression(event.player);
		}
		if (TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(event.player)) {
			tickTaczReloadFastRunDashSuppression(event.player);
		}

		if (event.player.onGround()) {
			JumpInputConsumptionState.clear(event.player);
		} else {
			JumpInputConsumptionState.tick(event.player, isPhysicalJumpKeyDown());
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
		if (WOM_BACKFLIP_PHANTOM_LOCK_TICKS.containsKey(event.player) || isWomSpiderBackflipDataActive(event.player)) {
			tickWomBackflipPhantomLock(event.player);
		}
		if (PHANTOM_ASCENT_TICKS.containsKey(event.player)) {
			tickPhantomAscent(event.player);
		}
		if (PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(event.player)) {
			tickParCoolWallJumpPhantomLock(event.player);
		}
		if (PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.containsKey(event.player)) {
			tickParCoolWallJumpHandoffSuppress(event.player);
		}
		if (PARCOOL_WALL_RUN_HANDOFF_TICKS.containsKey(event.player)) {
			tickParCoolWallRunHandoff(event.player);
		}
		if (WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.containsKey(event.player)) {
			tickWallRunToParCoolWallJumpGliderSuppress(event.player);
		}
		if (PENDING_GLIDER_INPUT_ARBITRATION_TICKS.containsKey(event.player)) {
			tickPendingGliderInputArbitration(event.player);
		}
		if (PENDING_GLIDER_PREINPUTS.containsKey(event.player)) {
			tickPendingUnifiedGliderPreinput(event.player);
		}
		if (PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(event.player)) {
			tickPendingGliderToggleAfterPhantom(event.player);
		}
		if (PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.containsKey(event.player)) {
			tickPendingWomBackflipGliderToggle(event.player);
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
		NaturalSprinterStepFastRunState stepFastRunState = NATURAL_SPRINTER_STEP_FAST_RUN_STATES.get(event.player);
		if (stepFastRunState != null) {
			tickNaturalSprinterStepFastRun(event.player, stepFastRunState);
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
		DeferredNaturalSprinterDodgeStep deferredDodgeStep = NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.get(event.player);
		if (deferredDodgeStep != null) {
			tickDeferredDodgeSteps(event.player, deferredDodgeStep);
		}
		if (!PENDING_FAST_RUN_DASHES.isEmpty()) {
			playPendingFastRunDash(event.player);
		}
	}

	private static boolean hasClientTickWork(Player player) {
		return PHANTOM_ASCENT_TICKS.containsKey(player)
				|| PHANTOM_ASCENT_USED_AIRBORNE.containsKey(player)
				|| PHANTOM_ASCENT_STARTED_TICKS.containsKey(player)
				|| PENDING_GLIDER_INPUT_ARBITRATION_TICKS.containsKey(player)
				|| PENDING_GLIDER_PREINPUTS.containsKey(player)
				|| PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(player)
				|| PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.containsKey(player)
				|| PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.containsKey(player)
				|| PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(player)
				|| PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.containsKey(player)
				|| PARCOOL_WALL_RUN_HANDOFF_TICKS.containsKey(player)
				|| WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.containsKey(player)
				|| DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.containsKey(player)
				|| PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.containsKey(player)
				|| PENDING_FORCED_PHANTOM_ASCENT_TICKS.containsKey(player)
				|| PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT.containsKey(player)
				|| WOM_BACKFLIP_PHANTOM_LOCK_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_STEP_FAST_RUN_STATES.containsKey(player)
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
				|| NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.containsKey(player)
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
		if (shouldStopFastRunForGlider(player)) {
			if (playerPatch != null) {
				PENDING_FAST_RUN_DASHES.remove(playerPatch);
			}
			suppressFastRunAnimationForGlider(player);
			return;
		}

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

	public static void deferStepAnimForDodge(Player player, AssetAccessor<? extends StaticAnimation> animation) {
		deferStepForDodge(player, animation);
	}

	public static boolean shouldDelayNaturalSprinterStepForDodge(Player player) {
		if (player == null || !player.isLocalPlayer() || !isParCoolDodgeBlockingNaturalSprinterStep(player)) {
			return false;
		}

		return true;
	}

	public static boolean isParCoolDodgeBlockingNaturalSprinterStep(Player player) {
		return NaturalSprinterFastRunHandler.isParCoolDodgeDoing(player) || hasParCoolDodgeAnimator(player) || hasDodgeRollBaseAnimation(player);
	}

	public static void deferStepForDodge(Player player, AssetAccessor<? extends StaticAnimation> animation) {
		if (player == null || animation == null) {
			return;
		}

		DeferredNaturalSprinterDodgeStep state = NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.get(player);
		if (state == null) {
			NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.put(player, new DeferredNaturalSprinterDodgeStep(player.tickCount, animation));
		} else {
			state.clearTick = -1;
			state.deferredStep = animation;
		}
	}

	private static void tickDeferredDodgeSteps(Player player, DeferredNaturalSprinterDodgeStep state) {
		if (player == null || !player.isLocalPlayer() || !EPMConfig.naturalSprinterAnimations()) {
			clearDeferredDodgeStep(player);
			return;
		}

		if (state == null || state.deferredStep == null) {
			clearDeferredDodgeStep(player);
			return;
		}

		int elapsedTicks = player.tickCount - state.startTick;
		boolean dodgeBlockingStep = isParCoolDodgeBlockingNaturalSprinterStep(player);
		if (dodgeBlockingStep && elapsedTicks < NATURAL_SPRINTER_DODGE_STEP_MAX_DELAY_TICKS) {
			state.clearTick = -1;
			return;
		}
		if (dodgeBlockingStep) {
			clearDeferredDodgeStep(player);
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		boolean dodgeRollAnimation = isDodgeRollAnimation(currentAnimation);
		if (dodgeRollAnimation && elapsedTicks < NATURAL_SPRINTER_DODGE_STEP_MAX_DELAY_TICKS) {
			state.clearTick = -1;
			return;
		}
		if (dodgeRollAnimation) {
			clearDeferredDodgeStep(player);
			return;
		}

		if (state.clearTick < 0) {
			state.clearTick = player.tickCount;
			return;
		}

		int clearElapsedTicks = player.tickCount - state.clearTick;
		if (clearElapsedTicks < NATURAL_SPRINTER_DODGE_STEP_CLEAR_GRACE_TICKS
				&& elapsedTicks < NATURAL_SPRINTER_DODGE_STEP_MAX_DELAY_TICKS) {
			return;
		}

		NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.remove(player);
		AssetAccessor<? extends StaticAnimation> deferred = state.deferredStep;
		if (deferred == null) {
			return;
		}

		if (playerPatch == null || !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return;
		}
		if (!isParCoolFastRunDoing(player)) {
			requestNaturalSprinterStepFastRun(player, deferred);
			return;
		}
		if (!NaturalSprinterState.consumeStep(playerPatch)) {
			return;
		}

		NaturalSprinterFastRunHandler.advanceSprintStepPublic(playerPatch);
		queueNaturalSprinterFastRunDash(playerPatch, deferred);
	}

	private static void clearDeferredDodgeStep(Player player) {
		if (player == null) {
			return;
		}

		NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.remove(player);
	}

	private static boolean hasParCoolDodgeAnimator(Player player) {
		if (player == null) {
			return false;
		}

		try {
			Animation animation = Animation.get(player);
			if (!(animation instanceof ParCoolAnimationAccessor accessor)) {
				return false;
			}

			com.alrex.parcool.client.animation.Animator animator = accessor.epm$getAnimator();
			if (animator == null || !isParCoolDodgeAnimator(animator)) {
				return false;
			}

			Parkourability parkourability = Parkourability.get(player);
			return parkourability == null || !animator.shouldRemoved(player, parkourability);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolDodgeAnimator(com.alrex.parcool.client.animation.Animator animator) {
		String simpleName = animator.getClass().getSimpleName();
		return "DodgeAnimator".equals(simpleName) || "ExaggeratedSideDodgeAnimator".equals(simpleName);
	}

	private static boolean isDodgeRollAnimation(AssetAccessor<?> animation) {
		ResourceLocation registryName = safeRegistryName(animation);
		if (registryName == null) {
			return false;
		}

		String namespace = registryName.getNamespace();
		String path = registryName.getPath();
		return "epicparcool".equals(namespace) && path.startsWith("biped/roll_")
				|| "epicfight".equals(namespace) && path.startsWith("biped/skill/roll_");
	}

	private static boolean hasDodgeRollBaseAnimation(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return isDodgeRollAnimation(currentBaseAnimation(playerPatch));
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

	private static boolean markForPhantomAscent(Player player, PhantomAscentPrimeSource source) {
		if (!canStartPhantomAscentPrime(player, source)) {
			return false;
		}

		PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(0));
		PHANTOM_ASCENT_SOURCES.put(player, source);
		PHANTOM_ASCENT_STARTED_TICKS.remove(player);
		logGliderOpeningDelayDiagnostic(player, "phantom_mark_" + source.name().toLowerCase(), true, false, false);
		phantomJumpWasDown = isPhysicalJumpKeyDown();
		return true;
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

		if (JumpInputConsumptionState.isCurrentPressConsumed(player)) {
			phantomJumpWasDown = isPhysicalJumpKeyDown();
			PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(nextTick));
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
		PhantomAscentPrimeSource source = PHANTOM_ASCENT_SOURCES.get(player);
		PHANTOM_ASCENT_TICKS.remove(player);
		PHANTOM_ASCENT_SOURCES.remove(player);
		if (source == PhantomAscentPrimeSource.WALL_JUMP && !Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player))
				&& PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player) != null) {
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_clear", false, false, false);
		}
		phantomJumpWasDown = false;
	}

	private static void markPhantomAscentConsumedJump(Player player, String reason) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		JumpInputConsumptionState.markConsumed(player, JumpInputConsumptionState.Consumer.PHANTOM_ASCENT, reason, isPhysicalJumpKeyDown());
		logGliderOpeningDelayDiagnostic(player, "phantom_consumed_jump_mark_" + reason, true, false, false);
	}

	private static boolean isPhantomAscentConsumedJumpHeld(Player player) {
		return JumpInputConsumptionState.isCurrentPressConsumed(player);
	}

	private static boolean isPhantomAscentConsumedJumpPendingDrop(Player player, int queuedTick) {
		return JumpInputConsumptionState.shouldDropQueuedPress(player, queuedTick);
	}

	private static void markSpiderWallJumpForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		PlayerPatch<?> playerPatch = snapshot.playerPatch();
		if (playerPatch != null && snapshot.womBackflipAnimation()) {
			markWomBackflipPhantomLock(player, "probe_wall_backflip_animation");
			if (!markForPhantomAscent(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP)) {
				logWomBackflipGliderGuard(player, "wom_backflip_phantom_prime_skip", "probe_unavailable_or_used", false, false);
			}
		}
	}

	private static boolean isCurrentWallJumpPhantomAscentPrime(Player player) {
		boolean parcoolWallJump = shouldProtectParCoolWallJumpForPhantom(player);
		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		boolean womWallJump = EPMConfig.spiderWallJumpPrimesPhantomAscent()
				&& snapshot.womBackflipAnimation()
				&& (PHANTOM_ASCENT_TICKS.containsKey(player) || canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP));

		return parcoolWallJump || womWallJump;
	}

	private static boolean isCurrentParCoolWallJumpAnimation(Player player) {
		return EPMConfig.wallJumpPrimesPhantomAscent() && GliderFrameState.snapshot(player).parCoolWallJumpAnimation();
	}

	private static void primePhantomAscentForNextInput(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		PhantomAscentPrimeSource source = PHANTOM_ASCENT_SOURCES.get(player);

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
			cancelCurrentActionBeforeNativePhantomAscent(player, localPlayerPatch, source);
			PENDING_FORCED_PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(1));
			PENDING_FORCED_PHANTOM_ASCENT_SOURCES.put(player, source);
		}
	}

	private static void tickPendingForcedPhantomAscent(Player player) {
		Integer ticks = PENDING_FORCED_PHANTOM_ASCENT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() > 3) {
			PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
			PENDING_FORCED_PHANTOM_ASCENT_SOURCES.remove(player);
			return;
		}

		PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
		PhantomAscentPrimeSource source = PENDING_FORCED_PHANTOM_ASCENT_SOURCES.remove(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			triggerNativePhantomAscent(player, localPlayerPatch, source);
		}
	}

	private static void cancelCurrentActionBeforeNativePhantomAscent(Player player, LocalPlayerPatch playerPatch, PhantomAscentPrimeSource source) {
		PHANTOM_ASCENT_USED_AIRBORNE.put(player, Boolean.TRUE);
		PHANTOM_ASCENT_STARTED_TICKS.put(player, Integer.valueOf(player.tickCount));
		markPhantomAscentConsumedJump(player, "native_start");
		clearGliderOpeningDelayState(player);
		if (PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(player) || PHANTOM_ASCENT_SOURCES.get(player) == PhantomAscentPrimeSource.WALL_JUMP) {
			PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.put(player, Integer.valueOf(player.tickCount));
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_handoff_start", true, false, false);
		}

		if (source == PhantomAscentPrimeSource.DEMOLITION_LEAP) {
			cancelDemolitionLeapBeforeNativePhantomAscent(player, playerPatch);
		}

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

	private static void cancelDemolitionLeapBeforeNativePhantomAscent(Player player, LocalPlayerPatch playerPatch) {
		logForcedPhantomAscent(player, playerPatch, PhantomAscentPrimeSource.DEMOLITION_LEAP, "phantom_force_demolition_cancel_before");
		stopPlaying(playerPatch, Animations.BIPED_DEMOLITION_LEAP, Animations.BIPED_DEMOLITION_LEAP_CHARGING);

		try {
			playerPatch.resetHolding();
			playerPatch.getClientAnimator().offAllLayers();
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
			playerPatch.setModelYRot(player.getYRot(), true);
		} catch (RuntimeException | LinkageError ignored) {
		}

		logForcedPhantomAscent(player, playerPatch, PhantomAscentPrimeSource.DEMOLITION_LEAP, "phantom_force_demolition_cancel_after");
	}

	private static void logForcedPhantomAscent(Player player, PlayerPatch<?> playerPatch, PhantomAscentPrimeSource source, String phase) {
		if (source != PhantomAscentPrimeSource.DEMOLITION_LEAP || player == null || !player.isLocalPlayer()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/DemolitionLeap] phase={} tick={} source={} inaction={} holdingAny={} currentAnimation={} delta={}",
				phase,
				Integer.valueOf(player.tickCount),
				source.name(),
				Boolean.valueOf(entityStateInaction(playerPatch)),
				Boolean.valueOf(isHoldingAny(playerPatch)),
				assetName(currentBaseAnimation(playerPatch)),
				player.getDeltaMovement());
	}

	private static void triggerNativePhantomAscent(Player player, LocalPlayerPatch playerPatch, PhantomAscentPrimeSource source) {
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

		logForcedPhantomAscent(player, playerPatch, source, "phantom_force_trigger_before");
		PhantomAscentPrimeSource previousSource = ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.get();
		ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.set(source);
		try {
			PlayerInputState inputState = PlayerInputState.fromVanillaInput(localPlayer.input).withJumping(true);
			MovementInputEvent movementInputEvent = new MovementInputEvent(playerPatch, inputState);
			playerPatch.getEventListener().triggerEvents(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, movementInputEvent);
			markPhantomAscentAirAttackWindow(player);
		} catch (RuntimeException | LinkageError ignored) {
		} finally {
			if (previousSource == null) {
				ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.remove();
			} else {
				ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.set(previousSource);
			}
		}
		logForcedPhantomAscent(player, playerPatch, source, "phantom_force_trigger_after");
	}

	private static void clearAirbornePhantomAscentLockIfLanded(Player player) {
		if (!player.onGround()) {
			return;
		}

		PHANTOM_ASCENT_USED_AIRBORNE.remove(player);
		PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
		PENDING_FORCED_PHANTOM_ASCENT_SOURCES.remove(player);
		PHANTOM_ASCENT_AIR_ATTACK_WINDOW_SENT.remove(player);
		PHANTOM_ASCENT_STARTED_TICKS.remove(player);
		JumpInputConsumptionState.clear(player);
		DELAYED_PHANTOM_ASCENT_AIR_ATTACKS.remove(player);
		PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_TICKS.remove(player);
		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
		PENDING_GLIDER_PREINPUTS.remove(player);
		PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
		PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		PARCOOL_WALL_JUMP_INPUT_DOWN.remove(player);
		PARCOOL_WALL_JUMP_STARTED_TICKS.remove(player);
		PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
		PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.remove(player);
		PARCOOL_WALL_RUN_HANDOFF_TICKS.remove(player);
		WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.remove(player);
		LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.remove(player);
		WOM_BACKFLIP_PHANTOM_LOCK_TICKS.remove(player);
		clearGliderOpeningDelayState(player);
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
		return AnimationQuery.safeRegistryName(animation);
	}

	private static String assetName(AssetAccessor<?> animation) {
		ResourceLocation registryName = safeRegistryName(animation);
		return registryName == null ? String.valueOf(animation) : registryName.toString();
	}

	private static boolean entityStateInaction(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch != null && playerPatch.getEntityState() != null && playerPatch.getEntityState().inaction();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isHoldingAny(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch != null && playerPatch.isHoldingAny();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
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
		if ((nextTick > 2 && player.onGround()) || player.isInWater() || nextTick > 60) {
			NATURAL_SPRINTER_CAT_LEAP_TICKS.remove(player);
			resetBaseAnimation(player);
			return;
		}

		NATURAL_SPRINTER_CAT_LEAP_TICKS.put(player, Integer.valueOf(nextTick));
	}

	private static void clearNaturalSprinterCatLeapForGlider(Player player, String phase) {
		Integer catLeapTick = NATURAL_SPRINTER_CAT_LEAP_TICKS.remove(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		boolean removedPatch = playerPatch != null && NATURAL_SPRINTER_CAT_LEAP_PATCHES.remove(playerPatch) != null;
		if (catLeapTick == null && !removedPatch) {
			return;
		}

		logGliderOpeningDelayDiagnostic(player, phase, false, false, false);
		if (playerPatch != null) {
			NaturalSprinterState.suppress(playerPatch);
			PENDING_FAST_RUN_DASHES.remove(playerPatch);
			if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
				stopPlaying(localPlayerPatch, WomAnimationRefs.bipedSprintJump());
			}
		}

		setSprintingWithDiagnostic(player, false, "cat_leap_clear_for_glider");
		clearParCoolAnimator(player);
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
		setSprintingWithDiagnostic(player, false, "reset_base_animation");
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
		if (animation != null && animation.hasAnimator()) {
			logGliderFastRunDiagnostic(player, "clear_animator_before", true);
			animation.removeAnimator();
			logGliderFastRunDiagnostic(player, "clear_animator_after", true);
		}
	}

	private static void tickVaultFastRunGraceWindow(Player player) {
		Integer ticks = VAULT_FAST_RUN_GRACE_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (!EPMConfig.fastRunVaultChainFix() || ticks.intValue() <= 0) {
			clearVaultFastRunState(player);
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
			logGliderFastRunDiagnostic(player, "tacz_suppress_end_start_restore", true);
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

		logGliderFastRunDiagnostic(player, "tacz_restore_tick", true);
		setSprintingWithDiagnostic(player, true, "tacz_restore_tick");
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
		clearNaturalSprinterStepFastRun(player);
		stopParCoolFastRunAction(player);
		setSprintingWithDiagnostic(player, false, "stop_local_sprint_and_fast_run");
		clearParCoolAnimator(player);
	}

	private static void setSprintingWithDiagnostic(Player player, boolean sprinting, String phase) {
		if (player == null) {
			return;
		}

		boolean before = player.isSprinting();
		if (before != sprinting) {
			logGliderFastRunDiagnostic(player, phase + "_before", true);
		}

		player.setSprinting(sprinting);

		if (before != sprinting || before != player.isSprinting()) {
			logGliderFastRunDiagnostic(player, phase + "_after", true);
		}
	}

	private static boolean hasGliderFastRunTrackedWork(Player player) {
		if (player == null) {
			return false;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		return WALL_JUMP_AUTO_SPRINT_TICKS.containsKey(player)
				|| VAULT_HOLD_FAST_RUN.containsKey(player)
				|| VAULT_FAST_RUN_GRACE_TICKS.containsKey(player)
				|| TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(player)
				|| TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.containsKey(player)
				|| TACZ_SHOOT_ACTIVE.containsKey(player)
				|| TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(player)
				|| TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_STEP_FAST_RUN_STATES.containsKey(player)
				|| NATURAL_SPRINTER_BREAKFALL_START_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.containsKey(player)
				|| snapshot.playerPatch() != null && PENDING_FAST_RUN_DASHES.containsKey(snapshot.playerPatch());
	}

	private static boolean hasGliderFastRunDiagnosticInterest(Player player) {
		return player != null && (player.isSprinting()
				|| isParCoolFastRunDoing(player)
				|| hasParCoolFastRunAnimator(player)
				|| hasGliderFastRunTrackedWork(player));
	}

	private static void logGliderFastRunDiagnostic(Player player, String phase, boolean force) {
		if (!EPMConfig.debugGliderState() || player == null || !player.isLocalPlayer()) {
			return;
		}

		GliderFrameState.Snapshot snapshot = GliderFrameState.snapshot(player);
		PlayerPatch<?> playerPatch = snapshot.playerPatch();
		boolean gliderActive = snapshot.gliderActive();
		boolean fastRunDoing = isParCoolFastRunDoing(player);
		boolean fastRunAnimator = hasParCoolFastRunAnimator(player);
		boolean pendingFastRunDash = playerPatch != null && PENDING_FAST_RUN_DASHES.containsKey(playerPatch);
		Integer lastGliderActiveTick = LAST_GLIDER_FAST_RUN_ACTIVE_TICKS.get(player);
		int lastGliderElapsed = lastGliderActiveTick == null ? -1 : player.tickCount - lastGliderActiveTick.intValue();
		Integer wallJumpAutoSprintTicks = WALL_JUMP_AUTO_SPRINT_TICKS.get(player);
		Integer vaultGraceTicks = VAULT_FAST_RUN_GRACE_TICKS.get(player);
		Integer taczSuppressTicks = TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.get(player);
		Integer taczRestoreTicks = TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.get(player);
		Vec3 delta = player.getDeltaMovement();

		GliderFastRunSnapshot previous = GLIDER_FAST_RUN_SNAPSHOTS.get(player);
		boolean changed = previous == null
				|| previous.gliderActive() != gliderActive
				|| previous.onGround() != snapshot.onGround()
				|| previous.sprinting() != snapshot.sprinting()
				|| previous.fastRunDoing() != fastRunDoing
				|| previous.fastRunAnimator() != fastRunAnimator
				|| previous.wallJumpAutoSprint() != (wallJumpAutoSprintTicks != null)
				|| previous.vaultHold() != VAULT_HOLD_FAST_RUN.containsKey(player)
				|| previous.vaultGrace() != (vaultGraceTicks != null)
				|| previous.taczSuppress() != (taczSuppressTicks != null)
				|| previous.taczRestore() != (taczRestoreTicks != null)
				|| previous.taczActive() != TACZ_SHOOT_ACTIVE.containsKey(player)
				|| previous.pendingFastRunDash() != pendingFastRunDash
				|| !previous.animationName().equals(snapshot.animationName());
		boolean periodic = previous == null || player.tickCount - previous.lastLogTick() >= 10;
		if (!force && !changed && !periodic) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/GliderFastRun] phase={} tick={} gliderActive={} lastGliderElapsed={} onGround={} inWater={} sprinting={} fastRunDoing={} fastRunAnimator={} wallJumpAutoSprint={} wallJumpAutoSprintTicks={} vaultHold={} vaultGrace={} vaultGraceTicks={} taczSuppress={} taczSuppressTicks={} taczRestore={} taczRestoreTicks={} taczActive={} pendingFastRunDash={} naturalCatLeap={} breakfallStart={} breakfallDash={} jumpDown={} attackDown={} fastRunKeyDown={} animation={} pos=({}, {}, {}) delta=({}, {}, {})",
				phase,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(gliderActive),
				Integer.valueOf(lastGliderElapsed),
				Boolean.valueOf(snapshot.onGround()),
				Boolean.valueOf(snapshot.inWater()),
				Boolean.valueOf(snapshot.sprinting()),
				Boolean.valueOf(fastRunDoing),
				Boolean.valueOf(fastRunAnimator),
				Boolean.valueOf(wallJumpAutoSprintTicks != null),
				wallJumpAutoSprintTicks,
				Boolean.valueOf(VAULT_HOLD_FAST_RUN.containsKey(player)),
				Boolean.valueOf(vaultGraceTicks != null),
				vaultGraceTicks,
				Boolean.valueOf(taczSuppressTicks != null),
				taczSuppressTicks,
				Boolean.valueOf(taczRestoreTicks != null),
				taczRestoreTicks,
				Boolean.valueOf(TACZ_SHOOT_ACTIVE.containsKey(player)),
				Boolean.valueOf(pendingFastRunDash),
				Boolean.valueOf(NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(player)),
				Boolean.valueOf(NATURAL_SPRINTER_BREAKFALL_START_TICKS.containsKey(player)),
				Boolean.valueOf(NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.containsKey(player)),
				Boolean.valueOf(snapshot.jumpDown()),
				Boolean.valueOf(snapshot.attackDown()),
				Boolean.valueOf(snapshot.fastRunDown()),
				snapshot.animationName(),
				Double.valueOf(player.getX()),
				Double.valueOf(player.getY()),
				Double.valueOf(player.getZ()),
				Double.valueOf(delta.x()),
				Double.valueOf(delta.y()),
				Double.valueOf(delta.z())
		);
		GLIDER_FAST_RUN_SNAPSHOTS.put(player, new GliderFastRunSnapshot(
				gliderActive,
				snapshot.onGround(),
				snapshot.sprinting(),
				fastRunDoing,
				fastRunAnimator,
				wallJumpAutoSprintTicks != null,
				VAULT_HOLD_FAST_RUN.containsKey(player),
				vaultGraceTicks != null,
				taczSuppressTicks != null,
				taczRestoreTicks != null,
				TACZ_SHOOT_ACTIVE.containsKey(player),
				pendingFastRunDash,
				snapshot.animationName(),
				player.tickCount
		));
	}

	private static void stopParCoolFastRunAction(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			if (fastRun != null && fastRun.isDoing()) {
				fastRun.finish(player);
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static boolean isParCoolFastRunDoing(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolVaultDoing(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			Vault vault = parkourability == null ? null : parkourability.get(Vault.class);
			return vault != null && vault.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean shouldLogVaultTick(WeakHashMap<Player, Integer> ticks, Player player) {
		if (player == null) {
			return false;
		}

		Integer lastTick = ticks.get(player);
		if (lastTick != null && lastTick.intValue() == player.tickCount) {
			return false;
		}

		ticks.put(player, Integer.valueOf(player.tickCount));
		return true;
	}

	private static void clearVaultLogTicks(Player player) {
		if (player == null) {
			return;
		}

		VAULT_GRACE_LOG_TICKS.remove(player);
		VAULT_EARLY_FINISH_LOG_TICKS.remove(player);
	}

	private static void clearVaultFastRunState(Player player) {
		if (player == null) {
			return;
		}

		VAULT_HOLD_FAST_RUN.remove(player);
		VAULT_FAST_RUN_GRACE_TICKS.remove(player);
		clearVaultLogTicks(player);
	}

	private static boolean hasVaultGraceMovementInput() {
		try {
			Vec3 moveVector = KeyBindings.getCurrentMoveVector();
			return moveVector != null && moveVector.lengthSqr() > 1.0E-6D;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static Parkourability safeParkourability(Player player) {
		try {
			return Parkourability.get(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static <T extends Action> T safeParCoolAction(Parkourability parkourability, Class<T> actionClass) {
		try {
			return parkourability == null ? null : parkourability.get(actionClass);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean safeParCoolActionEnabled(Parkourability parkourability, Class<? extends Action> actionClass) {
		try {
			return parkourability != null && parkourability.getActionInfo().can(actionClass);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasParCoolFastRunAnimator(Player player) {
		Animation animation = Animation.get(player);
		return animation != null && animation.hasAnimator();
	}

	private static void startTaczShootFastRunRestore(Player player) {
		if (!isHoldingTaczGun(player) || hasHardVaultFastRunBlocker(player)) {
			cancelTaczShootFastRunRestore(player);
			return;
		}

		TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS));
		logGliderFastRunDiagnostic(player, "tacz_restore_start", true);
		setSprintingWithDiagnostic(player, true, "tacz_restore_start");
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

	private static void logExhaustionPose(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		IStamina parcoolStamina = safeParCoolStamina(player);
		float epicFightStamina = safeEpicFightStamina(playerPatch);
		int parcoolStaminaValue = parcoolStamina == null ? -1 : parcoolStamina.get();
		boolean parcoolExhausted = parcoolStamina != null && parcoolStamina.isExhausted();
		boolean epicFightExhausted = epicFightStamina >= 0.0F && epicFightStamina < 0.1F;
		AssetAccessor<?> animation = playerPatch == null ? null : currentBaseAnimation(playerPatch);
		ResourceLocation animationId = safeRegistryName(animation);
		String animationName = animationId == null ? String.valueOf(animation) : animationId.toString();
		boolean efxIdle = animationId != null
				&& "epicfightx".equals(animationId.getNamespace())
				&& "biped/living/idle".equals(animationId.getPath());
		boolean lowStaminaWindow = epicFightStamina >= 0.0F && epicFightStamina <= 1.0F
				|| parcoolStaminaValue >= 0 && parcoolStaminaValue <= 5
				|| parcoolExhausted
				|| epicFightExhausted;

		ExhaustionPoseSnapshot previous = EXHAUSTION_POSE_SNAPSHOTS.get(player);
		boolean wasRecentlyLow = previous != null && previous.recentLowTicks() > 0;
		if (!lowStaminaWindow && !wasRecentlyLow) {
			EXHAUSTION_POSE_SNAPSHOTS.remove(player);
			return;
		}

		int recentLowTicks = lowStaminaWindow ? 20 : previous.recentLowTicks() - 1;
		boolean animationChanged = previous == null || !animationName.equals(previous.animationName());
		boolean stateChanged = previous == null
				|| previous.parcoolExhausted() != parcoolExhausted
				|| previous.epicFightExhausted() != epicFightExhausted
				|| previous.sprinting() != player.isSprinting()
				|| previous.crouching() != player.isCrouching()
				|| previous.swimming() != player.isSwimming();
		boolean periodic = previous == null || player.tickCount - previous.lastLogTick() >= 10;
		boolean shouldLog = lowStaminaWindow && (animationChanged || stateChanged || periodic);
		int lastLogTick = previous == null ? player.tickCount : previous.lastLogTick();

		if (shouldLog) {
			lastLogTick = player.tickCount;
			Vec3 movement = player.getDeltaMovement();
			EPM.LOGGER.info(
					"[EPM/ExhaustionPose] tick={} lowWindow={} efMode={} efStamina={} efExhausted={} parcoolStamina={} parcoolExhausted={} sprinting={} crouching={} swimming={} onGround={} inWater={} inWaterOrBubble={} inaction={} animation={} efxIdle={} delta=({}, {}, {})",
					Integer.valueOf(player.tickCount),
					Boolean.valueOf(lowStaminaWindow),
					Boolean.valueOf(playerPatch != null && playerPatch.isEpicFightMode()),
					Float.valueOf(epicFightStamina),
					Boolean.valueOf(epicFightExhausted),
					Integer.valueOf(parcoolStaminaValue),
					Boolean.valueOf(parcoolExhausted),
					Boolean.valueOf(player.isSprinting()),
					Boolean.valueOf(player.isCrouching()),
					Boolean.valueOf(player.isSwimming()),
					Boolean.valueOf(player.onGround()),
					Boolean.valueOf(player.isInWater()),
					Boolean.valueOf(player.isInWaterOrBubble()),
					Boolean.valueOf(playerPatch != null && playerPatch.getEntityState().inaction()),
					animationName,
					Boolean.valueOf(efxIdle),
					Double.valueOf(movement.x()),
					Double.valueOf(movement.y()),
					Double.valueOf(movement.z())
			);
		}

		EXHAUSTION_POSE_SNAPSHOTS.put(player, new ExhaustionPoseSnapshot(
				animationName,
				parcoolExhausted,
				epicFightExhausted,
				player.isSprinting(),
				player.isCrouching(),
				player.isSwimming(),
				recentLowTicks,
				lastLogTick
		));
	}

	private static IStamina safeParCoolStamina(Player player) {
		try {
			return IStamina.get(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static float safeEpicFightStamina(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return -1.0F;
		}

		try {
			return playerPatch.getStamina();
		} catch (RuntimeException | LinkageError ignored) {
			return -1.0F;
		}
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
			logGliderFastRunDiagnostic(player, "ensure_animator_before", true);
			animation.setAnimator(new FastRunningAnimator());
			logGliderFastRunDiagnostic(player, "ensure_animator_after", true);
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		return AnimationQuery.currentAnimation(playerPatch);
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

	private static boolean isPhysicalAttackKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyAttack.isDown();
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
		},
		DEMOLITION_LEAP {
			@Override
			boolean enabled() {
				return EPMConfig.demolitionLeapAirDoubleJump();
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

	private static final class NaturalSprinterStepFastRunState {
		private final int startTick;
		private AssetAccessor<? extends StaticAnimation> startupStep;

		private NaturalSprinterStepFastRunState(int startTick, AssetAccessor<? extends StaticAnimation> startupStep) {
			this.startTick = startTick;
			this.startupStep = startupStep;
		}
	}

	private static final class DeferredNaturalSprinterDodgeStep {
		private final int startTick;
		private int clearTick = -1;
		private AssetAccessor<? extends StaticAnimation> deferredStep;

		private DeferredNaturalSprinterDodgeStep(int startTick, AssetAccessor<? extends StaticAnimation> deferredStep) {
			this.startTick = startTick;
			this.deferredStep = deferredStep;
		}
	}

	private record CachedBoolean(int tick, boolean value) {
	}

	private record GliderFastRunSnapshot(
			boolean gliderActive,
			boolean onGround,
			boolean sprinting,
			boolean fastRunDoing,
			boolean fastRunAnimator,
			boolean wallJumpAutoSprint,
			boolean vaultHold,
			boolean vaultGrace,
			boolean taczSuppress,
			boolean taczRestore,
			boolean taczActive,
			boolean pendingFastRunDash,
			String animationName,
			int lastLogTick) {
	}

	private enum GliderInputArbitrationAction {
		NONE,
		DROP,
		CACHE
	}

	private enum GliderPreinputSource {
		PHANTOM_ASCENT,
		WOM_BACKFLIP,
		PARCOOL_WALL_JUMP,
		WALLRUN_TO_PARCOOL_WALL_JUMP,
		WALL_MOVEMENT
	}

	private record GliderInputArbitrationDecision(GliderInputArbitrationAction action, GliderPreinputSource source, String reason) {
		static GliderInputArbitrationDecision none() {
			return new GliderInputArbitrationDecision(GliderInputArbitrationAction.NONE, null, "none");
		}

		static GliderInputArbitrationDecision drop(GliderPreinputSource source, String reason) {
			return new GliderInputArbitrationDecision(GliderInputArbitrationAction.DROP, source, reason);
		}

		static GliderInputArbitrationDecision cache(GliderPreinputSource source, String reason) {
			return new GliderInputArbitrationDecision(GliderInputArbitrationAction.CACHE, source, reason);
		}
	}

	private record PendingGliderPreinput(int queuedTick, GliderPreinputSource source, int minDelayTicks, int maxAgeTicks) {
	}

	private record ExhaustionPoseSnapshot(
			String animationName,
			boolean parcoolExhausted,
			boolean epicFightExhausted,
			boolean sprinting,
			boolean crouching,
			boolean swimming,
			int recentLowTicks,
			int lastLogTick) {
	}

	private static final class GliderOpeningDelayState {
		private int checkedTick = Integer.MIN_VALUE;
		private boolean delay;
	}
}
