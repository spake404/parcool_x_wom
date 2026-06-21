package dev.spake404.epm;

import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;

final class NaturalSprinterFastRunStep {
	private static final NaturalSprinterFastRunStep NONE = new NaturalSprinterFastRunStep(null, null, false, false, false, false, false);

	private final AssetAccessor<? extends StaticAnimation> animation;
	private final AssetAccessor<? extends StaticAnimation> proceduralRunAnimation;
	private final boolean procedural;
	private final boolean rightStep;
	private final boolean defaultNaturalSprinter;
	private final boolean startupEffects;
	private final boolean manualEffects;

	private NaturalSprinterFastRunStep(
			AssetAccessor<? extends StaticAnimation> animation,
			AssetAccessor<? extends StaticAnimation> proceduralRunAnimation,
			boolean procedural,
			boolean rightStep,
			boolean defaultNaturalSprinter,
			boolean startupEffects,
			boolean manualEffects) {
		this.animation = animation;
		this.proceduralRunAnimation = proceduralRunAnimation;
		this.procedural = procedural;
		this.rightStep = rightStep;
		this.defaultNaturalSprinter = defaultNaturalSprinter;
		this.startupEffects = startupEffects;
		this.manualEffects = manualEffects;
	}

	static NaturalSprinterFastRunStep none() {
		return NONE;
	}

	static NaturalSprinterFastRunStep animation(AssetAccessor<? extends StaticAnimation> animation) {
		return animation == null ? NONE : configuredAnimation(animation, false, false);
	}

	static NaturalSprinterFastRunStep configuredAnimation(
			AssetAccessor<? extends StaticAnimation> animation,
			boolean startupEffects,
			boolean manualEffects) {
		return animation == null ? NONE : new NaturalSprinterFastRunStep(animation, null, false, false, false, startupEffects, manualEffects);
	}

	static NaturalSprinterFastRunStep defaultAnimation(AssetAccessor<? extends StaticAnimation> animation) {
		return animation == null ? NONE : new NaturalSprinterFastRunStep(animation, null, false, false, true, true, false);
	}

	static NaturalSprinterFastRunStep procedural(AssetAccessor<? extends StaticAnimation> runAnimation, boolean rightStep) {
		return runAnimation == null ? NONE : new NaturalSprinterFastRunStep(null, runAnimation, true, rightStep, false, true, false);
	}

	boolean isPresent() {
		return animation != null || proceduralRunAnimation != null;
	}

	AssetAccessor<? extends StaticAnimation> animation() {
		return animation;
	}

	AssetAccessor<? extends StaticAnimation> proceduralRunAnimation() {
		return proceduralRunAnimation;
	}

	boolean procedural() {
		return procedural;
	}

	boolean rightStep() {
		return rightStep;
	}

	boolean defaultNaturalSprinter() {
		return defaultNaturalSprinter;
	}

	boolean startupEffects() {
		return startupEffects;
	}

	boolean manualEffects() {
		return manualEffects;
	}

	boolean fullEffectsFor(Trigger trigger) {
		return trigger == Trigger.STARTUP ? startupEffects : manualEffects;
	}

	enum Trigger {
		STARTUP,
		MANUAL
	}
}
