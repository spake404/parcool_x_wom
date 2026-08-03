package dev.spake404.epm.skill.sandevistan.client.filter;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.spake404.epm.skill.sandevistan.client.debug.SandevistanRenderDiagnostics;
import net.minecraft.client.Minecraft;

public final class SandevistanStencilInitializer {
	private SandevistanStencilInitializer() {
	}

	public static void initialize() {
		RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
		if (target.isStencilEnabled()) {
			return;
		}

		SandevistanRenderDiagnostics.beforeStencilEnable(target);
		target.enableStencil();
		SandevistanRenderDiagnostics.afterStencilEnable(target);
	}
}
