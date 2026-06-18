package dev.spake404.epm;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.skill.SkillContainer;

public final class DemolitionLeapAirJumpHandler {
	private DemolitionLeapAirJumpHandler() {
	}

	public static void markLaunched(SkillContainer container) {
		if (container == null || container.getExecutor() == null) {
			return;
		}

		Player player = container.getExecutor().getOriginal();
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		boolean marked = EPMClientHooks.markDemolitionLeapForPhantomAscent(player);
		EPM.LOGGER.info(
				"[EPM/DemolitionLeap] phase={} tick={} configAirDoubleJump={} onGround={} delta={} slot={}",
				marked ? "demolition_phantom_prime" : "demolition_phantom_prime_skip",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(EPMConfig.demolitionLeapAirDoubleJump()),
				Boolean.valueOf(player.onGround()),
				player.getDeltaMovement(),
				container.getSlot());
	}

	public static void tickLocalPlayer(TickEvent.PlayerTickEvent event) {
	}
}
