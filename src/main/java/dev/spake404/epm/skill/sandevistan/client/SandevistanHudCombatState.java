package dev.spake404.epm.skill.sandevistan.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

public final class SandevistanHudCombatState {
	private static final int COMBAT_TIMEOUT_TICKS = 100;
	private static UUID playerId;
	private static int lastCombatTick = -1;
	private static int previousHurtTime;

	private SandevistanHudCombatState() {
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null) {
			reset();
			return;
		}

		if (!player.getUUID().equals(playerId)) {
			playerId = player.getUUID();
			lastCombatTick = -1;
			previousHurtTime = player.hurtTime;
		}

		if (player.hurtTime > previousHurtTime) {
			markCombatActivity();
		}
		previousHurtTime = player.hurtTime;

		LivingEntity target = EpicFightCameraAPI.getInstance().getFocusingEntity();
		if (target != null
				&& target.isAlive()
				&& !target.isRemoved()
				&& target != player
				&& !target.isAlliedTo(player)) {
			markCombatActivity();
		}
	}

	public static void markCombatActivity() {
		Player player = Minecraft.getInstance().player;
		if (player != null) {
			playerId = player.getUUID();
			lastCombatTick = player.tickCount;
		}
	}

	public static boolean isInCombat(Player player) {
		return player != null
				&& player.getUUID().equals(playerId)
				&& lastCombatTick >= 0
				&& player.tickCount >= lastCombatTick
				&& player.tickCount - lastCombatTick <= COMBAT_TIMEOUT_TICKS;
	}

	private static void reset() {
		playerId = null;
		lastCombatTick = -1;
		previousHurtTime = 0;
	}
}
