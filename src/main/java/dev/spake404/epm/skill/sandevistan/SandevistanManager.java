package dev.spake404.epm.skill.sandevistan;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.network.SandevistanNetwork;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class SandevistanManager {
	private static final double MANUAL_STOP_COOLDOWN_REFUND_FACTOR = 0.60D;
	private static final double MINIMUM_REMAINING_COOLDOWN_SECONDS = 10.0D;
	private static final UUID SPEED_MODIFIER_ID = UUID.fromString("bdb9a37a-e30c-4ffd-8097-308e9fcb2414");
	private static final Map<UUID, Activation> ACTIVE = new HashMap<>();

	private SandevistanManager() {
	}

	public static boolean activate(
			SkillContainer container,
			int initialDurationTicks,
			int effectiveMaxDurationTicks,
			int baseDurationTicks) {
		ServerPlayer player = container == null || container.getServerExecutor() == null
				? null
				: container.getServerExecutor().getOriginal();
		if (!canActivate(player, container)) {
			if (container != null) {
				container.deactivate();
			}
			return false;
		}

		SandevistanProfile profile = ((SandevistanSkill)container.getSkill()).getProfile();
		baseDurationTicks = Math.max(1, baseDurationTicks);
		effectiveMaxDurationTicks = Math.max(baseDurationTicks, effectiveMaxDurationTicks);
		initialDurationTicks = Math.max(1, Math.min(effectiveMaxDurationTicks, initialDurationTicks));
		container.setMaxDuration(effectiveMaxDurationTicks);
		if (profile.partialChargeActivation()) {
			container.setMaxResource(effectiveMaxDurationTicks / 20.0F);
		}
		container.setDuration(initialDurationTicks);

		Activation activation = new Activation(
				player,
				container,
				profile,
				baseDurationTicks,
				effectiveMaxDurationTicks);
		ACTIVE.put(player.getUUID(), activation);
		SandevistanStateView.setActive(player.getUUID(), true);
		applySpeedModifier(player, profile);
		syncContainerLimits(player, container, effectiveMaxDurationTicks);
		syncDuration(player, container);
		if (profile.partialChargeActivation()) {
			applyCooldownResource(player, container, 0.0F);
		}
		SandevistanNetwork.broadcastState(player, true, container.getRemainDuration(), null, profile.id());
		return true;
	}

	public static void requestStop(ServerPlayer player, SandevistanStopReason reason) {
		Activation activation = player == null ? null : ACTIVE.get(player.getUUID());
		if (activation != null && activation.pendingStop == null) {
			activation.pendingStop = reason;
		}
	}

	public static void stop(ServerPlayer player, SandevistanStopReason reason) {
		stop(player, reason, true);
	}

	private static void stop(ServerPlayer player, SandevistanStopReason reason, boolean cancelContainer) {
		Activation removed = player == null ? null : ACTIVE.remove(player.getUUID());
		if (removed == null) {
			return;
		}

		int remainingDurationTicks = Math.max(0, removed.container.getRemainDuration());
		float cooldownResource = -1.0F;
		if (!removed.profile.partialChargeActivation()
				&& reason == SandevistanStopReason.MANUAL
				&& removed.profile.manualCooldownRefundEnabled()) {
			cooldownResource = calculateManualStopCooldownResource(removed);
		}

		SandevistanStateView.setActive(player.getUUID(), false);
		removeSpeedModifier(player);
		if (removed.container.isActivated()) {
			if (cancelContainer && reason == SandevistanStopReason.MANUAL) {
				removed.container.getSkill().cancelOnServer(removed.container, null);
			}
			removed.container.deactivate();
		}
		if (removed.profile.partialChargeActivation()) {
			if (remainingDurationTicks > 0) {
				applyPartialReadyState(player, removed, remainingDurationTicks);
			} else {
				applyPartialCooldownState(player, removed);
			}
		} else if (cooldownResource >= 0.0F) {
			applyCooldownResource(player, removed.container, cooldownResource);
		}
		SandevistanNetwork.broadcastState(player, false, 0, reason, removed.profile.id());
	}

	private static float calculateManualStopCooldownResource(Activation activation) {
		double remainingRatio = Math.max(0.0D, Math.min(
				1.0D,
				(double)activation.container.getRemainDuration() / activation.effectiveMaxDurationTicks));
		double refund = activation.maxCooldownResource
				* remainingRatio
				* MANUAL_STOP_COOLDOWN_REFUND_FACTOR;
		double maximumResource = Math.max(
				0.0D,
				activation.maxCooldownResource - MINIMUM_REMAINING_COOLDOWN_SECONDS);
		return (float)Math.min(
				maximumResource,
				activation.container.getResource() + refund);
	}

	private static void applyCooldownResource(
			ServerPlayer player,
			SkillContainer container,
			float resource) {
		container.setResource(resource);
		EpicFightNetworkManager.sendToPlayer(
				SPSetSkillContainerValue.resource(container.getSlot(), container.getResource(), player.getId()),
				player);
		EpicFightNetworkManager.sendToPlayer(
				SPSetSkillContainerValue.stacks(container.getSlot(), container.getStack(), player.getId()),
				player);
	}

	private static void applyPartialReadyState(
			ServerPlayer player,
			Activation activation,
			int remainingDurationTicks) {
		SkillContainer container = activation.container;
		container.setMaxDuration(activation.effectiveMaxDurationTicks);
		container.setMaxResource(activation.effectiveMaxDurationTicks / 20.0F);
		container.setResource(remainingDurationTicks / 20.0F);
		container.setStack(1);
		syncContainerLimits(player, container, activation.effectiveMaxDurationTicks);
		applyCooldownResource(player, container, container.getResource());
	}

	private static void applyPartialCooldownState(ServerPlayer player, Activation activation) {
		SkillContainer container = activation.container;
		container.setMaxDuration(activation.baseDurationTicks);
		container.setMaxResource(container.getSkill().getConsumption());
		container.setStack(0);
		container.setResource(0.0F);
		syncContainerLimits(player, container, activation.baseDurationTicks);
		applyCooldownResource(player, container, 0.0F);
	}

	private static void syncContainerLimits(
			ServerPlayer player,
			SkillContainer container,
			int maxDurationTicks) {
		EpicFightNetworkManager.sendToPlayer(
				SPSetSkillContainerValue.maxDuration(
						container.getSlot(),
						Math.max(1, maxDurationTicks),
						player.getId()),
				player);
		EpicFightNetworkManager.sendToPlayer(
				SPSetSkillContainerValue.maxResource(
						container.getSlot(),
						container.getMaxResource(),
						player.getId()),
				player);
	}

	private static void syncDuration(ServerPlayer player, SkillContainer container) {
		EpicFightNetworkManager.sendToPlayer(
				SPSetSkillContainerValue.duration(container.getSlot(), container.getRemainDuration(), player.getId()),
				player);
	}

	public static void tick() {
		for (Activation activation : new ArrayList<>(ACTIVE.values())) {
			ServerPlayer player = activation.player;
			if (activation.pendingStop != null) {
				stop(player, activation.pendingStop);
				continue;
			}
			if (!isValid(player)) {
				stop(player, SandevistanStopReason.INVALID_STATE);
				continue;
			}
			if (!activation.container.isActivated() || activation.container.getRemainDuration() <= 0) {
				stop(player, SandevistanStopReason.TIMEOUT, false);
				continue;
			}
		}
	}

	public static boolean isActive(Player player) {
		return player != null && ACTIVE.containsKey(player.getUUID());
	}

	public static double timeScaleFor(Entity entity) {
		if (!EPMConfig.sandevistanEnabled() || entity == null) {
			return 1.0D;
		}

		double timeScale = 1.0D;
		for (Activation activation : ACTIVE.values()) {
			ServerPlayer source = activation.player;
			if (entity == source || entity == source.getRootVehicle()) {
				return 1.0D;
			}
			double radiusSquared = activation.profile.radius() * activation.profile.radius();
			if (source.level() == entity.level() && source.distanceToSqr(entity) <= radiusSquared) {
				timeScale = Math.min(timeScale, activation.profile.timeScale(source));
			}
		}
		return timeScale;
	}

	public static int remainingTicks(ServerPlayer player) {
		Activation activation = player == null ? null : ACTIVE.get(player.getUUID());
		return activation == null ? 0 : Math.max(0, activation.container.getRemainDuration());
	}

	public static SandevistanProfile activeProfile(ServerPlayer player) {
		Activation activation = player == null ? null : ACTIVE.get(player.getUUID());
		return activation == null ? null : activation.profile;
	}

	public static double outgoingDamageMultiplier(ServerPlayer player) {
		SandevistanProfile profile = activeProfile(player);
		return profile == null ? 1.0D : profile.outgoingDamageMultiplier(player);
	}

	public static double incomingDamageMultiplier(ServerPlayer player, DamageSource source) {
		SandevistanProfile profile = activeProfile(player);
		if (profile == null || source == null) {
			return 1.0D;
		}
		if (source.is(DamageTypeTags.IS_FIRE)
				|| source.is(DamageTypes.WITHER)
				|| source.is(DamageTypes.WITHER_SKULL)) {
			return profile.fireWitherDamageMultiplier();
		}
		if (source.is(DamageTypeTags.IS_FALL)) {
			return profile.incomingDamageMultiplier() * profile.fallDamageMultiplier();
		}
		return profile.incomingDamageMultiplier();
	}

	public static void rewardKill(ServerPlayer player) {
		Activation activation = player == null ? null : ACTIVE.get(player.getUUID());
		if (activation == null) {
			return;
		}

		SandevistanProfile profile = activation.profile;
		boolean durationChanged = false;
		if (profile.killDurationRestoreFraction() > 0.0D) {
			int restoreTicks = Math.max(
					1,
					(int)Math.round(activation.baseDurationTicks * profile.killDurationRestoreFraction()));
			int newDuration = Math.min(
					activation.effectiveMaxDurationTicks,
					activation.container.getRemainDuration() + restoreTicks);
			durationChanged = newDuration != activation.container.getRemainDuration();
			activation.container.setDuration(newDuration);
		}
		if (profile.killHealthRestoreFraction() > 0.0D) {
			player.heal((float)(player.getMaxHealth() * profile.killHealthRestoreFraction()));
		}
		if (profile.killStaminaRestoreFraction() > 0.0D) {
			PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
			if (playerPatch != null) {
				float restoredStamina = (float)(playerPatch.getMaxStamina() * profile.killStaminaRestoreFraction());
				playerPatch.setStamina(Math.min(
						playerPatch.getMaxStamina(),
						playerPatch.getStamina() + restoredStamina));
			}
		}
		if (durationChanged) {
			syncDuration(player, activation.container);
			SandevistanNetwork.broadcastState(
					player,
					true,
					activation.container.getRemainDuration(),
					null,
					profile.id());
		}
	}

	private static boolean canActivate(ServerPlayer player, SkillContainer container) {
		return EPMConfig.sandevistanEnabled()
				&& isValid(player)
				&& !isActive(player)
				&& container != null
				&& container.getSkill() instanceof SandevistanSkill
				&& container.isActivated();
	}

	private static boolean isValid(ServerPlayer player) {
		return player != null
				&& player.isAlive()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& player.connection != null;
	}

	private static void applySpeedModifier(ServerPlayer player, SandevistanProfile profile) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed == null) {
			return;
		}

		movementSpeed.removeModifier(SPEED_MODIFIER_ID);
		double amount = profile.playerSpeedMultiplier() - 1.0D;
		if (Math.abs(amount) <= 1.0E-6D) {
			return;
		}
		movementSpeed.addTransientModifier(new AttributeModifier(
				SPEED_MODIFIER_ID,
				"epic_parcool_momentum.sandevistan_speed",
				amount,
				AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	private static void removeSpeedModifier(ServerPlayer player) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed != null) {
			movementSpeed.removeModifier(SPEED_MODIFIER_ID);
		}
	}

	private static final class Activation {
		private final ServerPlayer player;
		private final SkillContainer container;
		private final SandevistanProfile profile;
		private final int baseDurationTicks;
		private final int effectiveMaxDurationTicks;
		private final float maxCooldownResource;
		private SandevistanStopReason pendingStop;

		private Activation(
				ServerPlayer player,
				SkillContainer container,
				SandevistanProfile profile,
				int baseDurationTicks,
				int effectiveMaxDurationTicks) {
			this.player = player;
			this.container = container;
			this.profile = profile;
			this.baseDurationTicks = baseDurationTicks;
			this.effectiveMaxDurationTicks = effectiveMaxDurationTicks;
			this.maxCooldownResource = Math.max(0.0F, container.getMaxResource());
		}
	}
}
