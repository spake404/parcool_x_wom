package dev.spake404.epm.mixin;

import com.alrex.parcool.client.gui.ColorTheme;
import com.alrex.parcool.client.gui.SettingActionLimitationScreen;
import com.alrex.parcool.common.action.ActionList;
import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.info.ActionInfo;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SettingActionLimitationScreen.class, remap = false)
public abstract class SettingActionLimitationScreenMixin {
	private static final Component DODGE_NOTICE_MESSAGE = Component.translatable("epic_parcool_momentum.parcool.dodge_notice");

	@Shadow
	@Final
	private Checkbox[] actionButtons;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void epm$addDodgeNotice(Component title, ActionInfo actionInfo, ColorTheme colorTheme, CallbackInfo callback) {
		Checkbox dodgeButton = parcoolxwom$getDodgeButton();
		if (dodgeButton != null) {
			dodgeButton.setMessage(DODGE_NOTICE_MESSAGE);
		}
	}

	private Checkbox parcoolxwom$getDodgeButton() {
		short index = ActionList.getIndexOf(Dodge.class);
		if (index < 0 || index >= actionButtons.length) {
			return null;
		}
		return actionButtons[index];
	}
}
