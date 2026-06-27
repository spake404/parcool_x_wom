package dev.spake404.epm.event;

import dev.spake404.epm.debug.CameraEventDebug;
import dev.spake404.epm.debug.RotationTraceDebug;
import dev.spake404.epm.demolition.DemolitionLeapCatJumpHandler;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.vault.VaultCameraAnimationSmoother;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
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

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void captureCameraEventStart(ViewportEvent.ComputeCameraAngles event) {
		CameraEventDebug.captureStart(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void smoothEpicParCoolVaultCamera(ViewportEvent.ComputeCameraAngles event) {
		VaultCameraAnimationSmoother.smooth(event);
	}

	@SubscribeEvent(priority = EventPriority.MONITOR)
	public static void logCameraEventEnd(ViewportEvent.ComputeCameraAngles event) {
		CameraEventDebug.logEnd(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void logClientTickStart(TickEvent.ClientTickEvent event) {
		RotationTraceDebug.logClientTick(event, "highest");
	}

	@SubscribeEvent(priority = EventPriority.MONITOR)
	public static void logClientTickEnd(TickEvent.ClientTickEvent event) {
		RotationTraceDebug.logClientTick(event, "monitor");
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void logRenderTickStart(TickEvent.RenderTickEvent event) {
		RotationTraceDebug.logRenderTick(event, "highest");
	}

	@SubscribeEvent(priority = EventPriority.MONITOR)
	public static void logRenderTickEnd(TickEvent.RenderTickEvent event) {
		RotationTraceDebug.logRenderTick(event, "monitor");
	}
}
