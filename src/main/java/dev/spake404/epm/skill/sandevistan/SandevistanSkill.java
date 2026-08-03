package dev.spake404.epm.skill.sandevistan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import dev.spake404.epm.skill.sandevistan.type.SandevistanProfile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

public final class SandevistanSkill extends Skill {
	private final SandevistanProfile profile;
	private final String tooltipTranslationKey;
	private final Map<SkillContainer, ActivationPlan> pendingActivations = new WeakHashMap<>();

	public SandevistanSkill(
			SkillBuilder<? extends Skill> builder,
			SandevistanProfile profile,
			String tooltipTranslationKey) {
		super(builder);
		this.profile = profile;
		this.tooltipTranslationKey = tooltipTranslationKey;
	}

	public SandevistanProfile getProfile() {
		return profile;
	}

	@Override
	public boolean checkExecuteCondition(SkillContainer container) {
		PlayerPatch<?> executor = container == null ? null : container.getExecutor();
		return executor != null
				&& executor.getOriginal().isAlive()
				&& !SandevistanStateView.isActive(executor.getOriginal());
	}

	@Override
	public boolean resourcePredicate(PlayerPatch<?> playerPatch, SkillCastEvent event) {
		SkillContainer container = event == null ? null : event.getSkillContainer();
		if (container == null) {
			return false;
		}
		if (profile.partialChargeActivation()
				&& container.getStack() <= 0
				&& !playerPatch.getOriginal().isCreative()) {
			return false;
		}

		ActivationPlan activationPlan = createActivationPlan(container, playerPatch);
		configureContainerForActivation(container, activationPlan);
		boolean accepted = super.resourcePredicate(playerPatch, event);
		if (accepted && !playerPatch.isLogicalClient()) {
			pendingActivations.put(container, activationPlan);
		}
		return accepted;
	}

	@Override
	public void executeOnServer(SkillContainer container, FriendlyByteBuf arguments) {
		ActivationPlan activationPlan = pendingActivations.remove(container);
		if (activationPlan == null) {
			activationPlan = createActivationPlan(container, container.getExecutor());
		}
		configureContainerForActivation(container, activationPlan);
		super.executeOnServer(container, arguments);
		container.setDuration(activationPlan.availableDurationTicks());
		SandevistanManager.activate(
				container,
				activationPlan.availableDurationTicks(),
				activationPlan.effectiveMaxDurationTicks(),
				activationPlan.baseDurationTicks());
	}

	@Override
	public void cancelOnClient(SkillContainer container, FriendlyByteBuf arguments) {
		super.cancelOnClient(container, arguments);
		if (container != null) {
			container.deactivate();
		}
	}

	@Override
	public float getCooldownRegenPerSecond(PlayerPatch<?> playerPatch) {
		if (profile.partialChargeActivation()) {
			if (playerPatch != null && SandevistanStateView.isActive(playerPatch.getOriginal())) {
				return 0.0F;
			}
			return (float)(getConsumption() / profile.cooldownSeconds());
		}
		return super.getCooldownRegenPerSecond(playerPatch);
	}

	@Override
	public ResourceLocation getSkillTexture() {
		return ResourceLocation.fromNamespaceAndPath(
				"epic_parcool_momentum",
				"textures/gui/skills/sandevistan.png");
	}

