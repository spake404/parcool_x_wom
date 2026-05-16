package dev.spake404.parcool_x_wom;

import com.yesman.epicparcool.animations.ParCoolAnimations;
import reascer.wom.gameasset.WOMAnimations;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.gameasset.Animations;

final class WomAnimationRefs {
	private WomAnimationRefs() {
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprint() {
		return safe(() -> WOMAnimations.BIPED_SPRINT);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintBarehand() {
		AssetAccessor<? extends StaticAnimation> animation = safe(() -> WOMAnimations.BIPED_SPRINT_BAREHAND);
		return animation != null ? animation : epicParCoolFastRun();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintStop() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_STOP);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintRightStep() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_RIGHT_STEP);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_LEFT_STEP);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_RIGHT_STEP_BAREHAND);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_LEFT_STEP_BAREHAND);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintSlide() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_SLIDE);
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintJump() {
		return safe(() -> WOMAnimations.BIPED_SPRINT_JUMP);
	}

	static AssetAccessor<? extends StaticAnimation> bipedIdle() {
		return safe(() -> Animations.BIPED_IDLE);
	}

	static AssetAccessor<? extends StaticAnimation> bipedPhantomAscentForward() {
		return safe(() -> Animations.BIPED_PHANTOM_ASCENT_FORWARD);
	}

	static AssetAccessor<? extends StaticAnimation> bipedPhantomAscentBackward() {
		return safe(() -> Animations.BIPED_PHANTOM_ASCENT_BACKWARD);
	}

	static AssetAccessor<? extends StaticAnimation> wallBackflip() {
		return safe(() -> WOMAnimations.WALL_BACKFLIP);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolFastRun() {
		return safe(() -> ParCoolAnimations.BIPED_FAST_RUN);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolCatLeap() {
		return safe(() -> ParCoolAnimations.BIPED_CAT_LEAP);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolCatLeapPreparation() {
		return safe(() -> ParCoolAnimations.BIPED_CAT_LEAP_PREPARATION);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeftStart() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_LEFT_START);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRightStart() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_RIGHT_START);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeft() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_LEFT);
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRight() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_RIGHT);
	}

	static boolean isMoonlessCollider(Object collider) {
		return collider != null && collider == safeCollider(() -> WOMWeaponColliders.MOONLESS);
	}

	static boolean isAny(AssetAccessor<?> current, AssetAccessor<?>... candidates) {
		if (current == null) {
			return false;
		}

		for (AssetAccessor<?> candidate : candidates) {
			if (candidate != null && current.equals(candidate)) {
				return true;
			}
		}

		return false;
	}

	private static AssetAccessor<? extends StaticAnimation> safe(AnimationSupplier supplier) {
		try {
			return supplier.get();
		} catch (LinkageError ignored) {
			return null;
		}
	}

	private static Collider safeCollider(ColliderSupplier supplier) {
		try {
			return supplier.get();
		} catch (LinkageError ignored) {
			return null;
		}
	}

	@FunctionalInterface
	private interface AnimationSupplier {
		AssetAccessor<? extends StaticAnimation> get();
	}

	@FunctionalInterface
	private interface ColliderSupplier {
		Collider get();
	}
}
