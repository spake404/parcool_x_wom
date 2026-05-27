package dev.spake404.epm;

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

	public static boolean consumeStep(PlayerPatch<?> playerPatch) {
		return WomCompatBridge.instance().consumeNaturalSprinterStep(playerPatch);
	}
}
