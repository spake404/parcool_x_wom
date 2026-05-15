package yesman.epicfight.api.animation;

import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;

public class AnimationPlayer {
	public AssetAccessor<?> getRealAnimation() {
		return null;
	}

	public void setPlayAnimation(AssetAccessor<? extends DynamicAnimation> animation) {
	}

	public float getElapsedTime() {
		return 0.0F;
	}

	public float getPrevElapsedTime() {
		return 0.0F;
	}

	public void setElapsedTime(float prevElapsedTime, float elapsedTime) {
	}
}
