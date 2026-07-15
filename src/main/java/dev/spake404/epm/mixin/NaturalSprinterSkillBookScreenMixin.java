package dev.spake404.epm.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.skill.Skill;

@Mixin(value = SkillBookScreen.class, remap = false)
public abstract class NaturalSprinterSkillBookScreenMixin extends Screen {
	private static final String NATURAL_SPRINTER_TRANSLATION_KEY = "skill.wom.natural_sprinter";

	@Shadow
	protected Skill skill;

	protected NaturalSprinterSkillBookScreenMixin(Component title) {
		super(title);
	}

	@Inject(
			method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIFZ)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
					remap = true
			),
			require = 0
	)
	private void parcoolxwom$renderNaturalSprinterFastRunBonus(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, boolean noBackground, CallbackInfo callback) {
		if (this.skill == null || !NATURAL_SPRINTER_TRANSLATION_KEY.equals(this.skill.getTranslationKey())) {
			return;
		}

		int rowLeft = this.width / 2 - 160;
		int rowTop = this.height / 2 + 37;
		TextureAtlasSprite speedIcon = Minecraft.getInstance().getMobEffectTextures().get(MobEffects.MOVEMENT_SPEED);
		guiGraphics.blit(rowLeft + 2, rowTop, 0, 12, 12, speedIcon);

		Component bonusText = Component.translatable("epic_parcool_momentum.skill.natural_sprinter.fast_run_bonus");
		guiGraphics.drawString(
				this.font,
				bonusText,
				rowLeft + 18,
				rowTop + 3,
				0,
				false
		);
	}
}
