package dev.spake404.parcool_x_wom;

import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;
import reascer.wom.skill.mover.SpiderTechniquesSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class SpiderTechniquesState {
	private static final ResourceLocation SPIDER_TECHNIQUES_ID = ResourceLocation.fromNamespaceAndPath("wom", "spider_techniques");

	private SpiderTechniquesState() {
	}

	public static boolean hasSpiderTechniques(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getSkillCapability() == null) {
			return false;
		}

		try (Stream<SkillContainer> containers = playerPatch.getSkillCapability().listSkillContainers()) {
			return containers.anyMatch(SpiderTechniquesState::isSpiderTechniquesContainer);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isSpiderTechniquesContainer(SkillContainer container) {
		if (container == null) {
			return false;
		}

		Skill skill = container.getSkill();
		return skill instanceof SpiderTechniquesSkill || skill != null && SPIDER_TECHNIQUES_ID.equals(skill.getRegistryName());
	}
}
