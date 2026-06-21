package dev.spake404.epm;

import com.alrex.parcool.config.ParCoolConfig;

public final class EPMParCoolGate {
	private EPMParCoolGate() {
	}

	public static boolean allowCrossModSkillCompat() {
		return isParCoolActive();
	}

	public static boolean isParCoolActive() {
		try {
			return Boolean.TRUE.equals(ParCoolConfig.Client.Booleans.ParCoolIsActive.get());
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
