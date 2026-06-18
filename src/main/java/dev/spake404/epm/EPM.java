package dev.spake404.epm;

import dev.spake404.epm.animation.EpmAnimations;
import dev.spake404.epm.animation.EpmLivingMotions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import yesman.epicfight.api.animation.LivingMotion;

@Mod(EPM.MODID)
public class EPM {
	public static final String MODID = "epic_parcool_momentum";
	public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	public EPM(FMLJavaModLoadingContext context) {
		LivingMotion.ENUM_MANAGER.registerEnumCls(MODID, EpmLivingMotions.class);
		context.getModEventBus().addListener(EpmAnimations::register);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EPMConfig.SPEC);
		EPMNetwork.register();
		MinecraftForge.EVENT_BUS.register(EPMEvents.class);
	}
}
