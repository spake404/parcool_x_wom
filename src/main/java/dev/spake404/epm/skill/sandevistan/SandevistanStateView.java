package dev.spake404.epm.skill.sandevistan;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

public final class SandevistanStateView {
	private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

	private SandevistanStateView() {
	}

	public static boolean isActive(Player player) {
		return player != null && isActive(player.getUUID());
	}

	public static boolean isActive(UUID playerId) {
		return playerId != null && ACTIVE_PLAYERS.contains(playerId);
	}

	public static void setActive(UUID playerId, boolean active) {
		if (playerId == null) {
			return;
		}

		if (active) {
			ACTIVE_PLAYERS.add(playerId);
		} else {
			ACTIVE_PLAYERS.remove(playerId);
		}
	}

	public static void clear() {
		ACTIVE_PLAYERS.clear();
	}
}
