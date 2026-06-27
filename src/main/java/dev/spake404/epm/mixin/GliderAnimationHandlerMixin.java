package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.LivingEntity;
import net.venturecraft.gliders.client.animation.AnimatedPlayer;
import net.venturecraft.gliders.client.animation.AnimationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnimationHandler.class, remap = false)
public abstract class GliderAnimationHandlerMixin {
	@Inject(method = "startGliderAnimation", at = @At("HEAD"), cancellable = true, require = 0)
	private static void parcoolxwom$delayGliderPlayerAnimationDuringPhantomAscent(LivingEntity entity, CallbackInfo originalCallback, CallbackInfo callback) {
		if (EPMClientHooks.shouldDelayGliderOpeningAnimation(entity)) {
			parcoolxwom$clearGliderPlayerAnimation(entity);
			callback.cancel();
		}
	}

	private static void parcoolxwom$clearGliderPlayerAnimation(LivingEntity entity) {
		try {
			if (entity instanceof AnimatedPlayer animatedPlayer) {
				var modifierLayer = animatedPlayer.gliders_getModifierLayer();
				if (modifierLayer != null) {
					modifierLayer.setAnimation(null);
				}
			}
		} catch (RuntimeException | LinkageError ignored) {
		}
	}
}
