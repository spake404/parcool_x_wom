package yesman.epicfight.api.animation.types;

import java.util.Optional;

import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;

public class StaticAnimation extends DynamicAnimation {
	public <A extends StaticAnimation, V> A addProperty(StaticAnimationProperty<V> propertyType, V value) {
		return null;
	}

	public <V> Optional<V> getProperty(AnimationProperty<V> propertyType) {
		return Optional.empty();
	}
}
