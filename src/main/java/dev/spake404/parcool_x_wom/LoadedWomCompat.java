package dev.spake404.parcool_x_wom;

import java.util.stream.Stream;
import java.util.WeakHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;
import reascer.wom.gameasset.WOMAnimations;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import reascer.wom.skill.WOMSkillDataKeys;
import reascer.wom.skill.mover.NaturalSprinterSkill;
import reascer.wom.skill.mover.SpiderTechniquesSkill;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

final class LoadedWomCompat implements WomCompat {
	private static final ResourceLocation NATURAL_SPRINTER_ID = ResourceLocation.fromNamespaceAndPath("wom", "natural_sprinter");
	private static final ResourceLocation SPIDER_TECHNIQUES_ID = ResourceLocation.fromNamespaceAndPath("wom", "spider_techniques");
	private final WeakHashMap<PlayerPatch<?>, SkillContainer> naturalSprinterCache = new WeakHashMap<>();
	private final WeakHashMap<PlayerPatch<?>, SkillContainer> spiderTechniquesCache = new WeakHashMap<>();

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprint() {
		return safe(() -> WOMAnimations.BIPED_SPRINT);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintBarehand() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_BAREHAND);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintStop() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_STOP);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintRightStep() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_RIGHT_STEP);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_LEFT_STEP);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_RIGHT_STEP_BAREHAND);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_LEFT_STEP_BAREHAND);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintSlide() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_SLIDE);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> bipedSprintJump() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_JUMP);
	}

	@Override
	public AssetAccessor<? extends StaticAnimation> wallBackflip() {
		return safe(() -> WOMAnimations.WALL_BACKFLIP);
	}

	@Override
	public boolean isMoonlessCollider(Object collider) {
		return collider != null && collider == WOMWeaponColliders.MOONLESS;
	}

	@Override
	public boolean hasNaturalSprinter(PlayerPatch<?> playerPatch) {
		return findNaturalSprinter(playerPatch) != null;
	}

	@Override
	public void suppressNaturalSprinter(PlayerPatch<?> playerPatch) {
		SkillContainer naturalSprinter = findNaturalSprinter(playerPatch);
		if (naturalSprinter == null) {
			return;
		}

		SkillDataManager dataManager = naturalSprinter.getDataManager();
		setData(dataManager, key(WOMSkillDataKeys.ACTIVE), Boolean.FALSE);
		setData(dataManager, key(WOMSkillDataKeys.BUFFING), Boolean.FALSE);
		setData(dataManager, key(WOMSkillDataKeys.TIMER), Integer.valueOf(0));
	}

	@Override
	public boolean hasSpiderTechniques(PlayerPatch<?> playerPatch) {
		return findSpiderTechniques(playerPatch) != null;
	}

	@Override
	public boolean shouldBlockSpiderTechniquesAttack(PlayerPatch<?> playerPatch) {
		SkillContainer spiderTechniques = findSpiderTechniques(playerPatch);
		if (spiderTechniques == null) {
			return false;
		}

		SkillDataManager dataManager = spiderTechniques.getDataManager();
		Integer wallRunning = getData(dataManager, key(WOMSkillDataKeys.WALL_RUNNING));
		Boolean wallGlide = getData(dataManager, key(WOMSkillDataKeys.WALL_GLIDE));
		Integer timer = getData(dataManager, key(WOMSkillDataKeys.TIMER));
		if (Boolean.TRUE.equals(wallGlide)) {
			return true;
		}

		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		return wallRunning != null
				&& wallRunning.intValue() == -2
				&& timer != null
				&& timer.intValue() > 0
				&& player != null
				&& !player.onGround();
	}

	@Override
	public String describeSpiderTechniquesState(PlayerPatch<?> playerPatch) {
		SkillContainer spiderTechniques = findSpiderTechniques(playerPatch);
		if (spiderTechniques == null) {
			return "spiderTechniques=false";
		}

		SkillDataManager dataManager = spiderTechniques.getDataManager();
		Integer wallRunning = getData(dataManager, key(WOMSkillDataKeys.WALL_RUNNING));
		Boolean wallGlide = getData(dataManager, key(WOMSkillDataKeys.WALL_GLIDE));
		Integer timer = getData(dataManager, key(WOMSkillDataKeys.TIMER));
		return "spiderTechniques=true, wallRunning=" + wallRunning + ", wallGlide=" + wallGlide + ", timer=" + timer;
	}

	private SkillContainer findSpiderTechniques(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getSkillCapability() == null) {
			return null;
		}

		SkillContainer cached = spiderTechniquesCache.get(playerPatch);
		if (isSpiderTechniquesContainer(cached)) {
			return cached;
		}

		try (Stream<SkillContainer> containers = playerPatch.getSkillCapability().listSkillContainers()) {
			SkillContainer found = containers
					.filter(this::isSpiderTechniquesContainer)
					.findFirst()
					.orElse(null);
			if (found != null) {
				spiderTechniquesCache.put(playerPatch, found);
			}
			return found;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private SkillContainer findNaturalSprinter(PlayerPatch<?> playerPatch) {
		if (playerPatch == null || playerPatch.getSkillCapability() == null) {
			return null;
		}

		SkillContainer cached = naturalSprinterCache.get(playerPatch);
		if (isNaturalSprinterContainer(cached)) {
			return cached;
		}

		try (Stream<SkillContainer> containers = playerPatch.getSkillCapability().listSkillContainers()) {
			SkillContainer found = containers
					.filter(this::isNaturalSprinterContainer)
					.findFirst()
					.orElse(null);
			if (found != null) {
				naturalSprinterCache.put(playerPatch, found);
			}
			return found;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private boolean isNaturalSprinterContainer(SkillContainer container) {
		if (container == null) {
			return false;
		}

		Skill skill = container.getSkill();
		return skill instanceof NaturalSprinterSkill || skill != null && NATURAL_SPRINTER_ID.equals(skill.getRegistryName());
	}

	private boolean isSpiderTechniquesContainer(SkillContainer container) {
		if (container == null) {
			return false;
		}

		Skill skill = container.getSkill();
		return skill instanceof SpiderTechniquesSkill || skill != null && SPIDER_TECHNIQUES_ID.equals(skill.getRegistryName());
	}

	private static <T> SkillDataKey<T> key(RegistryObject<SkillDataKey<T>> registryObject) {
		return registryObject == null ? null : registryObject.get();
	}

	private static <T> void setData(SkillDataManager dataManager, SkillDataKey<T> key, T value) {
		if (dataManager == null || key == null) {
			return;
		}

		try {
			dataManager.setDataSync(key, value);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static <T> T getData(SkillDataManager dataManager, SkillDataKey<T> key) {
		if (dataManager == null || key == null) {
			return null;
		}

		try {
			return dataManager.getDataValue(key);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static AssetAccessor<? extends StaticAnimation> safe(AnimationSupplier supplier) {
		try {
			return supplier.get();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	@FunctionalInterface
	private interface AnimationSupplier {
		AssetAccessor<? extends StaticAnimation> get();
	}
}
