package dev.spake404.epm.parcool;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.WomAnimationRefs;

import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class ParCoolClimbUpState {
	private ParCoolClimbUpState() {
	}

	public static boolean isActiveOrAnimating(Player player, PlayerPatch<?> playerPatch) {
		return isActive(player) || isAnimationActive(playerPatch);
	}

	public static boolean isActive(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			ClimbUp climbUp = parkourability == null ? null : parkourability.get(ClimbUp.class);
			return climbUp != null && climbUp.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	public static boolean isAnimationActive(PlayerPatch<?> playerPatch) {
		AssetAccessor<?> currentAnimation = AnimationQuery.currentAnimation(playerPatch);
		return WomAnimationRefs.isAny(
				currentAnimation,
				WomAnimationRefs.epicParCoolFlipForward(),
				WomAnimationRefs.epicParCoolClimbUp(),
				WomAnimationRefs.epicParCoolClimbUpNoAction());
	}
}
