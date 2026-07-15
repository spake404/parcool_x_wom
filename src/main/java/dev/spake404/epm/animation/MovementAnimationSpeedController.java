package dev.spake404.epm.animation;

import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class MovementAnimationSpeedController {
	private static final double VANILLA_SPRINT_BLOCKS_PER_TICK = 5.612D / 20.0D;
	private static final float VANILLA_SPRINT_ANIMATION_SPEED = 1.16F;
	private static final float SPEED_SMOOTHING = 0.4F;
	private static final WeakHashMap<LivingEntity, SpeedSample> SPEED_SAMPLES = new WeakHashMap<>();

	private MovementAnimationSpeedController() {
	}

	public static float modifyPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation, float originalPlaySpeed) {
		if (entityPatch == null || animation == null || animation.isLinkAnimation()) {
			return originalPlaySpeed;
		}

		LivingEntity entity = entityPatch.getOriginal();
		if (!(entity instanceof Player)) {
			return originalPlaySpeed;
		}

		double horizontalSpeed = smoothedHorizontalSpeed(entity);
		if (horizontalSpeed <= VANILLA_SPRINT_BLOCKS_PER_TICK) {
			return originalPlaySpeed;
		}

		double capMultiplier = EPMConfig.movementAnimationSpeedCapMultiplier();
		double capSpeed = VANILLA_SPRINT_BLOCKS_PER_TICK * capMultiplier;
		if (capSpeed <= VANILLA_SPRINT_BLOCKS_PER_TICK) {
			return EPMConfig.movementAnimationMaxPlaySpeed();
		}

		float progress = (float) ((horizontalSpeed - VANILLA_SPRINT_BLOCKS_PER_TICK)
				/ (capSpeed - VANILLA_SPRINT_BLOCKS_PER_TICK));
		return Mth.lerp(
				Mth.clamp(progress, 0.0F, 1.0F),
				VANILLA_SPRINT_ANIMATION_SPEED,
				EPMConfig.movementAnimationMaxPlaySpeed());
	}

	private static double smoothedHorizontalSpeed(LivingEntity entity) {
		SpeedSample sample = SPEED_SAMPLES.computeIfAbsent(entity, ignored -> new SpeedSample());
		if (sample.lastTick == entity.tickCount) {
			return sample.smoothedSpeed;
		}

		double deltaX = entity.getX() - entity.xo;
		double deltaZ = entity.getZ() - entity.zo;
		double measuredSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		if (sample.lastTick == Integer.MIN_VALUE) {
			sample.smoothedSpeed = measuredSpeed;
		} else {
			sample.smoothedSpeed += (measuredSpeed - sample.smoothedSpeed) * SPEED_SMOOTHING;
		}
		sample.lastTick = entity.tickCount;
		return sample.smoothedSpeed;
	}

	private static final class SpeedSample {
		private int lastTick = Integer.MIN_VALUE;
		private double smoothedSpeed;
	}
}
