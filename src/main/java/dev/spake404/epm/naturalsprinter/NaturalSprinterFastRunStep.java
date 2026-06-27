package dev.spake404.epm.naturalsprinter;

import dev.spake404.epm.EPM;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;

public final class NaturalSprinterFastRunStep {
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

	public static NaturalSprinterFastRunStep none() {
		return NONE;
	}
	public static NaturalSprinterFastRunStep animation(AssetAccessor<? extends StaticAnimation> animation) {
		return animation == null ? NONE : configuredAnimation(animation, false, false);
	}
	public static NaturalSprinterFastRunStep configuredAnimation(
			AssetAccessor<? extends StaticAnimation> animation,
			boolean startupEffects,
			boolean manualEffects) {
		return animation == null ? NONE : new NaturalSprinterFastRunStep(animation, null, false, false, false, startupEffects, manualEffects);
	}

	public static NaturalSprinterFastRunStep defaultAnimation(AssetAccessor<? extends StaticAnimation> animation) {
		return animation == null ? NONE : new NaturalSprinterFastRunStep(animation, null, false, false, true, true, false);
	}
	public static NaturalSprinterFastRunStep procedural(AssetAccessor<? extends StaticAnimation> runAnimation, boolean rightStep) {
		return runAnimation == null ? NONE : new NaturalSprinterFastRunStep(null, runAnimation, true, rightStep, false, true, false);
	}
	public boolean isPresent() {
		return animation != null || proceduralRunAnimation != null;
	}
	public AssetAccessor<? extends StaticAnimation> animation() {
		return animation;
	}
	public AssetAccessor<? extends StaticAnimation> proceduralRunAnimation() {
		return proceduralRunAnimation;
	}
	public boolean procedural() {
		return procedural;
	}
	public boolean rightStep() {
		return rightStep;
	}
	public boolean defaultNaturalSprinter() {
		return defaultNaturalSprinter;
	}
	public boolean startupEffects() {
		return startupEffects;
	}
	public boolean manualEffects() {
		return manualEffects;
	}
	public boolean fullEffectsFor(Trigger trigger) {
		return switch (trigger) {
			case STARTUP -> startupEffects;
			case AUTO_STARTUP -> false;
			case MANUAL -> manualEffects;
		};
	}
	public enum Trigger {
		STARTUP,
		AUTO_STARTUP,
		MANUAL
	}
}
