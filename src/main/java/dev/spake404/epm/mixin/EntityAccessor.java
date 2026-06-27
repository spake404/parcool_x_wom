package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
	@Accessor("DATA_POSE")
	static EntityDataAccessor<Pose> epm$getDataPose() {
		throw new AssertionError();
	}
}
