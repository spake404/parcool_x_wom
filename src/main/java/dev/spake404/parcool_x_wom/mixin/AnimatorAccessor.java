package dev.spake404.parcool_x_wom.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;

@Mixin(value = Animator.class, remap = false)
public interface AnimatorAccessor {
	@Accessor("livingAnimations")
	Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> parcoolxwom$livingAnimations();
}
