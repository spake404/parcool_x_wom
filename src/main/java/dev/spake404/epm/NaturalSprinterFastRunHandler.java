package dev.spake404.epm;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.WeakHashMap;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.yesman.epicparcool.ParcoolLivingMotions;
import dev.spake404.epm.mixin.AnimatorAccessor;
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
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

final class NaturalSprinterFastRunHandler {
	private static final int FAST_RUN_ANIMATION_REPLAY_COOLDOWN_TICKS = 2;
	private static final WeakHashMap<PlayerPatch<?>, Boolean> FAST_RUN_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> MANUAL_FAST_RUN_KEY_CONSUMED = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Integer> FAST_RUN_ANIMATION_REPLAY_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> NEXT_FAST_RUN_STEP_RIGHT = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, String> LAST_SPRINT_PROFILE_LOG = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> MANUAL_FAST_RUN_STEP_KEY_HELD = new WeakHashMap<>();
	private static TaczGunTypeResolver taczGunTypeResolver;
	private static TaczReloadStateResolver taczReloadStateResolver;

	private NaturalSprinterFastRunHandler() {
	}

	static void registerFastRunAnimation(InitAnimatorEvent event) {
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

	static void chooseFastRunAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || !ModCompat.isWomLoaded()) {
			clearFastRunState(event.getPlayerPatch());
			return;
		}

		if (event.getMotion() != ParcoolLivingMotions.FAST_RUN) {
			clearFastRunState(event.getPlayerPatch());
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		if (EPMClientHooks.shouldStopFastRunForGlider(playerPatch.getOriginal())) {
			clearFastRunState(playerPatch);
			EPMClientHooks.suppressFastRunAnimationForGlider(playerPatch.getOriginal());
			event.setMotion(LivingMotions.FALL);
			return;
		}

		if (!NaturalSprinterState.hasNaturalSprinter(playerPatch) || !EPMConfig.naturalSprinterAnimations()) {
			clearFastRunState(playerPatch);
			event.setMotion(LivingMotions.RUN);
			return;
		}

		if (EPMClientHooks.isParCoolDodgeBlockingNaturalSprinterStep(playerPatch.getOriginal())) {
			return;
		}

		boolean handledStartupStep = EPMClientHooks.playPendingNaturalSprinterStepFastRun(playerPatch);
		triggerNaturalSprinterDashOnFastRunStart(playerPatch, handledStartupStep);
		applyFastRunAnimation(playerPatch);
	}

	static boolean tryManualFastRunStep(Player player) {
		return tryManualFastRunStep(player, false);
	}

	static boolean tryArbitratedFastRunStep(Player player) {
		return tryManualFastRunStep(player, true);
	}

