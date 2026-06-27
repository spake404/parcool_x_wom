package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.common.capability.Animation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Animation.class, remap = false)
public interface ParCoolAnimationAccessor {
	@Accessor("animator")
	Animator epm$getAnimator();
}
