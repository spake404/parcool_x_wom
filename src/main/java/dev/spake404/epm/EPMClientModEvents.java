package dev.spake404.epm;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.config.ParCoolConfig;
import net.minecraftforge.api.distmarker.Dist;
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
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		EPMKeyMappings.register(event);
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
