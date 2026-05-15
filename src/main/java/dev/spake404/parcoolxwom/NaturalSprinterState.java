package dev.spake404.parcoolxwom;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class NaturalSprinterState {
	private static final ResourceLocation NATURAL_SPRINTER_ID = ResourceLocation.fromNamespaceAndPath("wom", "natural_sprinter");
	private static volatile SkillDataKey<?> activeKey;
	private static volatile SkillDataKey<?> buffingKey;
	private static volatile SkillDataKey<?> timerKey;

	private NaturalSprinterState() {
	}

	public static void suppress(PlayerPatch<?> playerPatch) {
		try {
			SkillContainer naturalSprinter = findNaturalSprinter(playerPatch);
			if (naturalSprinter == null) {
				return;
			}

			setSkillDataSync(naturalSprinter, getActiveKey(), Boolean.FALSE);
			setSkillDataSync(naturalSprinter, getBuffingKey(), Boolean.FALSE);
			setSkillDataSync(naturalSprinter, getTimerKey(), Integer.valueOf(0));
		} catch (Throwable ignored) {
		}
	}

	public static boolean hasNaturalSprinter(PlayerPatch<?> playerPatch) {
		return findNaturalSprinter(playerPatch) != null;
	}

	private static SkillContainer findNaturalSprinter(PlayerPatch<?> playerPatch) {
		try {
			Stream<SkillContainer> containers = playerPatch.getSkillCapability().listSkillContainers();
			return containers
					.filter(container -> hasSkillId(container, NATURAL_SPRINTER_ID))
					.findFirst()
					.orElse(null);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static boolean hasSkillId(SkillContainer container, ResourceLocation skillId) {
		Skill skill = container.getSkill();
		if (skill == null) {
			return false;
		}

		if (skillId.equals(skill.getRegistryName())) {
			return true;
		}

		return "reascer.wom.skill.mover.NaturalSprinterSkill".equals(skill.getClass().getName());
	}

	private static SkillDataKey<?> getActiveKey() {
		SkillDataKey<?> key = activeKey;
		if (key == null) {
			key = readWomDataKey("ACTIVE");
			activeKey = key;
		}
		return key;
	}

	private static SkillDataKey<?> getBuffingKey() {
		SkillDataKey<?> key = buffingKey;
		if (key == null) {
			key = readWomDataKey("BUFFING");
			buffingKey = key;
		}
		return key;
	}

	private static SkillDataKey<?> getTimerKey() {
		SkillDataKey<?> key = timerKey;
		if (key == null) {
			key = readWomDataKey("TIMER");
			timerKey = key;
		}
		return key;
	}

	private static SkillDataKey<?> readWomDataKey(String fieldName) {
		try {
			Class<?> keysClass = Class.forName("reascer.wom.skill.WOMSkillDataKeys");
			Field field = keysClass.getField(fieldName);
			Object registryObject = field.get(null);
			if (registryObject instanceof RegistryObject<?> typedRegistryObject) {
				Object key = typedRegistryObject.get();
				if (key instanceof SkillDataKey<?> skillDataKey) {
					return skillDataKey;
				}
			}
		} catch (Throwable ignored) {
		}

		throw new IllegalStateException("Unable to resolve WOM skill data key: " + fieldName);
	}

	private static void setSkillDataSync(SkillContainer container, SkillDataKey<?> key, Object value) {
		try {
			container.getDataManager().getClass()
					.getMethod("setDataSync", SkillDataKey.class, Object.class)
					.invoke(container.getDataManager(), key, value);
		} catch (Throwable ignored) {
		}
	}
}
