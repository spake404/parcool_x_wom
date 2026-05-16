package dev.spake404.parcool_x_wom;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ParcoolXWom.MODID)
public class ParcoolXWom {
	public static final String MODID = "parcool_x_wom";
	
	public ParcoolXWom(FMLJavaModLoadingContext context) {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ParcoolXWomConfig.SPEC);
		MinecraftForge.EVENT_BUS.register(ParcoolXWomEvents.class);
	}
}
