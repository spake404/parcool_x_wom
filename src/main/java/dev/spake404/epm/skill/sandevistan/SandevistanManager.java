package dev.spake404.epm.skill.sandevistan;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.network.SandevistanNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

public final class SandevistanManager {
	private static final UUID SPEED_MODIFIER_ID = UUID.fromString("bdb9a37a-e30c-4ffd-8097-308e9fcb2414");
	private static final UUID BASIC_ATTACK_LISTENER_ID = UUID.fromString("38a5af50-7b5b-4e67-8f4f-04cc6bb12093");
	private static final UUID ACTION_LISTENER_ID = UUID.fromString("0e376a28-fb2b-459a-960c-593acb774277");
	private static final UUID SKILL_CAST_LISTENER_ID = UUID.fromString("f3d74bc5-7757-457e-96a2-af5cf1b6339f");
	private static final Map<UUID, Activation> ACTIVE = new HashMap<>();

	private SandevistanManager() {
	}

	public static boolean activate(SkillContainer container) {
		ServerPlayer player = container == null || container.getServerExecutor() == null
				? null
				: container.getServerExecutor().getOriginal();
		if (!canActivate(player, container)) {
			if (container != null) {
				container.deactivate();
			}
			return false;
		}

		ACTIVE.put(player.getUUID(), new Activation(player, container));
		SandevistanStateView.setActive(player.getUUID(), true);
		applySpeedModifier(player);
		registerAttackListeners(player);
		SandevistanNetwork.broadcastState(player, true, container.getRemainDuration(), null);
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

		SandevistanStateView.setActive(player.getUUID(), false);
		removeSpeedModifier(player);
		removeAttackListeners(player);
		if (removed.container.isActivated()) {
			if (cancelContainer && (reason == SandevistanStopReason.ATTACK || reason == SandevistanStopReason.MANUAL)) {
				removed.container.getSkill().cancelOnServer(removed.container, null);
			}
			removed.container.deactivate();
		}
		SandevistanNetwork.broadcastState(player, false, 0, reason);
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

	public static int tickIntervalFor(Entity entity) {
		if (!EPMConfig.sandevistanEnabled() || entity == null) {
			return 1;
		}

		double radiusSquared = EPMConfig.sandevistanRadius() * EPMConfig.sandevistanRadius();
		boolean affected = false;
		for (Activation activation : ACTIVE.values()) {
			ServerPlayer source = activation.player;
			if (entity == source || entity == source.getRootVehicle()) {
				return 1;
			}
			if (source.level() == entity.level() && source.distanceToSqr(entity) <= radiusSquared) {
				affected = true;
				break;
			}
		}
		if (!affected) {
			return 1;
		}
		return timeDilationInterval();
	}

	public static int remainingTicks(ServerPlayer player) {
		Activation activation = player == null ? null : ACTIVE.get(player.getUUID());
		if (activation == null) {
			return 0;
		}
		return Math.max(0, activation.container.getRemainDuration());
	}

	public static int timeDilationInterval() {
		return Math.max(1, (int)Math.round(1.0D / EPMConfig.sandevistanTimeScale()));
	}

	private static boolean canActivate(ServerPlayer player, SkillContainer container) {
		return EPMConfig.sandevistanEnabled()
				&& isValid(player)
				&& !isActive(player)
				&& container != null
				&& container.isActivated();
	}

	private static boolean isValid(ServerPlayer player) {
		return player != null
				&& player.isAlive()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& player.connection != null;
	}

	private static void applySpeedModifier(ServerPlayer player) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed == null) {
			return;
		}

		movementSpeed.removeModifier(SPEED_MODIFIER_ID);
		double amount = Math.max(0.0D, EPMConfig.sandevistanPlayerSpeedMultiplier() - 1.0D);
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

	private static void registerAttackListeners(ServerPlayer player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		PlayerEventListener listener = playerPatch.getEventListener();
		listener.addEventListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT, BASIC_ATTACK_LISTENER_ID,
				event -> requestStop(player, SandevistanStopReason.ATTACK));
		listener.addEventListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, ACTION_LISTENER_ID, event -> {
			DynamicAnimation animation = event.getAnimation().get();
			if (animation != null && animation.isBasicAttackAnimation()) {
				requestStop(player, SandevistanStopReason.ATTACK);
			}
		});
		listener.addEventListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, SKILL_CAST_LISTENER_ID,
				event -> stopForOffensiveSkill(player, event));
	}

	private static void removeAttackListeners(ServerPlayer player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		PlayerEventListener listener = playerPatch.getEventListener();
		listener.removeListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT, BASIC_ATTACK_LISTENER_ID);
		listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, ACTION_LISTENER_ID);
		listener.removeListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, SKILL_CAST_LISTENER_ID);
	}

	private static void stopForOffensiveSkill(ServerPlayer player, SkillCastEvent event) {
		if (event == null || event.getSkillContainer() == null || event.getSkillContainer().getSkill() == null) {
			return;
		}

		Object category = event.getSkillContainer().getSkill().getCategory();
		if (category == SkillCategories.BASIC_ATTACK || category == SkillCategories.WEAPON_INNATE) {
			requestStop(player, SandevistanStopReason.ATTACK);
		}
	}

	private static final class Activation {
		private final ServerPlayer player;
		private final SkillContainer container;
		private SandevistanStopReason pendingStop;

		private Activation(ServerPlayer player, SkillContainer container) {
			this.player = player;
			this.container = container;
		}
	}
}

