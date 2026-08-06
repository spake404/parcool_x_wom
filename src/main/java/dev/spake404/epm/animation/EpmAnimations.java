package dev.spake404.epm.animation;

import com.mojang.datafixers.util.Pair;
import dev.spake404.epm.EPM;
import dev.spake404.epm.crawl.CrawlAnimationHandler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class EpmAnimations {
	private static final float DEMOLITION_LEAP_CHARGE_HOLD_EPSILON = 0.001F;
	private static final float CRAWL_PLAY_SPEED_MULTIPLIER = 2.0F;

	private static AnimationManager.AnimationAccessor<MovementAnimation> MERMAID_FAST_SWIM;
	private static AnimationManager.AnimationAccessor<StaticAnimation> DEMOLITION_LEAP_CHARGE_JUMP;
	private static AnimationManager.AnimationAccessor<ActionAnimation> DOUBLE_JUMP_JUMP;
	private static AnimationManager.AnimationAccessor<StaticAnimation> DOUBLE_JUMP_FALL;
	private static AnimationManager.AnimationAccessor<StaticAnimation> DOUBLE_JUMP_LAND;
	private static AnimationManager.AnimationAccessor<StaticAnimation> CRAWL_ENTER;
	private static AnimationManager.AnimationAccessor<StaticAnimation> CRAWL_IDLE;
	private static AnimationManager.AnimationAccessor<MovementAnimation> CRAWL_MOVE_LEFT;
	private static AnimationManager.AnimationAccessor<MovementAnimation> CRAWL_MOVE_RIGHT;
	private static AnimationManager.AnimationAccessor<StaticAnimation> CRAWL_EXIT;

	private EpmAnimations() {
	}

	public static void register(AnimationManager.AnimationRegistryEvent event) {
		event.newBuilder(EPM.MODID, EpmAnimations::build);
	}

	public static AssetAccessor<? extends MovementAnimation> mermaidFastSwim() {
		return MERMAID_FAST_SWIM;
	}

	public static AssetAccessor<? extends StaticAnimation> demolitionLeapChargeJump() {
		return DEMOLITION_LEAP_CHARGE_JUMP;
	}

	public static AssetAccessor<? extends ActionAnimation> doubleJumpJump() {
		return DOUBLE_JUMP_JUMP;
	}

	public static AssetAccessor<? extends StaticAnimation> doubleJumpFall() {
		return DOUBLE_JUMP_FALL;
	}

	public static AssetAccessor<? extends StaticAnimation> doubleJumpLand() {
		return DOUBLE_JUMP_LAND;
	}

	public static AssetAccessor<? extends StaticAnimation> crawlEnter() {
		return CRAWL_ENTER;
	}

	public static AssetAccessor<? extends StaticAnimation> crawlIdle() {
		return CRAWL_IDLE;
	}

	public static AssetAccessor<? extends StaticAnimation> crawlMoveLeft() {
		return CRAWL_MOVE_LEFT;
	}

	public static AssetAccessor<? extends StaticAnimation> crawlMoveRight() {
		return CRAWL_MOVE_RIGHT;
	}

	public static AssetAccessor<? extends StaticAnimation> crawlExit() {
		return CRAWL_EXIT;
	}

	private static void build(AnimationManager.AnimationBuilder builder) {
		MERMAID_FAST_SWIM = builder.nextAccessor("biped/living/mermaid_fast_swim", EpmAnimations::createMermaidFastSwim);
		DEMOLITION_LEAP_CHARGE_JUMP = builder.nextAccessor(
				"biped/living/demolition_leap_charge_jump",
				EpmAnimations::createDemolitionLeapChargeJump);
		DOUBLE_JUMP_JUMP = builder.nextAccessor(
				"biped/living/double_jump/jump",
				EpmAnimations::createDoubleJumpJump);
		DOUBLE_JUMP_FALL = builder.nextAccessor(
				"biped/living/double_jump/fall",
				EpmAnimations::createDoubleJumpFall);
		DOUBLE_JUMP_LAND = builder.nextAccessor(
				"biped/living/double_jump/land",
				EpmAnimations::createDoubleJumpLand);
		CRAWL_ENTER = builder.nextAccessor(
				"biped/living/crawl/enter",
				EpmAnimations::createCrawlEnter);
		CRAWL_IDLE = builder.nextAccessor(
				"biped/living/crawl/idle",
				EpmAnimations::createCrawlIdle);
		CRAWL_MOVE_LEFT = builder.nextAccessor(
				"biped/living/crawl/move_left",
				EpmAnimations::createCrawlMoveLeft);
		CRAWL_MOVE_RIGHT = builder.nextAccessor(
				"biped/living/crawl/move_right",
				EpmAnimations::createCrawlMoveRight);
		CRAWL_EXIT = builder.nextAccessor(
				"biped/living/crawl/exit",
				EpmAnimations::createCrawlExit);
	}

	private static MovementAnimation createMermaidFastSwim(AnimationManager.AnimationAccessor<MovementAnimation> accessor) {
		return new MovementAnimation(true, accessor, Armatures.BIPED)
				.newTimePair(0.0F, 0.5F)
				.addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, Boolean.TRUE)
				.addStateRemoveOld(EntityState.UPDATE_LIVING_MOTION, Boolean.TRUE);
	}

	private static StaticAnimation createDemolitionLeapChargeJump(AnimationManager.AnimationAccessor<StaticAnimation> accessor) {
		return new StaticAnimation(0.15F, false, accessor, Armatures.BIPED)
				.addProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER, EpmAnimations::holdDemolitionLeapChargeLastFrame)
				.newTimePair(0.0F, 10.0F)
				.addStateRemoveOld(EntityState.INACTION, Boolean.FALSE)
				.setResourceLocation("epicfight", "biped/skill/demolition_leap_charge");
	}

	private static ActionAnimation createDoubleJumpJump(AnimationManager.AnimationAccessor<ActionAnimation> accessor) {
		return new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addEvents(
						StaticAnimationProperty.ON_BEGIN_EVENTS,
						new AnimationEvent[]{
								AnimationEvent.SimpleEvent.create(EpmAnimations::playPhantomAscentStartEffects, AnimationEvent.Side.CLIENT)
						});
	}

	private static StaticAnimation createDoubleJumpFall(AnimationManager.AnimationAccessor<StaticAnimation> accessor) {
		return new StaticAnimation(0.05F, true, accessor, Armatures.BIPED);
	}

	private static StaticAnimation createDoubleJumpLand(AnimationManager.AnimationAccessor<StaticAnimation> accessor) {
		return new StaticAnimation(0.05F, false, accessor, Armatures.BIPED);
	}

	private static StaticAnimation createCrawlEnter(AnimationManager.AnimationAccessor<StaticAnimation> accessor) {
		return addCrawlEndEvent(addCrawlPlaySpeed(new StaticAnimation(0.08F, false, accessor, Armatures.BIPED)));
	}

	private static StaticAnimation createCrawlIdle(AnimationManager.AnimationAccessor<StaticAnimation> accessor) {
		return addCrawlPlaySpeed(new StaticAnimation(0.08F, true, accessor, Armatures.BIPED));
	}

	private static MovementAnimation createCrawlMoveLeft(AnimationManager.AnimationAccessor<MovementAnimation> accessor) {
		return addCrawlEndEvent(addCrawlPlaySpeed(new MovementAnimation(0.12F, false, accessor, Armatures.BIPED)));
	}

	private static MovementAnimation createCrawlMoveRight(AnimationManager.AnimationAccessor<MovementAnimation> accessor) {
		return addCrawlEndEvent(addCrawlPlaySpeed(new MovementAnimation(0.12F, false, accessor, Armatures.BIPED)));
	}

	private static StaticAnimation createCrawlExit(AnimationManager.AnimationAccessor<StaticAnimation> accessor) {
		return addCrawlEndEvent(addCrawlPlaySpeed(new StaticAnimation(0.08F, false, accessor, Armatures.BIPED)));
	}

	private static <T extends StaticAnimation> T addCrawlPlaySpeed(T animation) {
		return animation.addProperty(
				StaticAnimationProperty.PLAY_SPEED_MODIFIER,
				(self, entityPatch, speed, prevElapsedTime, elapsedTime) -> speed * CRAWL_PLAY_SPEED_MULTIPLIER);
	}

	private static <T extends StaticAnimation> T addCrawlEndEvent(T animation) {
		return animation.addEvents(
				StaticAnimationProperty.ON_END_EVENTS,
				AnimationEvent.SimpleEvent.create(CrawlAnimationHandler::onAnimationEnd, AnimationEvent.Side.CLIENT));
	}

	private static void playPhantomAscentStartEffects(
			LivingEntityPatch<?> entityPatch,
			AssetAccessor<?> ignoredAnimation,
			AnimationParameters ignoredParameters) {
		if (entityPatch == null) {
			return;
		}

		LivingEntity entity = entityPatch.getOriginal();
		SoundEvent sound = EpicFightSounds.TUMBLE.get();
		ParticleOptions particle = EpicFightParticles.AIR_BURST.get();
		Vec3 position = entity.position();

		entityPatch.playSound(sound, 0.0F, 0.0F);
		entity.level().addAlwaysVisibleParticle(
				particle,
				position.x,
				position.y + entity.getBbHeight() * 0.5D,
				position.z,
				0.0D,
				-1.0D,
				2.0D);
	}

	private static Pair<Float, Float> holdDemolitionLeapChargeLastFrame(
			DynamicAnimation animation,
			LivingEntityPatch<?> entityPatch,
			float speed,
			float prevElapsedTime,
			float elapsedTime) {
		if (animation == null || animation.isLinkAnimation()) {
			return Pair.of(Float.valueOf(prevElapsedTime), Float.valueOf(elapsedTime));
		}

		float totalTime = animation.getTotalTime();
		if (totalTime <= 0.0F || elapsedTime < totalTime - DEMOLITION_LEAP_CHARGE_HOLD_EPSILON) {
			return Pair.of(Float.valueOf(prevElapsedTime), Float.valueOf(elapsedTime));
		}

		return Pair.of(Float.valueOf(Math.min(prevElapsedTime, totalTime)), Float.valueOf(totalTime));
	}
}
