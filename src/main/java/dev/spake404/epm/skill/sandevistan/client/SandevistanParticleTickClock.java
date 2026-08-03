package dev.spake404.epm.skill.sandevistan.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;

public final class SandevistanParticleTickClock {
	private static final ConcurrentMap<Particle, LocalClock> CLOCKS = new ConcurrentHashMap<>();

	private SandevistanParticleTickClock() {
	}

	public static boolean beginTick(Particle particle, double timeScale) {
		if (particle == null || timeScale >= 1.0D) {
			CLOCKS.remove(particle);
			return true;
		}

		LocalClock clock = CLOCKS.computeIfAbsent(particle, ignored -> new LocalClock(timeScale));
		synchronized (clock) {
			clock.updateTimeScale(timeScale);
			clock.accumulator += clock.timeScale;
			clock.ticking = clock.accumulator >= 1.0D;
			if (clock.ticking) {
				clock.accumulator -= 1.0D;
			}
			return clock.ticking;
		}
	}

	public static void endTick(Particle particle, ClientLevel level) {
		LocalClock clock = CLOCKS.get(particle);
		if (clock == null || level == null) {
			return;
		}

		synchronized (clock) {
			if (clock.ticking) {
				clock.lastLocalTickGameTime = level.getGameTime();
				clock.ticking = false;
			}
		}
		if (!particle.isAlive()) {
			CLOCKS.remove(particle, clock);
		}
	}

	public static float localPartialTick(Particle particle, ClientLevel level, float globalPartialTick) {
		LocalClock clock = CLOCKS.get(particle);
		if (clock == null || level == null) {
			return globalPartialTick;
		}

		synchronized (clock) {
			if (clock.lastLocalTickGameTime == Long.MIN_VALUE) {
				return globalPartialTick;
			}
			return calculatePartialTick(clock, level.getGameTime(), globalPartialTick);
		}
	}

	public static void clear() {
		CLOCKS.clear();
	}

	public static boolean interpolationWindow(Particle particle, ClientLevel level, float[] output) {
		LocalClock clock = CLOCKS.get(particle);
		if (clock == null || level == null) {
			return false;
		}

		synchronized (clock) {
			if (clock.lastLocalTickGameTime == Long.MIN_VALUE) {
				return false;
			}
			long gameTime = level.getGameTime();
			output[0] = calculatePartialTick(clock, gameTime, 0.0F);
			output[1] = calculatePartialTick(clock, gameTime, 1.0F);
			return true;
		}
	}

	private static float calculatePartialTick(LocalClock clock, long gameTime, float globalPartialTick) {
		double elapsedGlobalTicks = gameTime
				- clock.lastLocalTickGameTime
				+ globalPartialTick;
		return Mth.clamp((float)(elapsedGlobalTicks * clock.timeScale), 0.0F, 1.0F);
	}

	private static final class LocalClock {
		private double timeScale;
		private double accumulator;
		private long lastLocalTickGameTime = Long.MIN_VALUE;
		private boolean ticking;

		private LocalClock(double timeScale) {
			this.timeScale = normalize(timeScale);
			this.accumulator = 1.0D - this.timeScale;
		}

		private void updateTimeScale(double timeScale) {
			this.timeScale = normalize(timeScale);
		}

		private static double normalize(double value) {
			return Math.max(0.05D, Math.min(1.0D, value));
		}
	}
}
