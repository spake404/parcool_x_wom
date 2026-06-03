package dev.spake404.epm;

import net.minecraft.world.entity.player.Player;

public final class WomSpiderWallHooks {
	private WomSpiderWallHooks() {
	}

	public static void tickMovement(Player player) {
		WomSpiderWallRunHandler.tick(player);
		WomSpiderWallSlideHandler.tick(player);
	}

	public static void tickYawLock(Player player) {
		WomSpiderWallYawLock.tick(player);
	}
}
