package dev.spake404.epm.mixin;

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
			case "NaturalSprinterSkillMixin", "SpiderTechniquesSkillMixin", "VerticalWallRunMixin" -> isLoaded(WOM);
			case "ComboBasicAttackMixin", "InvincibleJumpConditionMixin" -> isLoaded(INVINCIBLE);
			case "EFNAirborneConditionMixin", "EFNOnGroundConditionMixin" -> isLoaded(NIGHTFALL);
			case "LocalPlayerReloadMixin", "LocalPlayerShootMixin", "MinecraftAttackMixin" -> isLoaded(TACZ);
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