	private static boolean tryManualFastRunStep(Player player, boolean fromStepDodgeConflict) {
		if (!canUseManualNaturalSprinterStep(player)) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		boolean hasPatch = playerPatch != null;
		boolean logicalClient = hasPatch && playerPatch.isLogicalClient();
		boolean hasNaturalSprinter = logicalClient && NaturalSprinterState.hasNaturalSprinter(playerPatch);
		if (!hasPatch || !logicalClient || !hasNaturalSprinter) {
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

		if (!NaturalSprinterState.consumeStep(playerPatch)) {
			return false;
		}

		advanceSprintStep(playerPatch);
		EPMClientHooks.playNaturalSprinterFastRunStep(playerPatch, step, NaturalSprinterFastRunStep.Trigger.MANUAL);
		return true;
	}

	static boolean canArbitrateStepDodgeConflict(Player player) {
		if (!canUseManualNaturalSprinterStep(player)) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return playerPatch != null
				&& playerPatch.isLogicalClient()
				&& NaturalSprinterState.hasNaturalSprinter(playerPatch)
				&& currentSprintStep(playerPatch).isPresent();
	}

	private static boolean canUseManualNaturalSprinterStep(Player player) {
		return player != null
				&& player.isLocalPlayer()
				&& EPMParCoolGate.allowCrossModSkillCompat()
				&& ModCompat.isWomLoaded()
				&& EPMConfig.naturalSprinterAnimations()
				&& EPMConfig.naturalSprinterManualStep()
				&& canManualFastRunStep(player)
				&& !isTaczReloading(player);
	}

	static void tickManualFastRunStepKey(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| !ModCompat.isWomLoaded()
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

	static void cancelManualFastRunStepKey(Player player) {
		if (player != null) {
			MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player);
			NaturalSprinterDodgeStepArbiter.clear(player);
		}
	}

	private static void clearFastRunState(PlayerPatch<?> playerPatch) {
		NaturalSprinterProceduralStepPulse.clear(playerPatch);
		FAST_RUN_ACTIVE.remove(playerPatch);
		MANUAL_FAST_RUN_KEY_CONSUMED.remove(playerPatch);
		FAST_RUN_ANIMATION_REPLAY_TICKS.remove(playerPatch);
		LAST_SPRINT_PROFILE_LOG.remove(playerPatch);
	}

	private static void applyFastRunAnimation(PlayerPatch<?> playerPatch) {
		SprintProfile profile = chooseSprintProfile(playerPatch);
		AssetAccessor<? extends StaticAnimation> animation = profile.animation();
		if (animation == null) {
			NaturalSprinterProceduralStepPulse.configureSustainedFastRunPose(
					playerPatch,
					NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED);
			return;
		}

		NaturalSprinterProceduralStepPulse.configureSustainedFastRunPose(
				playerPatch,
				profile.customRunAnimation()
						? profile.runPose()
						: NaturalSprinterFastRunAnimationOverrides.RunPoseSettings.DISABLED);

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (profile.acceptsCurrentAnimation(currentAnimation)) {
			logFastRunAnimationSwap(playerPatch, "accepted_current", profile, currentAnimation, animation, false);
			return;
		}

		putLivingAnimationSilently(playerPatch.getAnimator(), ParcoolLivingMotions.FAST_RUN, animation);
		boolean swapped = swapPrimarySprintAnimationPreservingTime(playerPatch, profile, animation, currentAnimation);
		logFastRunAnimationSwap(playerPatch, swapped ? "swap_preserve_time" : "swap_deferred", profile, currentAnimation, animation, swapped);
		if (!swapped) {
			replayFastRunAnimationIfOverridden(playerPatch, animation, currentAnimation);
		}
	}

	private static void triggerNaturalSprinterDashOnFastRunStart(PlayerPatch<?> playerPatch, boolean startupStepHandled) {
		if (!EPMConfig.naturalSprinterAnimations()) {
			clearFastRunState(playerPatch);
			return;
		}

		boolean wasFastRunActive = Boolean.TRUE.equals(FAST_RUN_ACTIVE.put(playerPatch, Boolean.TRUE));
		boolean shouldPlay = !startupStepHandled && shouldPlayFastRunStartStep(playerPatch, wasFastRunActive);
		boolean hasStamina = shouldPlay && playerPatch.hasStamina(2.0F);
		logStartupStepDecision(playerPatch, startupStepHandled, wasFastRunActive, shouldPlay, hasStamina);
		if (!shouldPlay || !hasStamina) {
			return;
		}

		consumeCurrentFastRunKeyPress(playerPatch);
		NaturalSprinterFastRunStep step = nextSprintStep(playerPatch);
		EPMClientHooks.playNaturalSprinterFastRunStep(playerPatch, step, NaturalSprinterFastRunStep.Trigger.STARTUP);
	}

	private static boolean shouldPlayFastRunStartStep(PlayerPatch<?> playerPatch, boolean wasFastRunActive) {
		if (!EPMConfig.naturalSprinterAnimations()) {
			return false;
		}

		if (!EPMConfig.fastRunStartStepAnimation()) {
			return false;
		}

		return shouldAutoTriggerFastRunDash(playerPatch, wasFastRunActive)
				|| shouldTriggerManualFastRunDash(playerPatch)
				|| shouldTriggerCombatMasteryHandoffDash(playerPatch);
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

	static boolean isParCoolDodgeDoing(Player player) {
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
		SprintProfile profile = chooseSprintProfile(playerPatch);
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));
		AssetAccessor<? extends StaticAnimation> animation = rightStep ? profile.rightStepAnimation() : profile.leftStepAnimation();
		if (animation != null) {
			return profile.defaultNaturalSprinter()
					? NaturalSprinterFastRunStep.defaultAnimation(animation)
					: NaturalSprinterFastRunStep.configuredAnimation(animation, profile.startupStepEffects(), profile.manualStepEffects());
		}
		return profile.proceduralStepPulse() ? NaturalSprinterFastRunStep.procedural(profile.animation(), rightStep) : NaturalSprinterFastRunStep.none();
	}

