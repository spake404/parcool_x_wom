package dev.spake404.epm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.network.common.AnimatorControlPacket;
import yesman.epicfight.network.server.SPAnimatorControl;

@Mixin(value = SPAnimatorControl.class, remap = false)
public interface SPAnimatorControlAccessor {
	@Accessor("entityId")
	int parcoolxwom$entityId();

	@Accessor("layer")
	AnimatorControlPacket.Layer parcoolxwom$layer();

	@Accessor("priority")
	AnimatorControlPacket.Priority parcoolxwom$priority();
}
