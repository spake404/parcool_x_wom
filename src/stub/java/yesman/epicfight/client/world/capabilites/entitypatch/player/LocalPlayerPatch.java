package yesman.epicfight.client.world.capabilites.entitypatch.player;

import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class LocalPlayerPatch extends PlayerPatch<LocalPlayer> {
	public void playAnimationInClientSide(AssetAccessor<?> animation, float transitionTimeModifier) {
	}

	public void playAnimationSynchronized(AssetAccessor<?> animation, float transitionTimeModifier) {
	}

	public void stopPlaying(AssetAccessor<?> animation) {
	}

	public void reserveAnimation(AssetAccessor<?> animation) {
	}
}
