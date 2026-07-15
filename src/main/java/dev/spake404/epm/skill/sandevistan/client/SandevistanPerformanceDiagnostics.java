package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;

public final class SandevistanPerformanceDiagnostics {
	private static final long LOG_COOLDOWN_NANOS = 500_000_000L;
	private static long previousFrameNanos;
	private static long lastLogNanos;
	private static long afterimageNanos;
	private static long longestAfterimageNanos;
	private static long postProcessNanos;
	private static long filterNanos;
	private static int afterimageCount;

	private SandevistanPerformanceDiagnostics() {
	}

	public static void recordAfterimage(long elapsedNanos) {
		if (!EPMConfig.debugSandevistanPerformance()) {
			return;
		}
		afterimageCount++;
		afterimageNanos += elapsedNanos;
		longestAfterimageNanos = Math.max(longestAfterimageNanos, elapsedNanos);
	}

	public static void recordPostProcess(long elapsedNanos) {
		if (EPMConfig.debugSandevistanPerformance()) {
			postProcessNanos += elapsedNanos;
		}
	}

	public static void recordFilter(long elapsedNanos) {
		if (EPMConfig.debugSandevistanPerformance()) {
			filterNanos += elapsedNanos;
		}
	}

	public static void finishFrame() {
		long now = System.nanoTime();
		if (!EPMConfig.debugSandevistanPerformance()) {
			previousFrameNanos = now;
			resetFrameCounters();
			return;
		}

		if (previousFrameNanos != 0L) {
			long frameNanos = now - previousFrameNanos;
			long thresholdNanos = EPMConfig.debugSandevistanPerformanceThresholdMs() * 1_000_000L;
			if (frameNanos >= thresholdNanos && now - lastLogNanos >= LOG_COOLDOWN_NANOS) {
				EPM.LOGGER.warn(
						"[EPM/SandevistanPerf] hitch={}ms afterimages={} afterimageCpu={}ms longestAfterimage={}ms postProcess={}ms filter={}ms blurSamples={} chromatic={} lifetimeTicks={} maxCount={}",
						millis(frameNanos),
						afterimageCount,
						millis(afterimageNanos),
						millis(longestAfterimageNanos),
						millis(postProcessNanos),
						millis(filterNanos),
						EPMConfig.sandevistanEdgeBlurSamples(),
						EPMConfig.sandevistanChromaticAberrationEnabled(),
						EPMConfig.sandevistanAfterimageLifetimeTicks(),
						EPMConfig.sandevistanAfterimageMaxCount());
				lastLogNanos = now;
			}
		}

		previousFrameNanos = now;
		resetFrameCounters();
	}

	private static double millis(long nanos) {
		return Math.round(nanos / 100_000.0D) / 10.0D;
	}

	private static void resetFrameCounters() {
		afterimageNanos = 0L;
		longestAfterimageNanos = 0L;
		postProcessNanos = 0L;
		filterNanos = 0L;
		afterimageCount = 0;
	}
}
