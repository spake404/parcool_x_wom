package dev.spake404.epm.skill.sandevistan.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.spake404.epm.EPM;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EPM.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SandevistanKeyMappings {
	public static final KeyMapping ACTIVATE = new KeyMapping(
			"key.epic_parcool_momentum.sandevistan",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			"key.categories.epic_parcool_momentum");

	private SandevistanKeyMappings() {
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ACTIVATE);
	}
}
