package yesman.epicfight.world.capabilities.entitypatch;

import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public class LivingEntityPatch<T> extends EntityPatch<T> {
	public CapabilitySkill getSkillCapability() {
		return null;
	}

	public Object getOriginal() {
		return null;
	}

	public void playAnimation(AssetAccessor<?> animation, float transitionTimeModifier) {
	}

	public void playAnimationInClientSide(AssetAccessor<?> animation, float transitionTimeModifier) {
	}

	public void playAnimationSynchronized(AssetAccessor<?> animation, float transitionTimeModifier) {
	}

	public void reserveAnimation(AssetAccessor<?> animation) {
	}

	public CapabilityItem getHoldingItemCapability(InteractionHand hand) {
		return null;
	}

	public CapabilityItem getAdvancedHoldingItemCapability(InteractionHand hand) {
		return null;
	}

	public <A extends Animator> A getAnimator() {
		return null;
	}

	public ClientAnimator getClientAnimator() {
		return null;
	}
}
