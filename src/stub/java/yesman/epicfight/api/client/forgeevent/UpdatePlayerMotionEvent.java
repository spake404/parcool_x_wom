package yesman.epicfight.api.client.forgeevent;

import net.minecraftforge.eventbus.api.Event;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

public abstract class UpdatePlayerMotionEvent extends Event {
	public AbstractClientPlayerPatch<?> getPlayerPatch() {
		return null;
	}

	public LivingMotion getMotion() {
		return null;
	}

	public void setMotion(LivingMotion motion) {
	}

	public static class BaseLayer extends UpdatePlayerMotionEvent {
	}
}
