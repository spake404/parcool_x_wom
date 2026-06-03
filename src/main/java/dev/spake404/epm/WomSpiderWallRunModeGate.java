package dev.spake404.epm;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallRunModeGate {
	private WomSpiderWallRunModeGate() {
	}

	public static boolean shouldDisableParCoolHorizontalWallRun(Player player) {
		if (player == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& (EPMConfig.parCoolSpiderWallRunMode() || EPMConfig.womSpiderWallRunMode());
	}

	public static boolean shouldDisableOriginalWomSprintTrigger(PlayerPatch<?> playerPatch) {
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		return canUseParCoolReplacement(player, playerPatch);
	}

	public static boolean shouldDisableParCoolHorizontalWallRunKey(Player player) {
		if (player == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& EPMConfig.womSpiderWallRunMode();
	}

	public static boolean shouldDisableParCoolWallSlide(Player player) {
		if (player == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& EPMConfig.womSpiderWallRunMode();
	}

	public static boolean canStabilizeOriginalWomWallRun(Player player, PlayerPatch<?> playerPatch) {
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& EPMConfig.womSpiderWallRunMode();
	}

	static boolean canUseParCoolReplacement(Player player, PlayerPatch<?> playerPatch) {
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& EPMConfig.parCoolSpiderWallRunMode();
	}

	private static boolean hasSpiderWallRunModeContext(Player player, PlayerPatch<?> playerPatch) {
		return ModCompat.isWomLoaded()
				&& player != null
				&& !player.isSpectator()
				&& !player.isDeadOrDying()
				&& playerPatch != null
				&& SpiderTechniquesState.hasSpiderTechniques(playerPatch);
	}
}
