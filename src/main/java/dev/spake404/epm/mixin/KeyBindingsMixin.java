package dev.spake404.epm.mixin;

import com.alrex.parcool.client.input.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = KeyBindings.class, remap = false)
public abstract class KeyBindingsMixin {
	@Redirect(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/RegisterKeyMappingsEvent;register(Lnet/minecraft/client/KeyMapping;)V", remap = true))
	private static void parcoolxwom$skipDodgeKey(RegisterKeyMappingsEvent event, KeyMapping keyMapping) {
		if (keyMapping != KeyBindings.getKeyDodge()) {
			event.register(keyMapping);
		}
	}
}
