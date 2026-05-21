package dev.spake404.parcool_x_wom.mixin;

import com.alrex.parcool.utilities.WorldUtil;
import dev.spake404.parcool_x_wom.ParcoolXWomConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = WorldUtil.class, remap = false)
public abstract class WorldUtilMixin {
	@ModifyConstant(method = "getVaultableStep", constant = @Constant(doubleValue = 0.86D), require = 1)
	private static double parcoolxwom$useConfiguredVaultHeightScale(double original) {
		return ParcoolXWomConfig.vaultHeightScale();
	}
}
