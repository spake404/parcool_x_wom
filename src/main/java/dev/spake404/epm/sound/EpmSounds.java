package dev.spake404.epm.sound;

import dev.spake404.epm.EPM;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EpmSounds {
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
			ForgeRegistries.SOUND_EVENTS,
			EPM.MODID);

	public static final RegistryObject<SoundEvent> SANDEVISTAN_START = SOUNDS.register(
			"sandevistan_start",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(
					EPM.MODID,
					"sandevistan_start")));

	private EpmSounds() {
	}

	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
	}
}
