package dev.spake404.epm;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.EpmAnimations;
import dev.spake404.epm.animation.EpmLivingMotions;
import dev.spake404.epm.animation.WomAnimationRefs;
import dev.spake404.epm.climb.ClingToCliffDebug;
import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.glider.GliderCompat;
import dev.spake404.epm.glider.GliderFrameState;
import dev.spake404.epm.mixin.AnimatorControlPacketAccessor;
import dev.spake404.epm.mixin.ParCoolAnimationAccessor;
import dev.spake404.epm.mixin.SPAnimatorControlAccessor;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunHandler;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunStep;
import dev.spake404.epm.naturalsprinter.NaturalSprinterProceduralStepPulse;
import dev.spake404.epm.naturalsprinter.NaturalSprinterState;
import dev.spake404.epm.network.EPMNetwork;
import dev.spake404.epm.phantom.MomentumAirAttackWindowState;
import dev.spake404.epm.phantom.PhantomAscentAirAttackState;
import dev.spake404.epm.vault.VaultDebug;
import dev.spake404.epm.vault.VaultStartFastRunGrace;
import dev.spake404.epm.walljump.JumpActionArbiter;
import dev.spake404.epm.walljump.ParCoolWallJumpHandoffState;
import dev.spake404.epm.wom.spider.WomSpiderWallHooks;
import dev.spake404.epm.wom.spider.WomSpiderWallContactResolver;
import dev.spake404.epm.wom.spider.WomSpiderWallJumpPriority;
import dev.spake404.epm.wom.spider.WomSpiderWallRunHandler;
import dev.spake404.epm.wom.spider.WomSpiderWallSlideHandler;
import java.lang.reflect.Field;
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
import com.alrex.parcool.common.action.impl.VerticalWallRun;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.ASMEventHandler;
import net.minecraftforge.eventbus.api.IEventListener;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.input.PlayerInputState;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
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
	private static final WeakHashMap<Player, PhantomAscentCycle> PHANTOM_ASCENT_CYCLES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_HOLD_FAST_RUN = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_FAST_RUN_GRACE_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_FAST_RUN_START_STEP_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_GRACE_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> VAULT_EARLY_FINISH_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> WALL_JUMP_AUTO_SPRINT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> JUMP_PRIORITY_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PARCOOL_WALL_JUMP_CANDIDATE_LOG_TICKS = new WeakHashMap<>();
	private static int INPUT_ORDER_SEQUENCE;
	private static boolean MOVEMENT_INPUT_LISTENERS_DUMPED;
	private static final WeakHashMap<Player, Integer> TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_SHOOT_FAST_RUN_RESTORE_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> TACZ_SHOOT_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, AssetAccessor<? extends StaticAnimation>> PENDING_FAST_RUN_DASHES = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, NaturalSprinterFastRunDashSource> PENDING_FAST_RUN_DASH_SOURCES = new WeakHashMap<>();
	private static final WeakHashMap<Player, NaturalSprinterStepFastRunState> NATURAL_SPRINTER_STEP_FAST_RUN_STATES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> NATURAL_SPRINTER_BREAKFALL_START_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, AssetAccessor<? extends StaticAnimation>> NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES = new WeakHashMap<>();
	private static final WeakHashMap<Player, DeferredNaturalSprinterDodgeStep> NATURAL_SPRINTER_DODGE_DEFERRED_STEPS = new WeakHashMap<>();
	private static final WeakHashMap<Player, NaturalSprinterStepPlaybackOwner> NATURAL_SPRINTER_STEP_PLAYBACK_OWNERS = new WeakHashMap<>();
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
	private static final WeakHashMap<Player, Integer> HANDLED_JUMP_HANDOFF_PRESS_SEQUENCES = new WeakHashMap<>();
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
	private static final int TACZ_SHOOT_FAST_RUN_SUPPRESS_DURATION_TICKS = 3;
	private static final int TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS = 12;
	private static final int TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS = 30;
	private static final int TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_DURATION_TICKS = 20;
	private static final int VAULT_FAST_RUN_CAN_ACT_GRACE_TICKS = 10;
	private static final int VAULT_FAST_RUN_START_STEP_SUPPRESS_GRACE_TICKS = VAULT_FAST_RUN_CAN_ACT_GRACE_TICKS + 4;
	private static final int NATURAL_SPRINTER_BREAKFALL_DASH_STARTUP_GRACE_TICKS = 3;
	private static final int NATURAL_SPRINTER_BREAKFALL_DASH_MAX_DELAY_TICKS = 40;
	private static final int NATURAL_SPRINTER_DODGE_STEP_CLEAR_GRACE_TICKS = 6;
	private static final int NATURAL_SPRINTER_DODGE_STEP_MAX_DELAY_TICKS = 60;
	private static final int NATURAL_SPRINTER_STEP_FAST_RUN_STARTUP_MAX_TICKS = 6;
	private static final int NATURAL_SPRINTER_BREAKFALL_STEP_OWNER_MAX_TICKS =
			NATURAL_SPRINTER_BREAKFALL_DASH_STARTUP_GRACE_TICKS + NATURAL_SPRINTER_STEP_FAST_RUN_STARTUP_MAX_TICKS;
	private static volatile boolean jumpSpeedModifierInstalled;
	private static boolean phantomJumpWasDown;
	private static final ThreadLocal<PhantomAscentPrimeSource> ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE = new ThreadLocal<>();

	private EPMClientHooks() {
	}

	public static void registerDoubleJumpFallAnimation(InitAnimatorEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| event == null
				|| !(event.getEntityPatch() instanceof PlayerPatch<?>)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = EpmAnimations.doubleJumpFall();
		if (animation != null) {
			event.getAnimator().addLivingAnimation(EpmLivingMotions.DOUBLE_JUMP_FALL, animation);
		}
	}

	public static void chooseDoubleJumpFallAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		if (event == null
				|| event.getMotion() != LivingMotions.FALL) {
			return;
		}
		if (!isDoubleJumpAnimationReplacementEnabled()) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle == null
				|| !cycle.usedAirborne
				|| !cycle.seenAirborne
				|| player == null
				|| player.onGround()
				|| player.isInWater()
				|| player.isDeadOrDying()) {
			return;
		}

		cycle.doubleJumpFallPlayed = true;
		event.setMotion(EpmLivingMotions.DOUBLE_JUMP_FALL);
	}

	public static AssetAccessor<? extends StaticAnimation> replaceDoubleJumpAnimation(
			Player player,
			AssetAccessor<? extends StaticAnimation> animation) {
		if (animation == null) {
			return null;
		}

		if (!isDoubleJumpAnimationReplacementEnabled()) {
			return animation;
		}

		if (WomAnimationRefs.isAny(
				animation,
				Animations.BIPED_PHANTOM_ASCENT_FORWARD,
				Animations.BIPED_PHANTOM_ASCENT_BACKWARD)) {
			AssetAccessor<? extends StaticAnimation> jump = EpmAnimations.doubleJumpJump();
			return jump == null ? animation : jump;
		}

		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle == null || !cycle.usedAirborne || cycle.playingSelectedLanding) {
			return animation;
		}

		if (WomAnimationRefs.isAny(animation, Animations.BIPED_FALL)) {
			AssetAccessor<? extends StaticAnimation> fall = EpmAnimations.doubleJumpFall();
			if (fall != null) {
				cycle.doubleJumpFallPlayed = true;
				return fall;
			}
		}

		if (WomAnimationRefs.isAny(animation, Animations.BIPED_LANDING)) {
			AssetAccessor<? extends StaticAnimation> land = EpmAnimations.doubleJumpLand();
			if (land != null && player != null && player.getRandom().nextBoolean()) {
				return land;
			}
		}

		return animation;
	}

	private static boolean isDoubleJumpAnimationReplacementEnabled() {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.replacePhantomAscentDoubleJumpAnimations();
	}

	public static void startNaturalSprinterCatLeap(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.customFastRunAnimations()
				|| !hasNaturalSprinter(player)) {
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
		if (EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
				&& player.isLocalPlayer()
				&& EPMConfig.catLeapPrimesPhantomAscent()) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.CAT_LEAP);
		}
	}

	public static boolean markDemolitionLeapForPhantomAscent(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
				&& player.isLocalPlayer()
				&& EPMConfig.demolitionLeapAirDoubleJump()
				&& markForPhantomAscent(player, PhantomAscentPrimeSource.DEMOLITION_LEAP);
	}

	public static boolean isForcedDemolitionPhantomAscent(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.get() == PhantomAscentPrimeSource.DEMOLITION_LEAP;
	}

	public static void logForcedDemolitionPhantomBypass(Player player, String phase, boolean originalValue) {
		if (!EPMConfig.debugDemolitionLeapState() || !isForcedDemolitionPhantomAscent(player)) {
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
		if (EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
				&& player.isLocalPlayer()
				&& EPMConfig.wallJumpPrimesPhantomAscent()) {
			if (!canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)) {
				logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_native_window_skip", false, false, false);
				return;
			}

			markForPhantomAscent(player, PhantomAscentPrimeSource.WALL_JUMP);
			PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.put(player, Integer.valueOf(player.tickCount));
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_native_window_start", true, false, false);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_mark", true, false, false);
		}
	}

	public static void markClimbUpForPhantomAscent(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return;
		}

		dropClimbUpGliderRequests(player, "climb_up_start");
		if (!EPMConfig.climbUpPrimesPhantomAscent()) {
			return;
		}

		markForPhantomAscent(player, PhantomAscentPrimeSource.CLIMB_UP);
	}

	public static void markWomWallRunToParCoolWallJumpStarted(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return;
		}

		markParCoolWallJumpInputBaseline(player);
		ParCoolWallJumpHandoffState.markStarted(player, ParCoolWallJumpHandoffState.Source.WOM_WALLRUN);
		markWallRunToParCoolWallJumpGliderSuppress(player, "wallrun_parcool_glider_lock_start");
		Integer previousTick = PARCOOL_WALL_RUN_HANDOFF_TICKS.put(player, Integer.valueOf(player.tickCount));
		if (previousTick == null || previousTick.intValue() != player.tickCount) {
			logGliderOpeningDelayDiagnostic(player, "parcool_wallrun_handoff_mark", true, false, false);
		}
	}

	public static void markWallRunToParCoolWallJumpGliderSuppress(Player player, String phase) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
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
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

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

	public static void markParCoolWallJumpHandoffStarted(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return;
		}

		markParCoolWallJumpInputBaseline(player);
		ParCoolWallJumpHandoffState.markStarted(player, ParCoolWallJumpHandoffState.Source.PARCOOL);
	}

	public static boolean claimParCoolWallJump(Player player, String reason) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return true;
		}

		boolean jumpDown = isPhysicalJumpKeyDown();
		if (!jumpDown) {
			return true;
		}

		return JumpActionArbiter.claim(
				player,
				JumpActionArbiter.Winner.PARCOOL_WALL_JUMP,
				reason == null ? "parcool_wall_jump" : reason,
				jumpDown);
	}

	public static boolean claimWomWallJump(Player player, String reason) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return true;
		}

		WomSpiderWallJumpPriority.Decision decision = WomSpiderWallJumpPriority.resolve(player);
		if (!decision.preferWom() && hasParCoolWallJumpPriorityCandidate(player)) {
			logJumpArbiter(player, "wom_wall_jump_yield_parcool_" + decision.reason());
			return false;
		}

		return JumpActionArbiter.claim(
				player,
				JumpActionArbiter.Winner.WOM_WALL_JUMP,
				reason == null ? "wom_wall_jump" : reason,
				isPhysicalJumpKeyDown());
	}

	public static boolean shouldBlockParCoolWallJumpAfterHigherPriority(Player player, String phase) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| player.onGround()
				|| player.isInWater()) {
			return false;
		}

		boolean higherPriorityClaimed = JumpActionArbiter.isClaimedByHigherOrEqual(
				player,
				JumpActionArbiter.Winner.PARCOOL_WALL_JUMP);
		boolean phantomAirborneLocked = isPhantomAscentUsedAirborne(player);
		if (!phantomAirborneLocked && !higherPriorityClaimed) {
			return false;
		}

		logJumpArbiter(player, phase == null ? "block_wall_jump_after_higher_priority" : phase);
		return true;
	}

	private static void logJumpArbiter(Player player, String phase) {
		if (!EPMConfig.debugActionArbitrationState() || player == null || !player.isLocalPlayer()) {
			return;
		}

		Integer previousTick = JUMP_PRIORITY_LOG_TICKS.get(player);
		if (previousTick != null && previousTick.intValue() == player.tickCount) {
			return;
		}
		JUMP_PRIORITY_LOG_TICKS.put(player, Integer.valueOf(player.tickCount));

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		EPM.LOGGER.info(
				"[EPM/JumpPriority] phase={} tick={} onGround={} jumpDown={} pressSeq={} pressElapsed={} winner={} winnerReason={} winnerElapsed={} phantomUsed={} phantomQueued={} currentAnimation={} delta={}",
				phase,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(jump.jumpDown()),
				Integer.valueOf(jump.pressSequence()),
				Integer.valueOf(jump.pressElapsed()),
				jump.winner(),
				jump.reason(),
				Integer.valueOf(jump.winnerElapsed()),
				Boolean.valueOf(isPhantomAscentUsedAirborne(player)),
				Boolean.valueOf(isPhantomAscentPrimed(player)),
				assetName(currentBaseAnimation(playerPatch)),
				player.getDeltaMovement());
	}

	public static void logMovementInputUpdateOrder(MovementInputUpdateEvent event, String phase) {
		if (event == null || event.getEntity() == null) {
			return;
		}
		Player player = event.getEntity();
		if ("highest_head".equals(phase)) {
			dumpMovementInputListenersOnce(event, player);
		}
		String input = event.getInput() == null
				? "null"
				: "jumping=" + event.getInput().jumping
						+ ",shift=" + event.getInput().shiftKeyDown
						+ ",forward=" + event.getInput().forwardImpulse
						+ ",left=" + event.getInput().leftImpulse;
		logInputOrder(player, "movement_input_" + phase, input);
	}

	public static void logParCoolKeyRecorderOrder(MovementInputUpdateEvent event, String phase) {
		Player player = event == null || event.getEntity() == null ? localPlayer() : event.getEntity();
		String input = event == null || event.getInput() == null
				? "eventInput=null"
				: "eventJumping=" + event.getInput().jumping
						+ ",eventShift=" + event.getInput().shiftKeyDown
						+ ",eventForward=" + event.getInput().forwardImpulse
						+ ",eventLeft=" + event.getInput().leftImpulse;
		logInputOrder(player, "parcool_" + phase, input);
	}

	private static void dumpMovementInputListenersOnce(MovementInputUpdateEvent event, Player player) {
		if (MOVEMENT_INPUT_LISTENERS_DUMPED
				|| !EPMConfig.debugActionArbitrationState()
				|| event == null
				|| player == null
				|| !player.isLocalPlayer()) {
			return;
		}

		boolean physicalJump = isPhysicalJumpKeyDown();
		KeyRecorder.KeyState wallJumpKey = safeWallJumpKeyState();
		boolean wallJumpPressed = wallJumpKey != null && wallJumpKey.isPressed();
		boolean wallJumpKeyDown = safeKeyDown(() -> KeyBindings.getKeyWallJump());
		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		if (!physicalJump && !wallJumpPressed && !wallJumpKeyDown && !jump.jumpDown()) {
			return;
		}

		MOVEMENT_INPUT_LISTENERS_DUMPED = true;
		try {
			Field busIdField = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("busID");
			busIdField.setAccessible(true);
			int busId = ((Integer) busIdField.get(MinecraftForge.EVENT_BUS)).intValue();
			IEventListener[] listeners = event.getListenerList().getListeners(busId);
			EPM.LOGGER.info(
					"[EPM/MovementInputListeners] dump_begin tick={} busID={} total={} physicalJump={} wallKeyDown={} wallPressed={} arbiterJump={} eventInput={} eventClass={}",
					Integer.valueOf(player.tickCount),
					Integer.valueOf(busId),
					Integer.valueOf(listeners.length),
					Boolean.valueOf(physicalJump),
					Boolean.valueOf(wallJumpKeyDown),
					Boolean.valueOf(wallJumpPressed),
					Boolean.valueOf(jump.jumpDown()),
					event.getInput() == null ? "null" : "jumping=" + event.getInput().jumping
							+ ",shift=" + event.getInput().shiftKeyDown
							+ ",forward=" + event.getInput().forwardImpulse
							+ ",left=" + event.getInput().leftImpulse,
					event.getClass().getName());
			for (int index = 0; index < listeners.length; index++) {
				IEventListener listener = listeners[index];
				EPM.LOGGER.info(
						"[EPM/MovementInputListeners] index={} priority={} listenerClass={} listenerName={} description={} fields={}",
						Integer.valueOf(index),
						listenerPriority(listener),
						listener == null ? "null" : listener.getClass().getName(),
						safeListenerName(listener),
						safeListenerDescription(listener),
						listenerFieldSummary(listener));
			}
			EPM.LOGGER.info("[EPM/MovementInputListeners] dump_end tick={} total={}", Integer.valueOf(player.tickCount), Integer.valueOf(listeners.length));
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
			EPM.LOGGER.warn("[EPM/MovementInputListeners] dump_failed tick={} error={} fields={}",
					Integer.valueOf(player.tickCount),
					ex.toString(),
					eventBusFieldSummary());
		}
	}

	private static String listenerPriority(IEventListener listener) {
		if (listener instanceof ASMEventHandler handler) {
			try {
				return String.valueOf(handler.getPriority());
			} catch (RuntimeException | LinkageError ignored) {
				return "asm_error";
			}
		}
		return "unknown";
	}

	private static String safeListenerName(IEventListener listener) {
		if (listener == null) {
			return "null";
		}
		try {
			return sanitizeLogValue(listener.listenerName());
		} catch (RuntimeException | LinkageError ex) {
			return "error:" + ex.getClass().getSimpleName();
		}
	}

	private static String safeListenerDescription(IEventListener listener) {
		if (listener == null) {
			return "null";
		}
		try {
			return sanitizeLogValue(listener.toString());
		} catch (RuntimeException | LinkageError ex) {
			return "error:" + ex.getClass().getSimpleName();
		}
	}

	private static String listenerFieldSummary(IEventListener listener) {
		if (listener == null) {
			return "null";
		}
		StringBuilder builder = new StringBuilder();
		Class<?> type = listener.getClass();
		int count = 0;
		while (type != null && type != Object.class && count < 12) {
			for (Field field : type.getDeclaredFields()) {
				if (count >= 12) {
					break;
				}
				try {
					field.setAccessible(true);
					Object value = field.get(listener);
					if (builder.length() > 0) {
						builder.append(",");
					}
					builder.append(field.getName()).append("=").append(compactObjectDescription(value));
					count++;
				} catch (ReflectiveOperationException | RuntimeException ignored) {
					// Some generated listener fields are not readable under every launch service.
				}
			}
			type = type.getSuperclass();
		}
		return builder.length() == 0 ? "none" : builder.toString();
	}

	private static String eventBusFieldSummary() {
		StringBuilder builder = new StringBuilder();
		Class<?> type = MinecraftForge.EVENT_BUS.getClass();
		while (type != null && type != Object.class) {
			for (Field field : type.getDeclaredFields()) {
				if (builder.length() > 0) {
					builder.append(",");
				}
				builder.append(type.getSimpleName()).append(".").append(field.getName());
			}
			type = type.getSuperclass();
		}
		return builder.length() == 0 ? "none" : builder.toString();
	}

	private static String compactObjectDescription(Object value) {
		if (value == null) {
			return "null";
		}
		Class<?> type = value.getClass();
		String text;
		if (type.isArray()) {
			text = type.getComponentType().getName() + "[]";
		} else if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
			text = value.toString();
		} else {
			text = type.getName() + ":" + value;
		}
		return sanitizeLogValue(text);
	}

	private static String sanitizeLogValue(String value) {
		if (value == null) {
			return "null";
		}
		String sanitized = value.replace('\n', ' ').replace('\r', ' ');
		return sanitized.length() <= 320 ? sanitized : sanitized.substring(0, 320) + "...";
	}

	public static void logPhantomAscentInputOrder(Player player, String phase) {
		logInputOrder(player, phase, "epicFightJump=" + isEpicFightJumpActionPressed());
	}

	private static void logInputOrder(Player player, String phase, String detail) {
		if (!EPMConfig.debugActionArbitrationState() || player == null || !player.isLocalPlayer()) {
			return;
		}

		boolean physicalJump = isPhysicalJumpKeyDown();
		KeyRecorder.KeyState wallJumpKey = safeWallJumpKeyState();
		boolean wallJumpPressed = wallJumpKey != null && wallJumpKey.isPressed();
		boolean wallJumpReleased = wallJumpKey != null && wallJumpKey.isReleased();
		int wallJumpTickDown = wallJumpKey == null ? -1 : wallJumpKey.getTickKeyDown();
		int wallJumpTickUp = wallJumpKey == null ? -1 : wallJumpKey.getTickNotKeyDown();
		boolean wallJumpKeyDown = safeKeyDown(() -> KeyBindings.getKeyWallJump());
		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);

		if (!physicalJump && !wallJumpPressed && !jump.jumpDown() && wallJumpTickDown <= 0) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		EPM.LOGGER.info(
				"[EPM/InputOrder] order={} phase={} tick={} nano={} thread={} physicalJump={} wallKeyDown={} wallPressed={} wallReleased={} wallTickDown={} wallTickUp={} arbiterJump={} pressSeq={} pressElapsed={} winner={} winnerReason={} onGround={} animation={} detail={} stack={}",
				Integer.valueOf(++INPUT_ORDER_SEQUENCE),
				phase,
				Integer.valueOf(player.tickCount),
				Long.valueOf(System.nanoTime()),
				Thread.currentThread().getName(),
				Boolean.valueOf(physicalJump),
				Boolean.valueOf(wallJumpKeyDown),
				Boolean.valueOf(wallJumpPressed),
				Boolean.valueOf(wallJumpReleased),
				Integer.valueOf(wallJumpTickDown),
				Integer.valueOf(wallJumpTickUp),
				Boolean.valueOf(jump.jumpDown()),
				Integer.valueOf(jump.pressSequence()),
				Integer.valueOf(jump.pressElapsed()),
				jump.winner(),
				jump.reason(),
				Boolean.valueOf(player.onGround()),
				assetName(currentBaseAnimation(playerPatch)),
				detail,
				compactInputOrderStack());
	}

	private static KeyRecorder.KeyState safeWallJumpKeyState() {
		try {
			return KeyRecorder.keyWallJump;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static LocalPlayer localPlayer() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft == null ? null : minecraft.player;
	}

	private interface KeyMappingSupplier {
		net.minecraft.client.KeyMapping get();
	}

	private static boolean safeKeyDown(KeyMappingSupplier supplier) {
		try {
			net.minecraft.client.KeyMapping keyMapping = supplier.get();
			return keyMapping != null && keyMapping.isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static String compactInputOrderStack() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for (StackTraceElement element : stack) {
			String className = element.getClassName();
			if (className.startsWith("java.lang.Thread")
					|| className.startsWith("dev.spake404.epm.EPMClientHooks")) {
				continue;
			}
			if (!className.startsWith("dev.spake404")
					&& !className.startsWith("com.alrex.parcool")
					&& !className.startsWith("yesman.epicfight")
					&& !className.startsWith("net.minecraftforge")
					&& !className.startsWith("net.minecraft.client")) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(" <- ");
			}
			builder.append(className).append("#").append(element.getMethodName()).append(":").append(element.getLineNumber());
			count++;
			if (count >= 10) {
				break;
			}
		}
		return builder.length() == 0 ? "none" : builder.toString();
	}

	public static void markWomWallJumpForPhantomAscent(Player player) {
		if (EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
				&& player.isLocalPlayer()
				&& EPMConfig.spiderWallJumpPrimesPhantomAscent()) {
			markWomBackflipPhantomLock(player, "mark_wom_wall_jump");
			if (!markForPhantomAscent(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP)) {
				logWomBackflipGliderGuard(player, "wom_backflip_phantom_prime_skip", "unavailable_or_used", false, false);
			}
		}
	}

	public static boolean shouldCancelPhantomAscentForJumpArbitration(SkillContainer skillContainer, MovementInputEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| skillContainer == null
				|| event == null) {
			return false;
		}

		LocalPlayerPatch localPlayerPatch = event.getPlayerPatch();
		Player player = localPlayerPatch.getOriginal();
		if (player == null || !player.isLocalPlayer()) {
			return false;
		}

		if (ACTIVE_FORCED_PHANTOM_ASCENT_SOURCE.get() != null) {
			logJumpArbiter(player, "phantom_forced_bypass_arbitration");
			return false;
		}

		boolean jumpDown = isEpicFightJumpActionPressed();
		JumpActionArbiter.tick(player, jumpDown);
		if (!jumpDown) {
			return false;
		}

		if (tryPrepareParCoolWallJumpForNativePhantom(skillContainer, player)) {
			return false;
		}

		if (shouldPreemptPhantomAscentForParCoolWallJump(skillContainer, player)) {
			return true;
		}

		if (ParCoolWallJumpHandoffState.shouldBlockInitialPress(player)) {
			setPhantomJumpPressedLastTick(skillContainer, true);
			logJumpArbiter(player, "phantom_block_wall_jump_initial_press");
			return true;
		}

		if (JumpActionArbiter.isClaimedByHigherOrEqual(player, JumpActionArbiter.Winner.PHANTOM_ASCENT)
				|| hasWomWallJumpPriorityCandidate(player)
				|| hasParCoolWallJumpPriorityCandidate(player)) {
			setPhantomJumpPressedLastTick(skillContainer, true);
			logJumpArbiter(player, "phantom_block_higher_priority_candidate");
			return true;
		}

		if (isPhantomJumpPressedLastTick(skillContainer)) {
			return false;
		}

		return false;
	}

	private static boolean shouldPreemptPhantomAscentForParCoolWallJump(SkillContainer skillContainer, Player player) {
		if (JumpActionArbiter.isClaimedByOther(player, JumpActionArbiter.Winner.PARCOOL_WALL_JUMP)) {
			return false;
		}
		WomSpiderWallJumpPriority.Decision decision = WomSpiderWallJumpPriority.resolve(player);
		if (decision.preferWom()) {
			logJumpArbiter(player, "phantom_keep_wom_wall_jump_priority_" + decision.reason());
			return false;
		}
		if (!hasParCoolWallJumpPriorityCandidate(player, true)) {
			return false;
		}
		if (!claimParCoolWallJump(player, "phantom_preempt_parcool_wall_jump")) {
			return false;
		}
		setPhantomJumpPressedLastTick(skillContainer, true);
		logJumpArbiter(player, "phantom_preempt_parcool_wall_jump");
		return true;
	}

	private static boolean tryPrepareParCoolWallJumpForNativePhantom(SkillContainer skillContainer, Player player) {
		if (!ParCoolWallJumpHandoffState.shouldAllowPhantom(player)) {
			return false;
		}
		if (!canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)) {
			logJumpArbiter(player, "phantom_wall_jump_handoff_unavailable");
			return false;
		}
		if (JumpActionArbiter.isClaimedByOther(player, JumpActionArbiter.Winner.PHANTOM_ASCENT)) {
			logJumpArbiter(player, "phantom_wall_jump_handoff_taken");
			return false;
		}

		prepareParCoolWallJumpForNativePhantom(player);
		setPhantomJumpPressedLastTick(skillContainer, false);
		markPhantomAscentConsumedJump(player, "wall_jump_handoff");
		logJumpArbiter(player, "phantom_allow_wall_jump_handoff");
		return true;
	}

	private static boolean hasWomWallJumpPriorityCandidate(Player player) {
		return (WomSpiderWallRunHandler.isWallRunActive(player)
				|| WomSpiderWallSlideHandler.shouldOwnWallState(player))
				&& WomSpiderWallJumpPriority.shouldPreferWom(player)
				|| isWomBackflipPhantomLockActive(player);
	}

	private static boolean hasParCoolWallJumpPriorityCandidate(Player player) {
		return hasParCoolWallJumpPriorityCandidate(player, false);
	}

	private static boolean hasParCoolWallJumpPriorityCandidate(Player player, boolean allowPreRecorderInput) {
		if (player == null) {
			return false;
		}
		if (player.onGround()) {
			return logParCoolWallJumpCandidate(player, false, "on_ground", null, null, null, null);
		}
		if (player.isInWaterOrBubble()) {
			return logParCoolWallJumpCandidate(player, false, "in_water", null, null, null, null);
		}
		if (player.isFallFlying()) {
			return logParCoolWallJumpCandidate(player, false, "fall_flying", null, null, null, null);
		}
		if (player.getAbilities().flying) {
			return logParCoolWallJumpCandidate(player, false, "creative_flying", null, null, null, null);
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			IStamina stamina = IStamina.get(player);
			if (parkourability == null) {
				return logParCoolWallJumpCandidate(player, false, "missing_parkourability", null, stamina, null, null);
			}
			if (stamina == null) {
				return logParCoolWallJumpCandidate(player, false, "missing_stamina", parkourability, null, null, null);
			}
			if (stamina.isExhausted()) {
				return logParCoolWallJumpCandidate(player, false, "stamina_exhausted", parkourability, stamina, null, null);
			}

			WallJump wallJump = parkourability.get(WallJump.class);
			if (wallJump == null) {
				return logParCoolWallJumpCandidate(player, false, "missing_wall_jump_action", parkourability, stamina, null, null);
			}
			boolean inputDone = wallJump.isInputDone() || allowPreRecorderInput && isParCoolWallJumpPhysicalInputDown();
			if (!inputDone) {
				return logParCoolWallJumpCandidate(player, false, "input_not_done", parkourability, stamina, wallJump, null);
			}

			Vec3 wall = WorldUtil.getWall(player, player.getBbWidth() * 0.65D);
			boolean womWallRunWall = false;
			if (wall == null && allowPreRecorderInput) {
				womWallRunWall = hasWomWallRunWallContact(player);
			}
			if (wall == null && !womWallRunWall) {
				return logParCoolWallJumpCandidate(player, false, "no_parcool_wall", parkourability, stamina, wallJump, null);
			}

			ClingToCliff cling = parkourability.get(ClingToCliff.class);
			boolean clingAllowsWallJump = (!cling.isDoing() && cling.getNotDoingTick() > 3)
					|| (cling.isDoing() && cling.getFacingDirection() != ClingToCliff.FacingDirection.ToWall);
			if (parkourability.getAdditionalProperties().getNotCreativeFlyingTick() <= 10) {
				return logParCoolWallJumpCandidate(player, false, "creative_flying_grace", parkourability, stamina, wallJump, wall);
			}
			if (!clingAllowsWallJump) {
				return logParCoolWallJumpCandidate(player, false, "cling_to_wall", parkourability, stamina, wallJump, wall);
			}
			if (parkourability.get(Crawl.class).isDoing()) {
				return logParCoolWallJumpCandidate(player, false, "crawl_doing", parkourability, stamina, wallJump, wall);
			}
			if (parkourability.get(VerticalWallRun.class).isDoing()) {
				return logParCoolWallJumpCandidate(player, false, "vertical_wall_run_doing", parkourability, stamina, wallJump, wall);
			}
			if (parkourability.get(RideZipline.class).isDoing()) {
				return logParCoolWallJumpCandidate(player, false, "ride_zipline_doing", parkourability, stamina, wallJump, wall);
			}
			if (parkourability.getAdditionalProperties().getNotLandingTick() <= 4) {
				return logParCoolWallJumpCandidate(player, false, "landing_grace", parkourability, stamina, wallJump, wall);
			}
			if (isParCoolWallJumpInCooldown(wallJump, parkourability)) {
				return logParCoolWallJumpCandidate(player, false, "cooldown", parkourability, stamina, wallJump, wall);
			}
			return logParCoolWallJumpCandidate(player, true, allowPreRecorderInput && !wallJump.isInputDone()
					? womWallRunWall ? "ok_preinput_wom_wall" : "ok_preinput"
					: "ok", parkourability, stamina, wallJump, wall);
		} catch (RuntimeException | LinkageError ignored) {
			return logParCoolWallJumpCandidate(player, false, "exception:" + ignored.getClass().getSimpleName(), null, null, null, null);
		}
	}

	private static boolean isParCoolWallJumpPhysicalInputDown() {
		try {
			WallJump.ControlType control = (WallJump.ControlType) ParCoolConfig.Client.WallJumpControl.get();
			if (control == WallJump.ControlType.ReleaseKey) {
				return false;
			}
			return isPhysicalJumpKeyDown() || safeKeyDown(() -> KeyBindings.getKeyWallJump());
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasWomWallRunWallContact(Player player) {
		if (player == null || !WomSpiderWallRunHandler.isWallRunActive(player)) {
			return false;
		}
		Direction activeWall = WomSpiderWallRunHandler.activeWallDirection(player);
		if (activeWall != null) {
			return WomSpiderWallContactResolver.hasAdjacentWallDirection(player, activeWall);
		}
		return WomSpiderWallContactResolver.detectAdjacentWallDirection(player) != null;
	}

	private static boolean logParCoolWallJumpCandidate(Player player, boolean result, String reason, Parkourability parkourability,
			IStamina stamina, WallJump wallJump, Vec3 wall) {
		if (!EPMConfig.debugActionArbitrationState() || player == null || !player.isLocalPlayer()) {
			return result;
		}
		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		if (!jump.jumpDown()) {
			return result;
		}

		Integer previousTick = PARCOOL_WALL_JUMP_CANDIDATE_LOG_TICKS.get(player);
		if (previousTick != null && previousTick.intValue() == player.tickCount) {
			return result;
		}
		PARCOOL_WALL_JUMP_CANDIDATE_LOG_TICKS.put(player, Integer.valueOf(player.tickCount));

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		ClingToCliff cling = parkourability == null ? null : parkourability.get(ClingToCliff.class);
		EPM.LOGGER.info(
				"[EPM/ParCoolWallJumpCandidate] tick={} result={} reason={} jumpDown={} pressSeq={} pressElapsed={} onGround={} inWater={} fallFlying={} flying={} staminaExhausted={} inputDone={} wall={} notCreativeFlyingTick={} notLandingTick={} clingDoing={} clingNotDoingTick={} clingFacing={} crawlDoing={} verticalWallRunDoing={} rideZiplineDoing={} cooldown={} currentAnimation={} delta={} womState={}",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(result),
				reason,
				Boolean.valueOf(jump.jumpDown()),
				Integer.valueOf(jump.pressSequence()),
				Integer.valueOf(jump.pressElapsed()),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(player.isInWaterOrBubble()),
				Boolean.valueOf(player.isFallFlying()),
				Boolean.valueOf(player.getAbilities().flying),
				stamina == null ? "null" : Boolean.valueOf(stamina.isExhausted()),
				wallJump == null ? "null" : Boolean.valueOf(wallJump.isInputDone()),
				wall,
				parkourability == null ? "null" : Integer.valueOf(parkourability.getAdditionalProperties().getNotCreativeFlyingTick()),
				parkourability == null ? "null" : Integer.valueOf(parkourability.getAdditionalProperties().getNotLandingTick()),
				cling == null ? "null" : Boolean.valueOf(cling.isDoing()),
				cling == null ? "null" : Integer.valueOf(cling.getNotDoingTick()),
				cling == null ? "null" : cling.getFacingDirection(),
				parkourability == null ? "null" : Boolean.valueOf(parkourability.get(Crawl.class).isDoing()),
				parkourability == null ? "null" : Boolean.valueOf(parkourability.get(VerticalWallRun.class).isDoing()),
				parkourability == null ? "null" : Boolean.valueOf(parkourability.get(RideZipline.class).isDoing()),
				wallJump == null || parkourability == null ? "null" : Boolean.valueOf(isParCoolWallJumpInCooldown(wallJump, parkourability)),
				assetName(currentBaseAnimation(playerPatch)),
				player.getDeltaMovement(),
				WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch));
		return result;
	}

	private static boolean isParCoolWallJumpInCooldown(WallJump wallJump, Parkourability parkourability) {
		return (parkourability.getClientInfo().get(ParCoolConfig.Client.Booleans.EnableWallJumpCooldown)
				|| !parkourability.getServerLimitation().get(ParCoolConfig.Server.Booleans.AllowDisableWallJumpCooldown))
				&& wallJump.getNotDoingTick() <= 8;
	}

	private static boolean isPhantomJumpPressedLastTick(SkillContainer skillContainer) {
		try {
			Object value = skillContainer.getDataManager().getDataValue(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get());
			return value instanceof Boolean pressed && pressed.booleanValue();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void setPhantomJumpPressedLastTick(SkillContainer skillContainer, boolean pressed) {
		try {
			skillContainer.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.valueOf(pressed));
		} catch (RuntimeException | LinkageError ignored) {
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
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !player.level().isClientSide()) {
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
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !player.level().isClientSide()) {
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
		if (EPMParCoolGate.allowCrossModSkillCompat() && player != null && player.isLocalPlayer()) {
			EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.put(player, Integer.valueOf(player.tickCount));
		}
	}

	public static void restoreClingMoveClimbUpVelocity(Player player) {
		restoreClingMoveClimbUpVelocity(player, false);
	}

	public static void queueNaturalSprinterFastRunDash(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| playerPatch == null
				|| animation == null
				|| !EPMConfig.customFastRunAnimations()) {
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

		queuePendingFastRunDash(playerPatch, animation, NaturalSprinterFastRunDashSource.ORDINARY);
	}
	public static void playNaturalSprinterFastRunStep(PlayerPatch<?> playerPatch, NaturalSprinterFastRunStep step) {
		playNaturalSprinterFastRunStep(playerPatch, step, NaturalSprinterFastRunStep.Trigger.MANUAL);
	}
	public static void playNaturalSprinterFastRunStep(
			PlayerPatch<?> playerPatch,
			NaturalSprinterFastRunStep step,
			NaturalSprinterFastRunStep.Trigger trigger) {
		NaturalSprinterFastRunStep.Trigger safeTrigger = trigger == null ? NaturalSprinterFastRunStep.Trigger.MANUAL : trigger;
		if (step == null || !step.isPresent()) {
			logNaturalSprinterStepPulse("play_skip", "missing_step", playerPatch, step, safeTrigger);
			return;
		}
		if (shouldSuppressDuplicateBreakfallNaturalSprinterStep(playerPatch, step, safeTrigger)) {
			logNaturalSprinterStepPulse("play_skip", "suppress_duplicate_breakfall_step", playerPatch, step, safeTrigger);
			return;
		}

		if (!step.procedural()) {
			if (step.fullEffectsFor(safeTrigger)) {
				String source = naturalSprinterStepEffectSource("configured", safeTrigger);
				if (step.defaultNaturalSprinter()) {
					NaturalSprinterProceduralStepPulse.playStepAfterimageOnly(playerPatch, source);
				} else {
					NaturalSprinterProceduralStepPulse.playStepVisualAndAudioEffects(playerPatch, source);
				}
			}
			logNaturalSprinterStepPulse("play_step_animation", "queued_animation_step", playerPatch, step, safeTrigger);
			queueNaturalSprinterFastRunDash(playerPatch, step.animation());
			return;
		}

		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| playerPatch == null
				|| !EPMConfig.customFastRunAnimations()) {
			logNaturalSprinterStepPulse("play_procedural_skip", "compat_or_config", playerPatch, step, safeTrigger);
			return;
		}

		Player player = playerPatch.getOriginal();
		if (shouldStopFastRunForGlider(player)) {
			logNaturalSprinterStepPulse("play_procedural_skip", "glider", playerPatch, step, safeTrigger);
			suppressFastRunAnimationForGlider(player);
			return;
		}
		if (shouldDelayNaturalSprinterDashForBreakfall(player) || isPhantomAscentAirborneLocked(player)) {
			logNaturalSprinterStepPulse("play_procedural_skip", "breakfall_or_phantom", playerPatch, step, safeTrigger);
			return;
		}

		logNaturalSprinterStepPulse("play_procedural_request", "request", playerPatch, step, safeTrigger);
		if (step.fullEffectsFor(safeTrigger)) {
			NaturalSprinterProceduralStepPulse.playStepEffects(
					playerPatch,
					naturalSprinterStepEffectSource("procedural", safeTrigger),
					step.stepPose());
		} else {
			NaturalSprinterProceduralStepPulse.playCleanStepEffects(
					playerPatch,
					naturalSprinterStepEffectSource("procedural_clean", safeTrigger),
					step.stepPose());
		}
		NaturalSprinterProceduralStepPulse.request(playerPatch, step.proceduralRunAnimation(), step.rightStep(), step.stepPose());
	}

	private static boolean shouldSuppressDuplicateBreakfallNaturalSprinterStep(
			PlayerPatch<?> playerPatch,
			NaturalSprinterFastRunStep step,
			NaturalSprinterFastRunStep.Trigger trigger) {
		if (trigger != NaturalSprinterFastRunStep.Trigger.MANUAL || playerPatch == null || step == null || !step.isPresent()) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		NaturalSprinterStepPlaybackOwner owner = NATURAL_SPRINTER_STEP_PLAYBACK_OWNERS.get(player);
		if (owner == null) {
			return false;
		}
		if (owner.source != NaturalSprinterFastRunDashSource.BREAKFALL_DELAYED_AUTO
				|| player.tickCount - owner.tick > NATURAL_SPRINTER_BREAKFALL_STEP_OWNER_MAX_TICKS) {
			NATURAL_SPRINTER_STEP_PLAYBACK_OWNERS.remove(player);
			return false;
		}
		if (!isSameAnimation(owner.animation, step.animation())) {
			return false;
		}

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (isSameAnimation(currentAnimation, owner.animation)) {
			return true;
		}

		NATURAL_SPRINTER_STEP_PLAYBACK_OWNERS.remove(player);
		return false;
	}

	private static void rememberBreakfallNaturalSprinterStepPlayback(
			Player player,
			AssetAccessor<? extends StaticAnimation> animation) {
		if (player == null || animation == null) {
			return;
		}

		NATURAL_SPRINTER_STEP_PLAYBACK_OWNERS.put(player,
				new NaturalSprinterStepPlaybackOwner(player.tickCount, animation, NaturalSprinterFastRunDashSource.BREAKFALL_DELAYED_AUTO));
	}

	private static String naturalSprinterStepEffectSource(String prefix, NaturalSprinterFastRunStep.Trigger trigger) {
		return prefix + "_" + switch (trigger) {
			case STARTUP -> "startup_step";
			case AUTO_STARTUP -> "auto_startup_step";
			case MANUAL -> "manual_step";
		};
	}

	public static boolean requestNaturalSprinterStepFastRun(Player player, AssetAccessor<? extends StaticAnimation> stepAnimation) {
		return requestNaturalSprinterStepFastRun(player, NaturalSprinterFastRunStep.animation(stepAnimation), "asset_accessor");
	}
	public static boolean requestNaturalSprinterStepFastRun(Player player, NaturalSprinterFastRunStep step) {
		return requestNaturalSprinterStepFastRun(player, step, "unspecified");
	}
	public static boolean requestNaturalSprinterStepFastRun(Player player, NaturalSprinterFastRunStep step, String source) {
		IStamina stamina = player == null ? null : IStamina.get(player);
		if (step == null || !step.isPresent() || !canKeepNaturalSprinterStepFastRun(player, stamina)) {
			logPendingNaturalSprinterStepFastRun("pending_step_fast_run_reject", source, player, step, null);
			clearNaturalSprinterStepFastRun(player);
			return false;
		}

		NATURAL_SPRINTER_STEP_FAST_RUN_STATES.put(player, new NaturalSprinterStepFastRunState(player.tickCount, step));
		logPendingNaturalSprinterStepFastRun("pending_step_fast_run_queue", source, player, step, null);
		setSprintingWithDiagnostic(player, true, "natural_sprinter_step_fast_run_request");
		return true;
	}

	public static boolean playPendingNaturalSprinterStepFastRun(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		NaturalSprinterStepFastRunState state = NATURAL_SPRINTER_STEP_FAST_RUN_STATES.get(player);
		NaturalSprinterFastRunStep step = state == null ? null : state.startupStep;
		if (step == null || !step.isPresent()) {
			return false;
		}
		logPendingNaturalSprinterStepFastRun("pending_step_fast_run_consume_attempt", "play_pending", player, step, state);

		if (player.tickCount - state.startTick > NATURAL_SPRINTER_STEP_FAST_RUN_STARTUP_MAX_TICKS) {
			logPendingNaturalSprinterStepFastRun("pending_step_fast_run_clear", "expired", player, step, state);
			clearNaturalSprinterStepFastRun(player);
			return true;
		}

		if (!canKeepNaturalSprinterStepFastRun(player, IStamina.get(player))
				|| !NaturalSprinterFastRunHandler.consumeFastRunStepBudget(playerPatch, "pending_step_fast_run")) {
			logPendingNaturalSprinterStepFastRun("pending_step_fast_run_clear", "invalid_or_no_budget", player, step, state);
			clearNaturalSprinterStepFastRun(player);
			return true;
		}

		state.startupStep = NaturalSprinterFastRunStep.none();
		logPendingNaturalSprinterStepFastRun("pending_step_fast_run_consume", "play_pending", player, step, state);
		NaturalSprinterFastRunHandler.advanceSprintStepPublic(playerPatch);
		playNaturalSprinterFastRunStep(playerPatch, step, NaturalSprinterFastRunStep.Trigger.MANUAL);
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
				&& state.startupStep.isPresent()
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
				|| !EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.customFastRunAnimations()
				|| !EPMConfig.naturalSprinterManualStep()
				|| stamina == null
				|| player.isSpectator()
				|| player.isDeadOrDying()
				|| !player.onGround()
				|| hasHardVaultFastRunBlocker(player)
				|| shouldStopFastRunForGlider(player)
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
			NaturalSprinterProceduralStepPulse.clear(player, "natural_sprinter_step_fast_run_clear");
		}
	}

	public static void markBreakfallStarted(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.customFastRunAnimations()) {
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

		if (!EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.fastRunVaultChainFix()) {
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
		if (fastRun != null && (fastRun.isDoing() || fastRunGrace || VaultStartFastRunGrace.hasRecent(player))) {
			VAULT_HOLD_FAST_RUN.put(player, Boolean.TRUE);
			markVaultFastRunStartStepSuppressed(player);
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
			markVaultFastRunStartStepSuppressed(player);
		}
	}

	public static boolean shouldAllowFastRunForVaultGrace(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.fastRunVaultChainFix()) {
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
		markVaultFastRunStartStepSuppressed(player);
		return true;
	}

	public static boolean shouldSuppressFastRunStartStepAfterVault(PlayerPatch<?> playerPatch) {
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (player == null || !player.isLocalPlayer() || !EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunStartStepSuppression(player);
			return false;
		}

		if (Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player)) || VAULT_FAST_RUN_GRACE_TICKS.containsKey(player)) {
			markVaultFastRunStartStepSuppressed(player);
			return true;
		}

		Integer expireTick = VAULT_FAST_RUN_START_STEP_SUPPRESS_TICKS.get(player);
		if (expireTick == null) {
			return false;
		}

		if (player.tickCount <= expireTick.intValue()) {
			return true;
		}

		VAULT_FAST_RUN_START_STEP_SUPPRESS_TICKS.remove(player);
		return false;
	}

	public static boolean shouldFinishVaultEarlyForCloseChain(Vault vault, Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.fastRunVaultChainFix()) {
			clearVaultFastRunState(player);
			return false;
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
			return false;
		}

		if (EPMConfig.debugVaultState() && shouldLogVaultTick(VAULT_EARLY_FINISH_LOG_TICKS, player)) {
			Vec3 delta = player.getDeltaMovement();
			EPM.LOGGER.info(
					"[EPM/VaultDebug] phase=synced_early_finish_request tick={} vaultTick={} pos=({}, {}, {}) delta=({}, {}, {})",
					Integer.valueOf(player.tickCount),
					Integer.valueOf(vault.getDoingTick()),
					Double.valueOf(player.getX()),
					Double.valueOf(player.getY()),
					Double.valueOf(player.getZ()),
					Double.valueOf(delta.x()),
					Double.valueOf(delta.y()),
					Double.valueOf(delta.z()));
		}
		clearVaultFastRunHold(player);
		return true;
	}

	public static boolean wasHoldingFastRunDuringVault(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.fastRunVaultChainFix()
				&& (Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player)) || VAULT_FAST_RUN_GRACE_TICKS.containsKey(player));
	}

	public static boolean vaultFastRunHoldForDebug(Player player) {
		return player != null && Boolean.TRUE.equals(VAULT_HOLD_FAST_RUN.get(player));
	}

	public static int vaultFastRunGraceTicksForDebug(Player player) {
		Integer ticks = player == null ? null : VAULT_FAST_RUN_GRACE_TICKS.get(player);
		return ticks == null ? 0 : ticks.intValue();
	}

	public static boolean shouldPreserveFastRunToggleDuringVault(Player player) {
		if (!wasHoldingFastRunDuringVault(player)) {
			return false;
		}

		return !isFastRunPressKeyControl() || isFastRunControlKeyDown();
	}

	public static boolean shouldKeepFastRunDuringVault(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.fastRunVaultChainFix()) {
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
		markVaultFastRunStartStepSuppressed(player);
		return true;
	}

	public static void markAutoSprintAfterWallJump(Player player) {
		if (player == null
				|| !player.isLocalPlayer()
				|| !EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.autoSprintAfterWallJump()
				|| GliderCompat.isGlidingWithActiveGlider(player)
				|| hasHardVaultFastRunBlocker(player)) {
			return;
		}

		WALL_JUMP_AUTO_SPRINT_TICKS.put(player, Integer.valueOf(WALL_JUMP_AUTO_SPRINT_DURATION_TICKS));
		setSprintingWithDiagnostic(player, true, "wall_jump_auto_sprint_mark");
		ensureFastRunAnimator(player);
	}

	public static boolean shouldPreserveFastRunToggleAfterWallJump(Player player, IStamina stamina) {
		return shouldKeepFastRunAfterWallJump(player, stamina);
	}

	public static boolean shouldKeepFastRunAfterWallJump(Player player, IStamina stamina) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			cancelAutoSprintAfterWallJump(player);
			return false;
		}

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
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
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

	public static boolean tryPrepareWallJumpAttackHandoff(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		if (MomentumAirAttackWindowState.canUseBasicAttack(playerPatch)) {
			consumeWallJumpHandoff(player, ParCoolWallJumpHandoffState.Consumer.ATTACK, false);
			return true;
		}
		return cancelWallJumpForAttackInput(player);
	}

	public static boolean cancelWallJumpForAttackInput(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !canUseWallJumpHandoff(player, ParCoolWallJumpHandoffState.Consumer.ATTACK)) {
			return false;
		}

		consumeWallJumpHandoff(player, ParCoolWallJumpHandoffState.Consumer.ATTACK, false);
		return true;
	}

	private static boolean canUseWallJumpHandoff(Player player, ParCoolWallJumpHandoffState.Consumer consumer) {
		if (consumer == ParCoolWallJumpHandoffState.Consumer.ATTACK
				&& MomentumAirAttackWindowState.isInWallJumpWindow(player)) {
			return true;
		}
		if (consumer == ParCoolWallJumpHandoffState.Consumer.ATTACK
				&& ParCoolWallJumpHandoffState.canAttackHandoff(player)) {
			return true;
		}
		return isWallJumpHandoffActiveFallback(player);
	}

	private static boolean isWallJumpHandoffActiveFallback(Player player) {
		if (MomentumAirAttackWindowState.isInWallJumpWindow(player) || ParCoolWallJumpHandoffState.isActive(player)) {
			return true;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			WallJump wallJump = parkourability == null ? null : parkourability.get(WallJump.class);
			if (wallJump != null && wallJump.isDoing()) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		return isCurrentParCoolWallJumpAnimation(player) || MomentumAirAttackWindowState.isInWallJumpWindow(player);
	}

	private static void prepareParCoolWallJumpForNativePhantom(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
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
		clearParCoolAnimator(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			removePendingFastRunDash(localPlayerPatch);
			stopPlaying(localPlayerPatch,
					WomAnimationRefs.epicParCoolWallJumpLeftStart(),
					WomAnimationRefs.epicParCoolWallJumpRightStart(),
					WomAnimationRefs.epicParCoolWallJumpLeft(),
					WomAnimationRefs.epicParCoolWallJumpRight());
			try {
				localPlayerPatch.getClientAnimator().resetMotion(true);
				localPlayerPatch.getClientAnimator().resetCompositeMotion();
				localPlayerPatch.setModelYRot(player.getYRot(), true);
			} catch (RuntimeException | LinkageError ignored) {
			}
		}

		logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_prepare_native_phantom", true, false, false);
	}

	private static void consumeWallJumpHandoff(Player player, ParCoolWallJumpHandoffState.Consumer consumer, boolean stopVerticalBoost) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
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
		clearParCoolAnimator(player);
		PARCOOL_WALL_JUMP_STARTED_TICKS.remove(player);
		PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
		PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.remove(player);
		PARCOOL_WALL_RUN_HANDOFF_TICKS.remove(player);
		WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.remove(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);

		if (stopVerticalBoost) {
			Vec3 movement = player.getDeltaMovement();
			if (movement.y() > 0.0D) {
				player.setDeltaMovement(movement.x(), 0.0D, movement.z());
			}
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			removePendingFastRunDash(localPlayerPatch);
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

		ParCoolWallJumpHandoffState.consume(player, consumer);
	}

	public static void rememberFastRunBeforeTaczShoot(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return;
		}

		TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.put(player, Integer.valueOf(TACZ_SHOOT_FAST_RUN_RESTORE_DURATION_TICKS));
		logGliderFastRunDiagnostic(player, "tacz_restore_remember", true);
	}

	public static boolean shouldStopFastRunForTaczShoot(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
				&& TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(player);
	}

	public static boolean isTaczShootFastRunHandoffActive(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
				&& player.isLocalPlayer()
				&& (TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.containsKey(player)
						|| TACZ_SHOOT_FAST_RUN_RESTORE_TICKS.containsKey(player)
						|| TACZ_SHOOT_ACTIVE.containsKey(player));
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
			removePendingFastRunDash(playerPatch);
		}

		clearParCoolAnimator(player);
	}

	public static boolean shouldPreserveFastRunAfterTaczShoot(Player player, IStamina stamina) {
		return shouldRestoreFastRunAfterTaczShoot(player, stamina);
	}

	public static boolean shouldRestoreFastRunAfterTaczShoot(Player player, IStamina stamina) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			cancelTaczShootFastRunRestore(player);
			return false;
		}

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
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& playerPatch != null
				&& (TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(playerPatch.getOriginal())
				|| TACZ_SHOOT_STOP_FAST_RUN_DASH_SUPPRESS_TICKS.containsKey(playerPatch.getOriginal()));
	}

	public static void markTaczShootActive(Player player) {
		if (EPMParCoolGate.allowCrossModSkillCompat() && player != null && player.isLocalPlayer() && isHoldingTaczGun(player)) {
			TACZ_SHOOT_ACTIVE.put(player, Boolean.TRUE);
		}
	}

	public static void suppressAutoFastRunDashForTaczReload(Player player) {
		if (EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
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
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& ModCompat.isTaczLoaded()
				&& isHoldingTaczGun(player);
	}

	public static boolean cancelWallJumpForTaczAttackInput(Player player) {
		if (player == null || !player.isLocalPlayer() || !EPMConfig.taczShootDuringWallJump() || !isHoldingTaczGun(player)) {
			return false;
		}

		if (!cancelWallJumpForAttackInput(player)) {
			return false;
		}

		MomentumAirAttackWindowState.clearWallJumpWindow(player);
		setSprintingWithDiagnostic(player, false, "cancel_wall_jump_for_tacz_shoot");
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
		if (player == null || !player.isLocalPlayer() || isHoldingPhantomAscentBlockedWeapon(player) || hasPhantomAscentAirAttackWindow(player)) {
			return;
		}

		PhantomAscentAirAttackState.mark(player);
		PhantomAscentCycle cycle = getOrCreatePhantomAscentCycle(player);
		if (cycle != null) {
			cycle.startedTick = player.tickCount;
			cycle.airAttackWindowSent = true;
			cycle.seenAirborne = cycle.seenAirborne || !player.onGround();
		}
		clearGliderOpeningDelayState(player);
		EPMNetwork.sendPhantomAscentAirAttackWindow();
	}

	public static void markNativePhantomAscentStarted(Player player) {
		if (player == null || !player.isLocalPlayer() || isHoldingPhantomAscentBlockedWeapon(player)) {
			return;
		}

		PhantomAscentCycle cycle = getOrCreatePhantomAscentCycle(player);
		PhantomAscentPrimeSource source = cycle == null ? null : cycle.source;
		if (cycle != null) {
			cycle.start(player);
		}
		boolean parCoolWallJumpHandoff = ParCoolWallJumpHandoffState.isActive(player)
				|| isCurrentParCoolWallJumpAnimation(player)
				|| PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(player)
				|| source == PhantomAscentPrimeSource.WALL_JUMP;
		if (!JumpActionArbiter.isClaimedBy(player, JumpActionArbiter.Winner.PHANTOM_ASCENT)) {
			markPhantomAscentConsumedJump(player, "native_start");
		} else {
			logJumpArbiter(player, "phantom_native_start_already_claimed");
		}

		if (parCoolWallJumpHandoff) {
			consumeWallJumpHandoff(player, ParCoolWallJumpHandoffState.Consumer.PHANTOM, false);
		}

		clearGliderOpeningDelayState(player);
		clearParCoolWallJumpPriorityStateAfterPhantom(player);
		if (parCoolWallJumpHandoff) {
			PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.put(player, Integer.valueOf(player.tickCount));
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_handoff_start", true, false, false);
		}
		markPhantomAscentAirAttackWindow(player);
		logJumpArbiter(player, "phantom_native_started");
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
			PhantomAscentCycle cycle = getOrCreatePhantomAscentCycle(player);
			if (cycle != null) {
				cycle.delayedAirAttack = new DelayedAnimatorControl(
						packet.parcoolxwom$action(),
						packet.parcoolxwom$animationId(),
						packet.parcoolxwom$transitionTimeModifier(),
						packet.parcoolxwom$pause(),
						serverPacket.parcoolxwom$layer(),
						serverPacket.parcoolxwom$priority());
			}
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

		boolean phantomQueued = isPhantomAscentPrimed(player);
		boolean realPhantomWindow = isGliderOpeningDelayWindowActive(player) || snapshot.phantomAnimation();
		boolean wallJumpPrime = snapshot.parCoolWallJumpAnimation() || (EPMConfig.spiderWallJumpPrimesPhantomAscent() && snapshot.womBackflipAnimation() && (isPhantomAscentPrimed(player) || canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP)));
		if (!phantomQueued && !realPhantomWindow && !wallJumpPrime) {
			return false;
		}

		logGliderOpeningDelayDiagnostic(player, "toggle_block_phantom_priority", true, true, false);
		if (realPhantomWindow
				&& (isPhantomAscentUsedAirborne(player) || !isParCoolWallJumpPhantomLockActive(player))) {
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

		if (JumpActionArbiter.isClaimedByHigherOrEqual(player, JumpActionArbiter.Winner.GLIDER_TOGGLE)) {
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_higher_priority_claim", true, true, false);
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
		if (ParCoolWallJumpHandoffState.shouldAllowGlider(player)) {
			return false;
		}
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
		if (hasPhantomAscentStarted(player)) {
			return;
		}

		if (GliderFrameState.snapshot(player).phantomAnimation()) {
			PhantomAscentCycle cycle = getOrCreatePhantomAscentCycle(player);
			if (cycle != null) {
				cycle.startedTick = player.tickCount;
				cycle.seenAirborne = cycle.seenAirborne || !player.onGround();
			}
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
		Integer startTick = phantomAscentStartedTick(player);
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
		boolean phantomUsed = isPhantomAscentUsedAirborne(player);
		JumpActionArbiter.Snapshot jumpInput = JumpActionArbiter.snapshot(player);
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
				Boolean.valueOf(jumpInput.jumpDown() && jumpInput.winner() != JumpActionArbiter.Winner.NONE),
				Boolean.valueOf(jumpInput.winner() != JumpActionArbiter.Winner.NONE),
				jumpInput.winner().name(),
				jumpInput.reason(),
				Integer.valueOf(jumpInput.pressSequence()),
				Integer.valueOf(jumpInput.pressStartTick()),
				Integer.valueOf(jumpInput.pressElapsed()),
				Integer.valueOf(jumpInput.winnerTick()),
				Integer.valueOf(jumpInput.winnerElapsed()),
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

		if (isPhantomAscentUsedAirborne(player)) {
			PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_complete", false, false, false);
			return;
		}

		int elapsed = player.tickCount - localStart.intValue();
		boolean invalid = elapsed < 0
				|| elapsed > PARCOOL_WALL_JUMP_PHANTOM_LOCK_MAX_TICKS
				|| player.onGround()
				|| player.isInWater()
				|| !isPhantomAscentPrimed(player) && !isCurrentParCoolWallJumpAnimation(player);
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

	private static void dropClimbUpGliderRequests(Player player, String phase) {
		if (player == null) {
			return;
		}

		boolean arbitrationRemoved = PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player) != null;
		boolean preinputRemoved = PENDING_GLIDER_PREINPUTS.remove(player) != null;
		boolean pendingAfterPhantomRemoved = PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player) != null;
		boolean replayRemoved = GLIDER_REPLAY_REQUESTS.remove(player) != null;
		boolean openingSoundRemoved = PENDING_GLIDER_OPENING_SOUNDS.remove(player) != null;
		if (arbitrationRemoved || preinputRemoved || pendingAfterPhantomRemoved || replayRemoved || openingSoundRemoved) {
			logGliderOpeningDelayDiagnostic(player, phase + "_glider_drop", true, false, false);
		}
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
		boolean allowWallJumpGlider = ParCoolWallJumpHandoffState.shouldAllowGlider(player);
		if (player == null
				|| !player.isLocalPlayer()
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)
				|| !allowWallJumpGlider && (isWallRunToParCoolWallJumpGliderSuppressActive(player)
				|| isPhantomAscentPrimed(player)
				|| isCurrentWallJumpPhantomAscentPrime(player)
				|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP))
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

		boolean allowWallJumpGlider = ParCoolWallJumpHandoffState.shouldAllowGlider(player);
		if (!allowWallJumpGlider && (isPhantomAscentPrimed(player)
				|| isCurrentWallJumpPhantomAscentPrime(player)
				|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
				|| isWallRunToParCoolWallJumpGliderSuppressActive(player))) {
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_drop_blocked", true, false, false);
			return;
		}

		if (!allowWallJumpGlider && (isCurrentParCoolWallJumpAnimation(player)
				|| isParCoolWallRunHandoffGliderBlockActive(player)
				|| isWallMovementAnimationActiveForGlider(player)
				|| GliderCompat.isGlidingWithActiveGlider(player))) {
			return;
		}

		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		consumeWallJumpHandoff(player, ParCoolWallJumpHandoffState.Consumer.GLIDER, false);
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_glider_preinput_replay", false, false, false);
		sendGliderToggleMessage(player);
	}

	private static boolean shouldProtectParCoolWallJumpForPhantom(Player player) {
		if (player == null || !player.isLocalPlayer() || !isParCoolWallJumpPhantomLockActive(player)) {
			return false;
		}
		if (isPhantomAscentUsedAirborne(player)) {
			return false;
		}

		return isPhantomAscentPrimed(player)
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

		return isPhantomAscentPrimed(player)
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
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer() || source == null || !source.enabled()) {
			return false;
		}
		if (isPhantomAscentUsedAirborne(player) || isHoldingPhantomAscentBlockedWeapon(player) || isParCoolHanging(player)) {
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
		if (!EPMConfig.debugGliderState() || player == null || !player.isLocalPlayer()) {
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
		Integer phantomStart = phantomAscentStartedTick(player);
		int phantomElapsed = phantomStart == null ? -1 : player.tickCount - phantomStart.intValue();
		boolean phantomWindow = phantomStart != null && phantomElapsed >= 0 && phantomElapsed < PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS;
		boolean phantomAnimation = playerPatch != null
				&& WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedPhantomAscentForward(), WomAnimationRefs.bipedPhantomAscentBackward());
		boolean phantomPrimeAvailable = canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
		boolean phantomUsed = isPhantomAscentUsedAirborne(player);
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
				Boolean.valueOf(isPhantomAscentPrimed(player)),
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
		Integer startTick = phantomAscentStartedTick(player);
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

		if (JumpActionArbiter.shouldDropQueuedPress(player, queuedTick.intValue())) {
			PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_claimed_press", true, false, false);
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
		if (!JumpActionArbiter.claim(player, JumpActionArbiter.Winner.GLIDER_TOGGLE, "glider_toggle_replay", true)) {
			logGliderOpeningDelayDiagnostic(player, "glider_input_arbiter_drop_claim_reject", true, false, false);
			return;
		}
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
		if (isPhantomAscentPrimed(player)
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
			case PHANTOM_ASCENT -> phantomAscentStartedTick(player);
			case WOM_BACKFLIP -> WOM_BACKFLIP_PHANTOM_LOCK_TICKS.get(player);
			case PARCOOL_WALL_JUMP -> {
				Integer handoffStart = ParCoolWallJumpHandoffState.startedTick(player);
				yield handoffStart != null ? handoffStart : PARCOOL_WALL_JUMP_STARTED_TICKS.get(player);
			}
			case WALLRUN_TO_PARCOOL_WALL_JUMP -> {
				Integer wallRunStart = WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.get(player);
				Integer parcoolStart = ParCoolWallJumpHandoffState.startedTick(player);
				if (parcoolStart == null) {
					parcoolStart = PARCOOL_WALL_JUMP_STARTED_TICKS.get(player);
				}
				yield wallRunStart != null ? wallRunStart : parcoolStart;
			}
			case WALL_MOVEMENT -> LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.get(player);
		};
	}

	private static boolean shouldDropGliderPreinputForPriority(Player player, GliderPreinputSource source) {
		return switch (source) {
			case PHANTOM_ASCENT -> false;
			case WOM_BACKFLIP -> shouldProtectWomBackflipForPhantom(player);
			case PARCOOL_WALL_JUMP, WALLRUN_TO_PARCOOL_WALL_JUMP -> shouldReserveWallJumpHandoffPressForPhantom(player)
					|| !ParCoolWallJumpHandoffState.shouldAllowGlider(player)
					&& (ParCoolWallJumpHandoffState.shouldBlockInitialPress(player)
					|| isPhantomAscentPrimed(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP));
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
		if (pending.source() == GliderPreinputSource.PARCOOL_WALL_JUMP
				|| pending.source() == GliderPreinputSource.WALLRUN_TO_PARCOOL_WALL_JUMP) {
			consumeWallJumpHandoff(player, ParCoolWallJumpHandoffState.Consumer.GLIDER, false);
		}
		GLIDER_REPLAY_REQUESTS.put(player, Boolean.TRUE);
		logGliderOpeningDelayDiagnostic(player, "glider_preinput_replay_" + pending.source().name().toLowerCase(), false, false, false);
		sendGliderToggleMessage(player);
	}

	private static boolean shouldHoldUnifiedGliderPreinput(Player player, GliderPreinputSource source) {
		ensureGliderOpeningDelayWindowFromCurrentAnimation(player);
		return switch (source) {
			case PHANTOM_ASCENT -> isPhantomAscentPrimed(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| isGliderOpeningDelayWindowActive(player)
					|| isWomBackflipStartSuppressActive(player)
					|| isWallMovementAnimationActiveForGlider(player);
			case WOM_BACKFLIP -> isWomBackflipStartSuppressActive(player)
					|| shouldProtectWomBackflipForPhantom(player)
					|| isWallMovementAnimationActiveForGlider(player);
			case PARCOOL_WALL_JUMP -> shouldReserveWallJumpHandoffPressForPhantom(player)
					|| isPhantomAscentPrimed(player)
					|| !ParCoolWallJumpHandoffState.shouldAllowGlider(player)
					&& (ParCoolWallJumpHandoffState.shouldBlockInitialPress(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
					|| isCurrentParCoolWallJumpAnimation(player)
					|| isParCoolWallJumpHandoffSuppressActive(player)
					|| isParCoolWallRunHandoffGliderBlockActive(player)
					|| isWallRunToParCoolWallJumpGliderSuppressActive(player)
					|| isWallMovementAnimationActiveForGlider(player));
			case WALLRUN_TO_PARCOOL_WALL_JUMP -> shouldReserveWallJumpHandoffPressForPhantom(player)
					|| isPhantomAscentPrimed(player)
					|| !ParCoolWallJumpHandoffState.shouldAllowGlider(player)
					&& (ParCoolWallJumpHandoffState.shouldBlockInitialPress(player)
					|| isCurrentWallJumpPhantomAscentPrime(player)
					|| canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)
					|| isWallRunToParCoolWallJumpGliderSuppressActive(player)
					|| isParCoolWallRunHandoffGliderBlockActive(player)
					|| isCurrentParCoolWallJumpAnimation(player)
					|| isWallMovementAnimationActiveForGlider(player));
			case WALL_MOVEMENT -> true;
		};
	}

	private static boolean shouldReserveWallJumpHandoffPressForPhantom(Player player) {
		if (!ParCoolWallJumpHandoffState.shouldAllowPhantom(player)) {
			return false;
		}
		if (!canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.WALL_JUMP)) {
			return false;
		}
		return !JumpActionArbiter.isClaimedByOther(player, JumpActionArbiter.Winner.PHANTOM_ASCENT);
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
		Integer startTick = phantomAscentStartedTick(player);
		if (startTick == null || player.tickCount - startTick.intValue() < PHANTOM_ASCENT_GLIDER_OPENING_DELAY_TICKS
				|| isPhantomAscentPrimed(player)
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
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		DelayedAnimatorControl delayedAttack = cycle == null ? null : cycle.delayedAirAttack;
		if (delayedAttack == null) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch) || player.onGround() || player.isDeadOrDying() || player.isInWater()) {
			cycle.delayedAirAttack = null;
			removePhantomAscentCycleIfEmpty(player, cycle);
			return;
		}

		if (phantomAscentElapsedTicks(player) < PHANTOM_ASCENT_AIR_ATTACK_DELAY_TICKS) {
			return;
		}

		cycle.delayedAirAttack = null;
		removePhantomAscentCycleIfEmpty(player, cycle);
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
		NaturalSprinterFastRunHandler.tickNoWomStaleCombatAnimationRecovery(event.player);
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
			JumpActionArbiter.clear(event.player);
		} else {
			JumpActionArbiter.tick(event.player, isPhysicalJumpKeyDown());
		}

		boolean hasTickWork = hasClientTickWork(event.player);
		boolean shouldProbeSpiderWallJump = shouldProbeSpiderWallJump(event.player);
		if (!hasTickWork && !shouldProbeSpiderWallJump) {
			return;
		}

		updatePhantomAscentAirborneState(event.player);
		clearAirbornePhantomAscentLockIfLanded(event.player);
		tickForcedPhantomAscent(event.player);
		tickJumpActionHandoffInput(event.player);
		if (NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(event.player)) {
			tickNaturalSprinterCatLeap(event.player);
		}
		if (shouldProbeSpiderWallJump) {
			markSpiderWallJumpForPhantomAscent(event.player);
		}
		if (WOM_BACKFLIP_PHANTOM_LOCK_TICKS.containsKey(event.player) || isWomSpiderBackflipDataActive(event.player)) {
			tickWomBackflipPhantomLock(event.player);
		}
		if (isPhantomAscentPrimed(event.player)) {
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
		if (hasDelayedPhantomAscentAirAttack(event.player)) {
			tickDelayedPhantomAscentAirAttack(event.player);
		}
		if (hasPhantomAscentAirAttackSprintSuppression(event.player)) {
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
		return hasPhantomAscentCycle(player)
				|| PENDING_GLIDER_INPUT_ARBITRATION_TICKS.containsKey(player)
				|| PENDING_GLIDER_PREINPUTS.containsKey(player)
				|| PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(player)
				|| PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.containsKey(player)
				|| PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.containsKey(player)
				|| PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(player)
				|| PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.containsKey(player)
				|| PARCOOL_WALL_RUN_HANDOFF_TICKS.containsKey(player)
				|| WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.containsKey(player)
				|| WOM_BACKFLIP_PHANTOM_LOCK_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_CAT_LEAP_TICKS.containsKey(player)
				|| NATURAL_SPRINTER_STEP_FAST_RUN_STATES.containsKey(player)
				|| VAULT_HOLD_FAST_RUN.containsKey(player)
				|| VAULT_FAST_RUN_GRACE_TICKS.containsKey(player)
				|| WALL_JUMP_AUTO_SPRINT_TICKS.containsKey(player)
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
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& ModCompat.isWomLoaded()
				&& EPMConfig.spiderWallJumpPrimesPhantomAscent()
				&& !isPhantomAscentPrimed(player)
				&& !isPhantomAscentUsedAirborne(player)
				&& !player.onGround()
				&& !player.isInWater();
	}

	private static void queuePendingFastRunDash(
			PlayerPatch<?> playerPatch,
			AssetAccessor<? extends StaticAnimation> animation,
			NaturalSprinterFastRunDashSource source) {
		if (playerPatch == null || animation == null) {
			return;
		}

		PENDING_FAST_RUN_DASHES.put(playerPatch, animation);
		if (source == null || source == NaturalSprinterFastRunDashSource.ORDINARY) {
			PENDING_FAST_RUN_DASH_SOURCES.remove(playerPatch);
		} else {
			PENDING_FAST_RUN_DASH_SOURCES.put(playerPatch, source);
		}
	}

	private static AssetAccessor<? extends StaticAnimation> removePendingFastRunDash(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return null;
		}

		PENDING_FAST_RUN_DASH_SOURCES.remove(playerPatch);
		return PENDING_FAST_RUN_DASHES.remove(playerPatch);
	}

	private static void playPendingFastRunDash(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.customFastRunAnimations()) {
			PENDING_FAST_RUN_DASHES.clear();
			PENDING_FAST_RUN_DASH_SOURCES.clear();
			NATURAL_SPRINTER_BREAKFALL_START_TICKS.remove(player);
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.remove(player);
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (shouldStopFastRunForGlider(player)) {
			if (playerPatch != null) {
				removePendingFastRunDash(playerPatch);
			}
			suppressFastRunAnimationForGlider(player);
			return;
		}

		if (isPhantomAscentAirborneLocked(player)) {
			if (playerPatch != null) {
				removePendingFastRunDash(playerPatch);
			}
			return;
		}

		if (playerPatch == null) {
			return;
		}

		NaturalSprinterFastRunDashSource source = PENDING_FAST_RUN_DASH_SOURCES.remove(playerPatch);
		AssetAccessor<? extends StaticAnimation> animation = PENDING_FAST_RUN_DASHES.remove(playerPatch);
		if (animation != null) {
			playerPatch.playAnimationInClientSide(animation, 0.0F);
			if (source == NaturalSprinterFastRunDashSource.BREAKFALL_DELAYED_AUTO) {
				rememberBreakfallNaturalSprinterStepPlayback(player, animation);
			}
		}
	}

	private static void tickBreakfallDelayedNaturalSprinterDash(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.customFastRunAnimations()) {
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
		if (playerPatch != null && !isPhantomAscentAirborneLocked(player)) {
			queuePendingFastRunDash(playerPatch, delayedDash, NaturalSprinterFastRunDashSource.BREAKFALL_DELAYED_AUTO);
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

		AssetAccessor<? extends StaticAnimation> pendingDash = removePendingFastRunDash(playerPatch);
		if (pendingDash != null) {
			NATURAL_SPRINTER_BREAKFALL_DELAYED_DASHES.put(player, pendingDash);
		}
	}

	public static void deferStepAnimForDodge(Player player, AssetAccessor<? extends StaticAnimation> animation) {
		deferStepForDodge(player, animation);
	}

	public static boolean shouldDelayNaturalSprinterStepForDodge(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !isParCoolDodgeBlockingNaturalSprinterStep(player)) {
			return false;
		}

		return true;
	}

	public static boolean isParCoolDodgeBlockingNaturalSprinterStep(Player player) {
		return classifyParCoolStepContext(player) == ParCoolStepContext.DODGE;
	}

	public static boolean isBreakfallFollowupBlockingNaturalSprinterStepDodge(Player player) {
		return classifyParCoolStepContext(player) == ParCoolStepContext.BREAKFALL_FOLLOWUP;
	}

	public static void deferStepForDodge(Player player, AssetAccessor<? extends StaticAnimation> animation) {
		deferStepForDodge(player, NaturalSprinterFastRunStep.animation(animation));
	}
	public static void deferStepForDodge(Player player, NaturalSprinterFastRunStep step) {
		if (player == null || step == null || !step.isPresent()) {
			return;
		}

		DeferredNaturalSprinterDodgeStep state = NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.get(player);
		if (state == null) {
			NATURAL_SPRINTER_DODGE_DEFERRED_STEPS.put(player, new DeferredNaturalSprinterDodgeStep(player.tickCount, step));
		} else {
			state.clearTick = -1;
			state.deferredStep = step;
		}
	}

	private static void tickDeferredDodgeSteps(Player player, DeferredNaturalSprinterDodgeStep state) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.customFastRunAnimations()) {
			clearDeferredDodgeStep(player);
			return;
		}

		if (state == null || state.deferredStep == null || !state.deferredStep.isPresent()) {
			clearDeferredDodgeStep(player);
			return;
		}

		int elapsedTicks = player.tickCount - state.startTick;
		if (isBreakfallFollowupBlockingNaturalSprinterStepDodge(player)) {
			clearDeferredDodgeStep(player);
			logNaturalSprinterStepPulse("dodge_deferred_clear", "breakfall_followup", EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class), state.deferredStep,
					NaturalSprinterFastRunStep.Trigger.MANUAL);
			return;
		}

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
		NaturalSprinterFastRunStep deferred = state.deferredStep;
		if (deferred == null || !deferred.isPresent()) {
			return;
		}

		if (playerPatch == null) {
			return;
		}
		if (!isParCoolFastRunDoing(player)) {
			requestNaturalSprinterStepFastRun(player, deferred, "deferred_dodge_step_not_fastrun");
			return;
		}
		if (!NaturalSprinterFastRunHandler.consumeFastRunStepBudget(playerPatch, "deferred_dodge_step")) {
			return;
		}

		NaturalSprinterFastRunHandler.advanceSprintStepPublic(playerPatch);
		playNaturalSprinterFastRunStep(playerPatch, deferred, NaturalSprinterFastRunStep.Trigger.MANUAL);
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

	private static ParCoolStepContext classifyParCoolStepContext(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return ParCoolStepContext.NONE;
		}

		if (NaturalSprinterFastRunHandler.isParCoolDodgeDoing(player) || hasParCoolDodgeAnimator(player)) {
			return ParCoolStepContext.DODGE;
		}
		if (isBreakfallFollowupDoing(player)) {
			return ParCoolStepContext.BREAKFALL_FOLLOWUP;
		}
		if (hasDodgeRollBaseAnimation(player)) {
			return ParCoolStepContext.DODGE;
		}
		return ParCoolStepContext.NONE;
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

		PhantomAscentCycle cycle = getOrCreatePhantomAscentCycle(player);
		if (cycle == null) {
			return false;
		}

		cycle.prime(source, player);
		logGliderOpeningDelayDiagnostic(player, "phantom_mark_" + source.name().toLowerCase(), true, false, false);
		phantomJumpWasDown = isPhysicalJumpKeyDown();
		return true;
	}

	private static void tickPhantomAscent(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle == null || !cycle.isPrimed()) {
			return;
		}

		int nextTick = cycle.primeElapsed + 1;
		if (!isPhantomAscentPrimeEnabled(player) || isHoldingPhantomAscentBlockedWeapon(player) || isParCoolHanging(player) || nextTick > 80) {
			clearPhantomAscent(player);
			return;
		}

		if (JumpActionArbiter.isClaimedByOther(player, JumpActionArbiter.Winner.PHANTOM_ASCENT)
				|| JumpActionArbiter.isClaimedBy(player, JumpActionArbiter.Winner.PHANTOM_ASCENT)) {
			phantomJumpWasDown = isPhysicalJumpKeyDown();
			cycle.primeElapsed = nextTick;
			return;
		}

		if (isJumpKeyRecentlyPressed()) {
			logJumpArbiter(player, "phantom_native_window_input_seen");
			requestForcedPhantomAscent(player, cycle);
			return;
		}

		cycle.primeElapsed = nextTick;
	}

	private static void tickJumpActionHandoffInput(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| player.onGround()
				|| player.isInWater()
				|| GliderCompat.isGlidingWithActiveGlider(player)) {
			return;
		}

		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		if (!jump.tracked()
				|| !jump.jumpDown()
				|| jump.pressElapsed() < 0
				|| jump.pressElapsed() > 1) {
			return;
		}

		Integer handledSequence = HANDLED_JUMP_HANDOFF_PRESS_SEQUENCES.get(player);
		if (handledSequence != null && handledSequence.intValue() == jump.pressSequence()) {
			return;
		}

		if (tryRoutePhantomGliderJumpHandoff(player, jump)) {
			HANDLED_JUMP_HANDOFF_PRESS_SEQUENCES.put(player, Integer.valueOf(jump.pressSequence()));
		}
	}

	private static boolean tryRoutePhantomGliderJumpHandoff(Player player, JumpActionArbiter.Snapshot jump) {
		if (!isPhantomAscentUsedAirborne(player)
				|| !hasGliderItem(player)
				|| !isCurrentPhantomAscentAnimation(player)
				&& !isGliderOpeningDelayWindowActive(player)
				|| JumpActionArbiter.isClaimedBy(player, JumpActionArbiter.Winner.PHANTOM_ASCENT)
				|| JumpActionArbiter.isClaimedByOther(player, JumpActionArbiter.Winner.GLIDER_TOGGLE)) {
			return false;
		}

		if (PENDING_GLIDER_INPUT_ARBITRATION_TICKS.containsKey(player)
				|| PENDING_GLIDER_PREINPUTS.containsKey(player)
				|| PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(player)
				|| GLIDER_REPLAY_REQUESTS.containsKey(player)) {
			return true;
		}

		boolean cached = cacheUnifiedGliderPreinput(player, GliderPreinputSource.PHANTOM_ASCENT, "jump_handoff_phantom");
		logGliderOpeningDelayDiagnostic(player, cached ? "jump_handoff_phantom_glider_cache" : "jump_handoff_phantom_glider_drop", true, cached, false);
		return true;
	}

	private static boolean hasGliderItem(Player player) {
		try {
			return player != null && !CuriosTrinketsUtil.getInstance().getFirstFoundGlider(player).isEmpty();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void clearPhantomAscent(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		PhantomAscentPrimeSource source = cycle == null ? null : cycle.source;
		boolean usedAirborne = cycle != null && cycle.usedAirborne;
		if (cycle != null) {
			cycle.clearPrime();
			removePhantomAscentCycleIfEmpty(player, cycle);
		}
		if (source == PhantomAscentPrimeSource.WALL_JUMP && !usedAirborne
				&& PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player) != null) {
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_lock_clear", false, false, false);
		}
		phantomJumpWasDown = false;
	}

	private static void requestForcedPhantomAscent(Player player, PhantomAscentCycle cycle) {
		if (player == null || !player.isLocalPlayer() || cycle == null || !cycle.isPrimed()) {
			return;
		}

		PhantomAscentPrimeSource source = cycle.source;
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch) || source == null) {
			clearPhantomAscent(player);
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

		prepareForcedPhantomAscent(player, localPlayerPatch, cycle, source);
		cycle.forcedTriggerTick = player.tickCount;
		cycle.forcedSource = source;
		logJumpArbiter(player, "phantom_forced_trigger_queued");
	}

	private static void tickForcedPhantomAscent(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle == null || cycle.forcedTriggerTick < 0) {
			return;
		}

		int elapsed = player.tickCount - cycle.forcedTriggerTick;
		if (elapsed < 1) {
			return;
		}
		if (elapsed > 3) {
			cycle.forcedTriggerTick = -1;
			cycle.forcedSource = null;
			removePhantomAscentCycleIfEmpty(player, cycle);
			logJumpArbiter(player, "phantom_forced_trigger_expired");
			return;
		}

		PhantomAscentPrimeSource source = cycle.forcedSource == null ? cycle.source : cycle.forcedSource;
		cycle.forcedTriggerTick = -1;
		cycle.forcedSource = null;
		removePhantomAscentCycleIfEmpty(player, cycle);

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			triggerForcedPhantomAscent(player, localPlayerPatch, source);
		}
	}

	private static void prepareForcedPhantomAscent(Player player, LocalPlayerPatch playerPatch, PhantomAscentCycle cycle, PhantomAscentPrimeSource source) {
		cycle.source = source;
		cycle.start(player);
		markPhantomAscentConsumedJump(player, "native_start");
		clearGliderOpeningDelayState(player);
		if (PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(player) || source == PhantomAscentPrimeSource.WALL_JUMP) {
			PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.put(player, Integer.valueOf(player.tickCount));
			logGliderOpeningDelayDiagnostic(player, "parcool_wall_jump_phantom_handoff_start", true, false, false);
		}

		if (source == PhantomAscentPrimeSource.DEMOLITION_LEAP) {
			cancelDemolitionLeapBeforeForcedPhantomAscent(player, playerPatch);
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

	private static void cancelDemolitionLeapBeforeForcedPhantomAscent(Player player, LocalPlayerPatch playerPatch) {
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

	private static void triggerForcedPhantomAscent(Player player, LocalPlayerPatch playerPatch, PhantomAscentPrimeSource source) {
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

	private static void logForcedPhantomAscent(Player player, PlayerPatch<?> playerPatch, PhantomAscentPrimeSource source, String phase) {
		if (!EPMConfig.debugDemolitionLeapState()
				|| source != PhantomAscentPrimeSource.DEMOLITION_LEAP
				|| player == null
				|| !player.isLocalPlayer()) {
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

	private static void markPhantomAscentConsumedJump(Player player, String reason) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		JumpActionArbiter.claim(player, JumpActionArbiter.Winner.PHANTOM_ASCENT, reason, true);
		logGliderOpeningDelayDiagnostic(player, "phantom_consumed_jump_mark_" + reason, true, false, false);
	}

	private static boolean isPhantomAscentConsumedJumpHeld(Player player) {
		return JumpActionArbiter.isClaimedBy(player, JumpActionArbiter.Winner.PHANTOM_ASCENT);
	}

	private static boolean isPhantomAscentConsumedJumpPendingDrop(Player player, int queuedTick) {
		return JumpActionArbiter.shouldDropQueuedPress(player, queuedTick);
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
				&& (isPhantomAscentPrimed(player) || canStartPhantomAscentPrime(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP));

		return parcoolWallJump || womWallJump;
	}

	private static boolean isCurrentParCoolWallJumpAnimation(Player player) {
		return EPMConfig.wallJumpPrimesPhantomAscent() && GliderFrameState.snapshot(player).parCoolWallJumpAnimation();
	}

	private static void clearParCoolWallJumpPriorityStateAfterPhantom(Player player) {
		if (player == null) {
			return;
		}

		PARCOOL_WALL_JUMP_STARTED_TICKS.remove(player);
		PARCOOL_WALL_RUN_HANDOFF_TICKS.remove(player);
		WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.remove(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		ParCoolWallJumpHandoffState.clear(player);
	}

	private static void clearAirbornePhantomAscentLockIfLanded(Player player) {
		if (player == null || !player.onGround()) {
			return;
		}

		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle == null || !cycle.seenAirborne) {
			return;
		}

		playDoubleJumpLandingChoice(player, cycle);
		clearPhantomAscentAirborneCycle(player, "landed");
	}

	private static void playDoubleJumpLandingChoice(Player player, PhantomAscentCycle cycle) {
		if (!isDoubleJumpAnimationReplacementEnabled()
				|| player == null
				|| cycle == null
				|| !cycle.usedAirborne
				|| cycle.playingSelectedLanding
				|| player.isInWater()
				|| player.isDeadOrDying()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (!(playerPatch instanceof LocalPlayerPatch localPlayerPatch) || !playerPatch.isEpicFightMode()) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> land = EpmAnimations.doubleJumpLand();
		AssetAccessor<? extends StaticAnimation> chosen = land != null && player.getRandom().nextBoolean()
				? land
				: Animations.BIPED_LANDING;
		if (chosen == null) {
			return;
		}

		cycle.playingSelectedLanding = true;
		try {
			localPlayerPatch.playAnimationInClientSide(chosen, 0.0F);
		} finally {
			cycle.playingSelectedLanding = false;
		}
	}

	private static void clearPhantomAscentAirborneCycle(Player player, String reason) {
		if (player == null) {
			return;
		}

		if (!EPMConfig.debugActionArbitrationState()) {
			PHANTOM_ASCENT_CYCLES.remove(player);
			JumpActionArbiter.clear(player);
			JUMP_PRIORITY_LOG_TICKS.remove(player);
			PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
			PENDING_GLIDER_PREINPUTS.remove(player);
			PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
			HANDLED_JUMP_HANDOFF_PRESS_SEQUENCES.remove(player);
			PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
			PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
			PARCOOL_WALL_JUMP_INPUT_DOWN.remove(player);
			PARCOOL_WALL_JUMP_STARTED_TICKS.remove(player);
			PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
			PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.remove(player);
			PARCOOL_WALL_RUN_HANDOFF_TICKS.remove(player);
			WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.remove(player);
			ParCoolWallJumpHandoffState.clear(player);
			LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.remove(player);
			WOM_BACKFLIP_PHANTOM_LOCK_TICKS.remove(player);
			clearGliderOpeningDelayState(player);
			phantomJumpWasDown = false;
			return;
		}

		PhantomAscentCycle cycle = phantomAscentCycle(player);
		Integer queuedTicks = cycle == null || !cycle.isPrimed() ? null : Integer.valueOf(cycle.primeElapsed);
		PhantomAscentPrimeSource source = cycle == null ? null : cycle.source;
		boolean queued = queuedTicks != null;
		boolean used = cycle != null && cycle.usedAirborne;
		boolean started = cycle != null && cycle.startedTick >= 0;
		boolean attackWindow = cycle != null && cycle.airAttackWindowSent;
		boolean delayedAttack = cycle != null && cycle.delayedAirAttack != null;
		boolean sprintSuppress = cycle != null && cycle.airAttackSprintSuppressTicks >= 0;
		boolean pendingGliderArbitration = PENDING_GLIDER_INPUT_ARBITRATION_TICKS.containsKey(player);
		boolean pendingGliderPreinput = PENDING_GLIDER_PREINPUTS.containsKey(player);
		boolean pendingGliderAfterPhantom = PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.containsKey(player);
		boolean handledJumpHandoff = HANDLED_JUMP_HANDOFF_PRESS_SEQUENCES.containsKey(player);
		boolean pendingWomGlider = PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.containsKey(player);
		boolean pendingParCoolGlider = PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.containsKey(player);
		boolean parCoolWallJumpInput = PARCOOL_WALL_JUMP_INPUT_DOWN.containsKey(player);
		boolean parCoolWallJumpStart = PARCOOL_WALL_JUMP_STARTED_TICKS.containsKey(player);
		boolean parCoolWallJumpLock = PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.containsKey(player);
		boolean parCoolWallJumpSuppress = PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.containsKey(player);
		boolean parCoolWallRunHandoff = PARCOOL_WALL_RUN_HANDOFF_TICKS.containsKey(player);
		boolean wallRunSuppress = WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.containsKey(player);
		boolean lastWallMovement = LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.containsKey(player);
		boolean womBackflipLock = WOM_BACKFLIP_PHANTOM_LOCK_TICKS.containsKey(player);
		boolean gliderOpeningDelay = GLIDER_OPENING_DELAY_STATES.containsKey(player)
				|| PENDING_GLIDER_OPENING_SOUNDS.containsKey(player);
		JumpActionArbiter.Snapshot jumpSnapshot = JumpActionArbiter.snapshot(player);

		PHANTOM_ASCENT_CYCLES.remove(player);
		JumpActionArbiter.clear(player);
		JUMP_PRIORITY_LOG_TICKS.remove(player);
		PENDING_GLIDER_INPUT_ARBITRATION_TICKS.remove(player);
		PENDING_GLIDER_PREINPUTS.remove(player);
		PENDING_GLIDER_TOGGLE_AFTER_PHANTOM.remove(player);
		HANDLED_JUMP_HANDOFF_PRESS_SEQUENCES.remove(player);
		PENDING_WOM_BACKFLIP_GLIDER_TOGGLE.remove(player);
		PENDING_PARCOOL_WALL_JUMP_GLIDER_TOGGLE.remove(player);
		PARCOOL_WALL_JUMP_INPUT_DOWN.remove(player);
		PARCOOL_WALL_JUMP_STARTED_TICKS.remove(player);
		PARCOOL_WALL_JUMP_PHANTOM_LOCK_TICKS.remove(player);
		PARCOOL_WALL_JUMP_HANDOFF_SUPPRESS_TICKS.remove(player);
		PARCOOL_WALL_RUN_HANDOFF_TICKS.remove(player);
		WALLRUN_TO_PARCOOL_WALL_JUMP_GLIDER_SUPPRESS_TICKS.remove(player);
		ParCoolWallJumpHandoffState.clear(player);
		LAST_WALL_MOVEMENT_FOR_GLIDER_TICKS.remove(player);
		WOM_BACKFLIP_PHANTOM_LOCK_TICKS.remove(player);
		clearGliderOpeningDelayState(player);
		phantomJumpWasDown = false;

		boolean cleared = queued
				|| used
				|| started
				|| attackWindow
				|| delayedAttack
				|| sprintSuppress
				|| pendingGliderArbitration
				|| pendingGliderPreinput
				|| pendingGliderAfterPhantom
				|| handledJumpHandoff
				|| pendingWomGlider
				|| pendingParCoolGlider
				|| parCoolWallJumpInput
				|| parCoolWallJumpStart
				|| parCoolWallJumpLock
				|| parCoolWallJumpSuppress
				|| parCoolWallRunHandoff
				|| wallRunSuppress
				|| lastWallMovement
				|| womBackflipLock
				|| gliderOpeningDelay
				|| jumpSnapshot.tracked();
		if (!cleared) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		EPM.LOGGER.info(
				"[EPM/PhantomCycle] phase=clear reason={} tick={} queued={} queuedTicks={} source={} used={} started={} attackWindow={} delayedAttack={} sprintSuppress={} pendingGliderArbitration={} pendingGliderPreinput={} pendingGliderAfterPhantom={} handledJumpHandoff={} pendingWomGlider={} pendingParCoolGlider={} parCoolWallJumpInput={} parCoolWallJumpStart={} parCoolWallJumpLock={} parCoolWallJumpSuppress={} parCoolWallRunHandoff={} wallRunSuppress={} lastWallMovement={} womBackflipLock={} gliderOpeningDelay={} jumpTracked={} jumpDown={} jumpWinner={} jumpReason={} onGround={} inWater={} animation={} delta={}",
				reason == null ? "unknown" : reason,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(queued),
				queuedTicks,
				source,
				Boolean.valueOf(used),
				Boolean.valueOf(started),
				Boolean.valueOf(attackWindow),
				Boolean.valueOf(delayedAttack),
				Boolean.valueOf(sprintSuppress),
				Boolean.valueOf(pendingGliderArbitration),
				Boolean.valueOf(pendingGliderPreinput),
				Boolean.valueOf(pendingGliderAfterPhantom),
				Boolean.valueOf(handledJumpHandoff),
				Boolean.valueOf(pendingWomGlider),
				Boolean.valueOf(pendingParCoolGlider),
				Boolean.valueOf(parCoolWallJumpInput),
				Boolean.valueOf(parCoolWallJumpStart),
				Boolean.valueOf(parCoolWallJumpLock),
				Boolean.valueOf(parCoolWallJumpSuppress),
				Boolean.valueOf(parCoolWallRunHandoff),
				Boolean.valueOf(wallRunSuppress),
				Boolean.valueOf(lastWallMovement),
				Boolean.valueOf(womBackflipLock),
				Boolean.valueOf(gliderOpeningDelay),
				Boolean.valueOf(jumpSnapshot.tracked()),
				Boolean.valueOf(jumpSnapshot.jumpDown()),
				jumpSnapshot.winner(),
				jumpSnapshot.reason(),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(player.isInWater()),
				assetName(currentBaseAnimation(playerPatch)),
				player.getDeltaMovement());
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
		PhantomAscentCycle cycle = getOrCreatePhantomAscentCycle(player);
		if (cycle != null) {
			cycle.airAttackSprintSuppressTicks = PHANTOM_ASCENT_AIR_ATTACK_SPRINT_SUPPRESS_DURATION_TICKS;
		}
		NaturalSprinterState.suppress(playerPatch);
		removePendingFastRunDash(playerPatch);
		clearVaultFastRunHold(player);
		cancelAutoSprintAfterWallJump(player);
		clearParCoolAnimator(player);
	}

	private static void tickPhantomAscentAirAttackSprintSuppression(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle == null || cycle.airAttackSprintSuppressTicks < 0) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (cycle.airAttackSprintSuppressTicks <= 0 || player.onGround() || player.isDeadOrDying() || player.isInWater()) {
			cycle.airAttackSprintSuppressTicks = -1;
			removePhantomAscentCycleIfEmpty(player, cycle);
			if (playerPatch != null) {
				removePendingFastRunDash(playerPatch);
			}
			return;
		}

		if (playerPatch != null) {
			NaturalSprinterState.suppress(playerPatch);
			removePendingFastRunDash(playerPatch);
		}
		clearVaultFastRunHold(player);
		cancelAutoSprintAfterWallJump(player);
		clearParCoolAnimator(player);
		cycle.airAttackSprintSuppressTicks--;
	}

	private static int phantomAscentElapsedTicks(Player player) {
		Integer startTick = phantomAscentStartedTick(player);
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

	private static void logPendingNaturalSprinterStepFastRun(
			String phase,
			String source,
			Player player,
			NaturalSprinterFastRunStep step,
			NaturalSprinterStepFastRunState state) {
		if (!debugNaturalSprinterStepPulse()) {
			return;
		}

		PlayerPatch<?> playerPatch = player == null ? null : EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		int tick = player == null ? -1 : player.tickCount;
		int startTick = state == null ? -1 : state.startTick;
		int age = startTick < 0 || tick < 0 ? -1 : tick - startTick;
		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase={} source={} tick={} startTick={} age={} stepPresent={} procedural={} rightStep={} defaultNaturalSprinter={} stepAnimation={} proceduralRunAnimation={} currentAnimation={} parCoolFastRunDoing={} customFastRunAnimations={} fastRunStartStepAnimation={} autoFastRunDash={} manualStep={}",
				phase,
				source == null ? "unspecified" : source,
				Integer.valueOf(tick),
				Integer.valueOf(startTick),
				Integer.valueOf(age),
				Boolean.valueOf(step != null && step.isPresent()),
				Boolean.valueOf(step != null && step.procedural()),
				Boolean.valueOf(step != null && step.rightStep()),
				Boolean.valueOf(step != null && step.defaultNaturalSprinter()),
				step == null ? "null" : assetName(step.animation()),
				step == null ? "null" : assetName(step.proceduralRunAnimation()),
				assetName(currentBaseAnimation(playerPatch)),
				Boolean.valueOf(isParCoolFastRunDoing(player)),
				Boolean.valueOf(EPMConfig.customFastRunAnimations()),
				Boolean.valueOf(EPMConfig.fastRunStartStepAnimation()),
				Boolean.valueOf(EPMConfig.autoFastRunDash()),
				Boolean.valueOf(EPMConfig.naturalSprinterManualStep()));
	}

	private static void logNaturalSprinterStepPulse(
			String phase,
			String reason,
			PlayerPatch<?> playerPatch,
			NaturalSprinterFastRunStep step) {
		logNaturalSprinterStepPulse(phase, reason, playerPatch, step, null);
	}

	private static void logNaturalSprinterStepPulse(
			String phase,
			String reason,
			PlayerPatch<?> playerPatch,
			NaturalSprinterFastRunStep step,
			NaturalSprinterFastRunStep.Trigger trigger) {
		if (!debugNaturalSprinterStepPulse()) {
			return;
		}

		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase={} reason={} trigger={} tick={} stepPresent={} procedural={} rightStep={} defaultNaturalSprinter={} startupEffects={} manualEffects={} stepAnimation={} proceduralRunAnimation={} currentAnimation={} elapsed={} customFastRunAnimations={} fastRunStartStepAnimation={} autoFastRunDash={}",
				phase,
				reason,
				trigger == null ? "unspecified" : trigger,
				Integer.valueOf(player == null ? -1 : player.tickCount),
				Boolean.valueOf(step != null && step.isPresent()),
				Boolean.valueOf(step != null && step.procedural()),
				Boolean.valueOf(step != null && step.rightStep()),
				Boolean.valueOf(step != null && step.defaultNaturalSprinter()),
				Boolean.valueOf(step != null && step.startupEffects()),
				Boolean.valueOf(step != null && step.manualEffects()),
				step == null ? "null" : assetName(step.animation()),
				step == null ? "null" : assetName(step.proceduralRunAnimation()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				Boolean.valueOf(EPMConfig.customFastRunAnimations()),
				Boolean.valueOf(EPMConfig.fastRunStartStepAnimation()),
				Boolean.valueOf(EPMConfig.autoFastRunDash()));
	}

	private static boolean debugNaturalSprinterStepPulse() {
		try {
			return EPMConfig.debugNaturalSprinterFastRunStepState();
		} catch (IllegalStateException ignored) {
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
			removePendingFastRunDash(playerPatch);
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

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopPlaying(localPlayerPatch, WomAnimationRefs.bipedSprintJump());
		}

		try {
			playerPatch.getClientAnimator().resetMotion(true);
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

		if (!EPMParCoolGate.allowCrossModSkillCompat() || !EPMConfig.fastRunVaultChainFix() || ticks.intValue() <= 0) {
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

		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| ticks.intValue() <= 0
				|| !EPMConfig.autoSprintAfterWallJump()
				|| hasHardVaultFastRunBlocker(player)) {
			cancelAutoSprintAfterWallJump(player);
			return;
		}

		player.setSprinting(true);
		ensureFastRunAnimator(player);
		WALL_JUMP_AUTO_SPRINT_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void tickTaczShootFastRunSuppression(Player player) {
		Integer ticks = TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			TACZ_SHOOT_FAST_RUN_SUPPRESS_TICKS.remove(player);
			cancelTaczShootFastRunRestore(player);
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

		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			cancelTaczShootFastRunRestore(player);
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
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| player == null
				|| !player.isLocalPlayer()
				|| !isHoldingTaczGun(player)) {
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

		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| ticks.intValue() <= 0
				|| player == null
				|| !player.isLocalPlayer()
				|| !isHoldingTaczGun(player)) {
			TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.remove(player);
			return;
		}

		TACZ_RELOAD_FAST_RUN_DASH_SUPPRESS_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static boolean shouldKeepTaczShootFastRunSuppression(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& player != null
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

	public static boolean isParCoolFastRunDoing(Player player) {
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
		clearVaultFastRunStartStepSuppression(player);
		VaultStartFastRunGrace.clear(player);
		clearVaultLogTicks(player);
	}

	private static void markVaultFastRunStartStepSuppressed(Player player) {
		if (player != null) {
			VAULT_FAST_RUN_START_STEP_SUPPRESS_TICKS.put(
					player,
					Integer.valueOf(player.tickCount + VAULT_FAST_RUN_START_STEP_SUPPRESS_GRACE_TICKS));
		}
	}

	private static void clearVaultFastRunStartStepSuppression(Player player) {
		if (player != null) {
			VAULT_FAST_RUN_START_STEP_SUPPRESS_TICKS.remove(player);
		}
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

	private static void restoreClingMoveClimbUpVelocity(Player player, boolean consume) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			if (consume) {
				EPIC_PARCOOL_CLING_MOVE_CLIMB_UP_TICKS.remove(player);
			}
			return;
		}

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
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| ticks <= 0
				|| velocity <= 0.0D
				|| player == null
				|| !player.isLocalPlayer()) {
			EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.remove(player);
			return;
		}

		EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.put(player, Integer.valueOf(player.tickCount));
	}

	private static void tickEpicParCoolClimbUpAirControl(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			EPIC_PARCOOL_CLIMB_UP_AIR_CONTROL_START_TICKS.remove(player);
			return;
		}

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
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| durationTicks <= 0
				|| velocity <= 0.0D
				|| player == null
				|| !player.isLocalPlayer()
				|| player.onGround()) {
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
		if (!EPMConfig.debugExhaustionPoseState() || player == null || !player.isLocalPlayer()) {
			if (player != null) {
				EXHAUSTION_POSE_SNAPSHOTS.remove(player);
			}
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

	public static boolean hasHardVaultFastRunBlockerForVaultStartGrace(Player player) {
		return hasHardVaultFastRunBlocker(player);
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

	private static PhantomAscentCycle phantomAscentCycle(Player player) {
		return player == null ? null : PHANTOM_ASCENT_CYCLES.get(player);
	}

	private static PhantomAscentCycle getOrCreatePhantomAscentCycle(Player player) {
		if (player == null) {
			return null;
		}

		PhantomAscentCycle cycle = PHANTOM_ASCENT_CYCLES.get(player);
		if (cycle == null) {
			cycle = new PhantomAscentCycle();
			PHANTOM_ASCENT_CYCLES.put(player, cycle);
		}
		return cycle;
	}

	private static boolean hasPhantomAscentCycle(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle != null && cycle.hasState();
	}

	private static boolean isPhantomAscentPrimed(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle != null && cycle.isPrimed();
	}

	private static Integer phantomAscentPrimeTicks(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle == null || !cycle.isPrimed() ? null : Integer.valueOf(cycle.primeElapsed);
	}

	private static PhantomAscentPrimeSource phantomAscentSource(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle == null ? null : cycle.source;
	}

	private static boolean isPhantomAscentUsedAirborne(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle != null && cycle.usedAirborne;
	}

	private static Integer phantomAscentStartedTick(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle == null || cycle.startedTick < 0 ? null : Integer.valueOf(cycle.startedTick);
	}

	private static boolean hasPhantomAscentStarted(Player player) {
		return phantomAscentStartedTick(player) != null;
	}

	private static boolean hasPhantomAscentAirAttackWindow(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle != null && cycle.airAttackWindowSent;
	}

	private static DelayedAnimatorControl delayedPhantomAscentAirAttack(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle == null ? null : cycle.delayedAirAttack;
	}

	private static boolean hasDelayedPhantomAscentAirAttack(Player player) {
		return delayedPhantomAscentAirAttack(player) != null;
	}

	private static boolean hasPhantomAscentAirAttackSprintSuppression(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle != null && cycle.airAttackSprintSuppressTicks >= 0;
	}

	private static void updatePhantomAscentAirborneState(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		if (cycle != null && !player.onGround()) {
			cycle.seenAirborne = true;
		}
	}

	private static void removePhantomAscentCycleIfEmpty(Player player, PhantomAscentCycle cycle) {
		if (player != null && cycle != null && !cycle.hasState()) {
			PHANTOM_ASCENT_CYCLES.remove(player);
		}
	}

	private static boolean isPhantomAscentAirborneLocked(Player player) {
		return player != null && isPhantomAscentUsedAirborne(player) && !player.onGround();
	}

	private static boolean isPhantomAscentPrimeEnabled(Player player) {
		PhantomAscentCycle cycle = phantomAscentCycle(player);
		return cycle != null && cycle.isPrimed() && cycle.source != null && cycle.source.enabled();
	}

	private static boolean isJumpKeyRecentlyPressed() {
		boolean down = isPhysicalJumpKeyDown();
		boolean pressed = down && !phantomJumpWasDown;
		phantomJumpWasDown = down;
		return pressed;
	}

	private static boolean isEpicFightJumpActionPressed() {
		return isPhysicalJumpKeyDown();
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
		CLIMB_UP {
			@Override
			boolean enabled() {
				return EPMConfig.climbUpPrimesPhantomAscent();
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

	private static final class PhantomAscentCycle {
		private PhantomAscentPrimeSource source;
		private int primeTick = -1;
		private int primeElapsed = -1;
		private int startedTick = -1;
		private boolean usedAirborne;
		private boolean seenAirborne;
		private boolean doubleJumpFallPlayed;
		private boolean playingSelectedLanding;
		private boolean airAttackWindowSent;
		private DelayedAnimatorControl delayedAirAttack;
		private int airAttackSprintSuppressTicks = -1;
		private int forcedTriggerTick = -1;
		private PhantomAscentPrimeSource forcedSource;

		private void prime(PhantomAscentPrimeSource source, Player player) {
			this.source = source;
			this.primeTick = player.tickCount;
			this.primeElapsed = 0;
			this.startedTick = -1;
			this.usedAirborne = false;
			this.seenAirborne = !player.onGround();
			this.doubleJumpFallPlayed = false;
			this.playingSelectedLanding = false;
			this.airAttackWindowSent = false;
			this.delayedAirAttack = null;
			this.airAttackSprintSuppressTicks = -1;
			this.forcedTriggerTick = -1;
			this.forcedSource = null;
		}

		private void start(Player player) {
			this.usedAirborne = true;
			this.startedTick = player.tickCount;
			this.primeTick = -1;
			this.primeElapsed = -1;
			this.seenAirborne = this.seenAirborne || !player.onGround();
			this.doubleJumpFallPlayed = false;
			this.playingSelectedLanding = false;
		}

		private boolean isPrimed() {
			return this.source != null && this.primeTick >= 0;
		}

		private void clearPrime() {
			this.source = null;
			this.primeTick = -1;
			this.primeElapsed = -1;
			this.seenAirborne = false;
			this.forcedTriggerTick = -1;
			this.forcedSource = null;
		}

		private boolean hasState() {
			return this.isPrimed()
					|| this.usedAirborne
					|| this.startedTick >= 0
					|| this.airAttackWindowSent
					|| this.delayedAirAttack != null
					|| this.airAttackSprintSuppressTicks >= 0
					|| this.forcedTriggerTick >= 0;
		}
	}

	private static final class NaturalSprinterStepFastRunState {
		private final int startTick;
		private NaturalSprinterFastRunStep startupStep;

		private NaturalSprinterStepFastRunState(int startTick, NaturalSprinterFastRunStep startupStep) {
			this.startTick = startTick;
			this.startupStep = startupStep;
		}
	}

	private static final class DeferredNaturalSprinterDodgeStep {
		private final int startTick;
		private int clearTick = -1;
		private NaturalSprinterFastRunStep deferredStep;

		private DeferredNaturalSprinterDodgeStep(int startTick, NaturalSprinterFastRunStep deferredStep) {
			this.startTick = startTick;
			this.deferredStep = deferredStep;
		}
	}

	private enum NaturalSprinterFastRunDashSource {
		ORDINARY,
		BREAKFALL_DELAYED_AUTO
	}

	private enum ParCoolStepContext {
		NONE,
		DODGE,
		BREAKFALL_FOLLOWUP
	}

	private static final class NaturalSprinterStepPlaybackOwner {
		private final int tick;
		private final AssetAccessor<? extends StaticAnimation> animation;
		private final NaturalSprinterFastRunDashSource source;

		private NaturalSprinterStepPlaybackOwner(
				int tick,
				AssetAccessor<? extends StaticAnimation> animation,
				NaturalSprinterFastRunDashSource source) {
			this.tick = tick;
			this.animation = animation;
			this.source = source;
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
