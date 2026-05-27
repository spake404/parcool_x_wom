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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SettingActionLimitationScreen.class, remap = false)
public abstract class SettingActionLimitationScreenMixin {
	private static final Component DODGE_DISABLED_MESSAGE = Component.translatable("epic_parcool_momentum.parcool.dodge_disabled");

	@Shadow
	@Final
	private Checkbox[] actionButtons;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void parcoolxwom$disableDodgeButton(Component title, ActionInfo actionInfo, ColorTheme colorTheme, CallbackInfo callback) {
		Checkbox dodgeButton = parcoolxwom$getDodgeButton();
		if (dodgeButton != null) {
			parcoolxwom$disableDodgeButton(dodgeButton);
		}
	}

	@Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true, remap = true)
	private void parcoolxwom$blockDodgeClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
		Checkbox dodgeButton = parcoolxwom$getDodgeButton();
		if (dodgeButton != null && dodgeButton.isMouseOver(mouseX, mouseY)) {
			parcoolxwom$disableDodgeButton(dodgeButton);
			callback.setReturnValue(true);
		}
	}

	@Inject(method = "save", at = @At("HEAD"))
	private void parcoolxwom$keepDodgeDisabled(CallbackInfo callback) {
		Checkbox dodgeButton = parcoolxwom$getDodgeButton();
		if (dodgeButton != null) {
			parcoolxwom$disableDodgeButton(dodgeButton);
		}
	}

	private void parcoolxwom$disableDodgeButton(Checkbox dodgeButton) {
		((CheckboxAccessor) dodgeButton).parcoolxwom$setSelected(false);
		dodgeButton.setMessage(DODGE_DISABLED_MESSAGE);
		dodgeButton.active = false;
	}

	private Checkbox parcoolxwom$getDodgeButton() {
		short index = ActionList.getIndexOf(Dodge.class);
		if (index < 0 || index >= actionButtons.length) {
			return null;
		}
		return actionButtons[index];
	}
}
