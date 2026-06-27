package dev.spake404.epm.demolition;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.EpmAnimations;
import dev.spake404.epm.animation.EpmLivingMotions;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.mixin.ControlEngineAccessor;
import dev.spake404.epm.mixin.ParCoolAnimationAccessor;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.client.animation.impl.JumpChargingAnimator;
import com.alrex.parcool.common.action.impl.ChargeJump;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.mover.DemolitionLeapSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class DemolitionLeapCatJumpHandler {
	private static final ResourceLocation DEMOLITION_LEAP = ResourceLocation.fromNamespaceAndPath("epicfight", "demolition_leap");
	private static final WeakHashMap<Player, State> STATES = new WeakHashMap<>();
	private static final WeakHashMap<Player, ChargeAnimationLogState> CHARGE_ANIMATION_LOG_STATES = new WeakHashMap<>();
	private static final ThreadLocal<PlayerPatch<?>> AUTHORIZED_START = new ThreadLocal<>();
	private static final int PARCOOL_SUPPRESS_TICKS = 4;
	private static final int SNEAK_SUPPRESS_TICKS = 8;

	private DemolitionLeapCatJumpHandler() {
	}

	public static void registerChargeJumpAnimation(InitAnimatorEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| event == null
				|| !(event.getEntityPatch() instanceof PlayerPatch<?>)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = demolitionLeapChargingAnimation();
		if (animation != null) {
			event.getAnimator().addLivingAnimation(EpmLivingMotions.DEMOLITION_LEAP_CHARGING, animation);
		}
	}

	public static void chooseChargeJumpAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || event == null) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (player == null || !player.isLocalPlayer() || !playerPatch.isEpicFightMode()) {
			return;
		}

		Parkourability parkourability = parkourability(player);
		ChargeJump chargeJump = chargeJump(parkourability);
		Animator animator = currentParCoolAnimator(player);
		AssetAccessor<? extends StaticAnimation> animation = demolitionLeapChargingAnimation();
		boolean configEnabled = EPMConfig.demolitionLeapChargeJumpAnimation();
		boolean learnedDemolition = findDemolitionLeap(playerPatch) != null;
		boolean charging = chargeJump != null && chargeJump.isCharging();
		boolean physicalShiftDown = isPhysicalShiftDown();
		boolean jumpDown = isJumpDown();
		boolean jumpChargingAnimator = animator instanceof JumpChargingAnimator;
		boolean animationPresent = animation != null;
		boolean replace = shouldUseDemolitionChargeMotion(
				configEnabled,
				learnedDemolition,
				charging,
				jumpChargingAnimator,
				event.inaction(),
				animationPresent);

		if (replace) {
			event.setMotion(EpmLivingMotions.DEMOLITION_LEAP_CHARGING);
		}

		logChargeAnimation(
				player,
				playerPatch,
				replace,
				configEnabled,
				learnedDemolition,
				charging,
				physicalShiftDown,
				jumpDown,
				jumpChargingAnimator,
				event.inaction(),
				chargeJump == null ? 0 : chargeJump.getChargingTick(),
				animator,
				event.getMotion(),
				animationPresent);
	}

	public static void tickLocalPlayer(TickEvent.PlayerTickEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| event == null
				|| event.player == null
				|| !event.player.isLocalPlayer()) {
			return;
		}

		Player player = event.player;
		if (!isShiftSpaceReplacementEnabled()) {
			STATES.remove(player);
			return;
		}

		State state = STATES.computeIfAbsent(player, ignored -> new State());
		SkillContainer container = findDemolitionLeap(player);
		boolean comboDown = isComboDown(player) && container != null;

		if (comboDown) {
			state.lastComboTick = player.tickCount;
			state.suppressParCoolUntilTick = Math.max(state.suppressParCoolUntilTick, player.tickCount + PARCOOL_SUPPRESS_TICKS);
			state.suppressSneakUntilTick = Math.max(state.suppressSneakUntilTick, player.tickCount + SNEAK_SUPPRESS_TICKS);
			if (!state.comboWasDown && !state.active) {
				tryStart(player, container, state);
			}
		}

		if (state.active) {
			if (!comboDown) {
				requestOriginalRelease(player, state);
			} else if (!isHoldingDemolition(player, state.container)) {
				state.suppressSneakUntilTick = Math.max(state.suppressSneakUntilTick, player.tickCount + SNEAK_SUPPRESS_TICKS);
				state.active = false;
				state.releaseRequested = false;
				state.container = null;
			}
		}

		state.comboWasDown = comboDown;
		if (!state.active
				&& !comboDown
				&& player.tickCount > state.suppressParCoolUntilTick
				&& player.tickCount > state.suppressSneakUntilTick) {
			STATES.remove(player);
		}
	}

	public static boolean shouldSuppressParCool(Player player) {
		if (!isShiftSpaceReplacementEnabled() || player == null || !player.isLocalPlayer()) {
			return false;
		}

		State state = STATES.get(player);
		if (state != null && (state.active || player.tickCount <= state.suppressParCoolUntilTick)) {
			return true;
		}

		return isComboDown(player) && findDemolitionLeap(player) != null;
	}

	public static boolean shouldSuppressJumpChargingForDemolitionLeap(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.demolitionLeapChargeJumpAnimation()
				&& player != null
				&& player.isLocalPlayer()
				&& player.isShiftKeyDown()
				&& findDemolitionLeap(player) != null;
	}

	public static boolean shouldSuppressPhantomAscent(Player player) {
		return shouldSuppressParCool(player);
	}

	public static boolean shouldSuppressVanillaJump(Player player) {
		if (!isShiftSpaceReplacementEnabled() || player == null || !player.isLocalPlayer()) {
			return false;
		}

		State state = STATES.get(player);
		SkillContainer container = findDemolitionLeap(player);
		return (state != null && state.active) || isComboDown(player) && canAttemptStart(player, container);
	}

	public static boolean shouldSuppressSneak(Player player) {
		if (!isShiftSpaceReplacementEnabled() || player == null || !player.isLocalPlayer()) {
			return false;
		}

		State state = STATES.get(player);
		if (state != null && (state.active || player.tickCount <= state.suppressSneakUntilTick)) {
			return true;
		}

		SkillContainer container = findDemolitionLeap(player);
		return isPhysicalShiftDown() && isJumpDown() && canAttemptStart(player, container);
	}

	public static boolean shouldReplaceChargeJumpAnimator(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.demolitionLeapChargeJumpAnimation()
				|| player == null
				|| !player.isLocalPlayer()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !playerPatch.isEpicFightMode()) {
			return false;
		}

		ChargeJump chargeJump = chargeJump(parkourability(player));
		Animator animator = currentParCoolAnimator(player);
		boolean learnedDemolition = findDemolitionLeap(playerPatch) != null;
		boolean shouldSuppress = !learnedDemolition || player.isShiftKeyDown();
		return shouldSuppress
				&& chargeJump != null && chargeJump.isCharging()
				&& animator instanceof JumpChargingAnimator
				&& demolitionLeapChargingAnimation() != null
				&& !entityStateInaction(playerPatch);
	}

	public static boolean isShiftSpaceReplacementEnabled() {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.demolitionLeapShiftSpaceReplacement();
	}

	public static boolean isAuthorizedDemolitionLeapStart(PlayerPatch<?> playerPatch) {
		PlayerPatch<?> authorized = AUTHORIZED_START.get();
		return authorized != null
				&& playerPatch != null
				&& (authorized == playerPatch || authorized.getOriginal() == playerPatch.getOriginal());
	}

	public static SkillContainer findDemolitionLeap(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return findDemolitionLeap(playerPatch);
	}

	public static SkillContainer findDemolitionLeap(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getSkillCapability() == null) {
			return null;
		}

		try (var containers = playerPatch.getSkillCapability().listSkillContainers()) {
			return containers
					.filter(DemolitionLeapCatJumpHandler::isDemolitionLeapContainer)
					.findFirst()
					.orElse(null);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Parkourability parkourability(Player player) {
		try {
			return Parkourability.get(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static ChargeJump chargeJump(Parkourability parkourability) {
		if (parkourability == null) {
			return null;
		}

		try {
			return parkourability.get(ChargeJump.class);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Animator currentParCoolAnimator(Player player) {
		try {
			Animation animation = Animation.get(player);
			return animation == null ? null : ((ParCoolAnimationAccessor) animation).epm$getAnimator();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static AssetAccessor<? extends StaticAnimation> demolitionLeapChargingAnimation() {
		try {
			return EpmAnimations.demolitionLeapChargeJump();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean shouldUseDemolitionChargeMotion(
			boolean configEnabled,
			boolean learnedDemolition,
			boolean charging,
			boolean jumpChargingAnimator,
			boolean inaction,
			boolean animationPresent) {
		return configEnabled
				&& !learnedDemolition
				&& charging
				&& jumpChargingAnimator
				&& animationPresent
				&& !inaction;
	}

	private static boolean entityStateInaction(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch == null || playerPatch.getEntityState() == null || playerPatch.getEntityState().inaction();
		} catch (RuntimeException | LinkageError ignored) {
			return true;
		}
	}

	private static void logChargeAnimation(
			Player player,
			PlayerPatch<?> playerPatch,
			boolean replace,
			boolean configEnabled,
			boolean learnedDemolition,
			boolean charging,
			boolean physicalShiftDown,
			boolean jumpDown,
			boolean jumpChargingAnimator,
			boolean inaction,
			int chargeTick,
			Animator animator,
			LivingMotion currentMotion,
			boolean animationPresent) {
		if (!EPMConfig.debugDemolitionLeapState()) {
			CHARGE_ANIMATION_LOG_STATES.remove(player);
			return;
		}

		ChargeAnimationLogState previous = CHARGE_ANIMATION_LOG_STATES.get(player);
		boolean stopping = previous != null && previous.replacing() && !replace;
		boolean relevant = replace || charging || jumpChargingAnimator || stopping;
		if (!relevant) {
			CHARGE_ANIMATION_LOG_STATES.remove(player);
			return;
		}

		String animatorName = animatorName(animator);
		String currentMotionName = motionName(currentMotion);
		AssetAccessor<?> currentAnimation = AnimationQuery.currentAnimation(playerPatch);
		String currentAnimationName = assetName(currentAnimation);
		float animationElapsedTime = AnimationQuery.currentElapsedTime(playerPatch);
		float animationTotalTime = AnimationQuery.currentAnimationTotalTime(playerPatch);
		boolean animationEnded = AnimationQuery.currentAnimationEnded(playerPatch);
		boolean holdingLastFrame = replace
				&& sameAnimation(currentAnimation, demolitionLeapChargingAnimation())
				&& animationTotalTime >= 0.0F
				&& animationElapsedTime >= animationTotalTime - 0.001F;
		boolean startingHold = holdingLastFrame && (previous == null || !previous.holdingLastFrame());
		String signature = configEnabled
				+ "|" + learnedDemolition
				+ "|" + charging
				+ "|" + jumpChargingAnimator
				+ "|" + inaction
				+ "|" + animatorName
				+ "|" + currentMotionName
				+ "|" + currentAnimationName
				+ "|" + animationPresent;
		if (previous != null && previous.signature().equals(signature) && !stopping && !startingHold) {
			return;
		}

		String phase;
		if (startingHold) {
			phase = "charge_anim_hold";
		} else if (replace) {
			phase = "charge_anim_start";
		} else if (stopping && !physicalShiftDown) {
			phase = "charge_anim_stop_shift_release";
		} else if (stopping) {
			phase = "charge_anim_stop";
		} else {
			phase = "charge_anim_skip";
		}

		EPM.LOGGER.info(
				"[EPM/DemolitionLeap] phase={} tick={} configChargeAnim={} learnedDemolition={} chargeJumpCharging={} physicalShiftDown={} jumpDown={} jumpChargingAnimator={} inaction={} chargeTick={} currentMotion={} targetMotion={} currentAnimation={} animationElapsed={} animationTotal={} animationEnded={} parcoolAnimator={} animationPresent={}",
				phase,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(configEnabled),
				Boolean.valueOf(learnedDemolition),
				Boolean.valueOf(charging),
				Boolean.valueOf(physicalShiftDown),
				Boolean.valueOf(jumpDown),
				Boolean.valueOf(jumpChargingAnimator),
				Boolean.valueOf(inaction),
				Integer.valueOf(chargeTick),
				currentMotionName,
				replace ? EpmLivingMotions.DEMOLITION_LEAP_CHARGING.name() : "none",
				currentAnimationName,
				Float.valueOf(animationElapsedTime),
				Float.valueOf(animationTotalTime),
				Boolean.valueOf(animationEnded),
				animatorName,
				Boolean.valueOf(animationPresent));

		if (!replace && !charging && !jumpChargingAnimator) {
			CHARGE_ANIMATION_LOG_STATES.remove(player);
		} else {
			CHARGE_ANIMATION_LOG_STATES.put(player, new ChargeAnimationLogState(signature, replace, holdingLastFrame));
		}
	}

	private static boolean sameAnimation(AssetAccessor<?> left, AssetAccessor<?> right) {
		ResourceLocation leftName = AnimationQuery.safeRegistryName(left);
		ResourceLocation rightName = AnimationQuery.safeRegistryName(right);
		return leftName != null && leftName.equals(rightName);
	}

	private static String animatorName(Animator animator) {
		return animator == null ? "null" : animator.getClass().getName();
	}

	private static String motionName(LivingMotion motion) {
		return motion == null ? "null" : String.valueOf(motion);
	}

	private static String assetName(AssetAccessor<?> animation) {
		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		return registryName == null ? String.valueOf(animation) : registryName.toString();
	}

	private static boolean isDemolitionLeapContainer(SkillContainer container) {
		if (container == null || container.isEmpty()) {
			return false;
		}

		Skill skill = container.getSkill();
		return skill instanceof DemolitionLeapSkill
				|| skill != null && DEMOLITION_LEAP.toString().equals(String.valueOf(skill.getRegistryName()));
	}

	private static void tryStart(Player player, SkillContainer container, State state) {
		if (!canAttemptStart(player, container)
				|| !(EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class) instanceof LocalPlayerPatch localPlayerPatch)) {
			return;
		}

		ControlEngine controlEngine = controlEngine();
		if (controlEngine == null) {
			return;
		}

		runAuthorizedStart(localPlayerPatch, () -> container.sendCastRequest(localPlayerPatch, controlEngine));
		if (isHoldingDemolition(player, container)) {
			state.active = true;
			state.container = container;
			state.suppressParCoolUntilTick = player.tickCount + PARCOOL_SUPPRESS_TICKS;
			state.suppressSneakUntilTick = player.tickCount + SNEAK_SUPPRESS_TICKS;
			if (EPMConfig.debugDemolitionLeapState()) {
				EPM.LOGGER.info(
						"[EPM/DemolitionLeap] phase=shift_space_start tick={} onGround={} delta={} slot={}",
						Integer.valueOf(player.tickCount),
						Boolean.valueOf(player.onGround()),
						player.getDeltaMovement(),
						container.getSlot());
			}
		}
	}

	private static boolean canAttemptStart(Player player, SkillContainer container) {
		if (player == null
				|| container == null
				|| container.getSkill() == null
				|| player.isSpectator()
				|| player.isDeadOrDying()
				|| player.getVehicle() != null
				|| !(EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class) instanceof LocalPlayerPatch localPlayerPatch)) {
			return false;
		}

		try {
			return callAuthorizedStart(localPlayerPatch, () -> container.getSkill().isExecutableState(localPlayerPatch));
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean callAuthorizedStart(PlayerPatch<?> playerPatch, BooleanSupplier action) {
		PlayerPatch<?> previous = AUTHORIZED_START.get();
		AUTHORIZED_START.set(playerPatch);
		try {
			return action.getAsBoolean();
		} finally {
			restoreAuthorizedStart(previous);
		}
	}

	private static void runAuthorizedStart(PlayerPatch<?> playerPatch, Runnable action) {
		PlayerPatch<?> previous = AUTHORIZED_START.get();
		AUTHORIZED_START.set(playerPatch);
		try {
			action.run();
		} finally {
			restoreAuthorizedStart(previous);
		}
	}

	private static void restoreAuthorizedStart(PlayerPatch<?> previous) {
		if (previous == null) {
			AUTHORIZED_START.remove();
		} else {
			AUTHORIZED_START.set(previous);
		}
	}

	private static void requestOriginalRelease(Player player, State state) {
		ControlEngine controlEngine = controlEngine();
		if (state.container != null
				&& controlEngine != null
				&& isHoldingDemolition(player, state.container)) {
			((ControlEngineAccessor) controlEngine).epm$setHoldingFinished(true);
			state.releaseRequested = true;
			state.suppressParCoolUntilTick = Math.max(state.suppressParCoolUntilTick, player.tickCount + PARCOOL_SUPPRESS_TICKS);
			state.suppressSneakUntilTick = Math.max(state.suppressSneakUntilTick, player.tickCount + SNEAK_SUPPRESS_TICKS);
			if (EPMConfig.debugDemolitionLeapState()) {
				EPM.LOGGER.info(
						"[EPM/DemolitionLeap] phase=shift_space_release tick={} chargeTicks={} chargeAmount={} delta={}",
						Integer.valueOf(player.tickCount),
						Integer.valueOf(state.container.getExecutor().getSkillChargingTicks()),
						Integer.valueOf(state.container.getExecutor().getChargingAmount()),
						player.getDeltaMovement());
			}
			return;
		}

		state.active = false;
		state.container = null;
		state.releaseRequested = false;
		state.suppressParCoolUntilTick = player.tickCount + PARCOOL_SUPPRESS_TICKS;
		state.suppressSneakUntilTick = player.tickCount + SNEAK_SUPPRESS_TICKS;
	}

	private static boolean isHoldingDemolition(Player player, SkillContainer container) {
		if (container == null || container.getSkill() == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return playerPatch instanceof LocalPlayerPatch localPlayerPatch
				&& localPlayerPatch.isHoldingSkill(container.getSkill());
	}

	private static ControlEngine controlEngine() {
		try {
			return ClientEngine.getInstance().controlEngine;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isComboDown(Player player) {
		return isShiftDown(player) && isJumpDown();
	}

	private static boolean isShiftDown(Player player) {
		try {
			if (InputManager.isActionActive(MinecraftInputAction.SNEAK)) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		if (player != null && player.isShiftKeyDown()) {
			return true;
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyShift.isDown();
	}

	private static boolean isPhysicalShiftDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyShift.isDown();
	}

	private static boolean isJumpDown() {
		try {
			if (InputManager.isActionActive(MinecraftInputAction.JUMP)) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
	}

	private record ChargeAnimationLogState(String signature, boolean replacing, boolean holdingLastFrame) {
	}

	private static final class State {
		private boolean comboWasDown;
		private boolean active;
		private boolean releaseRequested;
		private int lastComboTick = -1;
		private int suppressParCoolUntilTick = -1;
		private int suppressSneakUntilTick = -1;
		private SkillContainer container;
	}
}
