package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.EpmSkillSlots;
import dev.spake404.epm.skill.sandevistan.SandevistanStateView;
import dev.spake404.epm.skill.sandevistan.client.blur.SandevistanEdgeBlurRenderer;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanMaskRenderer;
import dev.spake404.epm.skill.sandevistan.network.SandevistanNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPSkillRequest;

@Mod.EventBusSubscriber(modid = EPM.MODID, value = Dist.CLIENT)
public final class SandevistanClientEvents {
	private SandevistanClientEvents() {
	}

	@SubscribeEvent
	public static void clientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		SandevistanClientState.tick();
		SandevistanHudCombatState.tick();
		SandevistanFilterRenderer.tick();
		SandevistanMaskRenderer.tick();
		SandevistanEdgeBlurRenderer.tick();
		while (SandevistanKeyMappings.ACTIVATE.consumeClick()) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null && SandevistanStateView.isActive(minecraft.player)) {
				SandevistanNetwork.sendManualStopRequest();
			} else {
				EpicFightNetworkManager.sendToServer(new CPSkillRequest(EpmSkillSlots.PARKOUR));
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void renderSandevistanMask(RenderLevelStageEvent event) {
		SandevistanMaskRenderer.render(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void renderSandevistanEdgeBlur(RenderLevelStageEvent event) {
		SandevistanEdgeBlurRenderer.render(event);
	}

	@SubscribeEvent
	public static void renderSandevistanAfterimages(RenderLevelStageEvent event) {
		SandevistanAfterimageRenderer.render(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void renderSandevistanFilter(RenderGuiEvent.Pre event) {
		long startedNanos = System.nanoTime();
		try {
			SandevistanFilterRenderer.renderFilter(event);
		} finally {
			SandevistanPerformanceDiagnostics.recordFilter(System.nanoTime() - startedNanos);
			SandevistanPerformanceDiagnostics.finishFrame();
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void renderSandevistanHud(RenderGuiOverlayEvent.Post event) {
		if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
			SandevistanSkillHud.render(event.getGuiGraphics(), event.getPartialTick());
		}
	}
}
