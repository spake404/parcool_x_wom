package dev.spake404.epm.mixin;

import java.util.Locale;

import dev.spake404.epm.skill.sandevistan.SandevistanSkill;

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
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

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
		if (this.skill == null) {
			return;
		}
		if (NATURAL_SPRINTER_TRANSLATION_KEY.equals(this.skill.getTranslationKey())) {
			renderNaturalSprinterFastRunBonus(guiGraphics);
			return;
		}
		if (this.skill instanceof SandevistanSkill sandevistanSkill) {
			renderSandevistanReactionModulation(guiGraphics, sandevistanSkill);
		}
	}

	private void renderNaturalSprinterFastRunBonus(GuiGraphics guiGraphics) {
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

	private void renderSandevistanReactionModulation(
			GuiGraphics guiGraphics,
			SandevistanSkill sandevistanSkill) {
		if (sandevistanSkill.getProfile().reactionSecondsPerMaxStamina() <= 0.0D
				|| Minecraft.getInstance().player == null) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(
				Minecraft.getInstance().player,
				PlayerPatch.class);
		double maxStamina = playerPatch == null ? 0.0D : playerPatch.getMaxStamina();
		double addedSeconds = maxStamina * sandevistanSkill.getProfile().reactionSecondsPerMaxStamina();
		int textLeft = this.width / 2 - 152;
		int textTop = this.height / 2 + 58;
		int lineSpacing = this.font.lineHeight + 4;

		drawReactionLine(
				guiGraphics,
				Component.translatable("skill.epic_parcool_momentum.sandevistan.reaction_book.title"),
				textLeft,
				textTop);
		drawReactionLine(
				guiGraphics,
				Component.translatable(
						"skill.epic_parcool_momentum.sandevistan.reaction_book.stamina",
						formatNumber(maxStamina)),
				textLeft,
				textTop + lineSpacing);
		drawReactionLine(
				guiGraphics,
				Component.translatable(
						"skill.epic_parcool_momentum.sandevistan.reaction_book.duration",
						formatNumber(addedSeconds)),
				textLeft,
				textTop + lineSpacing * 2);
	}

	private void drawReactionLine(
			GuiGraphics guiGraphics,
			Component text,
			int x,
			int y) {
		guiGraphics.drawString(this.font, text, x, y, 0, false);
	}

	private static String formatNumber(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.0001D) {
			return String.format(Locale.ROOT, "%.0f", value);
		}
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
