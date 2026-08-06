package dev.spake404.epm.skill.sandevistan.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.spake404.epm.EPM;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.client.gui.ScreenCalculations;
import yesman.epicfight.client.gui.screen.config.UISetupScreen;
import yesman.epicfight.client.gui.widgets.UIComponent;
import yesman.epicfight.config.OptionHandler;

public final class SandevistanHudSetupComponent extends UIComponent {
	private static final ResourceLocation GREEN_CELL = ResourceLocation.fromNamespaceAndPath(
			EPM.MODID,
			"textures/gui/sandevistan/hud_cell_green.png");
	private static final int SOURCE_WIDTH = 6;
	private static final int SOURCE_HEIGHT = 10;

	public SandevistanHudSetupComponent(
			int x,
			int y,
			OptionHandler<Integer> xCoord,
			OptionHandler<Integer> yCoord,
			OptionHandler<ScreenCalculations.HorizontalBasis> horizontalBasis,
			OptionHandler<ScreenCalculations.VerticalBasis> verticalBasis,
			SandevistanHudPosition.HudMetrics metrics,
			UISetupScreen parentScreen) {
		super(
				x,
				y,
				xCoord,
				yCoord,
				horizontalBasis,
				verticalBasis,
				metrics.previewWidth(),
				metrics.cellHeight(),
				0,
				0,
				SOURCE_WIDTH,
				SOURCE_HEIGHT,
				SOURCE_WIDTH,
				SOURCE_HEIGHT,
				255,
				255,
				255,
				parentScreen,
				GREEN_CELL);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		for (int index = 0; index < SandevistanHudPosition.PREVIEW_CELL_COUNT; index++) {
			graphics.blit(
					GREEN_CELL,
					this.getX() + index * this.width / SandevistanHudPosition.PREVIEW_CELL_COUNT,
					this.getY(),
					this.width / SandevistanHudPosition.PREVIEW_CELL_COUNT,
					this.height,
					0.0F,
					0.0F,
					SOURCE_WIDTH,
					SOURCE_HEIGHT,
					SOURCE_WIDTH,
					SOURCE_HEIGHT);
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (this.isHoveredOrFocused() || this.popupScreen.isOpen()) {
			this.drawOutline(graphics);
		}
		if (this.popupScreen.isOpen()) {
			this.popupScreen.render(graphics, mouseX, mouseY, partialTick);
		}
	}
}
