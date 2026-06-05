package dev.spake404.epm.aqua;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.FastSwim;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMConfig;
import dev.spake404.epm.ModCompat;
import dev.spake404.epm.WomAnimationRefs;
import dev.spake404.epm.WomCompatBridge;
import dev.spake404.epm.animation.EpmAnimations;
import dev.spake404.epm.animation.EpmLivingMotions;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.WeakHashMap;

public final class AquaManeuvreFastSwimHandler {
	private static final WeakHashMap<Player, TimedBoolean> CAN_WOM_OWN_AQUA_FAST_SWIM = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> TOGGLE_FAST_SWIM = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> LAST_FAST_SWIM_REQUEST = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> LAST_AQUA_MERMAID_MOVEMENT = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> LAST_AQUA_CRAWLING = new WeakHashMap<>();
	private static final WeakHashMap<Player, FastRun.ControlType> LAST_CONTROL_TYPE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Long> LAST_MOTION_LOG_TICK = new WeakHashMap<>();
	private static final WeakHashMap<Player, Long> LAST_ATTACK_SPRINT_SUPPRESS_LOG_TICK = new WeakHashMap<>();

	private AquaManeuvreFastSwimHandler() {
	}

	public static void registerFastSwimAnimation(InitAnimatorEvent event) {
		if (!isFastSwimAnimationEnabled()) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = EpmAnimations.mermaidFastSwim();
		if (animation != null) {
			event.getAnimator().addLivingAnimation(EpmLivingMotions.FAST_SWIM, animation);
		}

		AssetAccessor<? extends StaticAnimation> surfaceAnimation = WomAnimationRefs.bipedSwimCrawl();
		if (surfaceAnimation != null) {
			event.getAnimator().addLivingAnimation(EpmLivingMotions.SURFACE_FAST_SWIM, surfaceAnimation);
		}
	}

	public static void chooseFastSwimAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		if (!isFastSwimAnimationEnabled()) {
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (player == null || !playerPatch.isEpicFightMode()) {
			return;
		}

		if (hasAquaManeuvre(playerPatch)) {
			chooseLearnedAquaFastSwimAnimation(event, player);
			return;
		}

		Parkourability parkourability = Parkourability.get(player);
		if (!isParCoolFastSwimDoing(parkourability) || event.inaction()) {
			return;
		}

		event.setMotion(EpmLivingMotions.FAST_SWIM);
	}

	private static void chooseLearnedAquaFastSwimAnimation(UpdatePlayerMotionEvent.BaseLayer event, Player player) {
		if (event.inaction() || !isLastAquaFastSwimRequestActive(player)) {
			return;
		}

		EpmLivingMotions motion = chooseLearnedAquaFastSwimMotion(player);
		event.setMotion(motion);
		logLearnedAquaMotion(player, motion);
	}

	public static boolean shouldLetWomOwnAquaFastSwim(Player player) {
		if (!isFastSwimAnimationEnabled() || player == null) {
			return false;
		}

		long tick = gameTime(player);
		TimedBoolean cached = CAN_WOM_OWN_AQUA_FAST_SWIM.get(player);
		if (cached != null && cached.tick() == tick) {
			return cached.value();
		}

		PlayerPatch<?> playerPatch = getPlayerPatch(player);
		boolean canOwn = playerPatch != null && hasAquaManeuvre(playerPatch);
		CAN_WOM_OWN_AQUA_FAST_SWIM.put(player, new TimedBoolean(tick, canOwn));
		return canOwn;
	}

	public static boolean parCoolControlRequestsAquaFastSwim(Player player) {
		return parCoolControlRequestsAquaFastSwim(player, null, false, false);
	}

	public static boolean parCoolControlRequestsAquaFastSwim(Player player, Input movementInput, boolean mermaidMovement, boolean waterDashing) {
		boolean requested = computeParCoolControlRequest(player, movementInput, mermaidMovement, waterDashing, true);
		if (player != null) {
			LAST_FAST_SWIM_REQUEST.put(player, Boolean.valueOf(requested));
		}
		return requested;
	}

	public static void rememberWomAquaState(Player player, boolean mermaidMovement, boolean crawling) {
		if (player == null) {
			return;
		}

		LAST_AQUA_MERMAID_MOVEMENT.put(player, Boolean.valueOf(mermaidMovement));
		LAST_AQUA_CRAWLING.put(player, Boolean.valueOf(crawling));
	}

