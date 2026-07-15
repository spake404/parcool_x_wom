package dev.spake404.epm.skill.sandevistan;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.EpmSkillCategories;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.item.EpicFightCreativeTabs;
import yesman.epicfight.world.item.EpicFightItems;
import yesman.epicfight.world.item.SkillBookItem;

@Mod.EventBusSubscriber(modid = EPM.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SandevistanSkillRegistry {
	public static Skill SANDEVISTAN;

	private SandevistanSkillRegistry() {
	}

	@SubscribeEvent
	public static void buildSkills(SkillBuildEvent event) {
		SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker(EPM.MODID);
		SANDEVISTAN = worker.build(
				"sandevistan",
				SandevistanSkill::new,
				new yesman.epicfight.skill.SkillBuilder<Skill>()
						.setCategory(EpmSkillCategories.PARKOUR)
						.setActivateType(Skill.ActivateType.DURATION)
						.setResource(Skill.Resource.COOLDOWN));
	}

	@SubscribeEvent
	public static void addSkillBookToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (SANDEVISTAN == null || event.getTab() != EpicFightCreativeTabs.ITEMS.get()) {
			return;
		}

		ItemStack skillBook = new ItemStack(EpicFightItems.SKILLBOOK.get());
		SkillBookItem.setContainingSkill(SANDEVISTAN, skillBook);
		event.accept(skillBook);
	}
}
