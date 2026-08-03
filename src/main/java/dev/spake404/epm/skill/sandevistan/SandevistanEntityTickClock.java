package dev.spake404.epm.skill.sandevistan;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class SandevistanEntityTickClock {
	private static final Map<Entity, LocalClock> CLOCKS = new WeakHashMap<>();

	private SandevistanEntityTickClock() {
	}

	public static boolean shouldTick(Entity entity, double timeScale) {
		if (entity == null || timeScale >= 1.0D) {
			CLOCKS.remove(entity);
			return true;
		}

		LocalClock clock = CLOCKS.computeIfAbsent(entity, ignored -> new LocalClock(timeScale));
		clock.updateTimeScale(timeScale);
		clock.accumulator += clock.timeScale;
		if (clock.accumulator < 1.0D) {
			return false;
		}

		clock.accumulator -= 1.0D;
		return true;
	}

	public static void advanceUnscaledDamageTimers(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return;
		}

		if (entity.invulnerableTime > 0) {
			entity.invulnerableTime--;
		}
		if (livingEntity.hurtTime > 0) {
			livingEntity.hurtTime--;
		}
	}

	private static final class LocalClock {
		private double timeScale;
		private double accumulator;

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

		private static double normalize(double value) {
			return Math.max(0.05D, Math.min(1.0D, value));
		}
	}
}
