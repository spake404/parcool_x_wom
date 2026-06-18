package dev.spake404.epm;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EPM.MODID, value = Dist.CLIENT)
public final class EPMClientEvents {
	private EPMClientEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void delayGliderOpeningSound(PlaySoundEvent event) {
		EPMClientHooks.delayGliderOpeningSound(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void suppressVanillaJumpForDemolitionLeapCatJump(MovementInputUpdateEvent event) {
		if (DemolitionLeapCatJumpHandler.shouldSuppressVanillaJump(event.getEntity())) {
			event.getInput().jumping = false;
		}
		if (DemolitionLeapCatJumpHandler.shouldSuppressSneak(event.getEntity())) {
			event.getInput().shiftKeyDown = false;
		}
	}
}
