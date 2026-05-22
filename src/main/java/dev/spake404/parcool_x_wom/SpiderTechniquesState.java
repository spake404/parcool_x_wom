package dev.spake404.parcool_x_wom;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class SpiderTechniquesState {
	private SpiderTechniquesState() {
	}

	public static boolean hasSpiderTechniques(PlayerPatch<?> playerPatch) {
		return WomCompatBridge.instance().hasSpiderTechniques(playerPatch);
	}

	public static boolean shouldBlockAttack(PlayerPatch<?> playerPatch) {
		return WomCompatBridge.instance().shouldBlockSpiderTechniquesAttack(playerPatch);
	}

	public static void debugAttackState(PlayerPatch<?> playerPatch, String source, boolean originalExecutable) {
		if (!ParcoolXWomConfig.debugSpiderTechniquesAttackState()) {
			return;
		}

		String playerState = describePlayer(playerPatch);
		String spiderState = WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch);
		boolean blockAttack = WomCompatBridge.instance().shouldBlockSpiderTechniquesAttack(playerPatch);
		ParcoolXWom.LOGGER.info("[SpiderTechniquesAttackDebug] source={}, originalExecutable={}, blockAttack={}, {}, {}",
				source, Boolean.valueOf(originalExecutable), Boolean.valueOf(blockAttack), playerState, spiderState);
	}

	private static String describePlayer(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return "playerPatch=null";
		}

		Player player = playerPatch.getOriginal();
		if (player == null) {
			return "player=null, logicalClient=" + playerPatch.isLogicalClient() + ", epicFightMode=" + playerPatch.isEpicFightMode();
		}

		return "player=" + player.getScoreboardName()
				+ ", side=" + (player.level().isClientSide() ? "client" : "server")
				+ ", logicalClient=" + playerPatch.isLogicalClient()
				+ ", epicFightMode=" + playerPatch.isEpicFightMode()
				+ ", onGround=" + player.onGround()
				+ ", fallDistance=" + player.fallDistance
				+ ", inWater=" + player.isInWater();
	}
}
