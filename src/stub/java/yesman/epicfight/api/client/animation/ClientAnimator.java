package yesman.epicfight.api.client.animation;

import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class ClientAnimator extends Animator {
	public final Layer.BaseLayer baseLayer = null;
	private LivingEntityPatch<?> entitypatch;

	public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, float transitionTimeModifier) {
	}

	public void playAnimationInstantly(AssetAccessor<? extends StaticAnimation> nextAnimation) {
	}

	public void reserveAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation) {
	}

	public void resetMotion(boolean resetPrevMotion) {
	}

	public void resetCompositeMotion() {
	}

}
