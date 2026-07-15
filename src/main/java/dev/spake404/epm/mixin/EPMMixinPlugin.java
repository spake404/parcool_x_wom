package dev.spake404.epm.mixin;

import dev.spake404.epm.EPM;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.loading.LoadingModList;

public final class EPMMixinPlugin implements IMixinConfigPlugin {
	private static final String WOM = "wom";
	private static final String INVINCIBLE = "invincible";
	private static final String NIGHTFALL = "efn";
	private static final String TACZ = "tacz";
	private static final String PARCOOL = "parcool";
	private static final String EPICFIGHTX = "epicfightx";
	private static final String SSRCAMERA_FIXES = "ssrcamerafixes";
	private static final String VC_GLIDERS = "vc_gliders";

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return switch (simpleName(mixinClassName)) {
			case "AquaManeuvreSkillMixin", "NaturalSprinterSkillBookScreenMixin", "NaturalSprinterSkillMixin", "SpiderTechniquesSkillMixin", "VerticalWallRunMixin" -> isLoaded(WOM);
			case "CatLeapMixin", "ChargeJumpMixin", "JumpChargingAnimatorMixin", "ParCoolAnimationAccessor", "ParCoolSettingScreenMixin", "WallJumpMixin" -> isLoaded(PARCOOL);
			case "ComboBasicAttackMixin", "InvincibleJumpConditionMixin" -> isLoaded(INVINCIBLE);
			case "EFNAirborneConditionMixin", "EFNOnGroundConditionMixin" -> isLoaded(NIGHTFALL);
			case "LocalPlayerReloadMixin", "LocalPlayerShootMixin" -> isLoaded(TACZ);
			case "AbstractClientPlayerPatchMixin", "CombatMasteryIIMixin" -> isLoaded(EPICFIGHTX);
			case "SsrWallClimbBodyLockHandlerMixin" -> isLoaded(SSRCAMERA_FIXES) && isLoaded(WOM);
			case "GliderAnimationHandlerMixin", "GliderDataMixin", "GliderToggleMessageMixin", "PlayerGliderLayerMixin" -> isLoaded(VC_GLIDERS);
			default -> true;
		};
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	private static String simpleName(String className) {
		int index = className.lastIndexOf('.');
		return index < 0 ? className : className.substring(index + 1);
	}

	private static boolean isLoaded(String modId) {
		try {
			return LoadingModList.get().getModFileById(modId) != null;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}
}
