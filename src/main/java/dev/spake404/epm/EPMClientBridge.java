package dev.spake404.epm;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EPMClientBridge {
	private EPMClientBridge() {
	}

	public static boolean tryPrepareWallJumpAttackHandoff(PlayerPatch<?> playerPatch) {
		return isPhysicalClient() && Client.tryPrepareWallJumpAttackHandoff(playerPatch);
	}

	private static boolean isPhysicalClient() {
		return FMLEnvironment.dist == Dist.CLIENT;
	}

	private static final class Client {
		private static boolean tryPrepareWallJumpAttackHandoff(PlayerPatch<?> playerPatch) {
			return EPMClientHooks.tryPrepareWallJumpAttackHandoff(playerPatch);
		}
	}
}
