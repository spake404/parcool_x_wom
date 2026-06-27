package dev.spake404.epm.mixin;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import com.alrex.parcool.utilities.WorldUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = WorldUtil.class, remap = false)
public abstract class WorldUtilMixin {
	@ModifyConstant(method = "getVaultableStep", constant = @Constant(doubleValue = 0.86D), require = 1)
	private static double parcoolxwom$useConfiguredVaultHeightScale(double original) {
		return EPMConfig.vaultHeightScale();
	}

	@ModifyConstant(method = "getVaultableStep", constant = @Constant(doubleValue = 1.8D), require = 4)
	private static double parcoolxwom$shortenVaultableStepTopClearanceScan(double original) {
		return 1.5D;
	}
}
