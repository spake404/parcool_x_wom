package dev.spake404.epm;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

final class NaturalSprinterProceduralStepPulse {
	private static final String LOG_PREFIX = "[EPM/NaturalSprinterStepPulse]";
	private static final float DURATION_TICKS = 10.0F;
	private static final int RETAINED_TICKS = 12;
	private static final float PI = (float)Math.PI;
	private static final double STEP_FORWARD_STRENGTH = 0.8D;
	private static final int POOF_PARTICLE_COUNT = 10;
	private static final float STEP_SOUND_VOLUME = 0.1F;
	private static final float STEP_SOUND_BASE_PITCH = 1.6F;
	private static final float STEP_SOUND_PITCH_VARIANCE = 0.4F;
	private static final float POOF_SIDE_MIN = -0.1F;
	private static final float POOF_SIDE_MAX = 0.1F;
	private static final float POOF_HEIGHT_MIN = -0.0F;
	private static final float POOF_HEIGHT_MAX = 1.0F;
	private static final float POOF_BACK_MIN = -2.0F;
	private static final float POOF_BACK_MAX = 0.0F;
	private static final float POOF_SPEED_MIN = 0.1F;
	private static final float POOF_SPEED_MAX = 0.5F;
	private static final Set<StaticAnimation> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Set<StaticAnimation> SUSTAINED_FAST_RUN_POSE_INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());
	private static final WeakHashMap<LivingEntity, StepPulse> PULSES = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntity, Boolean> LAST_STEP_RIGHT = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntity, Integer> SUSTAINED_FAST_RUN_POSE_START_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntity, NaturalSprinterFastRunAnimationOverrides.RunPoseSettings> SUSTAINED_FAST_RUN_POSE_SETTINGS = new WeakHashMap<>();

	private NaturalSprinterProceduralStepPulse() {
	}

	static void install(AssetAccessor<? extends StaticAnimation> accessor) {
		if (accessor == null) {
			logInstallSkip("null_accessor", "null", "null");
			return;
		}

		String accessorName = assetName(accessor);
		try {
			StaticAnimation animation = accessor.get();
			if (animation == null) {
				logInstallSkip("null_animation", accessorName, "null");
				return;
			}
			if (INSTALLED.contains(animation)) {
				logInstallSkip("already_installed", accessorName, animationName(animation));
				return;
			}

			INSTALLED.add(animation);
			AnimationProperty.PoseModifier existing = animation.getProperty(StaticAnimationProperty.POSE_MODIFIER).orElse(null);
			if (debug()) {
				EPM.LOGGER.info("{} phase=install accessor={} animation={} hadExistingPoseModifier={}",
						LOG_PREFIX,
						accessorName,
						animationName(animation),
						Boolean.valueOf(existing != null));
			}
			animation.addProperty(StaticAnimationProperty.POSE_MODIFIER, (self, pose, entityPatch, elapsedTime, partialTicks) -> {
				if (existing != null) {
					existing.modify(self, pose, entityPatch, elapsedTime, partialTicks);
				}
				apply(self, pose, entityPatch, elapsedTime, partialTicks);
			});
		} catch (RuntimeException | LinkageError exception) {
			if (debug()) {
				EPM.LOGGER.warn("{} phase=install_fail accessor={} error={}", LOG_PREFIX, accessorName, exception.toString());
			}
		}
	}

	static void installSustainedFastRunPose(AssetAccessor<? extends StaticAnimation> accessor) {
		if (accessor == null) {
			logInstallSkip("null_accessor", "null", "null");
			return;
		}

		String accessorName = assetName(accessor);
		try {
			StaticAnimation animation = accessor.get();
			if (animation == null) {
				logInstallSkip("null_animation", accessorName, "null");
				return;
			}
			if (SUSTAINED_FAST_RUN_POSE_INSTALLED.contains(animation)) {
				logInstallSkip("sustained_already_installed", accessorName, animationName(animation));
				return;
			}

			SUSTAINED_FAST_RUN_POSE_INSTALLED.add(animation);
			AnimationProperty.PoseModifier existing = animation.getProperty(StaticAnimationProperty.POSE_MODIFIER).orElse(null);
			if (debug()) {
				EPM.LOGGER.info("{} phase=sustained_install accessor={} animation={} hadExistingPoseModifier={}",
						LOG_PREFIX,
						accessorName,
						animationName(animation),
						Boolean.valueOf(existing != null));
			}
			animation.addProperty(StaticAnimationProperty.POSE_MODIFIER, (self, pose, entityPatch, elapsedTime, partialTicks) -> {
				if (existing != null) {
					existing.modify(self, pose, entityPatch, elapsedTime, partialTicks);
				}
				applySustainedFastRunPose(self, pose, entityPatch, partialTicks);
			});
		} catch (RuntimeException | LinkageError exception) {
			if (debug()) {
				EPM.LOGGER.warn("{} phase=sustained_install_fail accessor={} error={}", LOG_PREFIX, accessorName, exception.toString());
			}
		}
	}

	static void configureSustainedFastRunPose(PlayerPatch<?> playerPatch, NaturalSprinterFastRunAnimationOverrides.RunPoseSettings settings) {
		LivingEntity entity = playerPatch == null ? null : playerPatch.getOriginal();
		if (entity == null) {
			return;
		}

		if (settings == null || !settings.enabled() || settings.scale() <= 0.0F) {
			SUSTAINED_FAST_RUN_POSE_SETTINGS.remove(entity);
			SUSTAINED_FAST_RUN_POSE_START_TICKS.remove(entity);
			return;
		}

		NaturalSprinterFastRunAnimationOverrides.RunPoseSettings previous = SUSTAINED_FAST_RUN_POSE_SETTINGS.put(entity, settings);
		if (previous == null || previous.enabled() != settings.enabled()
				|| Math.abs(previous.scale() - settings.scale()) > 0.0001F
				|| Math.abs(previous.blendTicks() - settings.blendTicks()) > 0.0001F) {
			SUSTAINED_FAST_RUN_POSE_START_TICKS.put(entity, Integer.valueOf(entity.tickCount));
		}
	}

	static void request(PlayerPatch<?> playerPatch, boolean rightStep) {
		request(playerPatch, null, rightStep);
	}

	static void request(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> runAnimation, boolean rightStep) {
		LivingEntity entity = playerPatch == null ? null : playerPatch.getOriginal();
		if (entity != null) {
			StepPulse pulse = new StepPulse(entity.tickCount, entity.tickCount + RETAINED_TICKS, rightStep);
			PULSES.put(entity, pulse);
			LAST_STEP_RIGHT.put(entity, Boolean.valueOf(rightStep));
			boolean replayed = replayRunAnimation(playerPatch, runAnimation);
			if (debug()) {
				EPM.LOGGER.info("{} phase=request tick={} rightStep={} runAnimation={} replayed={} currentAnimation={} elapsed={}",
						LOG_PREFIX,
						Integer.valueOf(entity.tickCount),
						Boolean.valueOf(rightStep),
						assetName(runAnimation),
						Boolean.valueOf(replayed),
						assetName(AnimationQuery.currentAnimation(playerPatch)),
						Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
			}
		} else if (debug()) {
			EPM.LOGGER.info("{} phase=request_skip reason=null_entity hasPlayerPatch={}",
					LOG_PREFIX,
					Boolean.valueOf(playerPatch != null));
		}
	}

	private static boolean replayRunAnimation(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> runAnimation) {
		if (playerPatch == null) {
			return false;
		}

		AssetAccessor<? extends StaticAnimation> animation = runAnimation;
		if (animation == null) {
			animation = configuredCurrentRunAnimation(playerPatch);
		}
		if (animation == null) {
			if (debug()) {
				EPM.LOGGER.info("{} phase=replay_skip tick={} reason=no_configured_run currentAnimation={}",
						LOG_PREFIX,
						Integer.valueOf(playerTick(playerPatch)),
						assetName(AnimationQuery.currentAnimation(playerPatch)));
			}
			return false;
		}

		try {
			playerPatch.playAnimationInClientSide(animation, 0.0F);
			return true;
		} catch (RuntimeException | LinkageError exception) {
			if (debug()) {
				EPM.LOGGER.warn("{} phase=replay_fail tick={} runAnimation={} error={}",
						LOG_PREFIX,
						Integer.valueOf(playerTick(playerPatch)),
						assetName(animation),
						exception.toString());
			}
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private static AssetAccessor<? extends StaticAnimation> configuredCurrentRunAnimation(PlayerPatch<?> playerPatch) {
		AssetAccessor<?> currentAnimation = AnimationQuery.currentAnimation(playerPatch);
		if (!NaturalSprinterFastRunAnimationOverrides.isConfiguredRunAnimation(currentAnimation)) {
			return null;
		}

		try {
			return currentAnimation.get() instanceof StaticAnimation
					? (AssetAccessor<? extends StaticAnimation>)currentAnimation
					: null;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	static void playStepEffects(PlayerPatch<?> playerPatch, String source) {
		LivingEntity entity = playerPatch == null ? null : playerPatch.getOriginal();
		if (entity == null) {
			if (debug()) {
				EPM.LOGGER.info("{} phase=effects_skip reason=null_entity source={} hasPlayerPatch={}",
						LOG_PREFIX,
						source,
						Boolean.valueOf(playerPatch != null));
			}
			return;
		}

		Vec3 impulse = applyForwardImpulse(entity, source);
		int particles = playVisualAndAudioEffects(entity);
		if (debug()) {
			EPM.LOGGER.info("{} phase=effects tick={} source={} impulseX={} impulseZ={} particles={} currentAnimation={} elapsed={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					source,
					Double.valueOf(impulse.x()),
					Double.valueOf(impulse.z()),
					Integer.valueOf(particles),
					assetName(AnimationQuery.currentAnimation(playerPatch)),
					Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
		}
	}

	static void playCleanStepEffects(PlayerPatch<?> playerPatch, String source) {
		LivingEntity entity = playerPatch == null ? null : playerPatch.getOriginal();
		if (entity == null) {
			if (debug()) {
				EPM.LOGGER.info("{} phase=clean_effects_skip reason=null_entity source={} hasPlayerPatch={}",
						LOG_PREFIX,
						source,
						Boolean.valueOf(playerPatch != null));
			}
			return;
		}

		Vec3 impulse = applyForwardImpulse(entity, source);
		int particles = playTrailingPoofParticles(entity);
		if (debug()) {
			EPM.LOGGER.info("{} phase=clean_effects tick={} source={} impulseX={} impulseZ={} particles={} currentAnimation={} elapsed={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					source,
					Double.valueOf(impulse.x()),
					Double.valueOf(impulse.z()),
					Integer.valueOf(particles),
					assetName(AnimationQuery.currentAnimation(playerPatch)),
					Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
		}
	}

	static void playStepVisualAndAudioEffects(PlayerPatch<?> playerPatch, String source) {
		LivingEntity entity = playerPatch == null ? null : playerPatch.getOriginal();
		if (entity == null) {
			if (debug()) {
				EPM.LOGGER.info("{} phase=visual_audio_skip reason=null_entity source={} hasPlayerPatch={}",
						LOG_PREFIX,
						source,
						Boolean.valueOf(playerPatch != null));
			}
			return;
		}

		int particles = playVisualAndAudioEffects(entity);
		if (debug()) {
			EPM.LOGGER.info("{} phase=visual_audio tick={} source={} particles={} currentAnimation={} elapsed={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					source,
					Integer.valueOf(particles),
					assetName(AnimationQuery.currentAnimation(playerPatch)),
					Float.valueOf(AnimationQuery.currentElapsedTime(playerPatch)));
		}
	}

	private static Vec3 applyForwardImpulse(LivingEntity entity, String source) {
		if (entity == null || !entity.onGround()) {
			return Vec3.ZERO;
		}

		Vec3 impulse = stepForwardImpulse(entity, STEP_FORWARD_STRENGTH);
		if (impulse.lengthSqr() < 1.0E-6D) {
			return Vec3.ZERO;
		}

		Vec3 movement = entity.getDeltaMovement();
		entity.setDeltaMovement(movement.x() + impulse.x(), movement.y(), movement.z() + impulse.z());
		if (debug()) {
			EPM.LOGGER.info("{} phase=movement_pulse tick={} source={} strength={} impulseX={} impulseZ={} beforeX={} beforeZ={} afterX={} afterZ={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					source,
					Double.valueOf(STEP_FORWARD_STRENGTH),
					Double.valueOf(impulse.x()),
					Double.valueOf(impulse.z()),
					Double.valueOf(movement.x()),
					Double.valueOf(movement.z()),
					Double.valueOf(entity.getDeltaMovement().x()),
					Double.valueOf(entity.getDeltaMovement().z()));
		}
		return impulse;
	}

	private static int playVisualAndAudioEffects(LivingEntity entity) {
		if (entity == null) {
			return 0;
		}

		Level level = entity.level();
		Player player = entity instanceof Player playerEntity ? playerEntity : null;
		int spawned = 0;
		if (player != null) {
			playStepSound(level, player);
			spawned += spawnPoofParticles(level, player);
		}

		try {
			level.addParticle(EpicFightParticles.WHITE_AFTERIMAGE.get(),
					entity.getX(),
					entity.getY(),
					entity.getZ(),
					Double.longBitsToDouble(entity.getId()),
					0.0D,
					0.0D);
			spawned++;
		} catch (RuntimeException | LinkageError exception) {
			if (debug()) {
				EPM.LOGGER.warn("{} phase=afterimage_skip tick={} error={}", LOG_PREFIX, Integer.valueOf(entity.tickCount), exception.toString());
			}
		}

		return spawned;
	}

	private static int playTrailingPoofParticles(LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return 0;
		}

		return spawnPoofParticles(player.level(), player);
	}

	private static Vec3 stepForwardImpulse(LivingEntity entity, double strength) {
		if (entity instanceof Player player) {
			Vec3 forwardHorizontal = Vec3.directionFromRotation(new Vec2(
					player.getViewXRot(1.0F),
					player.getViewYRot(1.0F)));
			Vec3 jumpDir = OpenMatrix4f.transform(
					OpenMatrix4f.createRotatorDeg(0.0F, Vec3f.Y_AXIS),
					forwardHorizontal.scale(strength));
			return new Vec3(jumpDir.x(), 0.0D, jumpDir.z());
		}

		Vec3 forward = horizontalForward(entity);
		return forward.lengthSqr() < 1.0E-6D ? Vec3.ZERO : forward.scale(strength);
	}

	private static void playStepSound(Level level, Player player) {
		try {
			RandomSource random = player.getRandom();
			level.playSound(
					player,
					player.getX(),
					player.getY(),
					player.getZ(),
					EpicFightSounds.SLAM_LIGHT.get(),
					player.getSoundSource(),
					STEP_SOUND_VOLUME,
					STEP_SOUND_BASE_PITCH + (random.nextFloat() - 0.5F) * STEP_SOUND_PITCH_VARIANCE);
		} catch (RuntimeException | LinkageError exception) {
			if (debug()) {
				EPM.LOGGER.warn("{} phase=sound_skip tick={} error={}", LOG_PREFIX, Integer.valueOf(player.tickCount), exception.toString());
			}
		}
	}

	private static int spawnPoofParticles(Level level, Player player) {
		RandomSource random = player.getRandom();
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		OpenMatrix4f rotation = new OpenMatrix4f().rotate(
				(float)-Math.toRadians(player.yBodyRotO),
				Vec3f.Y_AXIS);

		int spawned = 0;
		for (int i = 0; i < POOF_PARTICLE_COUNT; i++) {
			Vec3f direction = new Vec3f(
					randomFloat(random, POOF_SIDE_MIN, POOF_SIDE_MAX),
					randomFloat(random, POOF_HEIGHT_MIN, POOF_HEIGHT_MAX),
					randomFloat(random, POOF_BACK_MIN, POOF_BACK_MAX));
			Vec3f velocity = new Vec3f(
					0.0F,
					0.0F,
					-randomFloat(random, POOF_SPEED_MIN, POOF_SPEED_MAX));

			OpenMatrix4f.transform3v(rotation, direction, direction);
			OpenMatrix4f.transform3v(rotation, velocity, velocity);

			level.addParticle(
					ParticleTypes.POOF,
					x + direction.x,
					y + direction.y + 0.1F,
					z + direction.z,
					velocity.x,
					velocity.y,
					velocity.z);
			spawned++;
		}
		return spawned;
	}

	private static float randomFloat(RandomSource random, float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	private static Vec3 horizontalForward(LivingEntity entity) {
		float viewXRot = entity instanceof Player player ? player.getViewXRot(1.0F) : entity.getXRot();
		float viewYRot = entity instanceof Player player ? player.getViewYRot(1.0F) : entity.getYRot();
		Vec3 forward = Vec3.directionFromRotation(viewXRot, viewYRot);
		Vec3 horizontal = new Vec3(forward.x(), 0.0D, forward.z());
		return horizontal.lengthSqr() < 1.0E-6D ? Vec3.ZERO : horizontal.normalize();
	}

	static void clear(PlayerPatch<?> playerPatch) {
		LivingEntity entity = playerPatch == null ? null : playerPatch.getOriginal();
		clear(entity, "player_patch");
	}

	static void clear(LivingEntity entity, String source) {
		if (entity != null) {
			StepPulse removed = PULSES.remove(entity);
			SUSTAINED_FAST_RUN_POSE_SETTINGS.remove(entity);
			SUSTAINED_FAST_RUN_POSE_START_TICKS.remove(entity);
			if (removed != null && debug()) {
				EPM.LOGGER.info("{} phase=clear tick={} age={} rightStep={} source={}",
						LOG_PREFIX,
						Integer.valueOf(entity.tickCount),
						Integer.valueOf(entity.tickCount - removed.startTick),
						Boolean.valueOf(removed.rightStep),
						source);
			}
		}
	}

	static String debugState(LivingEntity entity) {
		if (entity == null) {
			return "none";
		}

		StepPulse pulse = PULSES.get(entity);
		if (pulse == null) {
			return "none";
		}
		boolean expired = isExpired(entity, pulse);
		return (expired ? "expired" : "active") + "(age=" + (entity.tickCount - pulse.startTick)
				+ ", expireTick=" + pulse.expireTick
				+ ", rightStep=" + pulse.rightStep
				+ ", firstApplyLogged=" + pulse.firstApplyLogged
				+ ", peakApplyLogged=" + pulse.peakApplyLogged
				+ ", noJointApplyLogged=" + pulse.noJointApplyLogged
				+ ")";
	}

	private static void apply(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entityPatch, float elapsedTime, float partialTicks) {
		if (pose == null || entityPatch == null || entityPatch.getOriginal() == null) {
			return;
		}

		LivingEntity entity = entityPatch.getOriginal();
		StepPulse state = activePulse(entity, "pose_modifier", animation);
		if (state == null) {
			return;
		}

		float ageTicks = entity.tickCount - state.startTick + Mth.clamp(partialTicks, 0.0F, 1.0F);
		if (ageTicks < 0.0F) {
			return;
		}

		float phase = Mth.clamp(ageTicks / DURATION_TICKS, 0.0F, 1.0F);
		float pulse = Mth.sin(PI * phase);
		float drive = 0.5F - 0.5F * Mth.cos(PI * Mth.clamp(phase / 0.55F, 0.0F, 1.0F));
		float recover = 1.0F - Mth.clamp((phase - 0.55F) / 0.45F, 0.0F, 1.0F);
		float impactHoldEnd = 0.50F;
		float impactRecoverEnd = 0.80F;
		float impactRecoverProgress = Mth.clamp((phase - impactHoldEnd) / (impactRecoverEnd - impactHoldEnd), 0.0F, 1.0F);
		float impactRecover = 1.0F - smoothStep(impactRecoverProgress);
		float stepPower = Mth.clamp(pulse * 1.25F, 0.0F, 1.0F);
		float plant = Mth.sin(PI * Mth.clamp((phase - 0.18F) / 0.82F, 0.0F, 1.0F));
		float forwardDrive = drive * recover;
		float bodyDrop = impactRecover;
		float torsoLean = impactRecover;
		float side = state.rightStep ? 1.0F : -1.0F;
		NaturalSprinterFastRunAnimationOverrides.RunPoseSettings sustainedSettings = sustainedRunPoseSettings(animation, entity);
		float sustainedScale = sustainedSettings == null ? 0.0F : currentSustainedFastRunPoseScale(entity, partialTicks, sustainedSettings);
		float bodyDropDelta = Math.max(bodyDrop - sustainedScale, 0.0F);
		float torsoLeanDelta = Math.max(torsoLean - sustainedScale, 0.0F);
		float forwardDriveDelta = Math.max(forwardDrive - sustainedScale, 0.0F);
		float upperStepPowerDelta = Math.max(stepPower - sustainedScale, 0.0F);

		int appliedTransforms = 0;
		appliedTransforms += translate(pose, "Root", side * 0.045F * stepPower, -0.095F * bodyDropDelta, 0.30F * forwardDriveDelta) ? 1 : 0;
		appliedTransforms += rotate(pose, "Torso", 0.30F * torsoLeanDelta, side * 0.035F * upperStepPowerDelta, side * 0.075F * upperStepPowerDelta) ? 1 : 0;
		appliedTransforms += rotate(pose, "Chest", 0.22F * torsoLeanDelta, side * 0.06F * upperStepPowerDelta, side * 0.055F * upperStepPowerDelta) ? 1 : 0;

		appliedTransforms += translate(pose, "Leg_R_IK", 0.0F, state.rightStep ? -0.56F * plant : 0.44F * plant, (state.rightStep ? 0.34F : -0.18F) * stepPower) ? 1 : 0;
		appliedTransforms += translate(pose, "Leg_L_IK", 0.0F, state.rightStep ? 0.44F * plant : -0.56F * plant, (state.rightStep ? -0.18F : 0.34F) * stepPower) ? 1 : 0;

		appliedTransforms += rotate(pose, "Thigh_R", state.rightStep ? -0.55F * stepPower : 0.38F * stepPower, 0.0F, side * 0.04F * stepPower) ? 1 : 0;
		appliedTransforms += rotate(pose, "Thigh_L", state.rightStep ? 0.38F * stepPower : -0.55F * stepPower, 0.0F, side * 0.04F * stepPower) ? 1 : 0;
		appliedTransforms += rotate(pose, "Leg_R", state.rightStep ? 0.66F * stepPower : -0.30F * stepPower, 0.0F, 0.0F) ? 1 : 0;
		appliedTransforms += rotate(pose, "Leg_L", state.rightStep ? -0.30F * stepPower : 0.66F * stepPower, 0.0F, 0.0F) ? 1 : 0;
		appliedTransforms += rotate(pose, "Knee_R", state.rightStep ? 0.30F * stepPower : -0.16F * stepPower, 0.0F, 0.0F) ? 1 : 0;
		appliedTransforms += rotate(pose, "Knee_L", state.rightStep ? -0.16F * stepPower : 0.30F * stepPower, 0.0F, 0.0F) ? 1 : 0;

		boolean debug = debug();
		if (debug && !state.firstApplyLogged) {
			state.firstApplyLogged = true;
			EPM.LOGGER.info(
					"{} phase=apply tick={} elapsedTime={} partialTicks={} age={} phaseValue={} pulse={} drive={} recover={} impactRecover={} stepPower={} forwardDrive={} bodyDrop={} plant={} rightStep={} appliedTransforms={} animation={} joints[root={},torso={},chest={},legRIK={},legLIK={},thighR={},thighL={},legR={},legL={},kneeR={},kneeL={}]",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					Float.valueOf(elapsedTime),
					Float.valueOf(partialTicks),
					Float.valueOf(ageTicks),
					Float.valueOf(phase),
					Float.valueOf(pulse),
					Float.valueOf(drive),
					Float.valueOf(recover),
					Float.valueOf(impactRecover),
					Float.valueOf(stepPower),
					Float.valueOf(forwardDrive),
					Float.valueOf(bodyDrop),
					Float.valueOf(plant),
					Boolean.valueOf(state.rightStep),
					Integer.valueOf(appliedTransforms),
					animationName(animation),
					Boolean.valueOf(hasTransform(pose, "Root")),
					Boolean.valueOf(hasTransform(pose, "Torso")),
					Boolean.valueOf(hasTransform(pose, "Chest")),
					Boolean.valueOf(hasTransform(pose, "Leg_R_IK")),
					Boolean.valueOf(hasTransform(pose, "Leg_L_IK")),
					Boolean.valueOf(hasTransform(pose, "Thigh_R")),
					Boolean.valueOf(hasTransform(pose, "Thigh_L")),
					Boolean.valueOf(hasTransform(pose, "Leg_R")),
					Boolean.valueOf(hasTransform(pose, "Leg_L")),
					Boolean.valueOf(hasTransform(pose, "Knee_R")),
					Boolean.valueOf(hasTransform(pose, "Knee_L")));
		}
		if (debug && !state.peakApplyLogged && ageTicks >= 2.0F) {
			state.peakApplyLogged = true;
			EPM.LOGGER.info(
					"{} phase=apply_peak tick={} elapsedTime={} partialTicks={} age={} phaseValue={} pulse={} drive={} recover={} impactRecover={} stepPower={} forwardDrive={} bodyDrop={} plant={} rightStep={} appliedTransforms={} animation={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					Float.valueOf(elapsedTime),
					Float.valueOf(partialTicks),
					Float.valueOf(ageTicks),
					Float.valueOf(phase),
					Float.valueOf(pulse),
					Float.valueOf(drive),
					Float.valueOf(recover),
					Float.valueOf(impactRecover),
					Float.valueOf(stepPower),
					Float.valueOf(forwardDrive),
					Float.valueOf(bodyDrop),
					Float.valueOf(plant),
					Boolean.valueOf(state.rightStep),
					Integer.valueOf(appliedTransforms),
					animationName(animation));
		}
		if (debug && appliedTransforms == 0 && !state.noJointApplyLogged) {
			state.noJointApplyLogged = true;
			EPM.LOGGER.warn("{} phase=apply_no_joints tick={} animation={} age={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					animationName(animation),
					Float.valueOf(ageTicks));
		}
	}

	private static void applySustainedFastRunPose(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entityPatch, float partialTicks) {
		if (!sustainedRunPoseInstalled(animation) || pose == null || entityPatch == null || entityPatch.getOriginal() == null) {
			return;
		}

		LivingEntity entity = entityPatch.getOriginal();
		NaturalSprinterFastRunAnimationOverrides.RunPoseSettings settings = sustainedRunPoseSettings(animation, entity);
		if (settings == null) {
			SUSTAINED_FAST_RUN_POSE_START_TICKS.remove(entity);
			return;
		}

		float side = Boolean.FALSE.equals(LAST_STEP_RIGHT.get(entity)) ? -1.0F : 1.0F;
		float scale = currentSustainedFastRunPoseScale(entity, partialTicks, settings);
		translate(pose, "Root", 0.0F, -0.095F * scale, 0.30F * scale);
		rotate(pose, "Torso", 0.30F * scale, side * 0.035F * scale, side * 0.075F * scale);
		rotate(pose, "Chest", 0.22F * scale, side * 0.06F * scale, side * 0.055F * scale);
	}

	private static NaturalSprinterFastRunAnimationOverrides.RunPoseSettings sustainedRunPoseSettings(DynamicAnimation animation, LivingEntity entity) {
		if (!sustainedRunPoseInstalled(animation) || !shouldApplySustainedFastRunPose(entity)) {
			return null;
		}

		NaturalSprinterFastRunAnimationOverrides.RunPoseSettings settings = SUSTAINED_FAST_RUN_POSE_SETTINGS.get(entity);
		return settings == null || !settings.enabled() || settings.scale() <= 0.0F ? null : settings;
	}

	private static float currentSustainedFastRunPoseScale(
			LivingEntity entity,
			float partialTicks,
			NaturalSprinterFastRunAnimationOverrides.RunPoseSettings settings) {
		if (entity == null || settings == null) {
			return 0.0F;
		}

		Integer startTick = SUSTAINED_FAST_RUN_POSE_START_TICKS.get(entity);
		if (startTick == null) {
			startTick = Integer.valueOf(entity.tickCount);
			SUSTAINED_FAST_RUN_POSE_START_TICKS.put(entity, startTick);
		}

		float ageTicks = entity.tickCount - startTick.intValue() + Mth.clamp(partialTicks, 0.0F, 1.0F);
		float blend = smoothStep(Mth.clamp(ageTicks / settings.blendTicks(), 0.0F, 1.0F));
		return settings.scale() * blend;
	}

	private static boolean sustainedRunPoseInstalled(DynamicAnimation animation) {
		if (!(animation instanceof StaticAnimation staticAnimation)) {
			return false;
		}
		return SUSTAINED_FAST_RUN_POSE_INSTALLED.contains(staticAnimation);
	}

	private static boolean shouldApplySustainedFastRunPose(LivingEntity entity) {
		if (!(entity instanceof Player player)
				|| !EPMParCoolGate.allowCrossModSkillCompat()
				|| !EPMConfig.naturalSprinterAnimations()) {
			return false;
		}

		try {
			Parkourability parkourability = Parkourability.get(player);
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static StepPulse activePulse(LivingEntity entity, String source, DynamicAnimation animation) {
		if (entity == null) {
			return null;
		}

		StepPulse pulse = PULSES.get(entity);
		if (pulse == null) {
			return null;
		}
		return expireIfNeeded(entity, pulse, source, animation) ? null : pulse;
	}

	private static boolean expireIfNeeded(LivingEntity entity, StepPulse pulse, String source, DynamicAnimation animation) {
		if (!isExpired(entity, pulse)) {
			return false;
		}

		PULSES.remove(entity);
		if (debug()) {
			int ageTicks = entity.tickCount - pulse.startTick;
			EPM.LOGGER.info("{} phase=expire tick={} age={} rightStep={} source={} animation={}",
					LOG_PREFIX,
					Integer.valueOf(entity.tickCount),
					Integer.valueOf(ageTicks),
					Boolean.valueOf(pulse.rightStep),
					source,
					animationName(animation));
		}
		return true;
	}

	private static boolean isExpired(LivingEntity entity, StepPulse pulse) {
		return entity == null || pulse == null || entity.tickCount > pulse.expireTick;
	}

	private static float smoothStep(float value) {
		return value * value * (3.0F - 2.0F * value);
	}

	private static boolean hasTransform(Pose pose, String joint) {
		return pose.hasTransform(joint) && pose.get(joint) != null;
	}

	private static boolean translate(Pose pose, String joint, float x, float y, float z) {
		if (!pose.hasTransform(joint)) {
			return false;
		}

		JointTransform transform = pose.get(joint);
		if (transform == null) {
			return false;
		}

		JointTransform copy = transform.copy();
		copy.translation().add(x, y, z);
		pose.putJointData(joint, copy);
		return true;
	}

	private static boolean rotate(Pose pose, String joint, float xRot, float yRot, float zRot) {
		if (!pose.hasTransform(joint)) {
			return false;
		}

		JointTransform transform = pose.get(joint);
		if (transform == null) {
			return false;
		}

		JointTransform copy = transform.copy();
		copy.rotation().mul(new Quaternionf().rotationXYZ(xRot, yRot, zRot));
		pose.putJointData(joint, copy);
		return true;
	}

	private static void logInstallSkip(String reason, String accessorName, String animationName) {
		if (debug()) {
			EPM.LOGGER.info("{} phase=install_skip reason={} accessor={} animation={}",
					LOG_PREFIX,
					reason,
					accessorName,
					animationName);
		}
	}

	private static boolean debug() {
		try {
			return EPMConfig.debugNaturalSprinterFastRunStepState();
		} catch (IllegalStateException ignored) {
			return false;
		}
	}

	private static String assetName(AssetAccessor<?> animation) {
		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		return registryName == null ? String.valueOf(animation) : registryName.toString();
	}

	private static int playerTick(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch == null || playerPatch.getOriginal() == null ? 0 : playerPatch.getOriginal().tickCount;
		} catch (RuntimeException | LinkageError ignored) {
			return 0;
		}
	}

	private static String animationName(DynamicAnimation animation) {
		if (animation == null) {
			return "null";
		}

		try {
			ResourceLocation registryName = animation.getRegistryName();
			return registryName == null ? String.valueOf(animation) : registryName.toString();
		} catch (RuntimeException | LinkageError ignored) {
			return String.valueOf(animation);
		}
	}

	private static final class StepPulse {
		private final int startTick;
		private final int expireTick;
		private final boolean rightStep;
		private boolean firstApplyLogged;
		private boolean peakApplyLogged;
		private boolean noJointApplyLogged;

		private StepPulse(int startTick, int expireTick, boolean rightStep) {
			this.startTick = startTick;
			this.expireTick = expireTick;
			this.rightStep = rightStep;
		}
	}
}
