package dev.spake404.epm.epicfightx;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMParCoolGate;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.asanginxst.epicfightx.gameassets.EFXSkillDataKeys;
import com.asanginxst.epicfightx.registries.EFXMobEffectRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EpicFightXCombatMasteryCompat {
	private static final int TOGGLE_PRESS_GRACE_TICKS = 2;
	private static final int PENDING_EXPIRE_GRACE_TICKS = 2;
	private static final int SPEED_DIAGNOSTIC_WINDOW_TICKS = 12;
	private static final String PARCOOL_FAST_RUN_MODIFIER_NAME = "parcool.modifier.fast_run";
	private static final UUID EFX_SPEED_MODIFIER_UUID = UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635");
	private static final Map<LocalPlayer, CombatMasteryState> COMBAT_MASTERY_STATES = new WeakHashMap<>();
	private static final Map<LocalPlayer, PendingToggleSprint> PENDING_TOGGLE_SPRINTS = new WeakHashMap<>();
	private static final Map<LocalPlayer, SpeedDiagnosticState> SPEED_DIAGNOSTIC_STATES = new WeakHashMap<>();

	private EpicFightXCombatMasteryCompat() {
	}

	public static void observeCombatMasteryInput(
			SkillDataManager dataManager,
			MovementInputEvent event,
			int sprintWindow) {
		if (!EPMConfig.epicFightXCombatMasteryFastRunControlCompatibility()) {
			return;
		}
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		LocalPlayerPatch playerPatch = eventPlayerPatch(event);
		LocalPlayer player = localPlayer(playerPatch);
		if (player == null || dataManager == null) {
			return;
		}

		CombatMasteryState state = COMBAT_MASTERY_STATES.computeIfAbsent(player, ignored -> new CombatMasteryState());
		state.dataManager = dataManager;
		state.sprintWindow = sprintWindow;
		state.lastObservedTick = tick(player);
		state.forwardMovementInput = hasForwardSprintInput(movementInput(event));
		state.lastMovementInputTick = tick(player);
		syncDodgeStart(player, state, dodgeStartTick(dataManager));
		rememberSeenDodgeAnimation(state, playerPatch);
		debugObserve(player, playerPatch, state);
	}

	public static void syncVanillaSprintBeforeEpicFightMotion(LocalPlayerPatch playerPatch) {
		if (!EPMConfig.epicFightXCombatMasteryFastRunControlCompatibility()) {
			return;
		}
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		LocalPlayer player = localPlayer(playerPatch);
		CombatMasteryState state = player == null ? null : COMBAT_MASTERY_STATES.get(player);
		if (player == null || state == null || state.dataManager == null) {
			return;
		}

		SkillDataManager dataManager = state.dataManager;
		syncDodgeStart(player, state, dodgeStartTick(dataManager));
		rememberSeenDodgeAnimation(state, playerPatch);

		boolean sprintAvailable = dataBoolean(dataManager, EFXSkillDataKeys.SPRINT_AVAILABLE.get());
		boolean sprintActive = dataBoolean(dataManager, EFXSkillDataKeys.SPRINT_ACTIVE.get());
		if (!sprintAvailable && !sprintActive) {
			return;
		}

		EPMConfig.CombatMasterySprintTriggerMode controlType = combatMasterySprintTriggerMode();
		if (controlType == null) {
			return;
		}

		boolean beforeSprinting = isSprinting(player);
		boolean keyPressed = fastRunKeyPressed();
		boolean keyDown = fastRunKeyDown();
		boolean dodgeAnimation = isDodgeAnimation(playerPatch);
		boolean forwardMovementInput = hasCurrentForwardMovementInput(player, state);
		boolean wantsSprint = wantsSprint(
				controlType,
				player,
				keyPressed,
				keyDown,
				tick(player),
				state.dodgeStartTick,
				state.sprintWindow);
		boolean ready = isReadyForCombatMasterySprint(player, playerPatch, state, dodgeAnimation, forwardMovementInput);
		String reason = "observing";

		if (sprintActive) {
			if (controlType == EPMConfig.CombatMasterySprintTriggerMode.PressKey && !keyDown) {
				setSprinting(player, false);
				reason = "press_key_release_active";
			} else {
				reason = "combat_mastery_active";
			}
		} else if (sprintAvailable && wantsSprint && ready) {
			setSprinting(player, true);
			PENDING_TOGGLE_SPRINTS.remove(player);
			reason = "enter_vanilla_sprint";
		} else if (sprintAvailable && shouldOwnSprintState(controlType)) {
			setSprinting(player, false);
			reason = wantsSprint && !state.seenDodgeAnimation ? "wait_for_dodge_animation"
					: wantsSprint && dodgeAnimation ? "wait_for_dodge_end"
					: wantsSprint && !forwardMovementInput ? "wait_for_forward_input" : "waiting_for_input";
		}

		debugMotion(
				player,
				playerPatch,
				state,
				controlType,
				keyPressed,
				keyDown,
				wantsSprint,
				ready,
				forwardMovementInput,
				dodgeAnimation,
				sprintAvailable,
				sprintActive,
				beforeSprinting,
				isSprinting(player),
				reason);
	}

	public static boolean shouldSuppressOrdinaryNaturalSprinterFastRunStart(PlayerPatch<?> playerPatch) {
		if (!EPMConfig.epicFightXCombatMasteryFastRunControlCompatibility()) {
			return false;
		}
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

		LocalPlayer player = localPlayer(playerPatch);
		CombatMasteryState state = player == null ? null : COMBAT_MASTERY_STATES.get(player);
		if (player == null || state == null || state.dataManager == null) {
			return false;
		}

		int currentTick = tick(player);
		int observedElapsed = currentTick - state.lastObservedTick;
		if (observedElapsed < 0 || observedElapsed > 1) {
			return false;
		}

		boolean sprintAvailable = dataBoolean(state.dataManager, EFXSkillDataKeys.SPRINT_AVAILABLE.get());
		boolean sprintActive = dataBoolean(state.dataManager, EFXSkillDataKeys.SPRINT_ACTIVE.get());
		if (!sprintAvailable && !sprintActive) {
			return false;
		}

		EPMConfig.CombatMasterySprintTriggerMode controlType = combatMasterySprintTriggerMode();
		if (!shouldOwnSprintState(controlType)) {
			return false;
		}

		if (sprintActive) {
			return true;
		}

		boolean keyPressed = fastRunKeyPressed();
		boolean keyDown = fastRunKeyDown();
		boolean wantsSprint = wantsSprint(
				controlType,
				player,
				keyPressed,
				keyDown,
				currentTick,
				state.dodgeStartTick,
				state.sprintWindow);
		return wantsSprint && state.seenDodgeAnimation && hasCurrentForwardMovementInput(player, state);
	}

	private static boolean wantsSprint(
			EPMConfig.CombatMasterySprintTriggerMode controlType,
			LocalPlayer player,
			boolean keyPressed,
			boolean keyDown,
			int tick,
			int dodgeStartTick,
			int sprintWindow) {
		if (controlType == EPMConfig.CombatMasterySprintTriggerMode.Auto) {
			return true;
		}

		if (controlType == EPMConfig.CombatMasterySprintTriggerMode.PressKey) {
			return keyDown;
		}

		if (controlType != EPMConfig.CombatMasterySprintTriggerMode.Toggle || player == null) {
			return false;
		}

		PendingToggleSprint pending = PENDING_TOGGLE_SPRINTS.get(player);
		if (pending != null && (pending.dodgeStartTick != dodgeStartTick || tick > pending.expireTick)) {
			PENDING_TOGGLE_SPRINTS.remove(player);
			pending = null;
		}

		if (keyPressed) {
			pending = new PendingToggleSprint(
					dodgeStartTick,
					tick + Math.max(1, sprintWindow) + PENDING_EXPIRE_GRACE_TICKS);
			PENDING_TOGGLE_SPRINTS.put(player, pending);
		}

		return pending != null;
	}

	private static boolean shouldOwnSprintState(EPMConfig.CombatMasterySprintTriggerMode controlType) {
		return controlType == EPMConfig.CombatMasterySprintTriggerMode.Auto
				|| controlType == EPMConfig.CombatMasterySprintTriggerMode.PressKey
				|| controlType == EPMConfig.CombatMasterySprintTriggerMode.Toggle;
	}

	private static boolean isReadyForCombatMasterySprint(
			LocalPlayer player,
			LocalPlayerPatch playerPatch,
			CombatMasteryState state,
			boolean dodgeAnimation,
			boolean forwardMovementInput) {
		return player != null
				&& player.onGround()
				&& !player.isSwimming()
				&& currentLivingMotion(playerPatch) != LivingMotions.CREATIVE_FLY
				&& forwardMovementInput
				&& state.seenDodgeAnimation
				&& !dodgeAnimation;
	}

	private static boolean hasCurrentForwardMovementInput(LocalPlayer player, CombatMasteryState state) {
		if (player == null) {
			return false;
		}

		if (isForwardKeyDown()) {
			return true;
		}

		if (state == null || !state.forwardMovementInput) {
			return false;
		}

		int tickDelta = tick(player) - state.lastMovementInputTick;
		return tickDelta >= 0 && tickDelta <= 1;
	}

	private static boolean hasForwardSprintInput(Input movementInput) {
		return movementInput != null && movementInput.up && !movementInput.down;
	}

	private static boolean isForwardKeyDown() {
		try {
			return Boolean.TRUE.equals(KeyBindings.isKeyForwardDown())
					&& !Boolean.TRUE.equals(KeyBindings.isKeyBackDown());
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void syncDodgeStart(LocalPlayer player, CombatMasteryState state, int dodgeStartTick) {
		if (dodgeStartTick >= 0 && state.dodgeStartTick != dodgeStartTick) {
			state.dodgeStartTick = dodgeStartTick;
			state.seenDodgeAnimation = false;
			PENDING_TOGGLE_SPRINTS.remove(player);
		}
	}

	private static void rememberSeenDodgeAnimation(CombatMasteryState state, LocalPlayerPatch playerPatch) {
		if (state.dodgeStartTick >= 0 && isDodgeAnimation(playerPatch)) {
			state.seenDodgeAnimation = true;
		}
	}

	private static EPMConfig.CombatMasterySprintTriggerMode combatMasterySprintTriggerMode() {
		try {
			return EPMConfig.epicFightXCombatMasterySprintTriggerMode();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean fastRunKeyPressed() {
		try {
			KeyRecorder.KeyState keyState = KeyRecorder.keyFastRunning;
			if (keyState != null && (keyState.isPressed()
					|| keyState.getTickKeyDown() > 0 && keyState.getTickKeyDown() <= TOGGLE_PRESS_GRACE_TICKS)) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return false;
	}

	private static boolean fastRunKeyDown() {
		try {
			KeyRecorder.KeyState keyState = KeyRecorder.keyFastRunning;
			if (keyState != null && keyState.getTickKeyDown() > 0) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		try {
			return KeyBindings.getKeyFastRunning() != null && KeyBindings.getKeyFastRunning().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static int dodgeStartTick(SkillDataManager dataManager) {
		try {
			Object value = dataManager.getDataValue(EFXSkillDataKeys.DODGE_START_TIME.get());
			if (value instanceof Integer tick) {
				return tick.intValue();
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return -1;
	}

	private static boolean dataBoolean(SkillDataManager dataManager, Object key) {
		try {
			Object value = dataManager.getDataValue((SkillDataKey<?>) key);
			if (value instanceof Boolean bool) {
				return bool.booleanValue();
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return false;
	}

	private static int tick(LocalPlayer player) {
		return player == null ? 0 : player.tickCount;
	}

	private static boolean isSprinting(LocalPlayer player) {
		return player != null && player.isSprinting();
	}

	private static void setSprinting(LocalPlayer player, boolean sprinting) {
		if (player != null && player.isSprinting() != sprinting) {
			player.setSprinting(sprinting);
		}
	}

	private static LivingMotion currentLivingMotion(LocalPlayerPatch playerPatch) {
		try {
			return playerPatch == null ? null : playerPatch.getCurrentLivingMotion();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isDodgeAnimation(LocalPlayerPatch playerPatch) {
		AssetAccessor<? extends DynamicAnimation> animation = currentBaseAnimation(playerPatch);
		AssetAccessor<? extends StaticAnimation> realAnimation = currentBaseRealAnimation(playerPatch);
		try {
			return animation != null && animation.get() instanceof DodgeAnimation;
		} catch (RuntimeException | LinkageError ignored) {
		}

		try {
			return realAnimation != null && realAnimation.get() instanceof DodgeAnimation;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static AssetAccessor<? extends DynamicAnimation> currentBaseAnimation(LocalPlayerPatch playerPatch) {
		try {
			if (playerPatch == null) {
				return null;
			}

			ClientAnimator animator = playerPatch.getClientAnimator();
			if (animator == null || animator.baseLayer == null || animator.baseLayer.animationPlayer == null) {
				return null;
			}

			return animator.baseLayer.animationPlayer.getAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static AssetAccessor<? extends StaticAnimation> currentBaseRealAnimation(LocalPlayerPatch playerPatch) {
		try {
			if (playerPatch == null) {
				return null;
			}

			ClientAnimator animator = playerPatch.getClientAnimator();
			if (animator == null || animator.baseLayer == null || animator.baseLayer.animationPlayer == null) {
				return null;
			}

			return animator.baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static LocalPlayerPatch eventPlayerPatch(MovementInputEvent event) {
		try {
			return event == null ? null : event.getPlayerPatch();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Input movementInput(MovementInputEvent event) {
		try {
			return event == null ? null : event.getMovementInput();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static LocalPlayer localPlayer(LocalPlayerPatch playerPatch) {
		try {
			return playerPatch == null ? null : (LocalPlayer) playerPatch.getOriginal();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static LocalPlayer localPlayer(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch == null ? null : (LocalPlayer) playerPatch.getOriginal();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static void debugObserve(LocalPlayer player, LocalPlayerPatch playerPatch, CombatMasteryState state) {
		if (!EPMConfig.debugEpicFightXCombatMasterySprintState()) {
			return;
		}

		boolean sprintAvailable = dataBoolean(state.dataManager, EFXSkillDataKeys.SPRINT_AVAILABLE.get());
		boolean sprintActive = dataBoolean(state.dataManager, EFXSkillDataKeys.SPRINT_ACTIVE.get());
		if (!sprintAvailable && !sprintActive && !isSprinting(player)) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/EFX_CM2][OBSERVE] tick={} dodgeStart={} sprintAvailable={} sprintActive={} playerSprinting={} motion={} seenDodgeAnimation={} forwardInput={} animation={} realAnimation={}",
				Integer.valueOf(tick(player)),
				Integer.valueOf(state.dodgeStartTick),
				Boolean.valueOf(sprintAvailable),
				Boolean.valueOf(sprintActive),
				Boolean.valueOf(isSprinting(player)),
				currentLivingMotion(playerPatch),
				Boolean.valueOf(state.seenDodgeAnimation),
				Boolean.valueOf(hasCurrentForwardMovementInput(player, state)),
				animationName(currentBaseAnimation(playerPatch)),
				animationName(currentBaseRealAnimation(playerPatch)));
		debugSpeedFactors(player, playerPatch, state, "observe", sprintAvailable, sprintActive);
	}

	private static void debugMotion(
			LocalPlayer player,
			LocalPlayerPatch playerPatch,
			CombatMasteryState state,
			EPMConfig.CombatMasterySprintTriggerMode controlType,
			boolean keyPressed,
			boolean keyDown,
			boolean wantsSprint,
			boolean ready,
			boolean forwardMovementInput,
			boolean dodgeAnimation,
			boolean sprintAvailable,
			boolean sprintActive,
			boolean beforeSprinting,
			boolean afterSprinting,
			String reason) {
		if (!EPMConfig.debugEpicFightXCombatMasterySprintState()
				|| (!keyPressed && !wantsSprint && beforeSprinting == afterSprinting)) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/EFX_CM2][VANILLA] tick={} dodgeStart={} control={} keyPressed={} keyDown={} wantsSprint={} ready={} forwardInput={} dodgeAnimation={} seenDodgeAnimation={} sprintAvailable={} sprintActive={} onGround={} swimming={} motion={} sprintBefore={} sprintAfter={} animation={} realAnimation={} reason={}",
				Integer.valueOf(tick(player)),
				Integer.valueOf(state.dodgeStartTick),
				controlType,
				Boolean.valueOf(keyPressed),
				Boolean.valueOf(keyDown),
				Boolean.valueOf(wantsSprint),
				Boolean.valueOf(ready),
				Boolean.valueOf(forwardMovementInput),
				Boolean.valueOf(dodgeAnimation),
				Boolean.valueOf(state.seenDodgeAnimation),
				Boolean.valueOf(sprintAvailable),
				Boolean.valueOf(sprintActive),
				Boolean.valueOf(player != null && player.onGround()),
				Boolean.valueOf(player != null && player.isSwimming()),
				currentLivingMotion(playerPatch),
				Boolean.valueOf(beforeSprinting),
				Boolean.valueOf(afterSprinting),
				animationName(currentBaseAnimation(playerPatch)),
				animationName(currentBaseRealAnimation(playerPatch)),
				reason);
		debugSpeedFactors(player, playerPatch, state, "vanilla/" + reason, sprintAvailable, sprintActive);
	}

	private static void debugSpeedFactors(
			LocalPlayer player,
			LocalPlayerPatch playerPatch,
			CombatMasteryState state,
			String phase,
			boolean sprintAvailable,
			boolean sprintActive) {
		if (!EPMConfig.debugEpicFightXCombatMasterySprintState() || player == null) {
			return;
		}

		int currentTick = tick(player);
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		double speedBase = movementSpeed == null ? Double.NaN : movementSpeed.getBaseValue();
		double speedValue = movementSpeed == null ? Double.NaN : movementSpeed.getValue();
		AttributeModifier efxSpeedModifier = movementSpeed == null ? null : movementSpeed.getModifier(EFX_SPEED_MODIFIER_UUID);
		AttributeModifier fastRunModifier = findModifierByName(movementSpeed, PARCOOL_FAST_RUN_MODIFIER_NAME);
		MobEffectInstance efxSpeed = efxSpeedInstance(player);
		boolean fastRunDoing = isFastRunDoing(player);
		boolean playerSprinting = isSprinting(player);
		boolean relevant = sprintAvailable
				|| sprintActive
				|| playerSprinting
				|| fastRunDoing
				|| efxSpeed != null
				|| fastRunModifier != null;
		if (!relevant) {
			SPEED_DIAGNOSTIC_STATES.remove(player);
			return;
		}

		SpeedDiagnosticState diagnosticState = SPEED_DIAGNOSTIC_STATES.computeIfAbsent(player, ignored -> new SpeedDiagnosticState());
		boolean changed = diagnosticState.lastTick < 0
				|| diagnosticState.lastSprintAvailable != sprintAvailable
				|| diagnosticState.lastSprintActive != sprintActive
				|| diagnosticState.lastPlayerSprinting != playerSprinting
				|| diagnosticState.lastFastRunDoing != fastRunDoing
				|| diagnosticState.lastEfxSpeedDuration != effectDuration(efxSpeed)
				|| diagnosticState.lastEfxSpeedAmplifier != effectAmplifier(efxSpeed)
				|| diagnosticState.lastHasFastRunModifier != (fastRunModifier != null)
				|| Math.abs(diagnosticState.lastSpeedValue - speedValue) > 0.0000001D;
		if (changed) {
			diagnosticState.logUntilTick = currentTick + SPEED_DIAGNOSTIC_WINDOW_TICKS;
		}
		if (currentTick > diagnosticState.logUntilTick) {
			updateSpeedDiagnosticState(diagnosticState, currentTick, sprintAvailable, sprintActive, playerSprinting, fastRunDoing, efxSpeed, fastRunModifier, speedValue);
			return;
		}

		Vec3 movement = player.getDeltaMovement();
		EPM.LOGGER.info(
				"[EPM/EFX_CM2][SPEED] phase={} tick={} dodgeStart={} sprintAvailable={} sprintActive={} playerSprinting={} fastRunDoing={} motion={} speedBase={} speedValue={} walkSpeed={} fovActual={} fovFromSpeed={} fovEffectScale={} efxSpeedDuration={} efxSpeedAmplifier={} efxSpeedModifier={} fastRunModifier={} horizontalDelta={} delta=({}, {}, {}) speedModifiers={} animation={} realAnimation={}",
				phase,
				Integer.valueOf(currentTick),
				Integer.valueOf(state.dodgeStartTick),
				Boolean.valueOf(sprintAvailable),
				Boolean.valueOf(sprintActive),
				Boolean.valueOf(playerSprinting),
				Boolean.valueOf(fastRunDoing),
				currentLivingMotion(playerPatch),
				Double.valueOf(speedBase),
				Double.valueOf(speedValue),
				Float.valueOf(player.getAbilities().getWalkingSpeed()),
				Float.valueOf(actualFovModifier(player)),
				Double.valueOf(fovFromSpeed(player, speedValue)),
				Double.valueOf(fovEffectScale()),
				Integer.valueOf(effectDuration(efxSpeed)),
				Integer.valueOf(effectAmplifier(efxSpeed)),
				modifierSummary(efxSpeedModifier),
				modifierSummary(fastRunModifier),
				Double.valueOf(horizontalSpeed(movement)),
				Double.valueOf(movement.x()),
				Double.valueOf(movement.y()),
				Double.valueOf(movement.z()),
				speedModifierSummary(movementSpeed),
				animationName(currentBaseAnimation(playerPatch)),
				animationName(currentBaseRealAnimation(playerPatch)));
		updateSpeedDiagnosticState(diagnosticState, currentTick, sprintAvailable, sprintActive, playerSprinting, fastRunDoing, efxSpeed, fastRunModifier, speedValue);
	}

	private static void updateSpeedDiagnosticState(
			SpeedDiagnosticState diagnosticState,
			int currentTick,
			boolean sprintAvailable,
			boolean sprintActive,
			boolean playerSprinting,
			boolean fastRunDoing,
			MobEffectInstance efxSpeed,
			AttributeModifier fastRunModifier,
			double speedValue) {
		diagnosticState.lastTick = currentTick;
		diagnosticState.lastSprintAvailable = sprintAvailable;
		diagnosticState.lastSprintActive = sprintActive;
		diagnosticState.lastPlayerSprinting = playerSprinting;
		diagnosticState.lastFastRunDoing = fastRunDoing;
		diagnosticState.lastEfxSpeedDuration = effectDuration(efxSpeed);
		diagnosticState.lastEfxSpeedAmplifier = effectAmplifier(efxSpeed);
		diagnosticState.lastHasFastRunModifier = fastRunModifier != null;
		diagnosticState.lastSpeedValue = speedValue;
	}

	private static AttributeModifier findModifierByName(AttributeInstance attribute, String name) {
		if (attribute == null || name == null) {
			return null;
		}

		try {
			for (AttributeModifier modifier : attribute.getModifiers()) {
				if (name.equals(modifier.getName())) {
					return modifier;
				}
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return null;
	}

	private static MobEffectInstance efxSpeedInstance(LocalPlayer player) {
		try {
			return player == null ? null : player.getEffect(EFXMobEffectRegistry.EFX_SPEED.get());
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isFastRunDoing(LocalPlayer player) {
		try {
			Parkourability parkourability = player == null ? null : Parkourability.get(player);
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static int effectDuration(MobEffectInstance effect) {
		return effect == null ? -1 : effect.getDuration();
	}

	private static int effectAmplifier(MobEffectInstance effect) {
		return effect == null ? -1 : effect.getAmplifier();
	}

	private static double fovFromSpeed(LocalPlayer player, double speedValue) {
		if (player == null || !Double.isFinite(speedValue)) {
			return Double.NaN;
		}

		float walkingSpeed = player.getAbilities().getWalkingSpeed();
		if (walkingSpeed <= 0.0F || Float.isNaN(walkingSpeed)) {
			return Double.NaN;
		}

		double fov = ((speedValue / (double) walkingSpeed) + 1.0D) / 2.0D;
		if (player.getAbilities().flying) {
			fov *= 1.1D;
		}
		double fovEffectScale = fovEffectScale();
		return 1.0D + (fov - 1.0D) * fovEffectScale;
	}

	private static float actualFovModifier(LocalPlayer player) {
		try {
			return player == null ? Float.NaN : player.getFieldOfViewModifier();
		} catch (RuntimeException | LinkageError ignored) {
			return Float.NaN;
		}
	}

	private static double fovEffectScale() {
		try {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft != null && minecraft.options != null) {
				return minecraft.options.fovEffectScale().get().doubleValue();
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return Double.NaN;
	}

	private static double horizontalSpeed(Vec3 movement) {
		return movement == null ? Double.NaN : Math.sqrt(movement.x() * movement.x() + movement.z() * movement.z());
	}

	private static String modifierSummary(AttributeModifier modifier) {
		if (modifier == null) {
			return "none";
		}

		return modifier.getName() + " amount=" + modifier.getAmount() + " operation=" + modifier.getOperation();
	}

	private static String speedModifierSummary(AttributeInstance attribute) {
		if (attribute == null) {
			return "none";
		}

		StringBuilder builder = new StringBuilder();
		try {
			for (AttributeModifier modifier : attribute.getModifiers()) {
				if (builder.length() > 0) {
					builder.append("; ");
				}
				builder.append(modifier.getName())
						.append(" amount=")
						.append(modifier.getAmount())
						.append(" operation=")
						.append(modifier.getOperation());
			}
		} catch (RuntimeException | LinkageError ignored) {
			return "unavailable";
		}
		return builder.length() == 0 ? "none" : builder.toString();
	}

	private static String animationName(AssetAccessor<?> animation) {
		if (animation == null) {
			return "null";
		}

		try {
			Object value = animation.get();
			return value == null ? String.valueOf(animation) : String.valueOf(value);
		} catch (RuntimeException | LinkageError ignored) {
			return String.valueOf(animation);
		}
	}

	private static final class CombatMasteryState {
		private SkillDataManager dataManager;
		private int sprintWindow;
		private int dodgeStartTick = -1;
		private int lastObservedTick;
		private int lastMovementInputTick = -1;
		private boolean seenDodgeAnimation;
		private boolean forwardMovementInput;
	}

	private static final class SpeedDiagnosticState {
		private int lastTick = -1;
		private int logUntilTick = -1;
		private int lastEfxSpeedDuration = -1;
		private int lastEfxSpeedAmplifier = -1;
		private double lastSpeedValue = Double.NaN;
		private boolean lastSprintAvailable;
		private boolean lastSprintActive;
		private boolean lastPlayerSprinting;
		private boolean lastFastRunDoing;
		private boolean lastHasFastRunModifier;
	}

	private static final class PendingToggleSprint {
		private final int dodgeStartTick;
		private final int expireTick;

		private PendingToggleSprint(int dodgeStartTick, int expireTick) {
			this.dodgeStartTick = dodgeStartTick;
			this.expireTick = expireTick;
		}
	}
}
