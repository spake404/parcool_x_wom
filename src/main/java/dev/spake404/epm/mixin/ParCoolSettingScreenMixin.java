package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.gui.EpmConfigSettingScreen;
import com.alrex.parcool.client.gui.ColorTheme;
import com.alrex.parcool.client.gui.ParCoolSettingScreen;
import com.alrex.parcool.common.info.ActionInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Supplier;

@Mixin(value = ParCoolSettingScreen.class, remap = false)
public abstract class ParCoolSettingScreenMixin {

	@Inject(method = "<init>", at = @At("TAIL"), remap = false)
	private void epm$addEpmConfigTab(Component title, ActionInfo info, ColorTheme theme, CallbackInfo ci) {
		try {
			Field screenListField = ParCoolSettingScreen.class.getDeclaredField("screenList");
			screenListField.setAccessible(true);

			Object[] oldList = (Object[]) screenListField.get(this);
			Class<?> screenSetClass = oldList[0].getClass();

			Object[] newList = (Object[]) Array.newInstance(screenSetClass, oldList.length + 1);
			System.arraycopy(oldList, 0, newList, 0, oldList.length);

			Constructor<?> ctor = screenSetClass.getDeclaredConstructors()[0];
			ctor.setAccessible(true);

			Supplier<ParCoolSettingScreen> supplier = () -> new EpmConfigSettingScreen(title, info, theme);
			Object epmTab = ctor.newInstance(Component.literal("EPM"), supplier);
			newList[newList.length - 1] = epmTab;

			screenListField.set(this, newList);
			EPM.LOGGER.info("[EPM] Added EPM config tab to ParCool settings");
		} catch (Exception e) {
			EPM.LOGGER.error("[EPM] Failed to add EPM config tab to ParCool settings", e);
		}
	}
}
