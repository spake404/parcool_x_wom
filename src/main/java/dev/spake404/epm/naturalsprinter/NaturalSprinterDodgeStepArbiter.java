package dev.spake404.epm.naturalsprinter;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.input.EPMKeyMappings;
import java.nio.ByteBuffer;
import java.util.WeakHashMap;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.action.impl.Roll;
import com.alrex.parcool.common.action.impl.Tap;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.common.network.SyncActionStateMessage;
import com.alrex.parcool.common.network.SyncStaminaMessage;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.extern.AdditionalMods;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

public final class NaturalSprinterDodgeStepArbiter {
	private static final WeakHashMap<Player, State> STATES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> DOUBLE_TAP_DODGE_STARTED_TICKS = new WeakHashMap<>();
	private static final ByteBuffer START_BUFFER = ByteBuffer.allocate(128);
	private static final int SHARED_KEY_CACHE_REFRESH_TICKS = 20;
	private static int sharedKeyCacheTick = Integer.MIN_VALUE;
	private static boolean sharedKeyCacheValue;
	private static EPMConfig.StepDodgeConflictMode sharedKeyCacheMode = EPMConfig.StepDodgeConflictMode.DISABLED;

	private NaturalSprinterDodgeStepArbiter() {
	}

	public static boolean tick(Player player) {
		if (!handlesSharedKey(player)) {
			clear(player);
			return false;
		}

		if (EPMClientHooks.isBreakfallFollowupBlockingNaturalSprinterStepDodge(player)) {
			clear(player);
			return true;
		}

		boolean down = EPMKeyMappings.isNaturalSprinterStepDown();
		if (shouldSuppressCompletedDoubleTapDodgePress(player, down)) {
			return true;
		}

		State state = STATES.get(player);
		if (state == null && !down) {
			return false;
		}
		if (state == null && !NaturalSprinterFastRunHandler.canArbitrateStepDodgeConflict(player)) {
			return false;
		}

		return switch (EPMConfig.naturalSprinterStepDodgeConflictMode()) {
			case SHORT_DODGE_LONG_STEP -> tickShortDodgeLongStep(player, down);
			case SHORT_STEP_HOLD_DODGE -> tickShortStepHoldDodge(player, down);
			case SINGLE_STEP_DOUBLE_DODGE -> tickSingleStepDoubleDodge(player, down);
			case DISABLED -> false;
		};
	}

	public static void clear(Player player) {
		if (player != null) {
			STATES.remove(player);
			DOUBLE_TAP_DODGE_STARTED_TICKS.remove(player);
		}
	}

	public static boolean shouldCancelDodgeStart(Player player) {
		if (EPMClientHooks.isBreakfallFollowupBlockingNaturalSprinterStepDodge(player)) {
			clear(player);
			return false;
		}
		if (!handlesSharedKey(player) || !EPMKeyMappings.isNaturalSprinterStepDown()) {
			return false;
		}
		State state = STATES.get(player);
		if (state == null && !NaturalSprinterFastRunHandler.canArbitrateStepDodgeConflict(player)) {
			return false;
		}

		EPMConfig.StepDodgeConflictMode mode = EPMConfig.naturalSprinterStepDodgeConflictMode();
		state = getOrCreatePressState(player);
		int heldTicks = heldTicks(player, state);
		return switch (mode) {
			case SHORT_DODGE_LONG_STEP -> true;
			case SHORT_STEP_HOLD_DODGE -> {
				if (heldTicks >= EPMConfig.naturalSprinterStepDodgeLongPressTicks()) {
					state.dodgeAllowed = true;
				}
				yield true;
			}
			case SINGLE_STEP_DOUBLE_DODGE -> {
				if (state.dodgeAllowedTick == player.tickCount || isSecondTapWithinWindow(player, state)) {
					allowDoubleTapDodge(player, state);
					yield false;
				}
				yield true;
			}
			case DISABLED -> false;
		};
	}

	public static boolean shouldUseStepOnlyFallback(Player player) {
		if (!hasSharedKeyArbiterConfig(player)) {
			return false;
		}

		if (isParCoolDodgeActionEnabled(player)) {
			return false;
		}

		clear(player);
		return true;
	}

	public static void markDodgeStarted(Player player) {
		State state = STATES.get(player);
		if (state != null) {
			state.dodgeStarted = true;
			state.dodgeAllowed = true;
			if (EPMConfig.naturalSprinterStepDodgeConflictMode() == EPMConfig.StepDodgeConflictMode.SINGLE_STEP_DOUBLE_DODGE) {
				DOUBLE_TAP_DODGE_STARTED_TICKS.put(player, Integer.valueOf(player.tickCount));
				STATES.remove(player);
			}
		}
	}

