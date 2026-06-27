package dev.spake404.epm.vault;

import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.config.ParCoolConfig;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.compat.ModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ViewportEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class VaultCameraAnimationSmoother {
	private static final int VAULT_CAMERA_ANIMATION_TICKS = 11;
	private static LocalPlayer activePlayer;
	private static VaultCameraState activeState;

	private VaultCameraAnimationSmoother() {
	}

	public static void markStart(Player player, Vault vault) {
		if (!shouldRegister(player) || !(player instanceof LocalPlayer localPlayer) || vault == null) {
			return;
		}

		Vault.AnimationType animationType = vault.getCurrentAnimation();
		if (animationType != null) {
			activePlayer = localPlayer;
			activeState = new VaultCameraState(localPlayer.tickCount, animationType);
		}
	}

	public static void smooth(ViewportEvent.ComputeCameraAngles event) {
		if (activeState == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || player != activePlayer) {
			clear();
			return;
		}

		if (minecraft.options == null || !minecraft.options.getCameraType().isFirstPerson()) {
			clear();
			return;
		}

		float elapsed = player.tickCount - activeState.startTick() + (float) event.getPartialTick();
		if (elapsed < 0.0F || elapsed >= VAULT_CAMERA_ANIMATION_TICKS) {
			clear();
			return;
		}

		applyParCoolVaultCamera(event, player, activeState.animationType(), elapsed);
	}

	private static void clear() {
		activePlayer = null;
		activeState = null;
	}

	private static void applyParCoolVaultCamera(ViewportEvent.ComputeCameraAngles event, LocalPlayer player,
			Vault.AnimationType animationType, float elapsed) {
		float phase = clamp01(elapsed / VAULT_CAMERA_ANIMATION_TICKS);
		switch (animationType) {
			case KONG_VAULT -> {
				float factor = vaultFactor(phase);
				event.setPitch(player.getViewXRot((float) event.getPartialTick()) + 30.0F * factor);
			}
			case SPEED_VAULT_RIGHT, SPEED_VAULT_LEFT -> {
				float factor = vaultFactor(phase);
				float forwardFactor = (float) Math.sin(phase * 2.0F * Math.PI) + 0.5F;
				event.setPitch(15.0F * forwardFactor);
				event.setRoll((animationType == Vault.AnimationType.SPEED_VAULT_RIGHT ? -25.0F : 25.0F) * factor);
			}
		}
	}

	private static float vaultFactor(float phase) {
		float mirrored = phase < 0.5F ? phase * 2.0F : 2.0F - phase * 2.0F;
		return sinInOutBySquare(clamp01(mirrored));
	}

	private static float sinInOutBySquare(float phase) {
		return phase < 0.5F ? 2.0F * phase * phase : 1.0F - 2.0F * (phase - 1.0F) * (phase - 1.0F);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static boolean shouldRegister(Player player) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()
				|| !ModCompat.isEpicParCoolLoaded()
				|| player == null
				|| !player.isLocalPlayer()
				|| !player.level().isClientSide()) {
			return false;
		}
		try {
			if (!ParCoolConfig.Client.Booleans.EnableCameraAnimationOfVault.get()) {
				return false;
			}
			PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
			return playerPatch != null && playerPatch.isEpicFightMode();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private record VaultCameraState(int startTick, Vault.AnimationType animationType) {
	}
}
