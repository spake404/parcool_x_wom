package dev.spake404.epm.compat;

import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomCompatBridge {
	private static final WomCompat INSTANCE = create();

	private WomCompatBridge() {
	}

	public static WomCompat instance() {
		return INSTANCE;
	}

	public static boolean hasAquaManeuvre(PlayerPatch<?> playerPatch) {
		return INSTANCE.hasAquaManeuvre(playerPatch);
	}

	private static WomCompat create() {
		if (!ModCompat.isWomLoaded()) {
			return new NoopWomCompat();
		}

		try {
			Class<?> compatClass = Class.forName("dev.spake404.epm.compat.LoadedWomCompat");
			return (WomCompat) compatClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return new NoopWomCompat();
		}
	}
}
