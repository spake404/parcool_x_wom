package dev.spake404.epm.mixin;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.client.SandevistanHudPosition;
import dev.spake404.epm.skill.sandevistan.client.SandevistanHudSetupComponent;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.gui.ScreenCalculations;
import yesman.epicfight.client.gui.screen.config.UISetupScreen;
import yesman.epicfight.config.OptionHandler;

@Mixin(value = UISetupScreen.class, remap = false)
public abstract class UISetupScreenMixin extends Screen {
	protected UISetupScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void epm$addSandevistanHudComponent(CallbackInfo callback) {
		UISetupScreen screen = (UISetupScreen)(Object)this;
		ScreenCalculations.HorizontalBasis horizontalBasis = SandevistanHudPosition.horizontalBasis();
		ScreenCalculations.VerticalBasis verticalBasis = SandevistanHudPosition.verticalBasis();
		SandevistanHudPosition.HudMetrics metrics = SandevistanHudPosition.metrics(this.width, this.height);
		OptionHandler<ScreenCalculations.HorizontalBasis> horizontalBasisOption = OptionHandler.of(
				horizontalBasis,
				SandevistanHudPosition::setHorizontalBasis);
		OptionHandler<ScreenCalculations.VerticalBasis> verticalBasisOption = OptionHandler.of(
				verticalBasis,
				SandevistanHudPosition::setVerticalBasis);
		OptionHandler<Integer> xCoord = OptionHandler.of(
				SandevistanHudPosition.setupXCoordinate(metrics),
				value -> SandevistanHudPosition.setXFromSetup(
						value,
						horizontalBasisOption.getValue(),
						metrics));
		OptionHandler<Integer> yCoord = OptionHandler.of(
				EPMConfig.sandevistanHudY(),
				SandevistanHudPosition::setY);
		int x = SandevistanHudPosition.screenX(this.width, metrics.previewWidth(), metrics);
		int y = SandevistanHudPosition.screenY(this.height, metrics.cellHeight(), metrics);

		this.addRenderableWidget(new SandevistanHudSetupComponent(
				x,
				y,
				xCoord,
				yCoord,
				horizontalBasisOption,
				verticalBasisOption,
				metrics,
				screen));
	}
}
