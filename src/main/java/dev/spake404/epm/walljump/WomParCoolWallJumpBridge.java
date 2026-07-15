package dev.spake404.epm.walljump;

import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import java.nio.ByteBuffer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class WomParCoolWallJumpBridge {
	private WomParCoolWallJumpBridge() {
	}

	public static boolean shouldBlockAfterPhantom(Player player, String phase) {
		return isPhysicalClient() && Client.shouldBlockAfterPhantom(player, phase);
	}

	public static boolean claimParCoolWallJump(Player player, String phase) {
		return !isPhysicalClient() || Client.claimParCoolWallJump(player, phase);
	}

	public static boolean writeFallbackStartInfo(WallJump wallJump, Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo) {
		return isPhysicalClient() && Client.writeFallbackStartInfo(wallJump, player, parkourability, stamina, startInfo);
	}

	public static void markNativeStartCandidate(WallJump wallJump, Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo) {
		if (isPhysicalClient()) {
			Client.markNativeStartCandidate(wallJump, player, parkourability, stamina, startInfo);
		}
	}

	public static void onWallJumpStarted(Player player, ByteBuffer startData) {
		if (isPhysicalClient()) {
			Client.onWallJumpStarted(player, startData);
		}
	}

	private static boolean isPhysicalClient() {
		return FMLEnvironment.dist == Dist.CLIENT;
	}

	private static final class Client {
		private static boolean shouldBlockAfterPhantom(Player player, String phase) {
			return WomParCoolWallJumpClientBridge.shouldBlockAfterPhantom(player, phase);
		}

		private static boolean claimParCoolWallJump(Player player, String phase) {
			return WomParCoolWallJumpClientBridge.claimParCoolWallJump(player, phase);
		}

		private static boolean writeFallbackStartInfo(WallJump wallJump, Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo) {
			return WomParCoolWallJumpClientBridge.writeFallbackStartInfo(wallJump, player, parkourability, stamina, startInfo);
		}

		private static void markNativeStartCandidate(WallJump wallJump, Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo) {
			WomParCoolWallJumpClientBridge.markNativeStartCandidate(wallJump, player, parkourability, stamina, startInfo);
		}

		private static void onWallJumpStarted(Player player, ByteBuffer startData) {
			WomParCoolWallJumpClientBridge.onWallJumpStarted(player, startData);
		}
	}
}
