package dev.spake404.epm.skill;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.skill.SkillCategory;

public final class EpmSkillCategories implements SkillCategory {
	public static final EpmSkillCategories PARKOUR = new EpmSkillCategories("PARKOUR");
	private static final EpmSkillCategories[] VALUES = {PARKOUR};

	private final String name;
	private final int id;

	private EpmSkillCategories(String name) {
		this.name = name;
		this.id = SkillCategory.ENUM_MANAGER.assign(this);
	}

	public static EpmSkillCategories[] values() {
		return VALUES.clone();
	}

	@Override
	public boolean shouldSave() {
		return true;
	}

	@Override
	public boolean shouldSynchronize() {
		return true;
	}

	@Override
	public boolean learnable() {
		return true;
	}

	@Override
	public ResourceLocation bookIcon() {
		return SkillCategory.DEFAULT_BOOK_ICON;
	}

	@Override
	public int universalOrdinal() {
		return this.id;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
