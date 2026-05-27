package dev.spake404.epm;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class EPMKeyMappings {
	private static final String CATEGORY = "key.categories.epic_parcool_momentum";
	private static final KeyMapping NATURAL_SPRINTER_STEP = new KeyMapping(
			"key.epic_parcool_momentum.natural_sprinter_step",
			GLFW.GLFW_KEY_R,
			CATEGORY);

	private EPMKeyMappings() {
	}

	public static void register(RegisterKeyMappingsEvent event) {
		event.register(NATURAL_SPRINTER_STEP);
	}

	public static boolean isNaturalSprinterStepDown() {
		return NATURAL_SPRINTER_STEP.isDown();
	}
}
