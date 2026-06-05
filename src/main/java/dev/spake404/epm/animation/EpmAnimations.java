package dev.spake404.epm.animation;

import dev.spake404.epm.EPM;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Armatures;

public final class EpmAnimations {
	private static AnimationManager.AnimationAccessor<MovementAnimation> MERMAID_FAST_SWIM;

	private EpmAnimations() {
	}

	public static void register(AnimationManager.AnimationRegistryEvent event) {
		event.newBuilder(EPM.MODID, EpmAnimations::build);
	}

	public static AssetAccessor<? extends MovementAnimation> mermaidFastSwim() {
		return MERMAID_FAST_SWIM;
	}

	private static void build(AnimationManager.AnimationBuilder builder) {
		MERMAID_FAST_SWIM = builder.nextAccessor("biped/living/mermaid_fast_swim", EpmAnimations::createMermaidFastSwim);
	}

	private static MovementAnimation createMermaidFastSwim(AnimationManager.AnimationAccessor<MovementAnimation> accessor) {
		return new MovementAnimation(true, accessor, Armatures.BIPED)
				.newTimePair(0.0F, 0.5F)
				.addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, Boolean.TRUE)
				.addStateRemoveOld(EntityState.UPDATE_LIVING_MOTION, Boolean.TRUE);
	}
}
