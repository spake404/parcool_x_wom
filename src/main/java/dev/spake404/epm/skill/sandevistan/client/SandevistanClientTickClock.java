package dev.spake404.epm.skill.sandevistan.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class SandevistanClientTickClock {
	private static final Map<Entity, LocalClock> CLOCKS = new WeakHashMap<>();

	private SandevistanClientTickClock() {
	}

	public static boolean beginTick(Entity entity, double timeScale) {
		if (entity == null || timeScale >= 1.0D) {
			CLOCKS.remove(entity);
			return true;
		}

		LocalClock clock = CLOCKS.computeIfAbsent(entity, ignored -> new LocalClock(timeScale));
		clock.updateTimeScale(timeScale);
		clock.accumulator += clock.timeScale;
		clock.ticking = clock.accumulator >= 1.0D;
		if (clock.ticking) {
			clock.accumulator -= 1.0D;
		} else if (clock.lastLocalTickGameTime != Long.MIN_VALUE) {
			clock.nextLocalTickGameTime = entity.level().getGameTime() + clock.globalTicksUntilNextTick();
		}
		return clock.ticking;
	}

	public static void endTick(Entity entity) {
		LocalClock clock = CLOCKS.get(entity);
		if (clock == null || !clock.ticking) {
			return;
		}

		long gameTime = entity.level().getGameTime();
		clock.lastLocalTickGameTime = gameTime;
		clock.nextLocalTickGameTime = gameTime + clock.globalTicksUntilNextTick();
		clock.ticking = false;
	}

	public static float localPartialTick(Entity entity, float globalPartialTick) {
		LocalClock clock = CLOCKS.get(entity);
		if (clock == null
				|| clock.lastLocalTickGameTime == Long.MIN_VALUE
				|| clock.nextLocalTickGameTime <= clock.lastLocalTickGameTime) {
			return globalPartialTick;
		}

		double elapsedGlobalTicks = entity.level().getGameTime()
				- clock.lastLocalTickGameTime
				+ globalPartialTick;
		double intervalGlobalTicks = clock.nextLocalTickGameTime - clock.lastLocalTickGameTime;
		return Mth.clamp((float)(elapsedGlobalTicks / intervalGlobalTicks), 0.0F, 1.0F);
	}

	public static void clear() {
		CLOCKS.clear();
	}

	private static final class LocalClock {
		private double timeScale;
		private double accumulator;
		private long lastLocalTickGameTime = Long.MIN_VALUE;
		private long nextLocalTickGameTime = Long.MIN_VALUE;
		private boolean ticking;

		private LocalClock(double timeScale) {
			this.timeScale = normalize(timeScale);
			this.accumulator = 1.0D - this.timeScale;
		}

		private void updateTimeScale(double timeScale) {
			double normalized = normalize(timeScale);
			if (Math.abs(this.timeScale - normalized) > 1.0E-6D) {
				this.timeScale = normalized;
			}
		}

		private long globalTicksUntilNextTick() {
			double remaining = Math.max(0.0D, 1.0D - this.accumulator);
			return Math.max(1L, (long)Math.ceil((remaining - 1.0E-9D) / this.timeScale));
		}

		private static double normalize(double value) {
			return Math.max(0.05D, Math.min(1.0D, value));
		}
	}
}
