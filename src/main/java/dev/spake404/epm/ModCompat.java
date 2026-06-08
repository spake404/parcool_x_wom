package dev.spake404.epm;

import net.minecraftforge.fml.ModList;

public final class ModCompat {
	public static final String WOM = "wom";
	public static final String INVINCIBLE = "invincible";
	public static final String NIGHTFALL = "efn";
	public static final String TACZ = "tacz";
	public static final String EPIC_ARSENAL = "epicarsenal";
	public static final String SSRCAMERA_FIXES = "ssrcamerafixes";
	public static final String VC_GLIDERS = "vc_gliders";
	private static final boolean WOM_LOADED = isLoaded(WOM);
	private static final boolean INVINCIBLE_LOADED = isLoaded(INVINCIBLE);
	private static final boolean NIGHTFALL_LOADED = isLoaded(NIGHTFALL);
	private static final boolean TACZ_LOADED = isLoaded(TACZ);
	private static final boolean EPIC_ARSENAL_LOADED = isLoaded(EPIC_ARSENAL);
	private static final boolean SSRCAMERA_FIXES_LOADED = isLoaded(SSRCAMERA_FIXES);
	private static final boolean GLIDERS_LOADED = isLoaded(VC_GLIDERS);

	private ModCompat() {
	}

	public static boolean isWomLoaded() {
		return WOM_LOADED;
	}

	public static boolean isInvincibleLoaded() {
		return INVINCIBLE_LOADED;
	}

	public static boolean isNightfallLoaded() {
		return NIGHTFALL_LOADED;
	}

	public static boolean isTaczLoaded() {
		return TACZ_LOADED;
	}

	public static boolean isEpicArsenalLoaded() {
		return EPIC_ARSENAL_LOADED;
	}

	public static boolean isSsrCameraFixesLoaded() {
		return SSRCAMERA_FIXES_LOADED;
	}

	public static boolean isGlidersLoaded() {
		return GLIDERS_LOADED;
	}

	private static boolean isLoaded(String modId) {
		try {
			return ModList.get().isLoaded(modId);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
