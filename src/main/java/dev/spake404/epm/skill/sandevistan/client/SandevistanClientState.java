package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.EpmSkillSlots;
import dev.spake404.epm.skill.sandevistan.SandevistanSkill;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import dev.spake404.epm.skill.sandevistan.SandevistanStopReason;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfile;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfiles;
import dev.spake404.epm.sound.EpmSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class SandevistanClientState {
	private static final Map<UUID, ClientActivation> ACTIVE = new ConcurrentHashMap<>();
	private static final Map<UUID, Deque<SandevistanAfterimageParticle>> AFTERIMAGES = new HashMap<>();

	private SandevistanClientState() {
	}

	public static void sync(
			UUID playerId,
			boolean active,
			int remainingTicks,
			SandevistanStopReason reason,
			ResourceLocation profileId) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || playerId == null) {
			return;
		}

		boolean wasActive = ACTIVE.containsKey(playerId);
		if (active) {
			SandevistanProfile profile = SandevistanProfiles.get(profileId);
			if (profile == null) {
				ACTIVE.remove(playerId);
				SandevistanStateView.setActive(playerId, false);
				return;
			}
			ACTIVE.put(playerId, new ClientActivation(
					level.getGameTime() + Math.max(1, remainingTicks),
					profile));
			SandevistanStateView.setActive(playerId, true);
		} else {
			ACTIVE.remove(playerId);
			SandevistanStateView.setActive(playerId, false);
		}

		if (minecraft.player != null && minecraft.player.getUUID().equals(playerId)) {
			if (!active) {
				deactivateLocalSkillContainer(minecraft.player);
			}
			if (active && !wasActive) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(EpmSounds.SANDEVISTAN_START.get(), 1.56F));
			}
		}
	}

	private static void deactivateLocalSkillContainer(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		SkillContainer container = playerPatch == null ? null : playerPatch.getSkill(EpmSkillSlots.PARKOUR);
		if (container != null
				&& container.getSkill() instanceof SandevistanSkill
				&& container.isActivated()) {
			container.deactivate();
		}
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			clear();
			return;
		}
		if (minecraft.isPaused()) {
			return;
		}

		tickAfterimages();
		pruneAfterimages();
		advancePendingAfterimages();
		Iterator<Map.Entry<UUID, ClientActivation>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ClientActivation> entry = iterator.next();
			if (level.getGameTime() >= entry.getValue().endGameTime) {
				SandevistanStateView.setActive(entry.getKey(), false);
				iterator.remove();
				continue;
			}

			Player player = level.getPlayerByUUID(entry.getKey());
			AfterimageKind afterimageKind = player == null
					? AfterimageKind.NONE
					: afterimageKind(player, entry.getValue());
			if (afterimageKind != AfterimageKind.NONE) {
				spawnAfterimage(player, afterimageKind);
			}
		}
	}

	static float remainingTicksForHud(Player player, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		ClientActivation activation = player == null ? null : ACTIVE.get(player.getUUID());
		if (level == null || activation == null) {
			return -1.0F;
		}
		return (float)Math.max(
				0.0D,
				activation.endGameTime - level.getGameTime() - Math.max(0.0F, partialTick));
	}

	public static double timeScaleFor(Entity entity) {
		if (entity == null) {
			return 1.0D;
		}

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || level != entity.level()) {
			return 1.0D;
		}

		for (Map.Entry<UUID, ClientActivation> entry : ACTIVE.entrySet()) {
			Player source = level.getPlayerByUUID(entry.getKey());
			if (source != null && (entity == source || entity == source.getRootVehicle())) {
				return 1.0D;
			}
		}
		return timeScaleAt(level, entity.getX(), entity.getY(), entity.getZ());
	}

	public static double timeScaleAt(ClientLevel level, double x, double y, double z) {
		if (level == null || ACTIVE.isEmpty()) {
			return 1.0D;
		}

		double timeScale = 1.0D;
		for (Map.Entry<UUID, ClientActivation> entry : ACTIVE.entrySet()) {
			Player source = level.getPlayerByUUID(entry.getKey());
			if (source == null) {
				continue;
			}

			ClientActivation activation = entry.getValue();
			double radiusSquared = activation.radius * activation.radius;
			if (source.distanceToSqr(x, y, z) <= radiusSquared) {
				timeScale = Math.min(timeScale, activation.profile.timeScale(source));
			}
		}
		return timeScale;
	}

	public static boolean hasActiveTimeFields() {
		return !ACTIVE.isEmpty();
	}

	public static boolean isSandevistanAfterimage(Particle particle) {
		return particle instanceof SandevistanAfterimageParticle;
	}

	private static AfterimageKind afterimageKind(Player player, ClientActivation activation) {
		int interval = activation.afterimageIntervalTicks;
		Vec3 position = player.position();
		double minDistance = EPMConfig.sandevistanAfterimageMinDistance();
		boolean movedFarEnough = activation.lastAfterimagePosition == null
				|| activation.lastAfterimagePosition.distanceToSqr(position) >= minDistance * minDistance;
		if (player.tickCount % interval == 0
				&& player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D
				&& movedFarEnough) {
			activation.lastAfterimagePosition = position;
			return AfterimageKind.MOVEMENT;
		}

		int actionInterval = EPMConfig.sandevistanStationaryActionAfterimageIntervalTicks();
		if (player.tickCount % actionInterval == 0 && isPerformingEpicFightAction(player)) {
			return AfterimageKind.STATIONARY_ACTION;
		}

		return AfterimageKind.NONE;
	}

	private static boolean isPerformingEpicFightAction(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return false;
		}

		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		try {
			if (animation != null && animation.get() instanceof AttackAnimation) {
				return true;
			}
			return playerPatch.getEntityState() != null && playerPatch.getEntityState().inaction();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static void spawnAfterimage(Player player, AfterimageKind kind) {
		try {
			SandevistanAfterimageParticle particle = kind == AfterimageKind.STATIONARY_ACTION
					? SandevistanAfterimageParticle.captureStationaryAction(player)
					: SandevistanAfterimageParticle.captureMovement(player);
			if (particle == null) {
				return;
			}

			Deque<SandevistanAfterimageParticle> afterimages = AFTERIMAGES.computeIfAbsent(
					player.getUUID(),
					ignored -> new ArrayDeque<>());
			int limit = player.isLocalPlayer()
					? EPMConfig.sandevistanAfterimageMaxCount()
					: EPMConfig.sandevistanRemoteAfterimageMaxCount();
			while (afterimages.size() >= limit) {
				SandevistanAfterimageParticle oldest = afterimages.removeFirst();
				oldest.remove();
			}

			afterimages.addLast(particle);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	static List<SandevistanAfterimageParticle> afterimagesForRendering() {
		List<SandevistanAfterimageParticle> liveAfterimages = new ArrayList<>();
		for (Deque<SandevistanAfterimageParticle> afterimages : AFTERIMAGES.values()) {
			for (SandevistanAfterimageParticle afterimage : afterimages) {
				if (afterimage.isReadyForRendering()) {
					liveAfterimages.add(afterimage);
				}
			}
		}
		return liveAfterimages;
	}

	private static void tickAfterimages() {
		for (Deque<SandevistanAfterimageParticle> afterimages : AFTERIMAGES.values()) {
			for (SandevistanAfterimageParticle afterimage : afterimages) {
				if (afterimage.isReadyForRendering()) {
					afterimage.tick();
				}
			}
		}
	}

	private static void advancePendingAfterimages() {
		for (Deque<SandevistanAfterimageParticle> afterimages : AFTERIMAGES.values()) {
			for (SandevistanAfterimageParticle afterimage : afterimages) {
				afterimage.advanceDisplayDelay();
			}
		}
	}

	private static void pruneAfterimages() {
		Iterator<Map.Entry<UUID, Deque<SandevistanAfterimageParticle>>> iterator = AFTERIMAGES.entrySet().iterator();
		while (iterator.hasNext()) {
			Deque<SandevistanAfterimageParticle> afterimages = iterator.next().getValue();
			afterimages.removeIf(particle -> !particle.isAlive());
			if (afterimages.isEmpty()) {
				iterator.remove();
			}
		}
	}

	private static void clear() {
		for (UUID playerId : ACTIVE.keySet()) {
			SandevistanStateView.setActive(playerId, false);
		}
		ACTIVE.clear();
		AFTERIMAGES.clear();
		SandevistanClientTickClock.clear();
		SandevistanParticleTickClock.clear();
		SandevistanWeatherClock.clear();
	}

	private static final class ClientActivation {
		private final long endGameTime;
		private final SandevistanProfile profile;
		private final double radius;
		private final int afterimageIntervalTicks;
		private Vec3 lastAfterimagePosition;

		private ClientActivation(long endGameTime, SandevistanProfile profile) {
			this.endGameTime = endGameTime;
			this.profile = profile;
			this.radius = profile.radius();
			this.afterimageIntervalTicks = profile.afterimageIntervalTicks();
		}
	}

	private enum AfterimageKind {
		NONE,
		MOVEMENT,
		STATIONARY_ACTION
	}
}
