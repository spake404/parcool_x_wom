package dev.spake404.epm.event;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.input.EPMKeyMappings;
import dev.spake404.epm.skill.sandevistan.client.blur.SandevistanEdgeBlurRenderer;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanStencilInitializer;
import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EPM.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EPMClientModEvents {
	private EPMClientModEvents() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(EPMClientModEvents::applyParCoolDodgeDefault);
		event.enqueueWork(SandevistanStencilInitializer::initialize);
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		EPMKeyMappings.register(event);
	}

	@SubscribeEvent
	public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(SandevistanEdgeBlurRenderer.reloadListener());
	}

	private static void applyParCoolDodgeDefault() {
		if (EPMConfig.parCoolDodgeDefaultMigrationApplied()) {
			return;
		}

		ParCoolConfig.Client.getPossibilityOf(Dodge.class).set(false);
		ParCoolConfig.Client.BUILT_CONFIG.save();
		EPMConfig.markParCoolDodgeDefaultMigrationApplied();
	}
}