	private static boolean tickShortDodgeLongStep(Player player, boolean down) {
		State state = STATES.get(player);
		if (down) {
			getOrCreatePressState(player);
			return true;
		}

		if (state != null && state.pressing) {
			int heldTicks = heldTicks(player, state);
			STATES.remove(player);
			if (heldTicks >= EPMConfig.naturalSprinterStepDodgeLongPressTicks()) {
				NaturalSprinterFastRunHandler.tryArbitratedFastRunStep(player);
			} else {
				startDodge(player);
			}
		}
		return true;
	}

	private static boolean tickShortStepHoldDodge(Player player, boolean down) {
		State state = STATES.get(player);
		if (down) {
			state = getOrCreatePressState(player);
			if (heldTicks(player, state) >= EPMConfig.naturalSprinterStepDodgeLongPressTicks()) {
				state.dodgeAllowed = true;
				if (!state.dodgeStarted) {
					state.dodgeStarted = startDodge(player);
				}
			}
			return true;
		}

		if (state != null && state.pressing) {
			int heldTicks = heldTicks(player, state);
			boolean dodgeEnded = state.dodgeStarted && !EPMClientHooks.isParCoolDodgeBlockingNaturalSprinterStep(player);
			STATES.remove(player);
			if (heldTicks < EPMConfig.naturalSprinterStepDodgeLongPressTicks() || dodgeEnded) {
				NaturalSprinterFastRunHandler.tryArbitratedFastRunStep(player);
			}
		}
		return true;
	}

	private static boolean tickSingleStepDoubleDodge(Player player, boolean down) {
		State state = STATES.get(player);
		if (down) {
			if (state != null && isSecondTapWithinWindow(player, state)) {
				allowDoubleTapDodge(player, state);
				startDodge(player);
				return true;
			}
			getOrCreatePressState(player);
			return true;
		}

		if (state == null) {
			return true;
		}

		if (state.pressing) {
			int heldTicks = heldTicks(player, state);
			state.pressing = false;
			if (heldTicks <= EPMConfig.naturalSprinterStepDodgeFirstTapMaxTicks()) {
				state.pendingStep = true;
				state.pendingStepTick = player.tickCount;
			} else {
				STATES.remove(player);
				NaturalSprinterFastRunHandler.tryArbitratedFastRunStep(player);
			}
			return true;
		}

		if (state.pendingStep && player.tickCount - state.pendingStepTick > EPMConfig.naturalSprinterStepDodgeDoubleTapGapTicks()) {
			STATES.remove(player);
			NaturalSprinterFastRunHandler.tryArbitratedFastRunStep(player);
		}
		return true;
	}

	private static boolean handlesSharedKey(Player player) {
		return hasSharedKeyArbiterConfig(player) && isParCoolDodgeActionEnabled(player);
	}

	private static boolean hasSharedKeyArbiterConfig(Player player) {
		if (player == null
				|| !player.isLocalPlayer()
				|| !EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.customFastRunAnimations()
				|| !EPMConfig.naturalSprinterManualStep()) {
			return false;
		}

		EPMConfig.StepDodgeConflictMode mode = EPMConfig.naturalSprinterStepDodgeConflictMode();
		if (mode == EPMConfig.StepDodgeConflictMode.DISABLED) {
			return false;
		}

		int tick = player.tickCount;
		if (sharedKeyCacheMode == mode
				&& tick >= sharedKeyCacheTick
				&& tick - sharedKeyCacheTick < SHARED_KEY_CACHE_REFRESH_TICKS) {
			return sharedKeyCacheValue;
		}

		sharedKeyCacheValue = isNaturalSprinterStepBoundToDodge();
		sharedKeyCacheMode = mode;
		sharedKeyCacheTick = tick;
		return sharedKeyCacheValue;
	}

