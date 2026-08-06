package dev.spake404.epm.item;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.SandevistanSkillRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.world.item.EpicFightCreativeTabs;
import yesman.epicfight.world.item.EpicFightItems;
import yesman.epicfight.world.item.SkillBookItem;

public final class EpmCreativeTabs {
	private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
			Registries.CREATIVE_MODE_TAB,
			EPM.MODID);

	public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
			"main",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.epic_parcool_momentum.main"))
					.icon(EpmCreativeTabs::createIcon)
					.withTabsAfter(EpicFightCreativeTabs.ITEMS.getId())
					.displayItems((parameters, output) -> SandevistanSkillRegistry.addAllSkillBooks(output::accept))
					.build());

	private EpmCreativeTabs() {
	}

	public static void register(IEventBus modEventBus) {
		CREATIVE_MODE_TABS.register(modEventBus);
	}

	private static ItemStack createIcon() {
		ItemStack skillBook = new ItemStack(EpicFightItems.SKILLBOOK.get());
		if (SandevistanSkillRegistry.SANDEVISTAN != null) {
			SkillBookItem.setContainingSkill(SandevistanSkillRegistry.SANDEVISTAN, skillBook);
		}
		return skillBook;
	}
}