	@Override
	public List<Component> getTooltipOnItem(
			ItemStack itemStack,
			CapabilityItem capabilityItem,
			PlayerPatch<?> playerPatch) {
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(Component.translatable(tooltipTranslationKey + ".summary").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.empty());
		addSection(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.section.performance");
		if (sameValue(profile.groundTimeScale(), profile.airTimeScale())) {
			addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.time_scale",
					formatPercent(profile.groundTimeScale()));
		} else {
			addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.time_scale.dynamic",
					formatPercent(profile.groundTimeScale()),
					formatPercent(profile.airTimeScale()));
		}
		addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.radius",
				formatNumber(profile.radius()));
		addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.player_speed",
				formatNumber(profile.playerSpeedMultiplier()));
		if (sameValue(profile.groundDamageMultiplier(), profile.airDamageMultiplier())) {
			addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.damage",
					formatPercent(profile.groundDamageMultiplier() - 1.0D));
		} else if (profile.airDamageMultiplier() > 1.0D) {
			addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.damage.air",
					formatPercent(profile.airDamageMultiplier() - 1.0D));
		}

		if (profile.incomingDamageMultiplier() < 1.0D
				|| profile.fireWitherDamageMultiplier() < 1.0D
				|| profile.fallDamageMultiplier() < 1.0D) {
			tooltip.add(Component.empty());
			addSection(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.section.defense");
			if (profile.incomingDamageMultiplier() < 1.0D) {
				addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.damage_reduction",
						formatPercent(1.0D - profile.incomingDamageMultiplier()));
			}
			if (profile.fireWitherDamageMultiplier() < 1.0D) {
				addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.fire_wither_reduction",
						formatPercent(1.0D - profile.fireWitherDamageMultiplier()));
			}
			if (profile.fallDamageMultiplier() < 1.0D) {
				addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.fall_reduction",
						formatPercent(1.0D - profile.fallDamageMultiplier()));
			}
		}

		tooltip.add(Component.empty());
		addSection(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.section.duration");
		addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.max_duration",
				formatNumber(getMaxDuration() / 20.0D));
		addValue(tooltip,
				profile.partialChargeActivation()
						? "skill.epic_parcool_momentum.sandevistan.tooltip.full_recharge"
						: "skill.epic_parcool_momentum.sandevistan.tooltip.cooldown",
				formatNumber(profile.cooldownSeconds()));
		if (profile.partialChargeActivation()) {
			addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.partial_charge");
			addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.partial_charge_pause");
		}

		if (profile.killDurationRestoreFraction() > 0.0D
				|| profile.killHealthRestoreFraction() > 0.0D
				|| profile.killStaminaRestoreFraction() > 0.0D) {
			tooltip.add(Component.empty());
			addSection(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.section.kill_reward");
			if (profile.killDurationRestoreFraction() > 0.0D) {
				addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.kill_duration",
						formatPercent(profile.killDurationRestoreFraction()));
			}
			if (profile.killHealthRestoreFraction() > 0.0D) {
				addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.kill_health",
						formatPercent(profile.killHealthRestoreFraction()));
			}
			if (profile.killStaminaRestoreFraction() > 0.0D) {
				addValue(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.kill_stamina",
						formatPercent(profile.killStaminaRestoreFraction()));
			}
		}

		tooltip.add(Component.empty());
		addSection(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.section.rules");
		addValue(tooltip,
				profile.manualCooldownRefundEnabled()
						? "skill.epic_parcool_momentum.sandevistan.tooltip.manual_stop.refund"
						: "skill.epic_parcool_momentum.sandevistan.tooltip.manual_stop.no_refund");
		if (profile.reactionSecondsPerMaxStamina() > 0.0D) {
			double maxStamina = playerPatch == null ? 0.0D : playerPatch.getMaxStamina();
			double addedSeconds = maxStamina * profile.reactionSecondsPerMaxStamina();
			tooltip.add(Component.empty());
			addSection(tooltip, "skill.epic_parcool_momentum.sandevistan.tooltip.section.reaction");
			tooltip.add(Component.translatable(
					"skill.epic_parcool_momentum.sandevistan.reaction_modulation",
					formatNumber(maxStamina),
					formatNumber(addedSeconds)).withStyle(ChatFormatting.GREEN));
		}
		return tooltip;
	}

	private static void addSection(List<Component> tooltip, String translationKey) {
		tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.GOLD));
	}

	private static void addValue(List<Component> tooltip, String translationKey, Object... arguments) {
		tooltip.add(Component.translatable(translationKey, arguments).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	private static boolean sameValue(double first, double second) {
		return Math.abs(first - second) < 0.0001D;
	}

	private static String formatPercent(double value) {
		return formatNumber(value * 100.0D);
	}

	private static String formatNumber(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.0001D) {
			return String.format(Locale.ROOT, "%.0f", value);
		}
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private ActivationPlan createActivationPlan(SkillContainer container, PlayerPatch<?> playerPatch) {
		int baseDurationTicks = Math.max(1, getMaxDuration());
		boolean storedExtendedPool = container.getMaxResource() > getConsumption() + 0.0001F;
		if (profile.partialChargeActivation()
				&& container.getStack() > 0
				&& (container.getResource() > 0.0F || storedExtendedPool)) {
			int effectiveMaxDurationTicks = Math.max(
					baseDurationTicks,
					Math.round(container.getMaxResource() * 20.0F));
			int availableDurationTicks = container.getResource() > 0.0F
					? Math.max(
							1,
							Math.min(
									effectiveMaxDurationTicks,
									Math.round(container.getResource() * 20.0F)))
					: effectiveMaxDurationTicks;
			return new ActivationPlan(
					baseDurationTicks,
					effectiveMaxDurationTicks,
					availableDurationTicks);
		}

		int effectiveMaxDurationTicks = baseDurationTicks + reactionDurationTicks(playerPatch);
		return new ActivationPlan(
				baseDurationTicks,
				effectiveMaxDurationTicks,
				effectiveMaxDurationTicks);
	}

	private int reactionDurationTicks(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || profile.reactionSecondsPerMaxStamina() <= 0.0D) {
			return 0;
		}
		return Math.max(0, (int)Math.round(
				playerPatch.getMaxStamina()
						* profile.reactionSecondsPerMaxStamina()
						* 20.0D));
	}

	private void configureContainerForActivation(
			SkillContainer container,
			ActivationPlan activationPlan) {
		container.setMaxDuration(activationPlan.effectiveMaxDurationTicks());
		if (profile.partialChargeActivation()) {
			container.setMaxResource(activationPlan.effectiveMaxDurationTicks() / 20.0F);
		}
	}

	private record ActivationPlan(
			int baseDurationTicks,
			int effectiveMaxDurationTicks,
			int availableDurationTicks) {
	}
}