	private static void advanceSprintStep(PlayerPatch<?> playerPatch) {
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));
		NEXT_FAST_RUN_STEP_RIGHT.put(playerPatch, Boolean.valueOf(!rightStep));
	}

	public static void advanceSprintStepPublic(PlayerPatch<?> playerPatch) {
		advanceSprintStep(playerPatch);
	}

	private static void putLivingAnimationSilently(Animator animator, LivingMotion motion, AssetAccessor<? extends StaticAnimation> animation) {
		if (animator instanceof AnimatorAccessor accessor) {
			accessor.parcoolxwom$livingAnimations().put(motion, animation);
		}
	}

	@SuppressWarnings("unchecked")
	private static boolean swapPrimarySprintAnimationPreservingTime(PlayerPatch<?> playerPatch, SprintProfile profile, AssetAccessor<? extends StaticAnimation> animation, AssetAccessor<?> currentAnimation) {
		if (!isAnyPrimarySprintAnimation(currentAnimation) || profile.isPrimaryAnimation(currentAnimation)) {
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
				|| WomAnimationRefs.isAny(animation, WomAnimationRefs.epicParCoolFastRun())
				|| NaturalSprinterFastRunAnimationOverrides.isConfiguredRunAnimation(animation);
	}

	private static void replayFastRunAnimationIfOverridden(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation, AssetAccessor<?> currentAnimation) {
		if (!isMovementAnimation(currentAnimation)) {
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

	private static boolean isMovementAnimation(AssetAccessor<?> animation) {
		try {
			return animation != null && animation.get() instanceof MovementAnimation;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
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

	private static SprintProfile chooseSprintProfile(PlayerPatch<?> playerPatch) {
		DefaultSprintFamily defaultFamily = chooseDefaultSprintFamily(playerPatch);
		NaturalSprinterFastRunAnimationOverrides.RuleData override = NaturalSprinterFastRunAnimationOverrides.select(
				mainHandItemId(playerPatch),
				weaponCategoryName(playerPatch));
		if (override == null) {
			SprintProfile profile = SprintProfile.defaultProfile(defaultFamily);
			logSprintProfile(playerPatch, null, defaultFamily, profile);
			return profile;
		}

		DefaultSprintFamily fallback = defaultFamily;
		if (override.fallback() == NaturalSprinterFastRunAnimationOverrides.FallbackFamily.BAREHAND) {
			fallback = DefaultSprintFamily.BAREHAND;
		} else if (override.fallback() == NaturalSprinterFastRunAnimationOverrides.FallbackFamily.WEAPON) {
			fallback = DefaultSprintFamily.WEAPON;
		}
		SprintProfile profile = SprintProfile.custom(override, fallback);
		logSprintProfile(playerPatch, override, fallback, profile);
		return profile;
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

	private static String weaponCategoryName(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return null;
		}

		try {
			CapabilityItem mainHand = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
			return mainHand == null ? null : categoryName(mainHand.getWeaponCategory());
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
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

	private static void logStartupStepDecision(
			PlayerPatch<?> playerPatch,
			boolean startupStepHandled,
			boolean wasFastRunActive,
			boolean shouldPlay,
			boolean hasStamina) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=start_check tick={} startupStepHandled={} wasFastRunActive={} shouldPlay={} hasStamina={} fastRunKeyDown={} fastRunKeyRecent={} currentAnimation={} elapsed={} item={} weaponType={}",
				Integer.valueOf(playerTick(playerPatch)),
				Boolean.valueOf(startupStepHandled),
				Boolean.valueOf(wasFastRunActive),
				Boolean.valueOf(shouldPlay),
				Boolean.valueOf(hasStamina),
				Boolean.valueOf(EPMClientHooks.isFastRunKeyDown()),
				Boolean.valueOf(EPMClientHooks.isFastRunKeyRecentlyPressed()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)),
				mainHandItemId(playerPatch),
				weaponCategoryName(playerPatch));
	}

	private static void logStepSelection(PlayerPatch<?> playerPatch, String phase, NaturalSprinterFastRunStep step) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase={} tick={} stepPresent={} procedural={} rightStep={} defaultNaturalSprinter={} stepAnimation={} currentAnimation={} elapsed={}",
				phase,
				Integer.valueOf(playerTick(playerPatch)),
				Boolean.valueOf(step != null && step.isPresent()),
				Boolean.valueOf(step != null && step.procedural()),
				Boolean.valueOf(step != null && step.rightStep()),
				Boolean.valueOf(step != null && step.defaultNaturalSprinter()),
				step == null ? "null" : assetName(step.animation()),
				assetName(currentBaseAnimation(playerPatch)),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
	}

	private static void logSprintProfile(
			PlayerPatch<?> playerPatch,
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
				+ "|" + profile.startupStepEffects()
				+ "|" + profile.manualStepEffects()
				+ "|" + profile.runPose().enabled()
				+ "|" + profile.runPose().scale()
				+ "|" + profile.runPose().blendTicks()
				+ "|" + profile.proceduralStepPulse();
		if (signature.equals(LAST_SPRINT_PROFILE_LOG.get(playerPatch))) {
			return;
		}

		LAST_SPRINT_PROFILE_LOG.put(playerPatch, signature);
		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase=profile tick={} source={} rule={} priority={} item={} weaponType={} fallback={} run={} leftStep={} rightStep={} startupEffects={} manualEffects={} runPose=(enabled={}, scale={}, blendTicks={}) procedural={}",
				Integer.valueOf(playerTick(playerPatch)),
				rule == null ? "default" : "datapack",
				rule == null ? "null" : rule.id(),
				Integer.valueOf(rule == null ? 0 : rule.priority()),
				mainHandItemId(playerPatch),
				weaponCategoryName(playerPatch),
				fallback,
				assetName(profile.animation()),
				assetName(profile.leftStepAnimation()),
				assetName(profile.rightStepAnimation()),
				Boolean.valueOf(profile.startupStepEffects()),
				Boolean.valueOf(profile.manualStepEffects()),
				Boolean.valueOf(profile.runPose().enabled()),
				Float.valueOf(profile.runPose().scale()),
				Float.valueOf(profile.runPose().blendTicks()),
				Boolean.valueOf(profile.proceduralStepPulse()));
	}

	private static void logFastRunAnimationSwap(
			PlayerPatch<?> playerPatch,
			String phase,
			SprintProfile profile,
			AssetAccessor<?> currentAnimation,
			AssetAccessor<? extends StaticAnimation> targetAnimation,
			boolean swapped) {
		if (!debugStepPulse()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/NaturalSprinterStepPulse] phase={} tick={} currentAnimation={} targetRun={} customRun={} fallback={} primaryCurrent={} acceptedCurrent={} swapped={} elapsed={}",
				phase,
				Integer.valueOf(playerTick(playerPatch)),
				assetName(currentAnimation),
				assetName(targetAnimation),
				Boolean.valueOf(profile.customRunAnimation()),
				profile.fallback(),
				Boolean.valueOf(isAnyPrimarySprintAnimation(currentAnimation)),
				Boolean.valueOf(profile.acceptsCurrentAnimation(currentAnimation)),
				Boolean.valueOf(swapped),
				Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
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
			boolean proceduralStepPulse) {
		private static SprintProfile defaultProfile(DefaultSprintFamily family) {
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
					false);
		}

		private static SprintProfile custom(NaturalSprinterFastRunAnimationOverrides.RuleData rule, DefaultSprintFamily fallback) {
			AssetAccessor<? extends StaticAnimation> animation = NaturalSprinterFastRunAnimationOverrides.resolveAnimation(rule.runAnimation());
			boolean customRunAnimation = animation != null;
			boolean hasLeftStep = rule.leftStepAnimation() != null;
			boolean hasRightStep = rule.rightStepAnimation() != null;
			AssetAccessor<? extends StaticAnimation> leftStepAnimation = hasLeftStep
					? NaturalSprinterFastRunAnimationOverrides.resolveAnimation(rule.leftStepAnimation())
					: null;
			AssetAccessor<? extends StaticAnimation> rightStepAnimation = hasRightStep
					? NaturalSprinterFastRunAnimationOverrides.resolveAnimation(rule.rightStepAnimation())
					: null;
			boolean proceduralStepPulse = animation != null && !hasLeftStep && !hasRightStep;
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
					proceduralStepPulse);
		}

		private boolean acceptsCurrentAnimation(AssetAccessor<?> currentAnimation) {
			return WomAnimationRefs.isAny(currentAnimation,
					animation,
					leftStepAnimation,
					rightStepAnimation,
					WomAnimationRefs.bipedSprintLeftStep(),
					WomAnimationRefs.bipedSprintRightStep(),
					WomAnimationRefs.bipedSprintLeftStepBarehand(),
					WomAnimationRefs.bipedSprintRightStepBarehand(),
					WomAnimationRefs.bipedSprintSlide(),
					WomAnimationRefs.bipedSprintJump(),
					WomAnimationRefs.bipedSprintStop());
		}

		private boolean isPrimaryAnimation(AssetAccessor<?> currentAnimation) {
			return WomAnimationRefs.isAny(currentAnimation, animation);
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

		static boolean isAnyPrimary(AssetAccessor<?> animation) {
			return BAREHAND.isPrimaryAnimation(animation) || WEAPON.isPrimaryAnimation(animation);
		}
	}
}
