package dev.spake404.parcool_x_wom;

import net.minecraftforge.fml.ModList;

public final class ModCompat {
	public static final String WOM = "wom";
	public static final String INVINCIBLE = "invincible";
	public static final String NIGHTFALL = "efn";

	private ModCompat() {
	}

	public static boolean isWomLoaded() {
		return isLoaded(WOM);
	}

	public static boolean isInvincibleLoaded() {
		return isLoaded(INVINCIBLE);
	}

	public static boolean isNightfallLoaded() {
		return isLoaded(NIGHTFALL);
	}

	private static boolean isLoaded(String modId) {
		try {
			return ModList.get().isLoaded(modId);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
