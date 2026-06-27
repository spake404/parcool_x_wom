package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
	@Accessor("accumulatedDX")
	double epm$getAccumulatedDX();

	@Accessor("accumulatedDY")
	double epm$getAccumulatedDY();

	@Accessor("mouseGrabbed")
	boolean epm$isMouseGrabbed();

	@Accessor("ignoreFirstMove")
	boolean epm$isIgnoreFirstMove();

	@Accessor("lastMouseEventTime")
	double epm$getLastMouseEventTime();
}
