package yesman.epicfight.world.capabilities;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

public final class EpicFightCapabilities {
	private EpicFightCapabilities() {
	}

	public static <T extends EntityPatch<?>> T getEntityPatch(Entity entity, Class<T> type) {
		return null;
	}
}
