package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.LivingEntity;
import net.venturecraft.gliders.data.GliderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GliderData.class, remap = false)
public abstract class GliderDataMixin {
	@Shadow
	public AnimationState glideAnimation;

	@Shadow
	public AnimationState gliderOpeningAnimation;

	@Unique
	private boolean parcoolxwom$delayGliderAnimations;

	@Inject(method = "glideAndFallLogic", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$stopGliderAnimationsDuringPhantomAscentDelay(LivingEntity entity, CallbackInfo callback) {
		if (EPMClientHooks.shouldHardSuppressGliderTick(entity)) {
			this.parcoolxwom$delayGliderAnimations = true;
			this.glideAnimation.stop();
			this.gliderOpeningAnimation.stop();
			EPMClientHooks.logGliderOpeningDelayProbe(entity, "wallrun_parcool_glider_animation_suppress", true, true);
			callback.cancel();
			return;
		}

		this.parcoolxwom$delayGliderAnimations = EPMClientHooks.shouldDelayGliderOpeningAnimation(entity);
		EPMClientHooks.logGliderOpeningDelayProbe(entity, "glide_logic_head", this.parcoolxwom$delayGliderAnimations, false);
		if (this.parcoolxwom$delayGliderAnimations) {
			this.glideAnimation.stop();
			this.gliderOpeningAnimation.stop();
		}
	}

	@Redirect(
			method = "glideAndFallLogic",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/AnimationState;start(I)V",
					remap = true
			),
			require = 0
	)
	private void parcoolxwom$delayGliderAnimationStateStart(AnimationState animationState, int tick, LivingEntity entity) {
		if (!this.parcoolxwom$delayGliderAnimations) {
			animationState.start(tick);
			if (animationState == this.gliderOpeningAnimation) {
				EPMClientHooks.logGliderOpeningDelayProbe(entity, "opening_animation_start", false, false);
				EPMClientHooks.playDelayedGliderOpeningSound(entity);
			}
		} else if (animationState == this.gliderOpeningAnimation) {
			EPMClientHooks.logGliderOpeningDelayProbe(entity, "opening_animation_suppressed", true, true);
		}
	}
}
