package dev.spake404.epm.animation;

import dev.spake404.epm.EPM;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class AnimationQuery {
	private AnimationQuery() {
	}

	public static AssetAccessor<?> currentAnimation(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return null;
		}

		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	public static ResourceLocation safeRegistryName(AssetAccessor<?> animation) {
		if (animation == null) {
			return null;
		}

		try {
			return animation.registryName();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	public static float currentElapsedTime(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return -1.0F;
		}

		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getElapsedTime();
		} catch (RuntimeException | LinkageError ignored) {
			return -1.0F;
		}
	}

	public static float currentAnimationTotalTime(PlayerPatch<?> playerPatch) {
		AssetAccessor<?> animation = currentAnimation(playerPatch);
		if (animation == null) {
			return -1.0F;
		}

		try {
			Object value = animation.get();
			return value instanceof DynamicAnimation dynamicAnimation ? dynamicAnimation.getTotalTime() : -1.0F;
		} catch (RuntimeException | LinkageError ignored) {
			return -1.0F;
		}
	}

	public static boolean currentAnimationEnded(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.isEnd();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