	private static boolean isNaturalSprinterStepBoundToDodge() {
		try {
			InputConstants.Key stepKey = EPMKeyMappings.naturalSprinterStepKey();
			InputConstants.Key dodgeKey = KeyBindings.getKeyDodge().getKey();
			return stepKey != null && stepKey.equals(dodgeKey);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolDodgeActionEnabled(Player player) {
		try {
			if (!ParCoolConfig.Client.getPossibilityOf(Dodge.class).get()) {
				return false;
			}

			Parkourability parkourability = Parkourability.get(player);
			return parkourability != null && parkourability.getActionInfo().can(Dodge.class);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static State getOrCreatePressState(Player player) {
		State state = STATES.get(player);
		if (state == null || !state.pressing && !state.pendingStep) {
			state = new State(player.tickCount);
			STATES.put(player, state);
			return state;
		}
		if (!state.pressing && state.pendingStep) {
			state.pressing = true;
			state.pressStartTick = player.tickCount;
		}
		return state;
	}

	private static boolean isSecondTapWithinWindow(Player player, State state) {
		return state != null
				&& state.pendingStep
				&& player.tickCount - state.pendingStepTick <= EPMConfig.naturalSprinterStepDodgeDoubleTapGapTicks();
	}

	private static boolean shouldSuppressCompletedDoubleTapDodgePress(Player player, boolean down) {
		if (EPMConfig.naturalSprinterStepDodgeConflictMode() != EPMConfig.StepDodgeConflictMode.SINGLE_STEP_DOUBLE_DODGE
				|| !DOUBLE_TAP_DODGE_STARTED_TICKS.containsKey(player)) {
			return false;
		}

		if (down) {
			return true;
		}

		DOUBLE_TAP_DODGE_STARTED_TICKS.remove(player);
		return true;
	}

	private static void allowDoubleTapDodge(Player player, State state) {
		state.dodgeAllowed = true;
		state.dodgeAllowedTick = player.tickCount;
		state.pressing = true;
		state.pressStartTick = player.tickCount;
	}

	private static int heldTicks(Player player, State state) {
		return Math.max(0, player.tickCount - state.pressStartTick);
	}

	private static boolean startDodge(Player player) {
		if (player == null || NaturalSprinterFastRunHandler.isParCoolDodgeDoing(player)) {
			return false;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			IStamina stamina = IStamina.get(player);
			if (parkourability == null || stamina == null || !parkourability.getActionInfo().can(Dodge.class)) {
				return false;
			}

			Dodge dodge = parkourability.get(Dodge.class);
			if (!canStartDodge(player, parkourability, stamina, dodge)) {
				return false;
			}

			START_BUFFER.clear();
			if (!writeDodgeStartInfo(START_BUFFER)) {
				return false;
			}
			START_BUFFER.flip();

			MinecraftForge.EVENT_BUS.post(new ParCoolActionEvent.Start.Pre(player, dodge));
			dodge.start(player, parkourability, START_BUFFER, stamina);
			MinecraftForge.EVENT_BUS.post(new ParCoolActionEvent.StartEvent(player, dodge));
			MinecraftForge.EVENT_BUS.post(new ParCoolActionEvent.Start.Post(player, dodge));
			stamina.consume(parkourability.getActionInfo().getStaminaConsumptionOf(Dodge.class));

			START_BUFFER.rewind();
			SyncActionStateMessage.Encoder encoder = SyncActionStateMessage.Encoder.reset();
			encoder.appendStartData(parkourability, dodge, START_BUFFER);
			SyncActionStateMessage.sync(player, encoder);
			SyncStaminaMessage.sync(player);
			markDodgeStarted(player);
			return true;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean canStartDodge(Player player, Parkourability parkourability, IStamina stamina, Dodge dodge) {
		return !player.isSpectator()
				&& !player.isDeadOrDying()
				&& !dodge.isDoing()
				&& (parkourability.getAdditionalProperties().getLandingTick() > 5
						|| parkourability.getAdditionalProperties().getPreviousNotLandingTick() < 2)
				&& player.onGround()
				&& !dodge.isInSuccessiveCoolDown(parkourability.getActionInfo())
				&& dodge.getCoolTime() <= 0
				&& !player.isInWaterOrBubble()
				&& !player.isShiftKeyDown()
				&& !player.getAbilities().flying
				&& !stamina.isExhausted()
				&& !parkourability.get(Crawl.class).isDoing()
				&& !parkourability.get(Roll.class).isDoing()
				&& !parkourability.get(Tap.class).isDoing();
	}

	private static boolean writeDodgeStartInfo(ByteBuffer buffer) {
		Dodge.DodgeDirection direction = null;
		if (Boolean.TRUE.equals(KeyBindings.isKeyBackDown())) {
			direction = Dodge.DodgeDirection.Back;
		}
		if (Boolean.TRUE.equals(KeyBindings.isKeyForwardDown())) {
			direction = Dodge.DodgeDirection.Front;
		}
		if (Boolean.TRUE.equals(KeyBindings.isKeyLeftDown())) {
			direction = Dodge.DodgeDirection.Left;
		}
		if (Boolean.TRUE.equals(KeyBindings.isKeyRightDown())) {
			direction = Dodge.DodgeDirection.Right;
		}

		Vec3 dodgeVec = KeyBindings.getCurrentMoveVector();
		if (direction == null || dodgeVec == null || dodgeVec.lengthSqr() <= 1.0E-6D) {
			return false;
		}

		direction = AdditionalMods.betterThirdPerson().handleCustomCameraRotationForDodge(direction);
		direction = AdditionalMods.shoulderSurfing().handleCustomCameraRotationForDodge(direction);
		buffer.putInt(direction.ordinal());
		buffer.putDouble(dodgeVec.x());
		buffer.putDouble(dodgeVec.z());
		return true;
	}

	private static final class State {
		private int pressStartTick;
		private int pendingStepTick;
		private boolean pressing = true;
		private boolean pendingStep;
		private boolean dodgeAllowed;
		private boolean dodgeStarted;
		private int dodgeAllowedTick = -1;

		private State(int pressStartTick) {
			this.pressStartTick = pressStartTick;
		}
	}
}
