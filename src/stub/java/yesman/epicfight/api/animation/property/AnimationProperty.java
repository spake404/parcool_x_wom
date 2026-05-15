package yesman.epicfight.api.animation.property;

import java.util.Optional;

import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public abstract class AnimationProperty<T> {
	public static class StaticAnimationProperty<T> extends AnimationProperty<T> {
		public static final StaticAnimationProperty<PlaybackSpeedModifier> PLAY_SPEED_MODIFIER = new StaticAnimationProperty<>();
	}

	@FunctionalInterface
	public interface PlaybackSpeedModifier {
		float modify(DynamicAnimation self, LivingEntityPatch<?> entitypatch, float speed, float prevElapsedTime, float elapsedTime);
	}

	public Optional<T> empty() {
		return Optional.empty();
	}
}
