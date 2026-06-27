package dev.spake404.epm.wom.spider;

import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMParCoolGate;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class WomSpiderWallRunModeGate {
	private WomSpiderWallRunModeGate() {
	}

	public static boolean shouldDisableParCoolHorizontalWallRun(Player player) {
		if (player == null || !EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& (EPMConfig.parCoolSpiderWallRunMode() || EPMConfig.womSpiderWallRunMode());
	}

	public static boolean shouldDisableOriginalWomSprintTrigger(PlayerPatch<?> playerPatch) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		return canUseParCoolReplacement(player, playerPatch);
	}

	public static boolean shouldDisableParCoolHorizontalWallRunKey(Player player) {
		if (player == null || !EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& EPMConfig.womSpiderWallRunMode();
	}

	public static boolean shouldDisableParCoolWallSlide(Player player) {
		if (player == null || !EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& EPMConfig.womSpiderWallRunMode();
	}

	public static boolean shouldDisableParCoolWallSlideAction(Player player) {
		if (player == null || !EPMParCoolGate.allowCrossModSkillCompat()) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& (EPMConfig.parCoolSpiderWallRunMode() || EPMConfig.womSpiderWallRunMode());
	}

	public static boolean canStabilizeOriginalWomWallRun(Player player, PlayerPatch<?> playerPatch) {
		return hasSpiderWallRunModeContext(player, playerPatch)
				&& (EPMConfig.defaultSpiderWallRunMode()
				|| EPMConfig.womSpiderWallRunMode()
				|| canUseParCoolOriginalAdapter(player, playerPatch));
	}

	public static boolean canUseParCoolReplacement(Player player, PlayerPatch<?> playerPatch) {
		// Test build: ParCool mode now routes into WOM's original wall-run through a thin input adapter.
		// Keep the legacy replacement handler parked so it can be compared or restored without deleting it.
		return false;
	}

	public static boolean canUseParCoolOriginalAdapter(Player player, PlayerPatch<?> playerPatch) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& hasSpiderWallRunModeContext(player, playerPatch)
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
