package dev.spake404.epm;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallRunReplacementGate {
	private WomSpiderWallRunReplacementGate() {
	}

	public static boolean shouldReplaceParCoolHorizontalWallRun(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return canUseReplacement(player, playerPatch);
	}

	static boolean canUseReplacement(Player player, PlayerPatch<?> playerPatch) {
		return ModCompat.isWomLoaded()
				&& EPMConfig.parCoolSpiderWallRunMode()
				&& player != null
				&& !player.isSpectator()
				&& !player.isDeadOrDying()
				&& playerPatch != null
				&& SpiderTechniquesState.hasSpiderTechniques(playerPatch);
	}
}
