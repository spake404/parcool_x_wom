package dev.spake404.epm;

import java.lang.reflect.Method;

import net.minecraft.world.entity.LivingEntity;

final class GliderCompat {
	private static Method isGlidingWithActiveGlider;

	private GliderCompat() {
	}

	static boolean isGlidingWithActiveGlider(LivingEntity entity) {
		if (!ModCompat.isGlidersLoaded() || entity == null) {
			return false;
		}

		try {
			if (isGlidingWithActiveGlider == null) {
				isGlidingWithActiveGlider = Class.forName("net.venturecraft.gliders.util.GliderUtil")
						.getMethod("isGlidingWithActiveGlider", LivingEntity.class);
			}
			return Boolean.TRUE.equals(isGlidingWithActiveGlider.invoke(null, entity));
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
