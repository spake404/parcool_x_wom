package dev.spake404.epm.skill.sandevistan.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface SandevistanEntityRenderDispatcherAccessor {
	@Accessor("shouldRenderShadow")
	boolean epm$getRenderShadow();
}
