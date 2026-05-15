package dev.spake404.parcoolxwom;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ParCoolXWom.MOD_ID)
public final class ParCoolXWom {
	public static final String MOD_ID = "parcool_x_wom";

	public ParCoolXWom() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ParCoolXWomConfig.SPEC);
		MinecraftForge.EVENT_BUS.register(ParCoolXWomEvents.class);
	}
}
