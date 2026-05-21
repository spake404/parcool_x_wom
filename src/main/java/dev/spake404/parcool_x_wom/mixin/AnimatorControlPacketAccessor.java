package dev.spake404.parcool_x_wom.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.network.common.AnimatorControlPacket;

@Mixin(value = AnimatorControlPacket.class, remap = false)
public interface AnimatorControlPacketAccessor {
	@Accessor("action")
	AnimatorControlPacket.Action parcoolxwom$action();

	@Accessor("animationId")
	int parcoolxwom$animationId();

	@Accessor("transitionTimeModifier")
	float parcoolxwom$transitionTimeModifier();

	@Accessor("pause")
	boolean parcoolxwom$pause();
}
