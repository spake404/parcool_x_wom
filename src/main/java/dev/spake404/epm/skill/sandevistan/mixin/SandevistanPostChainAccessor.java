package dev.spake404.epm.skill.sandevistan.mixin;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostChain.class)
public interface SandevistanPostChainAccessor {
	@Accessor("passes")
	List<PostPass> epm$getPasses();
}
