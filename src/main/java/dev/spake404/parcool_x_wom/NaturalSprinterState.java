package dev.spake404.parcool_x_wom;

import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;
import reascer.wom.skill.WOMSkillDataKeys;
import reascer.wom.skill.mover.NaturalSprinterSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class NaturalSprinterState {
	private static final ResourceLocation NATURAL_SPRINTER_ID = ResourceLocation.fromNamespaceAndPath("wom", "natural_sprinter");

	private NaturalSprinterState() {
	}

	public static void suppress(PlayerPatch<?> playerPatch) {
		SkillContainer naturalSprinter = findNaturalSprinter(playerPatch);
		if (naturalSprinter == null) {
			return;
		}

		SkillDataManager dataManager = naturalSprinter.getDataManager();
		setData(dataManager, activeKey(), Boolean.FALSE);
		setData(dataManager, buffingKey(), Boolean.FALSE);
		setData(dataManager, timerKey(), Integer.valueOf(0));
	}

	public static boolean hasNaturalSprinter(PlayerPatch<?> playerPatch) {
		return findNaturalSprinter(playerPatch) != null;
	}

	private static SkillContainer findNaturalSprinter(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getSkillCapability() == null) {
			return null;
		}

		try (Stream<SkillContainer> containers = playerPatch.getSkillCapability().listSkillContainers()) {
			return containers
					.filter(NaturalSprinterState::isNaturalSprinterContainer)
					.findFirst()
					.orElse(null);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isNaturalSprinterContainer(SkillContainer container) {
		if (container == null) {
			return false;
		}

		Skill skill = container.getSkill();
		if (skill == null) {
			return false;
		}

		if (skill instanceof NaturalSprinterSkill) {
			return true;
		}

		return NATURAL_SPRINTER_ID.equals(skill.getRegistryName());
	}

	private static SkillDataKey<Boolean> activeKey() {
		return key(WOMSkillDataKeys.ACTIVE);
	}

	private static SkillDataKey<Boolean> buffingKey() {
		return key(WOMSkillDataKeys.BUFFING);
	}

	private static SkillDataKey<Integer> timerKey() {
		return key(WOMSkillDataKeys.TIMER);
	}

	private static <T> SkillDataKey<T> key(RegistryObject<SkillDataKey<T>> registryObject) {
		return registryObject == null ? null : registryObject.get();
	}

	private static <T> void setData(SkillDataManager dataManager, SkillDataKey<T> key, T value) {
		if (dataManager == null || key == null) {
			return;
		}

		try {
			dataManager.setDataSync(key, value);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}
}
