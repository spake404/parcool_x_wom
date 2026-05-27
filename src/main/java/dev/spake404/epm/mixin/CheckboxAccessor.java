package dev.spake404.epm.mixin;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface CheckboxAccessor {
	@Accessor("selected")
	void parcoolxwom$setSelected(boolean selected);
}
