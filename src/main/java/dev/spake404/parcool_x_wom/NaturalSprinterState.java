package dev.spake404.parcool_x_wom;

import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class NaturalSprinterState {
	private NaturalSprinterState() {
	}

	public static void suppress(PlayerPatch<?> playerPatch) {
		WomCompatBridge.instance().suppressNaturalSprinter(playerPatch);
	}

	public static boolean hasNaturalSprinter(PlayerPatch<?> playerPatch) {
		return WomCompatBridge.instance().hasNaturalSprinter(playerPatch);
	}
}
