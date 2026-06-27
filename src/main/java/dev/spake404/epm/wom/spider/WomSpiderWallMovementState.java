package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.WomAnimationRefs;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.config.EPMConfig;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

final class WomSpiderWallMovementState {
	private WomSpiderWallMovementState() {
	}

	static void clearUnownedAfterTick(Player player, PlayerPatch<?> playerPatch, String reason) {
		if (player == null
				|| !player.isLocalPlayer()
				|| !WomSpiderWallRunModeGate.canUseParCoolReplacement(player, playerPatch)
				|| WomSpiderWallRunHandler.isWallRunActive(player)
				|| WomSpiderWallSlideHandler.isWallSlideActive(player)
				|| isWallBackflipActive(playerPatch)) {
			return;
		}

		clear(player, playerPatch, reason, true);
	}

	static void clear(Player player, PlayerPatch<?> playerPatch, String reason, boolean resetAnimator) {
		if (player == null) {
			return;
		}

		boolean wallMovementAnimation = isWallMovementAnimation(playerPatch);
		boolean wallMovementData = WomCompatBridge.instance().isSpiderWallMovementActive(playerPatch);
		if (!wallMovementAnimation && !wallMovementData) {
			return;
		}

		WomCompatBridge.instance().clearSpiderWallRunState(playerPatch);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopWallMovementAnimations(localPlayerPatch);
			if (resetAnimator && wallMovementAnimation) {
				resetAnimator(localPlayerPatch);
			}
		}
		logClear(player, playerPatch, reason, wallMovementData, wallMovementAnimation);
	}

	static boolean isWallGlideAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(AnimationQuery.currentAnimation(playerPatch), WomAnimationRefs.wallGlide());
	}

	static boolean isWallMovementAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(
				AnimationQuery.currentAnimation(playerPatch),
				WomAnimationRefs.wallRunning(),
				WomAnimationRefs.wallRunLeftSide(),
				WomAnimationRefs.wallRunRightSide(),
				WomAnimationRefs.wallGlide());
	}

	static boolean isWallBackflipAnimation(PlayerPatch<?> playerPatch) {
		return WomAnimationRefs.isAny(AnimationQuery.currentAnimation(playerPatch), WomAnimationRefs.wallBackflip());
	}

	static boolean isWallBackflipActive(PlayerPatch<?> playerPatch) {
		return isWallBackflipAnimation(playerPatch)
				|| WomCompatBridge.instance().isSpiderWallBackflipActive(playerPatch);
	}

	static void stopWallMovementAnimations(LocalPlayerPatch playerPatch) {
		stopPlaying(playerPatch, WomAnimationRefs.wallRunning());
		stopPlaying(playerPatch, WomAnimationRefs.wallRunLeftSide());
		stopPlaying(playerPatch, WomAnimationRefs.wallRunRightSide());
		stopPlaying(playerPatch, WomAnimationRefs.wallGlide());
	}

	static void stopPlaying(LocalPlayerPatch playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		if (animation == null) {
			return;
		}

		try {
			playerPatch.stopPlaying(animation);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void resetAnimator(LocalPlayerPatch playerPatch) {
		try {
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void logClear(Player player, PlayerPatch<?> playerPatch, String reason, boolean wallMovementData, boolean wallMovementAnimation) {
		if (!EPMConfig.debugSpiderWallRunState()) {
			return;
		}

		EPM.LOGGER.info(
				"[WomSpiderWallMovement] clear reason={} tick={} dataWallState={} animationWallMovement={} runActive={} slideActive={} onGround={} state={}",
				reason,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(wallMovementData),
				Boolean.valueOf(wallMovementAnimation),
				Boolean.valueOf(WomSpiderWallRunHandler.isWallRunActive(player)),
				Boolean.valueOf(WomSpiderWallSlideHandler.isWallSlideActive(player)),
				Boolean.valueOf(player.onGround()),
				WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch));
	}
}
