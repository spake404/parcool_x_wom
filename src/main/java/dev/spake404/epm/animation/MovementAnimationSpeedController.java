package dev.spake404.epm.animation;

import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class MovementAnimationSpeedController {
	private static final double VANILLA_SPRINT_BLOCKS_PER_TICK = 5.612D / 20.0D;
	private static final float VANILLA_SPRINT_ANIMATION_SPEED = 1.16F;
	private static final float SPEED_SMOOTHING = 0.4F;
	private static final WeakHashMap<LivingEntity, SpeedSample> SPEED_SAMPLES = new WeakHashMap<>();

	private MovementAnimationSpeedController() {
	}

	public static float modifyPlaySpeed(
			MovementAnimation movementAnimation,
			LivingEntityPatch<?> entityPatch,
			DynamicAnimation animation,
			float originalPlaySpeed) {
		if (entityPatch == null || animation == null) {
			return originalPlaySpeed;
		}

		LivingEntity entity = entityPatch.getOriginal();
		if (!(entity instanceof Player player)) {
			return originalPlaySpeed;
		}

		SpeedSample sample = updateSpeedSample(entity);
		double horizontalSpeed = sample.smoothedSpeed;
		double capMultiplier = EPMConfig.movementAnimationSpeedCapMultiplier();
		double capSpeed = VANILLA_SPRINT_BLOCKS_PER_TICK * capMultiplier;
		float maxPlaySpeed = EPMConfig.movementAnimationMaxPlaySpeed();
		float modifiedPlaySpeed = originalPlaySpeed;
		String decision = "original";

		if (animation.isLinkAnimation()) {
			decision = "link_animation";
		} else if (horizontalSpeed <= VANILLA_SPRINT_BLOCKS_PER_TICK) {
			decision = "below_threshold";
		} else if (capSpeed <= VANILLA_SPRINT_BLOCKS_PER_TICK) {
			modifiedPlaySpeed = maxPlaySpeed;
			decision = "invalid_cap_use_max";
		} else {
			float progress = (float) ((horizontalSpeed - VANILLA_SPRINT_BLOCKS_PER_TICK)
					/ (capSpeed - VANILLA_SPRINT_BLOCKS_PER_TICK));
			modifiedPlaySpeed = Mth.lerp(
					Mth.clamp(progress, 0.0F, 1.0F),
					VANILLA_SPRINT_ANIMATION_SPEED,
					maxPlaySpeed);
			decision = "extended_curve";
		}

		logDiagnostics(
				player,
				movementAnimation,
				animation,
				sample,
				originalPlaySpeed,
				modifiedPlaySpeed,
				capMultiplier,
				maxPlaySpeed,
				decision);
		return modifiedPlaySpeed;
	}

	private static SpeedSample updateSpeedSample(LivingEntity entity) {
		SpeedSample sample = SPEED_SAMPLES.computeIfAbsent(entity, ignored -> new SpeedSample());
		if (sample.lastTick == entity.tickCount) {
			return sample;
		}

		Vec3 movement = entity.getDeltaMovement();
		sample.velocityX = movement.x;
		sample.velocityZ = movement.z;
		sample.measuredSpeed = movement.horizontalDistance();
		if (sample.lastTick == Integer.MIN_VALUE) {
			sample.smoothedSpeed = sample.measuredSpeed;
		} else {
			sample.smoothedSpeed += (sample.measuredSpeed - sample.smoothedSpeed) * SPEED_SMOOTHING;
		}
		sample.lastTick = entity.tickCount;
		return sample;
	}

	private static void logDiagnostics(
			Player player,
			MovementAnimation movementAnimation,
			DynamicAnimation animation,
			SpeedSample sample,
			float originalPlaySpeed,
			float modifiedPlaySpeed,
			double capMultiplier,
			float maxPlaySpeed,
			String decision) {
		if (!player.isLocalPlayer()
				|| sample.lastLoggedTick != Integer.MIN_VALUE && player.tickCount - sample.lastLoggedTick < 20) {
			return;
		}

		sample.lastLoggedTick = player.tickCount;
		double velocityHorizontal = player.getDeltaMovement().horizontalDistance();
		double movementAttribute = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		EPM.LOGGER.info(
				"[MovementAnimationSpeedDiag] tick={} owner={} ownerClass={} argument={} argumentClass={} link={} velocityX={} velocityZ={} measured={} smoothed={} velocity={} movementAttribute={} threshold={} original={} output={} capMultiplier={} maxPlaySpeed={} decision={}",
				player.tickCount,
				animationName(movementAnimation),
				movementAnimation == null ? "null" : movementAnimation.getClass().getName(),
				animationName(animation),
				animation.getClass().getName(),
				animation.isLinkAnimation(),
				sample.velocityX,
				sample.velocityZ,
				sample.measuredSpeed,
				sample.smoothedSpeed,
				velocityHorizontal,
				movementAttribute,
				VANILLA_SPRINT_BLOCKS_PER_TICK,
				originalPlaySpeed,
				modifiedPlaySpeed,
				capMultiplier,
				maxPlaySpeed,
				decision);
	}

	private static String animationName(DynamicAnimation animation) {
		if (animation instanceof StaticAnimation staticAnimation) {
			try {
				ResourceLocation registryName = staticAnimation.getRegistryName();
				if (registryName != null) {
					return registryName.toString();
				}
			} catch (RuntimeException | LinkageError ignored) {
			}
		}
		return String.valueOf(animation);
	}

	private static final class SpeedSample {
		private int lastTick = Integer.MIN_VALUE;
		private int lastLoggedTick = Integer.MIN_VALUE;
		private double velocityX;
		private double velocityZ;
		private double measuredSpeed;
		private double smoothedSpeed;
	}
}
