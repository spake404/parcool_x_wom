package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import dev.spake404.epm.skill.sandevistan.SandevistanStopReason;
import dev.spake404.epm.skill.sandevistan.network.SandevistanNetwork;
import dev.spake404.epm.sound.EpmSounds;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SandevistanClientState {
	private static final Map<UUID, ClientActivation> ACTIVE = new HashMap<>();
	private static final Map<UUID, Deque<SandevistanAfterimageParticle>> AFTERIMAGES = new HashMap<>();

	private SandevistanClientState() {
	}

	public static void sync(
			UUID playerId,
			boolean active,
			int remainingTicks,
			SandevistanStopReason reason,
			double timeScale,
			double radius,
			int afterimageIntervalTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || playerId == null) {
			return;
		}

		boolean wasActive = ACTIVE.containsKey(playerId);
		if (active) {
			ACTIVE.put(playerId, new ClientActivation(
					level.getGameTime() + Math.max(1, remainingTicks),
					timeScale,
					radius,
					afterimageIntervalTicks));
			SandevistanStateView.setActive(playerId, true);
		} else {
			ACTIVE.remove(playerId);
			SandevistanStateView.setActive(playerId, false);
		}

		if (minecraft.player != null && minecraft.player.getUUID().equals(playerId)) {
			if (active && !wasActive) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(EpmSounds.SANDEVISTAN_START.get(), 1.56F));
				minecraft.player.displayClientMessage(Component.translatable("epic_parcool_momentum.sandevistan.activated"), true);
			} else if (!active && wasActive) {
				minecraft.player.displayClientMessage(endMessage(reason), true);
			}
		}
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			clear();
			return;
		}

		pruneAfterimages();
		Iterator<Map.Entry<UUID, ClientActivation>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ClientActivation> entry = iterator.next();
			if (level.getGameTime() >= entry.getValue().endGameTime) {
				SandevistanStateView.setActive(entry.getKey(), false);
				iterator.remove();
				continue;
			}

			Player player = level.getPlayerByUUID(entry.getKey());
			if (player != null && shouldSpawnAfterimage(player, entry.getValue())) {
				spawnAfterimage(player);
			}
		}
	}

	public static int tickIntervalFor(Entity entity) {
		if (entity == null) {
			return 1;
		}

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || level != entity.level()) {
			return 1;
		}

		int interval = 1;
		for (Map.Entry<UUID, ClientActivation> entry : ACTIVE.entrySet()) {
			Player source = level.getPlayerByUUID(entry.getKey());
			ClientActivation activation = entry.getValue();
			double radiusSquared = activation.radius * activation.radius;
			if (source != null && (entity == source || entity == source.getRootVehicle())) {
				return 1;
			}
			if (source != null && source.distanceToSqr(entity) <= radiusSquared) {
				interval = Math.max(interval, activation.timeDilationInterval);
			}
		}
		return interval;
	}

	public static void onLocalAttackInput() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !SandevistanStateView.isActive(minecraft.player)) {
			return;
		}

		ACTIVE.remove(minecraft.player.getUUID());
		SandevistanStateView.setActive(minecraft.player.getUUID(), false);
		SandevistanNetwork.sendAttackStopRequest();
	}

	private static boolean shouldSpawnAfterimage(Player player, ClientActivation activation) {
		int interval = activation.afterimageIntervalTicks;
		if (player.tickCount % interval != 0 || player.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-4D) {
			return false;
		}

		Vec3 position = player.position();
		double minDistance = EPMConfig.sandevistanAfterimageMinDistance();
		if (activation.lastAfterimagePosition != null
				&& activation.lastAfterimagePosition.distanceToSqr(position) < minDistance * minDistance) {
			return false;
		}

		activation.lastAfterimagePosition = position;
		return true;
	}

	private static void spawnAfterimage(Player player) {
		try {
			SandevistanAfterimageParticle particle = SandevistanAfterimageParticle.capture(player);
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

			Minecraft.getInstance().particleEngine.add(particle);
			afterimages.addLast(particle);
		} catch (RuntimeException | LinkageError ignored) {
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

	private static Component endMessage(SandevistanStopReason reason) {
		if (reason == SandevistanStopReason.ATTACK) {
			return Component.translatable("epic_parcool_momentum.sandevistan.ended.attack");
		}
		if (reason == SandevistanStopReason.TIMEOUT) {
			return Component.translatable("epic_parcool_momentum.sandevistan.ended.timeout");
		}
		return Component.translatable("epic_parcool_momentum.sandevistan.ended");
	}

	private static void clear() {
		for (UUID playerId : ACTIVE.keySet()) {
			SandevistanStateView.setActive(playerId, false);
		}
		ACTIVE.clear();
		AFTERIMAGES.clear();
		SandevistanClientTickClock.clear();
	}

	private static final class ClientActivation {
		private final long endGameTime;
		private final int timeDilationInterval;
		private final double radius;
		private final int afterimageIntervalTicks;
		private Vec3 lastAfterimagePosition;

		private ClientActivation(long endGameTime, double timeScale, double radius, int afterimageIntervalTicks) {
			this.endGameTime = endGameTime;
			this.timeDilationInterval = Math.max(1, (int)Math.round(1.0D / Math.max(0.1D, timeScale)));
			this.radius = Math.max(0.0D, radius);
			this.afterimageIntervalTicks = Math.max(1, afterimageIntervalTicks);
		}
	}
}
