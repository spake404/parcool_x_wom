package yesman.epicfight.api.forgeevent;

import net.minecraftforge.eventbus.api.Event;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class InitAnimatorEvent extends Event {
	public LivingEntityPatch<?> getEntityPatch() {
		return null;
	}

	public Animator getAnimator() {
		return null;
	}
}
