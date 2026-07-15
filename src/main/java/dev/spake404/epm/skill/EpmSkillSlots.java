package dev.spake404.epm.skill;

import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

public final class EpmSkillSlots implements SkillSlot {
	public static final EpmSkillSlots PARKOUR = new EpmSkillSlots("PARKOUR", EpmSkillCategories.PARKOUR);
	private static final EpmSkillSlots[] VALUES = {PARKOUR};

	private final String name;
	private final SkillCategory category;
	private final int id;

	private EpmSkillSlots(String name, SkillCategory category) {
		this.name = name;
		this.category = category;
		this.id = SkillSlot.ENUM_MANAGER.assign(this);
	}

	public static EpmSkillSlots[] values() {
		return VALUES.clone();
	}

	@Override
	public SkillCategory category() {
		return this.category;
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
