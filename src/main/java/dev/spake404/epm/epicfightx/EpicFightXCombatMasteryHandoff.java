package dev.spake404.epm.epicfightx;

import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.naturalsprinter.NaturalSprinterState;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.asanginxst.epicfightx.registries.EFXMobEffectRegistry;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EpicFightXCombatMasteryHandoff {
	private static final int NATURAL_SPRINTER_STEP_REQUEST_TICKS = 8;
	private static final String PARCOOL_FAST_RUN_MODIFIER_NAME = "parcool.modifier.fast_run";
	private static final UUID EFX_SPEED_MODIFIER_UUID = UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635");
	private static final Map<Player, Integer> NATURAL_SPRINTER_STEP_REQUESTS = new WeakHashMap<>();

	private EpicFightXCombatMasteryHandoff() {
	}

	public static void afterCombatMasteryStopSprinting(SkillContainer container) {
		if (!EPMConfig.epicFightXCombatMasteryFastRunControlCompatibility()) {
			return;
		}
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		PlayerPatch<?> playerPatch = playerPatch(container);
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (player == null) {
			return;
		}

		boolean fastRunDoing = isFastRunDoing(player);
		boolean fastRunModifier = hasFastRunModifier(player);
		if (!fastRunDoing && !fastRunModifier) {
			return;
		}

		clearCombatMasterySpeed(player, fastRunDoing, fastRunModifier, "stop_sprinting_handoff");
		clearStaleParCoolFastRunSpeed(player, fastRunDoing, fastRunModifier, "stop_sprinting_handoff");
		markNaturalSprinterStepRequest(player, playerPatch);
	}

	public static boolean consumeNaturalSprinterHandoffStep(PlayerPatch<?> playerPatch) {
		if (playerPatch == null
				|| !EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.epicFightXCombatMasteryFastRunControlCompatibility()
				|| !EPMConfig.naturalSprinterAnimations()
				|| !EPMConfig.fastRunStartStepAnimation()) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		Integer expireTick = player == null ? null : NATURAL_SPRINTER_STEP_REQUESTS.get(player);
		if (player == null || expireTick == null) {
			return false;
		}

		NATURAL_SPRINTER_STEP_REQUESTS.remove(player);
		if (player.tickCount > expireTick.intValue()
				|| !player.isLocalPlayer()
				|| !ModCompat.isWomLoaded()
				|| !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return false;
		}

		if (EPMConfig.debugEpicFightXCombatMasterySprintState()) {
			EPM.LOGGER.info(
					"[EPM/EFX_CM2][HANDOFF_STEP] phase=consume tick={} expireTick={} fastRunDoing={} playerSprinting={}",
					Integer.valueOf(player.tickCount),
					expireTick,
					Boolean.valueOf(isFastRunDoing(player)),
					Boolean.valueOf(player.isSprinting()));
		}
		return true;
	}

	private static void markNaturalSprinterStepRequest(Player player, PlayerPatch<?> playerPatch) {
		if (player == null
				|| playerPatch == null
				|| !EPMParCoolGate.allowCrossModSkillCompat()
				|| !player.isLocalPlayer()
				|| !ModCompat.isWomLoaded()
				|| !EPMConfig.naturalSprinterAnimations()
				|| !EPMConfig.fastRunStartStepAnimation()
				|| !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return;
		}

		int expireTick = player.tickCount + NATURAL_SPRINTER_STEP_REQUEST_TICKS;
		NATURAL_SPRINTER_STEP_REQUESTS.put(player, Integer.valueOf(expireTick));
		if (EPMConfig.debugEpicFightXCombatMasterySprintState()) {
			EPM.LOGGER.info(
					"[EPM/EFX_CM2][HANDOFF_STEP] phase=mark tick={} expireTick={} fastRunDoing={} playerSprinting={}",
					Integer.valueOf(player.tickCount),
					Integer.valueOf(expireTick),
					Boolean.valueOf(isFastRunDoing(player)),
					Boolean.valueOf(player.isSprinting()));
		}
	}

	private static void clearCombatMasterySpeed(Player player, boolean fastRunDoing, boolean fastRunModifier, String phase) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		MobEffectInstance effect = effect(player);
		boolean hadEffect = effect != null;
		boolean hadModifier = movementSpeed != null && movementSpeed.getModifier(EFX_SPEED_MODIFIER_UUID) != null;
		if (!hadEffect && !hadModifier) {
			return;
		}

		if (hadEffect) {
			player.removeEffect(EFXMobEffectRegistry.EFX_SPEED.get());
		}
		if (hadModifier) {
			movementSpeed.removeModifier(EFX_SPEED_MODIFIER_UUID);
		}

		if (EPMConfig.debugEpicFightXCombatMasterySprintState()) {
			EPM.LOGGER.info(
					"[EPM/EFX_CM2][SPEED_CLEANUP] phase={} tick={} fastRunDoing={} fastRunModifier={} removedEffect={} removedModifier={} speedValueAfter={}",
					phase,
					Integer.valueOf(player.tickCount),
					Boolean.valueOf(fastRunDoing),
					Boolean.valueOf(fastRunModifier),
					Boolean.valueOf(hadEffect),
					Boolean.valueOf(hadModifier),
					Double.valueOf(movementSpeed == null ? Double.NaN : movementSpeed.getValue()));
		}
	}

	private static void clearStaleParCoolFastRunSpeed(Player player, boolean fastRunDoing, boolean fastRunModifier, String phase) {
		if (player == null || fastRunDoing || !fastRunModifier) {
			return;
		}

		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed == null) {
			return;
		}

		double speedBefore = movementSpeed.getValue();
		AttributeModifier removedModifier = null;
		try {
			for (AttributeModifier modifier : movementSpeed.getModifiers()) {
				if (PARCOOL_FAST_RUN_MODIFIER_NAME.equals(modifier.getName())) {
					removedModifier = modifier;
					movementSpeed.removeModifier(modifier.getId());
					break;
				}
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		if (EPMConfig.debugEpicFightXCombatMasterySprintState()) {
			EPM.LOGGER.info(
					"[EPM/EFX_CM2][PARCOOL_SPEED_CLEANUP] phase={} tick={} fastRunDoing={} playerSprinting={} removedModifier={} speedBefore={} speedAfter={}",
					phase,
					Integer.valueOf(player.tickCount),
					Boolean.valueOf(fastRunDoing),
					Boolean.valueOf(player.isSprinting()),
					modifierSummary(removedModifier),
					Double.valueOf(speedBefore),
					Double.valueOf(movementSpeed.getValue()));
		}
	}

	private static PlayerPatch<?> playerPatch(SkillContainer container) {
		try {
			return container == null ? null : container.getExecutor();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static MobEffectInstance effect(Player player) {
		try {
			return player == null ? null : player.getEffect(EFXMobEffectRegistry.EFX_SPEED.get());
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isFastRunDoing(Player player) {
		try {
			Parkourability parkourability = player == null ? null : Parkourability.get(player);
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasFastRunModifier(Player player) {
		AttributeInstance movementSpeed = player == null ? null : player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed == null) {
			return false;
		}

		try {
			for (AttributeModifier modifier : movementSpeed.getModifiers()) {
				if (PARCOOL_FAST_RUN_MODIFIER_NAME.equals(modifier.getName())) {
					return true;
				}
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
		return false;
	}

	private static String modifierSummary(AttributeModifier modifier) {
		if (modifier == null) {
			return "none";
		}
		return modifier.getName() + " amount=" + modifier.getAmount() + " operation=" + modifier.getOperation();
	}
}
