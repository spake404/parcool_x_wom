package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.EpmSkillSlots;
import dev.spake404.epm.skill.sandevistan.client.blur.SandevistanEdgeBlurRenderer;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
		SandevistanFilterRenderer.tick();
		SandevistanEdgeBlurRenderer.tick();
		while (SandevistanKeyMappings.ACTIVATE.consumeClick()) {
			EpicFightNetworkManager.sendToServer(new CPSkillRequest(EpmSkillSlots.PARKOUR));
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void clearFilterStencil(RenderLevelStageEvent event) {
		SandevistanFilterRenderer.clearStencil(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void renderSandevistanEdgeBlur(RenderLevelStageEvent event) {
		SandevistanEdgeBlurRenderer.render(event);
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
}
