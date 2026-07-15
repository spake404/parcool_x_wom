package dev.spake404.epm.skill.sandevistan.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class SandevistanClientTickClock {
	private static final Map<Entity, LocalClock> CLOCKS = new WeakHashMap<>();

	private SandevistanClientTickClock() {
	}

	public static boolean beginTick(Entity entity, int interval) {
		if (entity == null || interval <= 1) {
			CLOCKS.remove(entity);
			return true;
		}

		LocalClock clock = CLOCKS.computeIfAbsent(entity, ignored -> new LocalClock(interval));
		clock.updateInterval(interval);
		clock.accumulatedTicks++;
		clock.ticking = clock.accumulatedTicks >= clock.interval;
		if (clock.ticking) {
			clock.accumulatedTicks = 0;
		}
		return clock.ticking;
	}

	public static void endTick(Entity entity) {
		LocalClock clock = CLOCKS.get(entity);
		if (clock == null || !clock.ticking) {
			return;
		}

		clock.lastLocalTickGameTime = entity.level().getGameTime();
		clock.ticking = false;
	}

	public static float localPartialTick(Entity entity, float globalPartialTick) {
		LocalClock clock = CLOCKS.get(entity);
		if (clock == null || clock.lastLocalTickGameTime == Long.MIN_VALUE) {
			return globalPartialTick;
		}

		double elapsedGlobalTicks = entity.level().getGameTime()
				- clock.lastLocalTickGameTime
				+ globalPartialTick;
		return Mth.clamp((float)(elapsedGlobalTicks / clock.interval), 0.0F, 1.0F);
	}

	public static void clear() {
		CLOCKS.clear();
	}

	private static final class LocalClock {
		private int interval;
		private int accumulatedTicks;
		private long lastLocalTickGameTime = Long.MIN_VALUE;
		private boolean ticking;

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
