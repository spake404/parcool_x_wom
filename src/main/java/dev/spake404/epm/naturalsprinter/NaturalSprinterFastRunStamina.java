package dev.spake404.epm.naturalsprinter;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class NaturalSprinterFastRunStamina {
	public static final float GENERIC_FAST_RUN_STEP_STAMINA_COST = 2.0F;

	private NaturalSprinterFastRunStamina() {
	}

	public static boolean consumeGenericFastRunStepStamina(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getOriginal() == null) {
			return false;
		}
		if (!playerPatch.hasStamina(GENERIC_FAST_RUN_STEP_STAMINA_COST)) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		if (!player.getAbilities().instabuild) {
			try {
				playerPatch.resetActionTick();
				playerPatch.setStamina(Math.max(0.0F, playerPatch.getStamina() - GENERIC_FAST_RUN_STEP_STAMINA_COST));
			} catch (RuntimeException | LinkageError ignored) {
				return false;
			}
		}
		return true;
	}
}
