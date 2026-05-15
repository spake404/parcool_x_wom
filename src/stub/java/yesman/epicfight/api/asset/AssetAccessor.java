package yesman.epicfight.api.asset;

import net.minecraft.resources.ResourceLocation;

public interface AssetAccessor<T> {
	ResourceLocation registryName();

	T get();
}
