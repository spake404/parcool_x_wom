package dev.spake404.epm.skill.sandevistan;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public final class SandevistanSkill extends Skill {
	public SandevistanSkill(SkillBuilder<? extends Skill> builder) {
		super(builder);
	}

	@Override
	public boolean checkExecuteCondition(SkillContainer container) {
		PlayerPatch<?> executor = container == null ? null : container.getExecutor();
		return executor != null
				&& executor.getOriginal().isAlive()
				&& !SandevistanStateView.isActive(executor.getOriginal());
	}

	@Override
	public void executeOnServer(SkillContainer container, FriendlyByteBuf arguments) {
		super.executeOnServer(container, arguments);
		SandevistanManager.activate(container);
	}

	@Override
	public ResourceLocation getSkillTexture() {
		return ResourceLocation.fromNamespaceAndPath(
				"epicfight",
				"textures/gui/skills/mover/phantom_ascent.png");
	}

	@Override
	public List<Component> getTooltipOnItem(
			ItemStack itemStack,
			CapabilityItem capabilityItem,
			PlayerPatch<?> playerPatch) {
		return List.of(Component.translatable("skill.epic_parcool_momentum.sandevistan.tooltip"));
	}
}
