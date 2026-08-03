package dev.spake404.epm.skill.sandevistan.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;

public final class SandevistanWeatherClock {
	private static final double STALE_TICKS = 40.0D;
	private static final Map<Long, LocalClock> CLOCKS = new HashMap<>();
	private static ClientLevel currentLevel;

	private SandevistanWeatherClock() {
	}

	public static boolean shouldOverrideVanilla() {
		return SandevistanClientState.hasActiveTimeFields() || !CLOCKS.isEmpty();
	}

	public static void beginFrame(ClientLevel level) {
		if (currentLevel != level) {
			clear();
			currentLevel = level;
		}
	}

	public static double localTime(
			ClientLevel level,
			int x,
			double y,
			int z,
			double globalTime) {
		double timeScale = SandevistanClientState.timeScaleAt(level, x + 0.5D, y, z + 0.5D);
		long key = columnKey(x, z);
		LocalClock clock = CLOCKS.get(key);
		if (clock == null) {
			if (timeScale >= 1.0D) {
				return globalTime;
			}
			clock = new LocalClock(globalTime);
			CLOCKS.put(key, clock);
		}

		clock.advance(globalTime, timeScale);
		return clock.localTime;
	}

	public static void endFrame(double globalTime) {
		Iterator<LocalClock> iterator = CLOCKS.values().iterator();
		while (iterator.hasNext()) {
			LocalClock clock = iterator.next();
			if (globalTime - clock.lastSeenGlobalTime > STALE_TICKS) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		CLOCKS.clear();
		currentLevel = null;
	}

	private static long columnKey(int x, int z) {
		return (long)x << 32 ^ z & 0xFFFFFFFFL;
	}

	private static final class LocalClock {
		private double localTime;
		private double lastGlobalTime;
		private double lastSeenGlobalTime;

		private LocalClock(double globalTime) {
			this.localTime = globalTime;
			this.lastGlobalTime = globalTime;
			this.lastSeenGlobalTime = globalTime;
		}

		private void advance(double globalTime, double timeScale) {
			double elapsed = globalTime - this.lastGlobalTime;
			if (elapsed < 0.0D || elapsed > 20.0D) {
				this.localTime = globalTime;
			} else {
				this.localTime += elapsed * normalize(timeScale);
			}
			this.lastGlobalTime = globalTime;
			this.lastSeenGlobalTime = globalTime;
		}

		private static double normalize(double value) {
			return Math.max(0.05D, Math.min(1.0D, value));
		}
	}
}
