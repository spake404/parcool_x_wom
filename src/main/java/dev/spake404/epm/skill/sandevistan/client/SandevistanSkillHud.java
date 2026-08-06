package dev.spake404.epm.skill.sandevistan.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.EpmSkillSlots;
import dev.spake404.epm.skill.sandevistan.SandevistanSkill;
import dev.spake404.epm.skill.sandevistan.type.SandevistanProfile;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.client.gui.screen.config.UISetupScreen;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class SandevistanSkillHud {
	private static final ResourceLocation GREEN_CELL = ResourceLocation.fromNamespaceAndPath(
			EPM.MODID,
			"textures/gui/sandevistan/hud_cell_green.png");
	private static final ResourceLocation RED_CELL = ResourceLocation.fromNamespaceAndPath(
			EPM.MODID,
			"textures/gui/sandevistan/hud_cell_red.png");
	private static final Map<SandevistanSkill, HudState> STATES = new IdentityHashMap<>();
	private static final int SOURCE_WIDTH = 6;
	private static final int SOURCE_HEIGHT = 10;
	private static final int FULL_HIDE_DELAY_TICKS = 20;
	private static final int FADE_TICKS = 20;
	private static final float EPSILON = 1.0E-4F;

	private SandevistanSkillHud() {
	}

	public static void render(GuiGraphics graphics, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null
				|| minecraft.options.hideGui
				|| minecraft.screen instanceof UISetupScreen) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(
				minecraft.player,
				PlayerPatch.class);
		SkillContainer container = playerPatch == null ? null : playerPatch.getSkill(EpmSkillSlots.PARKOUR);
		if (container == null
				|| container.isEmpty()
				|| !(container.getSkill() instanceof SandevistanSkill skill)) {
			return;
		}
		draw(skill, container, graphics, partialTick);
	}

	private static void draw(
			SandevistanSkill skill,
			SkillContainer container,
			GuiGraphics graphics,
			float partialTick) {
		PlayerPatch<?> playerPatch = container == null ? null : container.getExecutor();
		if (skill == null
				|| graphics == null
				|| playerPatch == null) {
			return;
		}
		Player player = playerPatch.getOriginal();

		HudState state = state(skill, player);
		float syncedRemainingTicks = SandevistanClientState.remainingTicksForHud(player, partialTick);
		boolean energyIncomplete = isEnergyIncomplete(
				skill,
				container,
				state,
				partialTick,
				syncedRemainingTicks);
		float alpha = visibilityAlpha(
				player,
				state,
				SandevistanHudCombatState.isInCombat(player),
				energyIncomplete,
				partialTick);
		if (alpha <= EPSILON) {
			return;
		}

		int capacityTicks = resolveCapacityTicks(
				skill,
				container,
				playerPatch,
				state,
				syncedRemainingTicks >= 0.0F);
		int cellCount = Math.max(1, (int)Math.floor(capacityTicks / 20.0D + 0.5D));
		int greenCells = resolveGreenCells(
				skill,
				container,
				capacityTicks,
				cellCount,
				partialTick,
				state,
				syncedRemainingTicks);
		Minecraft minecraft = Minecraft.getInstance();
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = minecraft.getWindow().getGuiScaledHeight();
		SandevistanHudPosition.HudMetrics metrics = SandevistanHudPosition.metrics(screenWidth, screenHeight);
		int barWidth = metrics.barWidth(cellCount);
		int[] origin = resolveOrigin(screenWidth, screenHeight, barWidth, metrics);

		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 100.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		for (int index = 0; index < cellCount; index++) {
			ResourceLocation texture = index < greenCells ? GREEN_CELL : RED_CELL;
			int x = origin[0] + index * metrics.cellWidth();
			graphics.blit(
					texture,
					x,
					origin[1],
					metrics.cellWidth(),
					metrics.cellHeight(),
					0.0F,
					0.0F,
					SOURCE_WIDTH,
					SOURCE_HEIGHT,
					SOURCE_WIDTH,
					SOURCE_HEIGHT);
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.pose().popPose();
	}

	private static HudState state(SandevistanSkill skill, Player player) {
		HudState state = STATES.computeIfAbsent(skill, ignored -> new HudState());
		UUID playerId = player.getUUID();
		if (!playerId.equals(state.playerId)) {
			state.playerId = playerId;
			state.capacityTicks = 0;
			state.visibilityInitialized = false;
			state.visibleConditionActive = false;
			state.hideStartedTick = -1;
		}
		return state;
	}

	private static float visibilityAlpha(
			Player player,
			HudState state,
			boolean inCombat,
			boolean energyIncomplete,
			float partialTick) {
		boolean visibleCondition = inCombat || energyIncomplete;
		if (!state.visibilityInitialized) {
			state.visibilityInitialized = true;
			state.visibleConditionActive = visibleCondition;
			return visibleCondition ? 1.0F : 0.0F;
		}

		if (visibleCondition) {
			state.visibleConditionActive = true;
			state.hideStartedTick = -1;
			return 1.0F;
		}

		if (state.visibleConditionActive) {
			state.visibleConditionActive = false;
			state.hideStartedTick = player.tickCount;
		}
		if (state.hideStartedTick < 0 || player.tickCount < state.hideStartedTick) {
			return 0.0F;
		}

		float elapsedTicks = player.tickCount + partialTick - state.hideStartedTick;
		if (elapsedTicks <= FULL_HIDE_DELAY_TICKS) {
			return 1.0F;
		}
		return Mth.clamp(
				1.0F - (elapsedTicks - FULL_HIDE_DELAY_TICKS) / FADE_TICKS,
				0.0F,
				1.0F);
	}

	private static boolean isEnergyIncomplete(
			SandevistanSkill skill,
			SkillContainer container,
			HudState state,
			float partialTick,
			float syncedRemainingTicks) {
		if (syncedRemainingTicks >= 0.0F) {
			return true;
		}
		if (container.getExecutor().getOriginal().isCreative()) {
			return false;
		}
		if (hasStoredPartialDuration(skill, container, state)) {
			return container.getResource(partialTick) < 1.0F - EPSILON;
		}
		if (container.getStack() > 0) {
			return false;
		}
		return container.getResource(partialTick) < 1.0F - EPSILON;
	}

	private static int resolveCapacityTicks(
			SandevistanSkill skill,
			SkillContainer container,
			PlayerPatch<?> playerPatch,
			HudState state,
			boolean clientActivationActive) {
		int calculatedCapacity = Math.max(
				1,
				skill.getMaxDuration() + (int)Math.round(
						playerPatch.getMaxStamina()
								* skill.getProfile().reactionSecondsPerMaxStamina()
								* 20.0D));

		if (clientActivationActive) {
			float exactRatio = container.getDurationRatio(1.0F);
			if (container.isActivated() && exactRatio > EPSILON && container.getRemainDuration() > 0) {
				state.capacityTicks = Math.max(
						1,
						Math.round(container.getRemainDuration() / exactRatio));
			} else if (state.capacityTicks <= 0) {
				state.capacityTicks = calculatedCapacity;
			}
			return state.capacityTicks;
		}

		if (hasStoredPartialDuration(skill, container, state)) {
			state.capacityTicks = Math.max(1, Math.round(container.getMaxResource() * 20.0F));
			return state.capacityTicks;
		}

		if (container.getStack() > 0 || state.capacityTicks <= 0) {
			state.capacityTicks = calculatedCapacity;
		}
		return state.capacityTicks;
	}

	private static int resolveGreenCells(
			SandevistanSkill skill,
			SkillContainer container,
			int capacityTicks,
			int cellCount,
			float partialTick,
			HudState state,
			float syncedRemainingTicks) {
		if (syncedRemainingTicks >= 0.0F) {
			float ratio = Mth.clamp(syncedRemainingTicks / Math.max(1.0F, capacityTicks), 0.0F, 1.0F);
			return Mth.clamp((int)Math.ceil(ratio * cellCount - EPSILON), 0, cellCount);
		}

		if (container.getExecutor().getOriginal().isCreative()) {
			return cellCount;
		}

		if (hasStoredPartialDuration(skill, container, state)) {
			float ratio = Mth.clamp(container.getResource(partialTick), 0.0F, 1.0F);
			return Mth.clamp((int)Math.ceil(ratio * cellCount - EPSILON), 0, cellCount);
		}

		if (container.getStack() > 0) {
			return cellCount;
		}

		float cooldownRatio = Mth.clamp(container.getResource(partialTick), 0.0F, 1.0F);
		return Mth.clamp((int)Math.floor(cooldownRatio * cellCount + EPSILON), 0, cellCount);
	}

	private static boolean hasStoredPartialDuration(
			SandevistanSkill skill,
			SkillContainer container,
			HudState state) {
		SandevistanProfile profile = skill.getProfile();
		if (!profile.partialChargeActivation()
				|| container.getResource() <= EPSILON) {
			return false;
		}

		float maximumResource = container.getMaxResource();
		return Math.abs(maximumResource - skill.getConsumption()) > EPSILON
				|| state.capacityTicks > 0
				&& Math.abs(maximumResource * 20.0F - state.capacityTicks) < 1.0F;
	}

	private static int[] resolveOrigin(
			int screenWidth,
			int screenHeight,
			int barWidth,
			SandevistanHudPosition.HudMetrics metrics) {
		return new int[]{
				SandevistanHudPosition.screenX(screenWidth, barWidth, metrics),
				SandevistanHudPosition.screenY(screenHeight, metrics.cellHeight(), metrics)};
	}

	private static final class HudState {
		private UUID playerId;
		private int capacityTicks;
		private boolean visibilityInitialized;
		private boolean visibleConditionActive;
		private int hideStartedTick = -1;
	}
}
