package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import yesman.epicfight.client.events.engine.ControlEngine;

@Mixin(value = ControlEngine.class, remap = false)
public interface ControlEngineAccessor {
	@Accessor("holdingFinished")
	void epm$setHoldingFinished(boolean holdingFinished);
}
