package dev.spake404.epm.wom.spider;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallHooks {
	private WomSpiderWallHooks() {
	}

	public static void tickMovement(Player player) {
		WomSpiderWallRunHandler.tick(player);
		WomSpiderWallSlideHandler.tick(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		WomSpiderWallMovementState.clearUnownedAfterTick(player, playerPatch, "post_tick_unowned");
		WomSpiderWallRunStateTransitionDiagnostics.tick(player, "post_movement_tick");
	}

	public static void tickYawLock(Player player) {
		WomSpiderWallYawLock.tick(player);
	}
}
