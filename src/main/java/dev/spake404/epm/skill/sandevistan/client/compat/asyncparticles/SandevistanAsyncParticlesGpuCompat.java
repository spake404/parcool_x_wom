package dev.spake404.epm.skill.sandevistan.client.compat.asyncparticles;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.client.SandevistanClientState;
import dev.spake404.epm.skill.sandevistan.client.SandevistanParticleTickClock;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

public final class SandevistanAsyncParticlesGpuCompat {
	private static final int EXPECTED_RAW_PARTICLE_BYTES = 68;
	private static final int OLD_POSITION_OFFSET = 0;
	private static final int POSITION_OFFSET = 12;
	private static final int OLD_SIZE_OFFSET = 24;
	private static final int SIZE_OFFSET = 28;
	private static final int OLD_COLOR_OFFSET = 48;
	private static final int COLOR_OFFSET = 52;
	private static final int OLD_ROLL_OFFSET = 60;
	private static final int ROLL_OFFSET = 64;
	private static final ThreadLocal<float[]> INTERPOLATION_WINDOW =
			ThreadLocal.withInitial(() -> new float[2]);
	private static final boolean SUPPORTED_LAYOUT = validateLayout();

	private SandevistanAsyncParticlesGpuCompat() {
	}

	public static void warpParticleBuffer(TextureSheetParticle particle, long address) {
		if (!SUPPORTED_LAYOUT
				|| address == 0L
				|| SandevistanClientState.isSandevistanAfterimage(particle)) {
			return;
		}

		ClientLevel level = Minecraft.getInstance().level;
		float[] window = INTERPOLATION_WINDOW.get();
		if (!SandevistanParticleTickClock.interpolationWindow(particle, level, window)) {
			return;
		}

		float start = window[0];
		float end = window[1];
		if (start <= 1.0E-4F && end >= 1.0F - 1.0E-4F) {
			return;
		}

		for (int component = 0; component < 3; component++) {
			warpFloatPair(
					address,
					OLD_POSITION_OFFSET + component * Float.BYTES,
					POSITION_OFFSET + component * Float.BYTES,
					start,
					end);
		}
		warpFloatPair(address, OLD_SIZE_OFFSET, SIZE_OFFSET, start, end);
		warpColorPair(address, OLD_COLOR_OFFSET, COLOR_OFFSET, start, end);
		warpFloatPair(address, OLD_ROLL_OFFSET, ROLL_OFFSET, start, end);
	}

	private static void warpFloatPair(
			long address,
			int oldOffset,
			int currentOffset,
			float start,
			float end) {
		float oldValue = MemoryUtil.memGetFloat(address + oldOffset);
		float currentValue = MemoryUtil.memGetFloat(address + currentOffset);
		MemoryUtil.memPutFloat(address + oldOffset, Mth.lerp(start, oldValue, currentValue));
		MemoryUtil.memPutFloat(address + currentOffset, Mth.lerp(end, oldValue, currentValue));
	}

	private static void warpColorPair(
			long address,
			int oldOffset,
			int currentOffset,
			float start,
			float end) {
		for (int channel = 0; channel < Integer.BYTES; channel++) {
			int oldValue = MemoryUtil.memGetByte(address + oldOffset + channel) & 0xFF;
			int currentValue = MemoryUtil.memGetByte(address + currentOffset + channel) & 0xFF;
			MemoryUtil.memPutByte(
					address + oldOffset + channel,
					(byte)Math.round(Mth.lerp(start, oldValue, currentValue)));
			MemoryUtil.memPutByte(
					address + currentOffset + channel,
					(byte)Math.round(Mth.lerp(end, oldValue, currentValue)));
		}
	}

	private static boolean validateLayout() {
		String[] formatClasses = {
				"forge.fun.qu_an.minecraft.asyncparticles.client.particle.render.ParticleVertexFormats",
				"fun.qu_an.minecraft.asyncparticles.client.particle.render.ParticleVertexFormats"
		};
		for (String className : formatClasses) {
			try {
				Class<?> formatClass = Class.forName(
						className,
						false,
						SandevistanAsyncParticlesGpuCompat.class.getClassLoader());
				Field rawBytesField = formatClass.getField("RAW_PARTICLE_BYTES");
				int rawBytes = rawBytesField.getInt(null);
				if (rawBytes == EXPECTED_RAW_PARTICLE_BYTES) {
					EPM.LOGGER.info("Enabled Sandevistan smoothing for AsyncParticles GPU particles");
					return true;
				}
				EPM.LOGGER.warn(
						"Disabled Sandevistan AsyncParticles GPU smoothing: expected {} raw bytes but found {}",
						EXPECTED_RAW_PARTICLE_BYTES,
						rawBytes);
				return false;
			} catch (ClassNotFoundException ignored) {
			} catch (ReflectiveOperationException | LinkageError exception) {
				EPM.LOGGER.warn("Disabled Sandevistan AsyncParticles GPU smoothing", exception);
				return false;
			}
		}
		EPM.LOGGER.warn("Disabled Sandevistan AsyncParticles GPU smoothing: vertex format was not found");
		return false;
	}
}
