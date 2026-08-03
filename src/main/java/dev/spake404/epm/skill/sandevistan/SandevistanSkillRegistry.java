package dev.spake404.epm.skill.sandevistan;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.EpmSkillCategories;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfile;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfiles;
import java.util.function.Consumer;
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
	public static Skill QIANT_WARP_DANCER_MK5;
	public static Skill MILITECH_FALCON;
	public static Skill MILITECH_APOGEE;
	public static Skill ZETATECH;

	private SandevistanSkillRegistry() {
	}

	@SubscribeEvent
	public static void buildSkills(SkillBuildEvent event) {
		SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker(EPM.MODID);
		SANDEVISTAN = build(worker, "sandevistan", SandevistanProfiles.DINARA_MK4);
		QIANT_WARP_DANCER_MK5 = build(worker, "qiant_warp_dancer_mk5", SandevistanProfiles.QIANT_WARP_DANCER_MK5);
		MILITECH_FALCON = build(worker, "militech_falcon", SandevistanProfiles.MILITECH_FALCON);
		MILITECH_APOGEE = build(worker, "militech_apogee", SandevistanProfiles.MILITECH_APOGEE);
		ZETATECH = build(worker, "zetatech_sandevistan", SandevistanProfiles.ZETATECH);
	}

	@SubscribeEvent
	public static void addSkillBookToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTab() != EpicFightCreativeTabs.ITEMS.get()) {
			return;
		}

		addAllSkillBooks(event::accept);
	}

	public static void addAllSkillBooks(Consumer<ItemStack> output) {
		addSkillBook(output, SANDEVISTAN);
		addSkillBook(output, QIANT_WARP_DANCER_MK5);
		addSkillBook(output, MILITECH_FALCON);
		addSkillBook(output, MILITECH_APOGEE);
		addSkillBook(output, ZETATECH);
	}

	private static Skill build(
			SkillBuildEvent.ModRegistryWorker worker,
			String name,
			SandevistanProfile profile) {
		return worker.build(
				name,
				builder -> new SandevistanSkill(
						builder,
						profile,
						"skill." + EPM.MODID + "." + name + ".tooltip"),
				sandevistanBuilder());
	}

	private static yesman.epicfight.skill.SkillBuilder<Skill> sandevistanBuilder() {
		return new yesman.epicfight.skill.SkillBuilder<Skill>()
				.setCategory(EpmSkillCategories.PARKOUR)
				.setActivateType(Skill.ActivateType.DURATION)
				.setResource(Skill.Resource.COOLDOWN);
	}

	private static void addSkillBook(Consumer<ItemStack> output, Skill skill) {
		if (output == null || skill == null) {
			return;
		}

		ItemStack skillBook = new ItemStack(EpicFightItems.SKILLBOOK.get());
		SkillBookItem.setContainingSkill(skill, skillBook);
		output.accept(skillBook);
	}
}
