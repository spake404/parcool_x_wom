package dev.spake404.epm.naturalsprinter;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.WomAnimationRefs;
import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.epicfightx.EpicFightXCombatMasteryCompat;
import dev.spake404.epm.epicfightx.EpicFightXCombatMasteryHandoff;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.input.EPMKeyMappings;
import dev.spake404.epm.mixin.AnimatorAccessor;
import dev.spake404.epm.mixin.ClientAnimatorAccessor;
import dev.spake404.epm.network.EPMNetwork;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.yesman.epicparcool.ParcoolLivingMotions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public final class NaturalSprinterFastRunHandler {
	private static final int FAST_RUN_ANIMATION_REPLAY_COOLDOWN_TICKS = 2;
	private static final float GENERIC_FAST_RUN_STEP_STAMINA_COST = 2.0F;
	private static final WeakHashMap<PlayerPatch<?>, Boolean> FAST_RUN_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> MANUAL_FAST_RUN_KEY_CONSUMED = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Integer> FAST_RUN_ANIMATION_REPLAY_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> NEXT_FAST_RUN_STEP_RIGHT = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, String> LAST_SPRINT_PROFILE_LOG = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Integer> LAST_NO_OVERRIDE_LOG_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, AssetAccessor<? extends StaticAnimation>> LAST_FAST_RUN_TARGET = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, ResourceLocation> LAST_FAST_RUN_ITEM = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, ResourceLocation> LAST_FAST_RUN_WEAPON_TYPE = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Integer> STALE_COMBAT_ANIMATION_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Integer> LAST_STALE_COMBAT_RECOVERY_TICK = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, CachedSprintProfile> SPRINT_PROFILE_CACHE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> MANUAL_FAST_RUN_STEP_KEY_HELD = new WeakHashMap<>();
	private static TaczGunTypeResolver taczGunTypeResolver;
	private static TaczReloadStateResolver taczReloadStateResolver;

	private NaturalSprinterFastRunHandler() {
	}

	public static void registerFastRunAnimation(InitAnimatorEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| !ModCompat.isWomLoaded()
				|| !EPMConfig.naturalSprinterAnimations()) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> barehandSprint = WomAnimationRefs.bipedSprintBarehand();
		if (barehandSprint != null) {
			event.getAnimator().addLivingAnimation(ParcoolLivingMotions.FAST_RUN, barehandSprint);
		}
	}

	public static void chooseFastRunAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			clearFastRunState(event.getPlayerPatch());
			return;
		}

		if (event.getMotion() != ParcoolLivingMotions.FAST_RUN) {
			clearFastRunState(event.getPlayerPatch());
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch.getOriginal();
		if (EPMClientHooks.shouldStopFastRunForGlider(player)) {
			clearFastRunState(playerPatch);
			EPMClientHooks.suppressFastRunAnimationForGlider(player);
			event.setMotion(LivingMotions.FALL);
			return;
		}

		if (!EPMConfig.naturalSprinterAnimations()) {
			clearFastRunState(playerPatch);
			event.setMotion(LivingMotions.RUN);
			return;
		}

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (ModCompat.isWomLoaded() && isWomWallMovementVisual(currentAnimation)) {
			holdFastRunActiveWithoutVisualOverride(playerPatch, currentAnimation);
			return;
		}

		if (EPMClientHooks.isParCoolDodgeBlockingNaturalSprinterStep(playerPatch.getOriginal())) {
			return;
		}

		boolean handledStartupStep = EPMClientHooks.playPendingNaturalSprinterStepFastRun(playerPatch);
		triggerNaturalSprinterDashOnFastRunStart(playerPatch, handledStartupStep);
		applyFastRunAnimation(playerPatch);
	}

	public static boolean tryManualFastRunStep(Player player) {
		return tryManualFastRunStep(player, false);
	}

	public static boolean tryArbitratedFastRunStep(Player player) {
		return tryManualFastRunStep(player, true);
	}

	private static boolean tryManualFastRunStep(Player player, boolean fromStepDodgeConflict) {
		if (!canUseManualNaturalSprinterStep(player)) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		boolean hasPatch = playerPatch != null;
		boolean logicalClient = hasPatch && playerPatch.isLogicalClient();
		if (!hasPatch || !logicalClient) {
			return false;
		}

		NaturalSprinterFastRunStep step = currentSprintStep(playerPatch);
		if (!step.isPresent()) {
			return false;
		}

		boolean fastRunDoing = isParCoolFastRunDoing(player);
		if (!fastRunDoing && !fromStepDodgeConflict) {
			return false;
		}

		if (EPMClientHooks.shouldDelayNaturalSprinterStepForDodge(player)) {
			EPMClientHooks.deferStepForDodge(player, step);
			return true;
		}

		if (fromStepDodgeConflict && !fastRunDoing) {
			return EPMClientHooks.requestNaturalSprinterStepFastRun(player, step);
		}

		if (!consumeFastRunStepBudget(playerPatch, fromStepDodgeConflict ? "manual_deferred_release" : "manual")) {
			return false;
		}

		advanceSprintStep(playerPatch);
		EPMClientHooks.playNaturalSprinterFastRunStep(playerPatch, step, NaturalSprinterFastRunStep.Trigger.MANUAL);
		return true;
	}

	public static boolean canArbitrateStepDodgeConflict(Player player) {
		if (!canUseManualNaturalSprinterStep(player)) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return playerPatch != null
				&& playerPatch.isLogicalClient()
				&& currentSprintStep(playerPatch).isPresent();
	}

	private static boolean canUseManualNaturalSprinterStep(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.naturalSprinterAnimations()
				&& EPMConfig.naturalSprinterManualStep()
				&& canManualFastRunStep(player)
				&& !isTaczReloading(player);
	}

	public static void tickManualFastRunStepKey(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.naturalSprinterAnimations()
				|| !EPMConfig.naturalSprinterManualStep()) {
			MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player);
			return;
		}

		if (NaturalSprinterDodgeStepArbiter.tick(player)) {
			MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player);
			return;
		}

		if (EPMKeyMappings.isNaturalSprinterStepDown()) {
			if (!MANUAL_FAST_RUN_STEP_KEY_HELD.containsKey(player)) {
				MANUAL_FAST_RUN_STEP_KEY_HELD.put(player, Boolean.TRUE);
			}
			return;
		}

		if (Boolean.TRUE.equals(MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player))) {
			if (NaturalSprinterDodgeStepArbiter.shouldUseStepOnlyFallback(player)) {
				tryArbitratedFastRunStep(player);
			} else {
				tryManualFastRunStep(player);
			}
		}
	}

	public static void cancelManualFastRunStepKey(Player player) {
		if (player != null) {
			MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player);
			NaturalSprinterDodgeStepArbiter.clear(player);
		}
	}

	public static void tickNoWomStaleCombatAnimationRecovery(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| ModCompat.isWomLoaded()
				|| player == null
				|| !player.isLocalPlayer()
				|| !EPMConfig.naturalSprinterAnimations()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !playerPatch.isLogicalClient()) {
			return;
		}

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (!isCombatAnimation(currentAnimation) || !currentMotionIsIdleOrInaction(playerPatch) || !currentAnimationHasEnded(playerPatch, currentAnimation)) {
			STALE_COMBAT_ANIMATION_TICKS.remove(playerPatch);
			return;
		}

		int staleTicks = STALE_COMBAT_ANIMATION_TICKS.getOrDefault(playerPatch, Integer.valueOf(0)).intValue() + 1;
		STALE_COMBAT_ANIMATION_TICKS.put(playerPatch, Integer.valueOf(staleTicks));
		if (staleTicks < 2) {
			return;
		}

		int tick = playerTick(playerPatch);
		Integer lastRecoveryTick = LAST_STALE_COMBAT_RECOVERY_TICK.get(playerPatch);
		if (lastRecoveryTick != null && tick - lastRecoveryTick.intValue() < 10) {
			return;
		}

		LAST_STALE_COMBAT_RECOVERY_TICK.put(playerPatch, Integer.valueOf(tick));
		try {
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
			STALE_COMBAT_ANIMATION_TICKS.remove(playerPatch);
			logStaleCombatAnimationRecovery(playerPatch, currentAnimation, staleTicks, "reset_motion");
		} catch (RuntimeException | LinkageError exception) {
			logStaleCombatAnimationRecovery(playerPatch, currentAnimation, staleTicks, "reset_failed:" + exception.getClass().getSimpleName());
		}
	}

	private static void clearFastRunState(PlayerPatch<?> playerPatch) {
		NaturalSprinterProceduralStepPulse.clear(playerPatch);
		FAST_RUN_ACTIVE.remove(playerPatch);
		MANUAL_FAST_RUN_KEY_CONSUMED.remove(playerPatch);
		FAST_RUN_ANIMATION_REPLAY_TICKS.remove(playerPatch);
		LAST_SPRINT_PROFILE_LOG.remove(playerPatch);
		LAST_NO_OVERRIDE_LOG_TICKS.remove(playerPatch);
		LAST_FAST_RUN_TARGET.remove(playerPatch);
		LAST_FAST_RUN_ITEM.remove(playerPatch);
		LAST_FAST_RUN_WEAPON_TYPE.remove(playerPatch);
		STALE_COMBAT_ANIMATION_TICKS.remove(playerPatch);
		SPRINT_PROFILE_CACHE.remove(playerPatch);
	}

	private static void applyFastRunAnimation(PlayerPatch<?> playerPatch) {
		FastRunIdentity identity = fastRunIdentity(playerPatch);
		SprintProfile profile = chooseSprintProfile(playerPatch, identity);
		AssetAccessor<? extends StaticAnimation> animation = profile.animation();
		if (animation == null) {
			NaturalSprinterProceduralStepPulse.configureSustainedFastRunPose(
					playerPatch,
					NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED);
			LAST_FAST_RUN_TARGET.remove(playerPatch);
			LAST_FAST_RUN_ITEM.remove(playerPatch);
			LAST_FAST_RUN_WEAPON_TYPE.remove(playerPatch);
			logFastRunNoOverride(playerPatch, profile);
			return;
		}

		NaturalSprinterProceduralStepPulse.configureSustainedFastRunPose(
				playerPatch,
				profile.customRunAnimation()
						? profile.runPose()
						: NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED);

		syncFastRunMotion(playerPatch, profile, animation, identity);
	}

	private static void syncFastRunMotion(
			PlayerPatch<?> playerPatch,
			SprintProfile profile,
			AssetAccessor<? extends StaticAnimation> animation,
			FastRunIdentity identity) {
		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		AssetAccessor<? extends StaticAnimation> previousTarget = LAST_FAST_RUN_TARGET.get(playerPatch);
		AssetAccessor<? extends StaticAnimation> previousFastRunAnimation = livingAnimation(playerPatch, ParcoolLivingMotions.FAST_RUN);
		ResourceLocation item = identity.item();
		ResourceLocation weaponType = identity.weaponType();
		boolean itemChanged = !sameResourceLocation(item, LAST_FAST_RUN_ITEM.get(playerPatch));
		boolean weaponTypeChanged = !sameResourceLocation(weaponType, LAST_FAST_RUN_WEAPON_TYPE.get(playerPatch));
		AssetAccessor<? extends StaticAnimation> targetAnimation = animation;
		boolean targetChanged = !isSameAnimation(previousTarget, targetAnimation);
		boolean installed = installFastRunLivingAnimation(playerPatch, targetAnimation);
		boolean currentFastRunMotion = currentMotionIs(playerPatch, ParcoolLivingMotions.FAST_RUN);

		if (profile.acceptsCurrentAnimation(currentAnimation) || isSameAnimation(currentAnimation, targetAnimation)) {
			rememberFastRunTarget(playerPatch, targetAnimation, item, weaponType);
			logFastRunAnimationSwap(
					playerPatch,
					"accepted_current",
					profile,
					currentAnimation,
					targetAnimation,
					previousTarget,
					previousFastRunAnimation,
					false,
					installed,
					itemChanged,
					weaponTypeChanged,
					targetChanged,
					currentFastRunMotion);
			return;
		}

		boolean swapped = swapPrimarySprintAnimationPreservingTime(
				playerPatch,
				profile,
				targetAnimation,
				currentAnimation,
				previousTarget,
				previousFastRunAnimation);
		rememberFastRunTarget(playerPatch, targetAnimation, item, weaponType);
		String phase = swapped
				? (isSameAnimationVariant(currentAnimation, targetAnimation) ? "swap_variant_preserve_time" : "swap_preserve_time")
				: "force_target";
		logFastRunAnimationSwap(
				playerPatch,
				phase,
				profile,
				currentAnimation,
				targetAnimation,
				previousTarget,
				previousFastRunAnimation,
				swapped,
				installed,
				itemChanged,
				weaponTypeChanged,
				targetChanged,
				currentFastRunMotion);
		if (!swapped) {
			replayFastRunAnimationIfOverridden(playerPatch, targetAnimation, currentAnimation);
		}
	}

	private static void triggerNaturalSprinterDashOnFastRunStart(PlayerPatch<?> playerPatch, boolean startupStepHandled) {
		if (!EPMConfig.naturalSprinterAnimations()) {
			clearFastRunState(playerPatch);
			return;
		}

		boolean wasFastRunActive = Boolean.TRUE.equals(FAST_RUN_ACTIVE.put(playerPatch, Boolean.TRUE));
		StartupStepSource source = startupStepHandled ? StartupStepSource.NONE : fastRunStartStepSource(playerPatch, wasFastRunActive);
		boolean hasStamina = source.playsStep() && playerPatch.hasStamina(GENERIC_FAST_RUN_STEP_STAMINA_COST);
		logStartupStepDecision(playerPatch, startupStepHandled, wasFastRunActive, source, hasStamina);
		if (!source.playsStep() || !hasStamina) {
			return;
		}

		NaturalSprinterFastRunStep step = nextSprintStep(playerPatch);
		if (!step.isPresent() || !consumeFastRunStepBudget(playerPatch, source.name().toLowerCase(java.util.Locale.ROOT))) {
			return;
		}

		consumeCurrentFastRunKeyPress(playerPatch);
		EPMClientHooks.playNaturalSprinterFastRunStep(playerPatch, step, source.stepTrigger());
	}

	private static StartupStepSource fastRunStartStepSource(PlayerPatch<?> playerPatch, boolean wasFastRunActive) {
		if (!EPMConfig.naturalSprinterAnimations()) {
			return StartupStepSource.NONE;
		}

		if (!EPMConfig.fastRunStartStepAnimation()) {
			return StartupStepSource.NONE;
		}

		if (shouldTriggerCombatMasteryHandoffDash(playerPatch)) {
			return StartupStepSource.COMBAT_MASTERY_HANDOFF;
		}
		if (EpicFightXCombatMasteryCompat.shouldSuppressOrdinaryNaturalSprinterFastRunStart(playerPatch)) {
			return StartupStepSource.COMBAT_MASTERY_ACTIVE_SUPPRESS;
		}
		if (shouldTriggerManualFastRunDash(playerPatch)) {
			return StartupStepSource.MANUAL_FAST_RUN_KEY;
		}
		if (shouldAutoTriggerFastRunDash(playerPatch, wasFastRunActive)) {
			return StartupStepSource.AUTO;
		}
		return StartupStepSource.NONE;
	}

	private static boolean shouldAutoTriggerFastRunDash(PlayerPatch<?> playerPatch, boolean wasFastRunActive) {
		if (!EPMConfig.autoFastRunDash()) {
			return false;
		}

		return !wasFastRunActive && !EPMClientHooks.shouldSuppressAutoFastRunDashForTacz(playerPatch);
	}

	private static boolean shouldTriggerManualFastRunDash(PlayerPatch<?> playerPatch) {
		if (!EPMClientHooks.isFastRunKeyDown()) {
			MANUAL_FAST_RUN_KEY_CONSUMED.remove(playerPatch);
			return false;
		}

		if (!EPMClientHooks.isFastRunKeyRecentlyPressed()) {
			return false;
		}

		return !Boolean.TRUE.equals(MANUAL_FAST_RUN_KEY_CONSUMED.put(playerPatch, Boolean.TRUE));
	}

	private static void consumeCurrentFastRunKeyPress(PlayerPatch<?> playerPatch) {
		if (!EPMClientHooks.isFastRunKeyDown() || !EPMClientHooks.isFastRunKeyRecentlyPressed()) {
			return;
		}

		MANUAL_FAST_RUN_KEY_CONSUMED.put(playerPatch, Boolean.TRUE);
		if (debugStepPulse()) {
			EPM.LOGGER.info(
					"[EPM/NaturalSprinterStepPulse] phase=consume_fast_run_key tick={} reason=startup_step_triggered",
					Integer.valueOf(playerTick(playerPatch)));
		}
	}

	private static boolean shouldTriggerCombatMasteryHandoffDash(PlayerPatch<?> playerPatch) {
		return EpicFightXCombatMasteryHandoff.consumeNaturalSprinterHandoffStep(playerPatch);
	}

	private static boolean canManualFastRunStep(Player player) {
		return player.onGround()
				&& !player.isSpectator()
				&& !player.isDeadOrDying()
				&& !player.isShiftKeyDown()
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& player.getVehicle() == null;
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

	public static boolean isParCoolDodgeDoing(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			Dodge dodge = parkourability == null ? null : parkourability.get(Dodge.class);
			return dodge != null && dodge.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static NaturalSprinterFastRunStep nextSprintStep(PlayerPatch<?> playerPatch) {
		NaturalSprinterFastRunStep step = currentSprintStep(playerPatch);
		advanceSprintStep(playerPatch);
		logStepSelection(playerPatch, "next_step", step);
		return step;
	}

	private static NaturalSprinterFastRunStep currentSprintStep(PlayerPatch<?> playerPatch) {
		SprintProfile profile = chooseSprintProfile(playerPatch, fastRunIdentity(playerPatch));
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));
		if (profile.defaultNaturalSprinter()) {
			NaturalSprinterFastRunStep defaultStep = naturalSprinterDefaultStep(playerPatch, profile, rightStep);
			if (defaultStep.isPresent()) {
				return defaultStep;
			}
		}

		AssetAccessor<? extends StaticAnimation> animation = rightStep ? profile.rightStepAnimation() : profile.leftStepAnimation();
		if (animation != null) {
			return NaturalSprinterFastRunStep.configuredAnimation(animation, profile.startupStepEffects(), profile.manualStepEffects(), rightStep);
		}
		if (profile.proceduralStepPulse()) {
			return NaturalSprinterFastRunStep.procedural(profile.animation(), rightStep);
		}
		return NaturalSprinterFastRunStep.none();
	}

	private static NaturalSprinterFastRunStep naturalSprinterDefaultStep(PlayerPatch<?> playerPatch, SprintProfile profile, boolean rightStep) {
		DefaultSprintFamily currentFamily = defaultSprintFamilyForWomAnimation(currentBaseAnimation(playerPatch));
		DefaultSprintFamily profileFamily = profile == null ? null : defaultSprintFamilyForWomAnimation(profile.animation());
		DefaultSprintFamily family = currentFamily != null ? currentFamily : profileFamily;
		if (family == null && profile != null) {
			family = profile.fallback();
		}
		if (family == null && hasNaturalSprinter(playerPatch)) {
			family = chooseDefaultSprintFamily(playerPatch);
		}
		if (family == null) {
			return NaturalSprinterFastRunStep.none();
		}

		AssetAccessor<? extends StaticAnimation> animation = rightStep ? family.rightStepAnimation() : family.leftStepAnimation();
		NaturalSprinterFastRunStep step = NaturalSprinterFastRunStep.defaultAnimation(animation, rightStep);
		logDefaultStepFallback(playerPatch, "natural_sprinter_default_family", currentFamily, profileFamily, family, profile == null ? null : profile.animation(), step);
		return step;
	}

	private static boolean isRunLikeAnimationForNoOverrideStep(AssetAccessor<?> animation) {
		if (animation == null) {
			return false;
		}
		if (isAnyPrimarySprintAnimation(animation)) {
			return true;
		}

		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		if (registryName == null) {
			return false;
		}

		String path = registryName.getPath().toLowerCase(java.util.Locale.ROOT);
		if (path.contains("step")
				|| path.contains("jump")
				|| path.contains("fall")
				|| path.contains("vault")
				|| path.contains("slide")
				|| path.contains("stop")
				|| path.contains("dodge")
				|| path.contains("roll")) {
			return false;
		}
		return path.contains("run") || path.contains("sprint");
	}

	private static DefaultSprintFamily defaultSprintFamilyForWomAnimation(AssetAccessor<?> animation) {
		if (!ModCompat.isWomLoaded() || animation == null) {
			return null;
		}

		if (WomAnimationRefs.isAny(animation,
				WomAnimationRefs.bipedSprintBarehand(),
				WomAnimationRefs.bipedSprintLeftStepBarehand(),
				WomAnimationRefs.bipedSprintRightStepBarehand())) {
			return DefaultSprintFamily.BAREHAND;
		}
		if (WomAnimationRefs.isAny(animation,
				WomAnimationRefs.bipedSprint(),
				WomAnimationRefs.bipedSprintLeftStep(),
				WomAnimationRefs.bipedSprintRightStep())) {
			return DefaultSprintFamily.WEAPON;
		}

		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		if (registryName == null || !"wom".equals(registryName.getNamespace())) {
			return null;
		}

		String path = registryName.getPath();
		return switch (path) {
			case "biped/skill/biped_sprint_barehand",
					"biped/skill/biped_sprint_left_step_barehand",
					"biped/skill/biped_sprint_right_step_barehand" -> DefaultSprintFamily.BAREHAND;
			case "biped/skill/biped_sprint",
					"biped/skill/biped_sprint_left_step",
					"biped/skill/biped_sprint_right_step" -> DefaultSprintFamily.WEAPON;
			default -> null;
		};
	}

	private static void advanceSprintStep(PlayerPatch<?> playerPatch) {
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));
		NEXT_FAST_RUN_STEP_RIGHT.put(playerPatch, Boolean.valueOf(!rightStep));
	}

	public static void advanceSprintStepPublic(PlayerPatch<?> playerPatch) {
		advanceSprintStep(playerPatch);
	}

	public static boolean consumeFastRunStepBudget(PlayerPatch<?> playerPatch, String reason) {
		if (playerPatch == null || playerPatch.getOriginal() == null) {
			logStepBudget(playerPatch, reason, "none", false, "missing_patch", 0);
			return false;
		}

		if (hasNaturalSprinter(playerPatch)) {
			boolean consumed = NaturalSprinterState.consumeStep(playerPatch);
			logStepBudget(playerPatch, reason, "natural_sprinter", consumed, consumed ? "consumed" : "unavailable", 0);
			return consumed;
		}

		boolean consumed = consumeGenericFastRunStepStamina(playerPatch, true);
		logStepBudget(playerPatch, reason, "epicfight_stamina", consumed, consumed ? "consumed" : "insufficient_stamina", 0);
		return consumed;
	}

	public static boolean consumeGenericFastRunStepStaminaOnServer(PlayerPatch<?> playerPatch) {
		return consumeGenericFastRunStepStamina(playerPatch, false);
	}

	private static boolean consumeGenericFastRunStepStamina(PlayerPatch<?> playerPatch, boolean notifyServer) {
		if (playerPatch == null || playerPatch.getOriginal() == null) {
			return false;
		}
		if (!playerPatch.hasStamina(GENERIC_FAST_RUN_STEP_STAMINA_COST)) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		if (!player.getAbilities().instabuild) {
			try {
				playerPatch.resetActionTick();
				playerPatch.setStamina(Math.max(0.0F, playerPatch.getStamina() - GENERIC_FAST_RUN_STEP_STAMINA_COST));
			} catch (RuntimeException | LinkageError ignored) {
				return false;
			}
		}
		if (notifyServer && playerPatch.isLogicalClient()) {
			EPMNetwork.sendNaturalSprinterFastRunStepStaminaConsumption();
		}
		return true;
	}

	private static void putLivingAnimationSilently(Animator animator, LivingMotion motion, AssetAccessor<? extends StaticAnimation> animation) {
		if (animator instanceof AnimatorAccessor accessor) {
			accessor.parcoolxwom$livingAnimations().put(motion, animation);
		}
	}

	private static boolean installFastRunLivingAnimation(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		AssetAccessor<? extends StaticAnimation> currentFastRunAnimation = livingAnimation(playerPatch, ParcoolLivingMotions.FAST_RUN);
		boolean activeInstalled = !isSameAnimation(currentFastRunAnimation, animation);

		if (activeInstalled) {
			try {
				ClientAnimator animator = playerPatch.getClientAnimator();
				animator.addLivingAnimation(ParcoolLivingMotions.FAST_RUN, animation);
			} catch (RuntimeException | LinkageError ignored) {
				putLivingAnimationSilently(playerPatch == null ? null : playerPatch.getAnimator(), ParcoolLivingMotions.FAST_RUN, animation);
			}
		}

		boolean defaultInstalled = installDefaultFastRunLivingAnimation(playerPatch, animation);
		return activeInstalled || defaultInstalled;
	}

	private static boolean installDefaultFastRunLivingAnimation(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> defaultLivingAnimations = defaultLivingAnimations(playerPatch);
		if (defaultLivingAnimations == null) {
			return false;
		}

		AssetAccessor<? extends StaticAnimation> currentDefault = defaultLivingAnimations.get(ParcoolLivingMotions.FAST_RUN);
		if (isSameAnimation(currentDefault, animation)) {
			return false;
		}

		defaultLivingAnimations.put(ParcoolLivingMotions.FAST_RUN, animation);
		return true;
	}

	private static Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> defaultLivingAnimations(PlayerPatch<?> playerPatch) {
		try {
			ClientAnimator animator = playerPatch == null ? null : playerPatch.getClientAnimator();
			if (animator instanceof ClientAnimatorAccessor accessor) {
				return accessor.parcoolxwom$defaultLivingAnimations();
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static boolean swapPrimarySprintAnimationPreservingTime(
			PlayerPatch<?> playerPatch,
			SprintProfile profile,
			AssetAccessor<? extends StaticAnimation> animation,
			AssetAccessor<?> currentAnimation,
			AssetAccessor<?> previousTarget,
			AssetAccessor<?> previousFastRunAnimation) {
		boolean currentIsFastRunOwned = isAnyPrimarySprintAnimation(currentAnimation)
				|| isSameAnimation(currentAnimation, previousTarget)
				|| isSameAnimation(currentAnimation, previousFastRunAnimation)
				|| isSameAnimationVariant(currentAnimation, previousTarget)
				|| isSameAnimationVariant(currentAnimation, previousFastRunAnimation)
				|| isSameAnimationVariant(currentAnimation, LAST_FAST_RUN_TARGET.get(playerPatch));
		if (!currentIsFastRunOwned || profile.isPrimaryAnimation(currentAnimation)) {
			return false;
		}

		try {
			AnimationPlayer animationPlayer = playerPatch.getClientAnimator().baseLayer.animationPlayer;
			float previousTime = animationPlayer.getPrevElapsedTime();
			float elapsedTime = animationPlayer.getElapsedTime();
			animationPlayer.setPlayAnimation((AssetAccessor<? extends DynamicAnimation>) animation);
			animationPlayer.setElapsedTime(previousTime, elapsedTime);
			return true;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isAnyPrimarySprintAnimation(AssetAccessor<?> animation) {
		return DefaultSprintFamily.isAnyPrimary(animation)
				|| isEpicParCoolFastRun(animation)
				|| NaturalSprinterFastRunAnimationOverrides.isConfiguredRunAnimation(animation);
	}

	private static boolean isEpicParCoolFastRun(AssetAccessor<?> animation) {
		return WomAnimationRefs.isAny(animation, WomAnimationRefs.epicParCoolFastRun());
	}

	private static boolean isFastRunOwnedAnimation(
			PlayerPatch<?> playerPatch,
			AssetAccessor<?> currentAnimation,
			AssetAccessor<?> previousTarget,
			AssetAccessor<?> previousFastRunAnimation) {
		return isAnyPrimarySprintAnimation(currentAnimation)
				|| isSameAnimation(currentAnimation, previousTarget)
				|| isSameAnimation(currentAnimation, previousFastRunAnimation)
				|| isSameAnimation(currentAnimation, LAST_FAST_RUN_TARGET.get(playerPatch))
				|| isSameAnimationVariant(currentAnimation, previousTarget)
				|| isSameAnimationVariant(currentAnimation, previousFastRunAnimation)
				|| isSameAnimationVariant(currentAnimation, LAST_FAST_RUN_TARGET.get(playerPatch));
	}

	private static boolean currentMotionIs(PlayerPatch<?> playerPatch, LivingMotion motion) {
		try {
			LivingMotion currentMotion = playerPatch.getClientAnimator().currentMotion();
			return currentMotion == motion || currentMotion != null && currentMotion.isSame(motion);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean currentMotionIsIdleOrInaction(PlayerPatch<?> playerPatch) {
		try {
			LivingMotion currentMotion = playerPatch.getClientAnimator().currentMotion();
			if (currentMotion == null) {
				return false;
			}

			String motionName = String.valueOf(currentMotion).toUpperCase(java.util.Locale.ROOT);
			return motionName.contains("IDLE") || motionName.contains("INACTION");
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean currentAnimationHasEnded(PlayerPatch<?> playerPatch, AssetAccessor<?> currentAnimation) {
		float elapsedTime = AnimationQuery.currentElapsedTime(playerPatch);
		float totalTime = animationTotalTime(currentAnimation);
		return AnimationQuery.currentAnimationEnded(playerPatch)
				|| totalTime > 0.0F && elapsedTime >= totalTime - 0.02F;
	}

	private static float animationTotalTime(AssetAccessor<?> animation) {
		if (animation == null) {
			return -1.0F;
		}

		try {
			Object value = animation.get();
			return value instanceof DynamicAnimation dynamicAnimation ? dynamicAnimation.getTotalTime() : -1.0F;
		} catch (RuntimeException | LinkageError ignored) {
			return -1.0F;
		}
	}

	private static boolean isCombatAnimation(AssetAccessor<?> animation) {
		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		if (registryName == null) {
			return false;
		}

		String path = registryName.getPath().toLowerCase(java.util.Locale.ROOT);
		return path.contains("/combat/");
	}

	private static void rememberFastRunTarget(
			PlayerPatch<?> playerPatch,
			AssetAccessor<? extends StaticAnimation> animation,
			ResourceLocation item,
			ResourceLocation weaponType) {
		LAST_FAST_RUN_TARGET.put(playerPatch, animation);
		if (item == null) {
			LAST_FAST_RUN_ITEM.remove(playerPatch);
		} else {
			LAST_FAST_RUN_ITEM.put(playerPatch, item);
		}
		if (weaponType == null) {
			LAST_FAST_RUN_WEAPON_TYPE.remove(playerPatch);
		} else {
			LAST_FAST_RUN_WEAPON_TYPE.put(playerPatch, weaponType);
		}
	}

	private static boolean sameResourceLocation(ResourceLocation first, ResourceLocation second) {
		return first == null ? second == null : first.equals(second);
	}

	private static boolean isSameAnimation(AssetAccessor<?> first, AssetAccessor<?> second) {
		if (first == null || second == null) {
			return false;
		}
		if (first.equals(second)) {
			return true;
		}

		ResourceLocation firstRegistryName = AnimationQuery.safeRegistryName(first);
		ResourceLocation secondRegistryName = AnimationQuery.safeRegistryName(second);
		if (firstRegistryName == null || secondRegistryName == null) {
			return false;
		}
		return firstRegistryName.equals(secondRegistryName);
	}

	private static boolean isSameAnimationVariant(AssetAccessor<?> first, AssetAccessor<?> second) {
		if (first == null || second == null || isSameAnimation(first, second)) {
			return false;
		}

		ResourceLocation firstRegistryName = AnimationQuery.safeRegistryName(first);
		ResourceLocation secondRegistryName = AnimationQuery.safeRegistryName(second);
		if (firstRegistryName == null || secondRegistryName == null) {
			return false;
		}
		ResourceLocation normalizedFirst = normalizeAnimationRegistryName(firstRegistryName);
		ResourceLocation normalizedSecond = normalizeAnimationRegistryName(secondRegistryName);
		return normalizedFirst.equals(normalizedSecond);
	}

	private static ResourceLocation normalizeAnimationRegistryName(ResourceLocation animationId) {
		String path = animationId.getPath();
		String multilayerSuffix = "_multilayer";
		if (path.endsWith(multilayerSuffix)) {
			return ResourceLocation.fromNamespaceAndPath(
					animationId.getNamespace(),
					path.substring(0, path.length() - multilayerSuffix.length()));
		}
		return animationId;
	}

	private static void replayFastRunAnimationIfOverridden(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation, AssetAccessor<?> currentAnimation) {
		if (!shouldReplayFastRunTarget(currentAnimation)) {
			return;
		}

		int tick = playerTick(playerPatch);
		Integer lastReplayTick = FAST_RUN_ANIMATION_REPLAY_TICKS.get(playerPatch);
		if (lastReplayTick != null && tick - lastReplayTick.intValue() < FAST_RUN_ANIMATION_REPLAY_COOLDOWN_TICKS) {
			return;
		}

		FAST_RUN_ANIMATION_REPLAY_TICKS.put(playerPatch, Integer.valueOf(tick));
		try {
			playerPatch.playAnimationInClientSide(animation, 0.15F);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static boolean shouldReplayFastRunTarget(AssetAccessor<?> currentAnimation) {
		return isEpicParCoolFastRun(currentAnimation)
				|| isRunLikeAnimationForNoOverrideStep(currentAnimation)
				|| NaturalSprinterFastRunAnimationOverrides.isConfiguredRunAnimation(currentAnimation);
	}

	private static boolean isWomWallMovementVisual(AssetAccessor<?> animation) {
		return WomAnimationRefs.isAny(
				animation,
				WomAnimationRefs.wallRunning(),
				WomAnimationRefs.wallRunLeftSide(),
				WomAnimationRefs.wallRunRightSide(),
				WomAnimationRefs.wallGlide(),
				WomAnimationRefs.wallBackflip());
	}

	private static void holdFastRunActiveWithoutVisualOverride(PlayerPatch<?> playerPatch, AssetAccessor<?> currentAnimation) {
		FAST_RUN_ACTIVE.put(playerPatch, Boolean.TRUE);
		logWomWallMovementVisualHold(playerPatch, currentAnimation);
	}

	private static int playerTick(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch == null || playerPatch.getOriginal() == null ? 0 : playerPatch.getOriginal().tickCount;
		} catch (RuntimeException | LinkageError ignored) {
			return 0;
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		return AnimationQuery.currentAnimation(playerPatch);
	}

	private static FastRunIdentity fastRunIdentity(PlayerPatch<?> playerPatch) {
		ResourceLocation item = mainHandItemId(playerPatch);
		AssetAccessor<? extends StaticAnimation> ordinaryRunAnimation = livingAnimation(playerPatch, LivingMotions.RUN);
		return new FastRunIdentity(
				item,
				NaturalSprinterFastRunAnimationOverrides.weaponTypeForItem(item),
				ordinaryRunAnimation,
				AnimationQuery.safeRegistryName(ordinaryRunAnimation));
	}

	private static SprintProfile chooseSprintProfile(PlayerPatch<?> playerPatch, FastRunIdentity identity) {
		NaturalSprinterFastRunAnimationOverrides.RuleData override = NaturalSprinterFastRunAnimationOverrides.select(
				identity.item(),
				identity.weaponType());
		if (override == null) {
			if (!ModCompat.isWomLoaded()) {
				SprintProfile profile = chooseCachedNoWomSprintProfile(playerPatch, identity, null);
				logSprintProfile(playerPatch, identity, null, null, profile);
				return profile;
			}

			DefaultSprintFamily defaultFamily = chooseDefaultSprintFamily(playerPatch);
			SprintProfile profile = SprintProfile.defaultProfile(defaultFamily);
			logSprintProfile(playerPatch, identity, null, defaultFamily, profile);
			return profile;
		}

		if (!ModCompat.isWomLoaded()) {
			SprintProfile profile = chooseCachedNoWomSprintProfile(playerPatch, identity, override);
			logSprintProfile(playerPatch, identity, override, null, profile);
			return profile;
		}

		DefaultSprintFamily defaultFamily = chooseDefaultSprintFamily(playerPatch);
		DefaultSprintFamily fallback = defaultFamily;
		if (ModCompat.isWomLoaded() && override.fallback() == NaturalSprinterFastRunAnimationOverrides.FallbackFamily.BAREHAND) {
			fallback = DefaultSprintFamily.BAREHAND;
		} else if (ModCompat.isWomLoaded() && override.fallback() == NaturalSprinterFastRunAnimationOverrides.FallbackFamily.WEAPON) {
			fallback = DefaultSprintFamily.WEAPON;
		}
		SprintProfile profile = SprintProfile.custom(override, fallback, playerPatch);
		logSprintProfile(playerPatch, identity, override, fallback, profile);
		return profile;
	}

	private static SprintProfile chooseCachedNoWomSprintProfile(
			PlayerPatch<?> playerPatch,
			FastRunIdentity identity,
			NaturalSprinterFastRunAnimationOverrides.RuleData override) {
		if (override == null && isStaleNoWomWeaponRunFallback(playerPatch, identity)) {
			SPRINT_PROFILE_CACHE.remove(playerPatch);
			SprintProfile profile = SprintProfile.defaultFastRun(defaultFastRunAnimation());
			logStaleNoWomWeaponRunFallback(playerPatch, identity, profile);
			return profile;
		}

		SprintProfileCacheKey key = new SprintProfileCacheKey(
				identity.item(),
				identity.weaponType(),
				identity.ordinaryRunAnimationId(),
				EPMConfig.noWomProceduralWeaponFastRun(),
				override);
		CachedSprintProfile cached = SPRINT_PROFILE_CACHE.get(playerPatch);
		if (cached != null && cached.key().equals(key)) {
			return cached.profile();
		}

		SprintProfile profile = override == null
				? noWomUnmatchedSprintProfile(identity)
				: SprintProfile.custom(override, null, playerPatch);
		if (profile.animation() == null) {
			profile = SprintProfile.defaultFastRun(defaultFastRunAnimation());
		}

		SPRINT_PROFILE_CACHE.put(playerPatch, new CachedSprintProfile(key, profile));
		return profile;
	}

	private static boolean isStaleNoWomWeaponRunFallback(PlayerPatch<?> playerPatch, FastRunIdentity identity) {
		if (identity == null || identity.ordinaryRunAnimation() == null) {
			return false;
		}

		boolean itemChanged = !sameResourceLocation(identity.item(), LAST_FAST_RUN_ITEM.get(playerPatch));
		boolean weaponTypeChanged = !sameResourceLocation(identity.weaponType(), LAST_FAST_RUN_WEAPON_TYPE.get(playerPatch));
		if (!itemChanged && !weaponTypeChanged) {
			return false;
		}

		AssetAccessor<? extends StaticAnimation> previousTarget = LAST_FAST_RUN_TARGET.get(playerPatch);
		AssetAccessor<? extends StaticAnimation> previousFastRunAnimation = livingAnimation(playerPatch, ParcoolLivingMotions.FAST_RUN);
		return isSameAnimation(identity.ordinaryRunAnimation(), previousTarget)
				|| isSameAnimationVariant(identity.ordinaryRunAnimation(), previousTarget)
				|| isSameAnimation(identity.ordinaryRunAnimation(), previousFastRunAnimation)
				|| isSameAnimationVariant(identity.ordinaryRunAnimation(), previousFastRunAnimation);
	}

	private static SprintProfile noWomUnmatchedSprintProfile(FastRunIdentity identity) {
		if (EPMConfig.noWomProceduralWeaponFastRun()) {
			SprintProfile profile = SprintProfile.weaponRunFallback(
					identity,
					null,
					true,
					false,
					NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DEFAULT,
					0);
			if (profile.animation() != null) {
				return profile;
			}
		}
		return SprintProfile.defaultFastRun(defaultFastRunAnimation());
	}

	private static AssetAccessor<? extends StaticAnimation> defaultFastRunAnimation() {
		return WomAnimationRefs.epicParCoolFastRun();
	}

	private static AssetAccessor<? extends StaticAnimation> ordinaryRunAnimation(PlayerPatch<?> playerPatch) {
		AssetAccessor<? extends StaticAnimation> livingRun = livingAnimation(playerPatch, LivingMotions.RUN);
		return isUsableOrdinaryRunAnimation(livingRun) ? livingRun : null;
	}

	private static AssetAccessor<? extends StaticAnimation> ordinaryRunAnimation(FastRunIdentity identity) {
		return identity != null && isUsableOrdinaryRunAnimation(identity.ordinaryRunAnimation())
				? identity.ordinaryRunAnimation()
				: null;
	}

	private static AssetAccessor<? extends StaticAnimation> livingAnimation(PlayerPatch<?> playerPatch, LivingMotion motion) {
		try {
			if (playerPatch != null && playerPatch.getAnimator() instanceof AnimatorAccessor accessor) {
				return accessor.parcoolxwom$livingAnimations().get(motion);
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return null;
	}

	private static boolean isUsableOrdinaryRunAnimation(AssetAccessor<?> animation) {
		if (animation == null || isEpicParCoolFastRun(animation) || !isRunLikeAnimationForNoOverrideStep(animation)) {
			return false;
		}

		try {
			return animation.get() instanceof StaticAnimation;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static DefaultSprintFamily chooseDefaultSprintFamily(PlayerPatch<?> playerPatch) {
		DefaultSprintFamily taczFamily = chooseTaczSprintFamily(playerPatch);
		if (taczFamily != null) {
			return taczFamily;
		}

		CapabilityItem mainHand = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		String categoryName = categoryName(mainHand.getWeaponCategory());

		if ("FIST".equals(categoryName)
				|| "DAGGER".equals(categoryName)
				|| "ENDERBLASTER".equals(categoryName)
				|| "HOE".equals(categoryName)
				|| "AXE".equals(categoryName)
				|| "PICKAXE".equals(categoryName)
				|| "SHOVEL".equals(categoryName)
				|| "NOT_WEAPON".equals(categoryName)) {
			return DefaultSprintFamily.BAREHAND;
		}

		return WomAnimationRefs.isMoonlessCollider(mainHand.getWeaponCollider()) ? DefaultSprintFamily.BAREHAND : DefaultSprintFamily.WEAPON;
	}

	private static DefaultSprintFamily chooseTaczSprintFamily(PlayerPatch<?> playerPatch) {
		if (!ModCompat.isTaczLoaded() || playerPatch == null || playerPatch.getOriginal() == null) {
			return null;
		}

		String gunType = taczGunType(playerPatch.getOriginal().getMainHandItem());
		if (gunType == null) {
			return null;
		}

		return EPMConfig.isTaczBarehandSprintType(gunType) ? DefaultSprintFamily.BAREHAND : DefaultSprintFamily.WEAPON;
	}

	private static ResourceLocation mainHandItemId(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getOriginal() == null) {
			return null;
		}

		return NaturalSprinterFastRunAnimationOverrides.itemId(playerPatch.getOriginal().getMainHandItem());
	}

	private static ResourceLocation weaponTypeId(PlayerPatch<?> playerPatch) {
		return NaturalSprinterFastRunAnimationOverrides.weaponTypeForItem(mainHandItemId(playerPatch));
	}

	private static String taczGunType(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}

		try {
			if (taczGunTypeResolver == null) {
				taczGunTypeResolver = new TaczGunTypeResolver();
			}
			return taczGunTypeResolver.type(stack);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isTaczReloading(Player player) {
		if (!ModCompat.isTaczLoaded() || player == null) {
			return false;
		}

		try {
			if (taczReloadStateResolver == null) {
				taczReloadStateResolver = new TaczReloadStateResolver();
			}
			return taczReloadStateResolver.isReloading(player);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static String categoryName(Object category) {
		return category instanceof Enum<?> enumCategory ? enumCategory.name() : String.valueOf(category);
	}

	private static boolean hasNaturalSprinter(PlayerPatch<?> playerPatch) {
		return ModCompat.isWomLoaded() && NaturalSprinterState.hasNaturalSprinter(playerPatch);
	}

	private static void logStartupStepDecision(
			PlayerPatch<?> playerPatch,
			boolean startupStepHandled,
			boolean wasFastRunActive,
			StartupStepSource source,
			boolean hasStamina) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=start_check tick={} startupStepHandled={} wasFastRunActive={} source={} shouldPlay={} hasStamina={} fastRunKeyDown={} fastRunKeyRecent={} currentAnimation={} elapsed={} item={} weaponType={}",
				Integer.valueOf(playerTick(playerPatch)),
				Boolean.valueOf(startupStepHandled),
				Boolean.valueOf(wasFastRunActive),
				source,
				Boolean.valueOf(source.playsStep()),
				Boolean.valueOf(hasStamina),
				Boolean.valueOf(EPMClientHooks.isFastRunKeyDown()),
				Boolean.valueOf(EPMClientHooks.isFastRunKeyRecentlyPressed()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				mainHandItemId(playerPatch),
				weaponTypeId(playerPatch));
	}

	private static void logStepSelection(PlayerPatch<?> playerPatch, String phase, NaturalSprinterFastRunStep step) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase={} tick={} stepPresent={} procedural={} rightStep={} defaultNaturalSprinter={} stepAnimation={} proceduralRunAnimation={} currentAnimation={} elapsed={}",
				phase,
				Integer.valueOf(playerTick(playerPatch)),
				Boolean.valueOf(step != null && step.isPresent()),
				Boolean.valueOf(step != null && step.procedural()),
				Boolean.valueOf(step != null && step.rightStep()),
				Boolean.valueOf(step != null && step.defaultNaturalSprinter()),
				step == null ? "null" : assetName(step.animation()),
				step == null ? "null" : assetName(step.proceduralRunAnimation()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
	}

	private static void logDefaultStepFallback(
			PlayerPatch<?> playerPatch,
			String reason,
			DefaultSprintFamily currentFamily,
			DefaultSprintFamily profileFamily,
			DefaultSprintFamily family,
			AssetAccessor<?> profileRunAnimation,
			NaturalSprinterFastRunStep step) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=no_override_default_step_fallback tick={} reason={} currentFamily={} profileFamily={} selectedFamily={} stepPresent={} rightStep={} profileRun={} stepAnimation={} currentAnimation={} elapsed={} item={} weaponType={} hasNaturalSprinter={}",
				Integer.valueOf(playerTick(playerPatch)),
				reason,
				currentFamily,
				profileFamily,
				family,
				Boolean.valueOf(step != null && step.isPresent()),
				Boolean.valueOf(step != null && step.rightStep()),
				assetName(profileRunAnimation),
				step == null ? "null" : assetName(step.animation()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				mainHandItemId(playerPatch),
				weaponTypeId(playerPatch),
				Boolean.valueOf(hasNaturalSprinter(playerPatch)));
	}

	private static void logSprintProfile(
			PlayerPatch<?> playerPatch,
			FastRunIdentity identity,
			NaturalSprinterFastRunAnimationOverrides.RuleData rule,
			DefaultSprintFamily fallback,
			SprintProfile profile) {
		if (!debugStepPulse()) {
			return;
		}

		String signature = (rule == null ? "default" : String.valueOf(rule.id()))
				+ "|" + fallback
				+ "|" + assetName(profile.animation())
				+ "|" + assetName(profile.leftStepAnimation())
				+ "|" + assetName(profile.rightStepAnimation())
				+ "|" + defaultSprintFamilyForWomAnimation(currentBaseAnimation(playerPatch))
				+ "|" + defaultSprintFamilyForWomAnimation(profile.animation())
				+ "|" + profile.animationSetPriority()
				+ "|" + profile.startupStepEffects()
				+ "|" + profile.manualStepEffects()
				+ "|" + profile.runPose().enabled()
				+ "|" + profile.runPose().scale()
				+ "|" + profile.runPose().blendTicks()
				+ "|" + profile.proceduralStepPulse()
				+ "|" + profile.weaponRunFallback();
		if (signature.equals(LAST_SPRINT_PROFILE_LOG.get(playerPatch))) {
			return;
		}

		LAST_SPRINT_PROFILE_LOG.put(playerPatch, signature);
		String source = rule == null
				? (profile.animation() == null ? "no_override" : (profile.weaponRunFallback() ? "weapon_run_fallback" : "default"))
				: (profile.weaponRunFallback()
						? "weapon_run_fallback"
						: (!profile.customRunAnimation() && !profile.defaultNaturalSprinter() ? "default_fast_run" : "datapack"));
		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=profile tick={} source={} rule={} mainPriority={} animationPriority={} item={} weaponType={} hasNaturalSprinter={} womLoaded={} overrideApplied={} fallback={} currentFamily={} profileRunFamily={} run={} leftStep={} rightStep={} startupEffects={} manualEffects={} runPose=(enabled={}, scale={}, blendTicks={}) procedural={} weaponRunFallback={}",
				Integer.valueOf(playerTick(playerPatch)),
				source,
				rule == null ? "null" : rule.id(),
				Integer.valueOf(rule == null ? 0 : rule.mainPriority()),
				Integer.valueOf(profile.animationSetPriority()),
				identity == null ? null : identity.item(),
				identity == null ? null : identity.weaponType(),
				Boolean.valueOf(hasNaturalSprinter(playerPatch)),
				Boolean.valueOf(ModCompat.isWomLoaded()),
				Boolean.valueOf(profile.animation() != null),
				fallback,
				defaultSprintFamilyForWomAnimation(currentBaseAnimation(playerPatch)),
				defaultSprintFamilyForWomAnimation(profile.animation()),
				assetName(profile.animation()),
				assetName(profile.leftStepAnimation()),
				assetName(profile.rightStepAnimation()),
				Boolean.valueOf(profile.startupStepEffects()),
				Boolean.valueOf(profile.manualStepEffects()),
				Boolean.valueOf(profile.runPose().enabled()),
				Float.valueOf(profile.runPose().scale()),
				Float.valueOf(profile.runPose().blendTicks()),
				Boolean.valueOf(profile.proceduralStepPulse()),
				Boolean.valueOf(profile.weaponRunFallback()));
	}

	private static void logFastRunAnimationSwap(
			PlayerPatch<?> playerPatch,
			String phase,
			SprintProfile profile,
			AssetAccessor<?> currentAnimation,
			AssetAccessor<? extends StaticAnimation> targetAnimation,
			AssetAccessor<?> previousTarget,
			AssetAccessor<?> previousFastRunAnimation,
			boolean swapped,
			boolean installed,
			boolean itemChanged,
			boolean weaponTypeChanged,
			boolean targetChanged,
			boolean currentFastRunMotion) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase={} tick={} currentAnimation={} targetRun={} previousTarget={} previousFastRun={} customRun={} fallback={} primaryCurrent={} ownedCurrent={} acceptedCurrent={} installed={} swapped={} itemChanged={} weaponTypeChanged={} targetChanged={} currentFastRunMotion={} elapsed={} item={} weaponType={}",
				phase,
				Integer.valueOf(playerTick(playerPatch)),
				assetName(currentAnimation),
				assetName(targetAnimation),
				assetName(previousTarget),
				assetName(previousFastRunAnimation),
				Boolean.valueOf(profile.customRunAnimation()),
				profile.fallback(),
				Boolean.valueOf(isAnyPrimarySprintAnimation(currentAnimation)),
				Boolean.valueOf(isFastRunOwnedAnimation(playerPatch, currentAnimation, previousTarget, previousFastRunAnimation)),
				Boolean.valueOf(profile.acceptsCurrentAnimation(currentAnimation) || isSameAnimation(currentAnimation, targetAnimation)),
				Boolean.valueOf(installed),
				Boolean.valueOf(swapped),
				Boolean.valueOf(itemChanged),
				Boolean.valueOf(weaponTypeChanged),
				Boolean.valueOf(targetChanged),
				Boolean.valueOf(currentFastRunMotion),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				mainHandItemId(playerPatch),
				weaponTypeId(playerPatch));
	}

	private static void logStaleNoWomWeaponRunFallback(PlayerPatch<?> playerPatch, FastRunIdentity identity, SprintProfile profile) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=stale_no_wom_weapon_run_fallback tick={} item={} weaponType={} ordinaryRun={} previousTarget={} previousFastRun={} fallbackRun={} currentAnimation={} elapsed={}",
				Integer.valueOf(playerTick(playerPatch)),
				identity == null ? null : identity.item(),
				identity == null ? null : identity.weaponType(),
				assetName(identity == null ? null : identity.ordinaryRunAnimation()),
				assetName(LAST_FAST_RUN_TARGET.get(playerPatch)),
				assetName(livingAnimation(playerPatch, ParcoolLivingMotions.FAST_RUN)),
				assetName(profile == null ? null : profile.animation()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
	}

	private static void logFastRunNoOverride(PlayerPatch<?> playerPatch, SprintProfile profile) {
		if (!debugStepPulse()) {
			return;
		}

		int tick = playerTick(playerPatch);
		Integer lastLogTick = LAST_NO_OVERRIDE_LOG_TICKS.get(playerPatch);
		if (lastLogTick != null && tick - lastLogTick.intValue() < 20) {
			return;
		}
		LAST_NO_OVERRIDE_LOG_TICKS.put(playerPatch, Integer.valueOf(tick));

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=no_override tick={} item={} weaponType={} currentAnimation={} elapsed={} hasNaturalSprinter={} womLoaded={} procedural={} reason=no_matching_datapack_or_failed_animation_sets",
				Integer.valueOf(tick),
				mainHandItemId(playerPatch),
				weaponTypeId(playerPatch),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				Boolean.valueOf(hasNaturalSprinter(playerPatch)),
				Boolean.valueOf(ModCompat.isWomLoaded()),
				Boolean.valueOf(profile.proceduralStepPulse()));
	}

	private static void logStaleCombatAnimationRecovery(
			PlayerPatch<?> playerPatch,
			AssetAccessor<?> currentAnimation,
			int staleTicks,
			String result) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=stale_combat_recovery tick={} result={} staleTicks={} currentAnimation={} elapsed={} total={} ended={} item={} weaponType={} currentMotionIdleOrInaction={}",
				Integer.valueOf(playerTick(playerPatch)),
				result,
				Integer.valueOf(staleTicks),
				assetName(currentAnimation),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				Float.valueOf(AnimationQuery.currentAnimationTotalTime(playerPatch)),
				Boolean.valueOf(AnimationQuery.currentAnimationEnded(playerPatch)),
				mainHandItemId(playerPatch),
				weaponTypeId(playerPatch),
				Boolean.valueOf(currentMotionIsIdleOrInaction(playerPatch)));
	}

	private static void logStepBudget(PlayerPatch<?> playerPatch, String reason, String budget, boolean consumed, String result, int remainingCooldownTicks) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=step_budget tick={} reason={} budget={} consumed={} result={} remainingCooldownTicks={} item={} weaponType={} hasNaturalSprinter={} womLoaded={} currentAnimation={} elapsed={}",
				Integer.valueOf(playerTick(playerPatch)),
				reason,
				budget,
				Boolean.valueOf(consumed),
				result,
				Integer.valueOf(remainingCooldownTicks),
				mainHandItemId(playerPatch),
				weaponTypeId(playerPatch),
				Boolean.valueOf(hasNaturalSprinter(playerPatch)),
				Boolean.valueOf(ModCompat.isWomLoaded()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
	}

	private static void logWomWallMovementVisualHold(PlayerPatch<?> playerPatch, AssetAccessor<?> currentAnimation) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=hold_wom_wall_movement_visuals tick={} currentAnimation={} elapsed={} fastRunKeyDown={} fastRunKeyRecent={}",
				Integer.valueOf(playerTick(playerPatch)),
				assetName(currentAnimation),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				Boolean.valueOf(EPMClientHooks.isFastRunKeyDown()),
				Boolean.valueOf(EPMClientHooks.isFastRunKeyRecentlyPressed()));
	}

	private static boolean debugStepPulse() {
		try {
			return EPMConfig.debugNaturalSprinterFastRunStepState();
		} catch (IllegalStateException ignored) {
			return false;
		}
	}

	private static String assetName(AssetAccessor<?> animation) {
		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		return registryName == null ? String.valueOf(animation) : registryName.toString();
	}

	private static final class TaczGunTypeResolver {
		private final Class<?> gunClass;
		private final Method getGunId;
		private final Method getCommonGunIndex;
		private final Method getType;

		private TaczGunTypeResolver() throws ClassNotFoundException, NoSuchMethodException {
			gunClass = Class.forName("com.tacz.guns.api.item.IGun");
			getGunId = gunClass.getMethod("getGunId", ItemStack.class);
			getCommonGunIndex = Class.forName("com.tacz.guns.api.TimelessAPI").getMethod("getCommonGunIndex", ResourceLocation.class);
			getType = Class.forName("com.tacz.guns.resource.index.CommonGunIndex").getMethod("getType");
		}

		private String type(ItemStack stack) throws ReflectiveOperationException {
			Object item = stack.getItem();
			if (!gunClass.isInstance(item)) {
				return null;
			}

			Object gunId = getGunId.invoke(item, stack);
			if (!(gunId instanceof ResourceLocation resourceLocation)) {
				return null;
			}

			Object optional = getCommonGunIndex.invoke(null, resourceLocation);
			if (!(optional instanceof Optional<?> gunIndexOptional)) {
				return null;
			}

			Object gunIndex = gunIndexOptional.orElse(null);
			if (gunIndex == null) {
				return null;
			}

			Object type = getType.invoke(gunIndex);
			return type instanceof String typeName ? typeName : null;
		}
	}

	private static final class TaczReloadStateResolver {
		private final Method fromLivingEntity;
		private final Method getSynReloadState;
		private final Method getStateType;
		private final Method isReloading;

		private TaczReloadStateResolver() throws ClassNotFoundException, NoSuchMethodException {
			Class<?> gunOperatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
			Class<?> reloadStateClass = Class.forName("com.tacz.guns.api.entity.ReloadState");
			Class<?> reloadStateTypeClass = Class.forName("com.tacz.guns.api.entity.ReloadState$StateType");
			fromLivingEntity = gunOperatorClass.getMethod("fromLivingEntity", LivingEntity.class);
			getSynReloadState = gunOperatorClass.getMethod("getSynReloadState");
			getStateType = reloadStateClass.getMethod("getStateType");
			isReloading = reloadStateTypeClass.getMethod("isReloading");
		}

		private boolean isReloading(Player player) throws ReflectiveOperationException {
			Object operator = fromLivingEntity.invoke(null, player);
			if (operator == null) {
				return false;
			}

			Object reloadState = getSynReloadState.invoke(operator);
			if (reloadState == null) {
				return false;
			}

			Object stateType = getStateType.invoke(reloadState);
			return stateType != null && Boolean.TRUE.equals(isReloading.invoke(stateType));
		}
	}

	private record FastRunIdentity(
			ResourceLocation item,
			ResourceLocation weaponType,
			AssetAccessor<? extends StaticAnimation> ordinaryRunAnimation,
			ResourceLocation ordinaryRunAnimationId) {
	}

	private record SprintProfileCacheKey(
			ResourceLocation item,
			ResourceLocation weaponType,
			ResourceLocation ordinaryRunAnimationId,
			boolean noWomProceduralWeaponFastRun,
			NaturalSprinterFastRunAnimationOverrides.RuleData override) {
	}

	private record CachedSprintProfile(SprintProfileCacheKey key, SprintProfile profile) {
	}

	private enum StartupStepSource {
		NONE(NaturalSprinterFastRunStep.Trigger.MANUAL, false),
		COMBAT_MASTERY_ACTIVE_SUPPRESS(NaturalSprinterFastRunStep.Trigger.MANUAL, false),
		AUTO(NaturalSprinterFastRunStep.Trigger.AUTO_STARTUP, true),
		MANUAL_FAST_RUN_KEY(NaturalSprinterFastRunStep.Trigger.STARTUP, true),
		COMBAT_MASTERY_HANDOFF(NaturalSprinterFastRunStep.Trigger.STARTUP, true);

		private final NaturalSprinterFastRunStep.Trigger stepTrigger;
		private final boolean playsStep;

		StartupStepSource(NaturalSprinterFastRunStep.Trigger stepTrigger, boolean playsStep) {
			this.stepTrigger = stepTrigger;
			this.playsStep = playsStep;
		}

		private NaturalSprinterFastRunStep.Trigger stepTrigger() {
			return stepTrigger;
		}

		private boolean playsStep() {
			return playsStep;
		}
	}

	private record SprintProfile(
			DefaultSprintFamily fallback,
			AssetAccessor<? extends StaticAnimation> animation,
			AssetAccessor<? extends StaticAnimation> leftStepAnimation,
			AssetAccessor<? extends StaticAnimation> rightStepAnimation,
			boolean defaultNaturalSprinter,
			boolean customRunAnimation,
			boolean startupStepEffects,
			boolean manualStepEffects,
			NaturalSprinterFastRunAnimationOverrides.RunPoseSettings runPose,
			boolean proceduralStepPulse,
			int animationSetPriority,
			boolean weaponRunFallback) {
		private static SprintProfile noOverride() {
			return new SprintProfile(
					null,
					null,
					null,
					null,
					false,
					false,
					false,
					false,
					NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED,
					false,
					0,
					false);
		}

		private static SprintProfile defaultFastRun(AssetAccessor<? extends StaticAnimation> animation) {
			if (animation == null) {
				return noOverride();
			}

			return new SprintProfile(
					null,
					animation,
					null,
					null,
					false,
					false,
					false,
					false,
					NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED,
					false,
					0,
					false);
		}

		private static SprintProfile defaultProfile(DefaultSprintFamily family) {
			if (family == null || family.animation() == null) {
				return noOverride();
			}

			return new SprintProfile(
					family,
					family.animation(),
					family.leftStepAnimation(),
					family.rightStepAnimation(),
					true,
					false,
					true,
					false,
					NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED,
					false,
					0,
					false);
		}

		private static SprintProfile weaponRunFallback(
				PlayerPatch<?> playerPatch,
				DefaultSprintFamily fallback,
				boolean startupStepEffects,
				boolean manualStepEffects,
				NaturalSprinterFastRunAnimationOverrides.RunPoseSettings runPose,
				int animationSetPriority) {
			AssetAccessor<? extends StaticAnimation> animation = ordinaryRunAnimation(playerPatch);
			return weaponRunFallback(animation, fallback, startupStepEffects, manualStepEffects, runPose, animationSetPriority);
		}

		private static SprintProfile weaponRunFallback(
				FastRunIdentity identity,
				DefaultSprintFamily fallback,
				boolean startupStepEffects,
				boolean manualStepEffects,
				NaturalSprinterFastRunAnimationOverrides.RunPoseSettings runPose,
				int animationSetPriority) {
			AssetAccessor<? extends StaticAnimation> animation = ordinaryRunAnimation(identity);
			return weaponRunFallback(animation, fallback, startupStepEffects, manualStepEffects, runPose, animationSetPriority);
		}

		private static SprintProfile weaponRunFallback(
				AssetAccessor<? extends StaticAnimation> animation,
				DefaultSprintFamily fallback,
				boolean startupStepEffects,
				boolean manualStepEffects,
				NaturalSprinterFastRunAnimationOverrides.RunPoseSettings runPose,
				int animationSetPriority) {
			if (animation == null) {
				return noOverride();
			}

			NaturalSprinterProceduralStepPulse.installSustainedFastRunPose(animation);
			NaturalSprinterProceduralStepPulse.install(animation);
			return new SprintProfile(
					fallback,
					animation,
					null,
					null,
					false,
					true,
					startupStepEffects,
					manualStepEffects,
					runPose,
					true,
					animationSetPriority,
					true);
		}

		private static SprintProfile custom(
				NaturalSprinterFastRunAnimationOverrides.RuleData rule,
				DefaultSprintFamily fallback,
				PlayerPatch<?> playerPatch) {
			for (NaturalSprinterFastRunAnimationOverrides.AnimationSet animationSet : rule.animationSets()) {
				SprintProfile profile = custom(rule, animationSet, fallback);
				if (profile != null) {
					return profile;
				}
			}
			if (fallback == null) {
				return noOverride();
			}
			return weaponRunFallback(playerPatch, fallback, rule.startupStepEffects(), rule.manualStepEffects(), rule.runPose(), 0);
		}

		private static SprintProfile custom(
				NaturalSprinterFastRunAnimationOverrides.RuleData rule,
				NaturalSprinterFastRunAnimationOverrides.AnimationSet animationSet,
				DefaultSprintFamily fallback) {
			DefaultSprintFamily defaultSprintFamily = defaultSprintFamilyForRun(animationSet.runAnimation());
			if (defaultSprintFamily != null) {
				return defaultProfile(defaultSprintFamily);
			}

			AssetAccessor<? extends StaticAnimation> animation = NaturalSprinterFastRunAnimationOverrides.resolveAnimation(animationSet.runAnimation());
			boolean customRunAnimation = animation != null;
			boolean hasStepAnimations = animationSet.leftStepAnimation() != null && animationSet.rightStepAnimation() != null;
			AssetAccessor<? extends StaticAnimation> leftStepAnimation = hasStepAnimations
					? NaturalSprinterFastRunAnimationOverrides.resolveAnimation(animationSet.leftStepAnimation())
					: null;
			AssetAccessor<? extends StaticAnimation> rightStepAnimation = hasStepAnimations
					? NaturalSprinterFastRunAnimationOverrides.resolveAnimation(animationSet.rightStepAnimation())
					: null;
			if (animation == null || hasStepAnimations && (leftStepAnimation == null || rightStepAnimation == null)) {
				return null;
			}

			boolean proceduralStepPulse = !hasStepAnimations;
			if (animation != null) {
				NaturalSprinterProceduralStepPulse.installSustainedFastRunPose(animation);
			}
			if (proceduralStepPulse) {
				NaturalSprinterProceduralStepPulse.install(animation);
			}
			return new SprintProfile(
					fallback,
					animation != null ? animation : fallback.animation(),
					leftStepAnimation != null ? leftStepAnimation : (proceduralStepPulse ? null : fallback.leftStepAnimation()),
					rightStepAnimation != null ? rightStepAnimation : (proceduralStepPulse ? null : fallback.rightStepAnimation()),
					false,
					customRunAnimation,
					rule.startupStepEffects(),
					rule.manualStepEffects(),
					rule.runPose(),
					proceduralStepPulse,
					animationSet.priority(),
					false);
		}

		private static DefaultSprintFamily defaultSprintFamilyForRun(ResourceLocation runAnimation) {
			if (!ModCompat.isWomLoaded()) {
				return null;
			}
			if (NaturalSprinterFastRunAnimationOverrides.isWomDefaultWeaponRun(runAnimation)) {
				return DefaultSprintFamily.WEAPON;
			}
			if (NaturalSprinterFastRunAnimationOverrides.isWomDefaultBarehandRun(runAnimation)) {
				return DefaultSprintFamily.BAREHAND;
			}
			return null;
		}

		private boolean acceptsCurrentAnimation(AssetAccessor<?> currentAnimation) {
			return isSameAnimation(currentAnimation, animation)
					|| isSameAnimation(currentAnimation, leftStepAnimation)
					|| isSameAnimation(currentAnimation, rightStepAnimation)
					|| WomAnimationRefs.isAny(currentAnimation,
							WomAnimationRefs.bipedSprintLeftStep(),
							WomAnimationRefs.bipedSprintRightStep(),
							WomAnimationRefs.bipedSprintLeftStepBarehand(),
							WomAnimationRefs.bipedSprintRightStepBarehand(),
							WomAnimationRefs.bipedSprintSlide(),
							WomAnimationRefs.bipedSprintJump(),
							WomAnimationRefs.bipedSprintStop());
		}

		private boolean isPrimaryAnimation(AssetAccessor<?> currentAnimation) {
			return isSameAnimation(currentAnimation, animation);
		}
	}

	private enum DefaultSprintFamily {
		BAREHAND {
			@Override
			AssetAccessor<? extends StaticAnimation> animation() {
				return WomAnimationRefs.bipedSprintBarehand();
			}

			@Override
			AssetAccessor<? extends StaticAnimation> leftStepAnimation() {
				return WomAnimationRefs.bipedSprintLeftStepBarehand();
			}

			@Override
			AssetAccessor<? extends StaticAnimation> rightStepAnimation() {
				return WomAnimationRefs.bipedSprintRightStepBarehand();
			}

			@Override
			boolean isPrimaryAnimation(AssetAccessor<?> animation) {
				return WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedSprintBarehand());
			}
		},
		WEAPON {
			@Override
			AssetAccessor<? extends StaticAnimation> animation() {
				return WomAnimationRefs.bipedSprint();
			}

			@Override
			AssetAccessor<? extends StaticAnimation> leftStepAnimation() {
				return WomAnimationRefs.bipedSprintLeftStep();
			}

			@Override
			AssetAccessor<? extends StaticAnimation> rightStepAnimation() {
				return WomAnimationRefs.bipedSprintRightStep();
			}

			@Override
			boolean isPrimaryAnimation(AssetAccessor<?> animation) {
				return WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedSprint());
			}
		};

		abstract AssetAccessor<? extends StaticAnimation> animation();

		abstract AssetAccessor<? extends StaticAnimation> leftStepAnimation();

		abstract AssetAccessor<? extends StaticAnimation> rightStepAnimation();

		abstract boolean isPrimaryAnimation(AssetAccessor<?> animation);

		public static boolean isAnyPrimary(AssetAccessor<?> animation) {
			return BAREHAND.isPrimaryAnimation(animation) || WEAPON.isPrimaryAnimation(animation);
		}
	}
}
