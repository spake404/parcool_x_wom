package dev.spake404.epm.skill.sandevistan;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;

public final class SandevistanEntityTickClock {
	private static final Map<Entity, LocalClock> CLOCKS = new WeakHashMap<>();

	private SandevistanEntityTickClock() {
	}

	public static boolean shouldTick(Entity entity, int interval) {
		if (entity == null || interval <= 1) {
			CLOCKS.remove(entity);
			return true;
		}

		LocalClock clock = CLOCKS.computeIfAbsent(entity, ignored -> new LocalClock(interval));
		clock.updateInterval(interval);
		clock.accumulatedTicks++;
		if (clock.accumulatedTicks < clock.interval) {
			return false;
		}

		clock.accumulatedTicks = 0;
		return true;
	}

	private static final class LocalClock {
		private int interval;
		private int accumulatedTicks;

		private LocalClock(int interval) {
			this.interval = Math.max(1, interval);
			this.accumulatedTicks = this.interval - 1;
		}

		private void updateInterval(int interval) {
			int normalizedInterval = Math.max(1, interval);
			if (this.interval != normalizedInterval) {
				this.interval = normalizedInterval;
				this.accumulatedTicks = Math.min(this.accumulatedTicks, normalizedInterval - 1);
			}
		}
	}
}
