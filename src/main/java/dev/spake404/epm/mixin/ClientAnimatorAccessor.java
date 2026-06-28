package dev.spake404.epm.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;

@Mixin(value = ClientAnimator.class, remap = false)
public interface ClientAnimatorAccessor {
	@Accessor("defaultLivingAnimations")
	Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> parcoolxwom$defaultLivingAnimations();
}
