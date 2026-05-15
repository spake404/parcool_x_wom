package yesman.epicfight.world.capabilities.entitypatch.player;

import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

public class PlayerPatch<T> extends LivingEntityPatch<T> {
	public CapabilitySkill getSkillCapability() {
		return null;
	}

	public PlayerEventListener getEventListener() {
		return null;
	}

	public Object getOriginal() {
		return null;
	}

	public boolean isLogicalClient() {
		return false;
	}

	public boolean isEpicFightMode() {
		return false;
	}

	public boolean isHoldingAny() {
		return false;
	}

	public EntityState getEntityState() {
		return EntityState.DEFAULT_STATE;
	}

	public boolean hasStamina(float amount) {
		return false;
	}

	public void playAnimationSynchronized(AssetAccessor<?> animation, float transitionTimeModifier) {
	}

	public void playAnimationInClientSide(AssetAccessor<?> animation, float transitionTimeModifier) {
	}
}