	public static boolean peekParCoolControlRequestsAquaFastSwim(Player player) {
		return peekParCoolControlRequestsAquaFastSwim(player, null, false, false);
	}

	public static boolean peekParCoolControlRequestsAquaFastSwim(Player player, Input movementInput, boolean mermaidMovement, boolean waterDashing) {
		return computeParCoolControlRequest(player, movementInput, mermaidMovement, waterDashing, false);
	}

	public static boolean isLastAquaFastSwimRequestActive(Player player) {
		return Boolean.TRUE.equals(LAST_FAST_SWIM_REQUEST.get(player)) && canUseAquaFastSwimInput(player);
	}

	public static boolean isAquaFastSwimAvailable(Player player) {
		return canUseAquaFastSwimInput(player);
	}

	public static boolean diagnosticsEnabled() {
		return EPMConfig.debugAquaManeuvreFastSwimState();
	}

	public static String fastRunControlModeName() {
		FastRun.ControlType controlType = currentFastRunControlType();
		return controlType == null ? "null" : controlType.name();
	}

	public static boolean shouldSuppressManualAquaSprintDuringInaction(Player player) {
		if (!shouldLetWomOwnAquaFastSwim(player)) {
			return false;
		}

		try {
			FastRun.ControlType controlType = currentFastRunControlType();
			if (controlType != FastRun.ControlType.PressKey && controlType != FastRun.ControlType.Toggle) {
				return false;
			}

			PlayerPatch<?> playerPatch = getPlayerPatch(player);
			return playerPatch != null
					&& playerPatch.getEntityState() != null
					&& playerPatch.getEntityState().inaction();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	public static void logSuppressedManualAquaSprint(Player player) {
		if (!diagnosticsEnabled() || player == null || player.level() == null) {
			return;
		}

		long tick = player.level().getGameTime();
		Long lastTick = LAST_ATTACK_SPRINT_SUPPRESS_LOG_TICK.get(player);
		if (lastTick != null && tick - lastTick.longValue() < 20L) {
			return;
		}

		LAST_ATTACK_SPRINT_SUPPRESS_LOG_TICK.put(player, Long.valueOf(tick));
		EPM.LOGGER.info(
				"[EPM/Aqua][ATTACK_PAUSE] tick={} mode={} suppressWomSprinting=true player.swimming={} player.sprinting={} player.underWater={}",
				Long.valueOf(tick),
				fastRunControlModeName(),
				Boolean.valueOf(player.isSwimming()),
				Boolean.valueOf(player.isSprinting()),
				Boolean.valueOf(player.isUnderWater())
		);
	}

	public static boolean fastRunKeyDownForDiagnostics() {
		return fastRunKeyDown();
	}

	public static boolean fastRunKeyPressedForDiagnostics() {
		return fastRunKeyPressed();
	}

	private static boolean canUseAquaFastSwimInput(Player player) {
		return shouldLetWomOwnAquaFastSwim(player)
				&& player.isInWaterOrBubble()
				&& player.isSwimming()
				&& player.getVehicle() == null
				&& !player.isFallFlying()
				&& !player.isSpectator()
				&& !player.isDeadOrDying();
	}

	private static EpmLivingMotions chooseLearnedAquaFastSwimMotion(Player player) {
		boolean mermaidMovement = Boolean.TRUE.equals(LAST_AQUA_MERMAID_MOVEMENT.get(player));
		boolean crawling = Boolean.TRUE.equals(LAST_AQUA_CRAWLING.get(player));
		if (crawling || !player.isUnderWater()) {
			return EpmLivingMotions.SURFACE_FAST_SWIM;
		}

		return EpmLivingMotions.FAST_SWIM;
	}

	private static boolean computeParCoolControlRequest(Player player, Input movementInput, boolean mermaidMovement, boolean waterDashing, boolean mutateToggle) {
		if (!canUseAquaFastSwimInput(player)) {
			if (mutateToggle) {
				clearToggle(player);
				clearAquaState(player);
			}
			return false;
		}

		try {
			FastRun.ControlType controlType = currentFastRunControlType();
			if (mutateToggle) {
				syncControlMode(player, controlType);
			}

			if (controlType == FastRun.ControlType.Auto) {
				return autoFastSwimRequested(player, movementInput, mermaidMovement, waterDashing);
			}

			if (controlType == FastRun.ControlType.PressKey) {
				return fastRunKeyDown();
			}

			if (controlType == FastRun.ControlType.Toggle) {
				boolean enabled = Boolean.TRUE.equals(TOGGLE_FAST_SWIM.get(player));
				if (fastRunKeyPressed()) {
					enabled = !enabled;
					if (mutateToggle) {
						TOGGLE_FAST_SWIM.put(player, Boolean.valueOf(enabled));
					}
				}
				return enabled;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		return false;
	}

	private static boolean autoFastSwimRequested(Player player, Input movementInput, boolean mermaidMovement, boolean waterDashing) {
		if (waterDashing) {
			return true;
		}

		if (movementInput != null) {
			return movementInput.up || movementInput.jumping;
		}

		return !mermaidMovement && player.isSwimming();
	}

	private static boolean fastRunKeyDown() {
		try {
			boolean keyMappingDown = KeyBindings.getKeyFastRunning() != null && KeyBindings.getKeyFastRunning().isDown();
			boolean recorderDown = KeyRecorder.keyFastRunning != null && KeyRecorder.keyFastRunning.getTickKeyDown() > 0;
			return keyMappingDown || recorderDown;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean fastRunKeyPressed() {
		try {
			return KeyRecorder.keyFastRunning != null && KeyRecorder.keyFastRunning.isPressed();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static FastRun.ControlType currentFastRunControlType() {
		try {
			return ParCoolConfig.Client.FastRunControl.get();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static void clearToggle(Player player) {
		if (player != null) {
			TOGGLE_FAST_SWIM.remove(player);
		}
	}

	private static void clearAquaState(Player player) {
		if (player != null) {
			LAST_AQUA_MERMAID_MOVEMENT.remove(player);
			LAST_AQUA_CRAWLING.remove(player);
		}
	}

	private static void syncControlMode(Player player, FastRun.ControlType controlType) {
		if (player == null) {
			return;
		}

		FastRun.ControlType previousControlType = LAST_CONTROL_TYPE.put(player, controlType);
		if (previousControlType != null && previousControlType != controlType) {
			TOGGLE_FAST_SWIM.remove(player);
			LAST_FAST_SWIM_REQUEST.put(player, Boolean.FALSE);
		}
	}

	private static void logLearnedAquaMotion(Player player, EpmLivingMotions motion) {
		if (!diagnosticsEnabled() || player == null || player.level() == null) {
			return;
		}

		long tick = player.level().getGameTime();
		Long lastTick = LAST_MOTION_LOG_TICK.get(player);
		if (lastTick != null && tick - lastTick.longValue() < 20L) {
			return;
		}

		LAST_MOTION_LOG_TICK.put(player, Long.valueOf(tick));
		EPM.LOGGER.info(
				"[EPM/Aqua][MOTION] tick={} mode={} request=true setMotion={} player.swimming={} player.sprinting={} player.underWater={} wom.mermaid={} wom.crawling={}",
				Long.valueOf(tick),
				fastRunControlModeName(),
				motion.name(),
				Boolean.valueOf(player.isSwimming()),
				Boolean.valueOf(player.isSprinting()),
				Boolean.valueOf(player.isUnderWater()),
				LAST_AQUA_MERMAID_MOVEMENT.get(player),
				LAST_AQUA_CRAWLING.get(player)
		);
	}

	private static boolean isFastSwimAnimationEnabled() {
		return ModCompat.isWomLoaded() && EPMConfig.aquaManeuvreFastSwimAnimation();
	}

	private static boolean hasAquaManeuvre(PlayerPatch<?> playerPatch) {
		return WomCompatBridge.hasAquaManeuvre(playerPatch);
	}

	private static boolean isParCoolFastSwimDoing(Parkourability parkourability) {
		if (parkourability == null) {
			return false;
		}

		try {
			FastSwim fastSwim = parkourability.get(FastSwim.class);
			return fastSwim != null && fastSwim.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static PlayerPatch<?> getPlayerPatch(Player player) {
		if (player == null) {
			return null;
		}

		try {
			return EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static long gameTime(Player player) {
		return player == null || player.level() == null ? Long.MIN_VALUE : player.level().getGameTime();
	}

	private record TimedBoolean(long tick, boolean value) {
	}
}
