package dev.spake404.parcool_x_wom;

import com.yesman.epicparcool.animations.ParCoolAnimations;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;

final class WomAnimationRefs {
	private WomAnimationRefs() {
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprint() {
		return WomCompatBridge.instance().bipedSprint();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintBarehand() {
		AssetAccessor<? extends StaticAnimation> animation = WomCompatBridge.instance().bipedSprintBarehand();
		return animation != null ? animation : epicParCoolFastRun();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintStop() {
		return WomCompatBridge.instance().bipedSprintStop();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintRightStep() {
		return WomCompatBridge.instance().bipedSprintRightStep();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep() {
		return WomCompatBridge.instance().bipedSprintLeftStep();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand() {
		return WomCompatBridge.instance().bipedSprintRightStepBarehand();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand() {
		return WomCompatBridge.instance().bipedSprintLeftStepBarehand();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintSlide() {
		return WomCompatBridge.instance().bipedSprintSlide();
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintJump() {
		return WomCompatBridge.instance().bipedSprintJump();
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
		return WomCompatBridge.instance().wallBackflip();
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
		return WomCompatBridge.instance().isMoonlessCollider(collider);
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

	@FunctionalInterface
	private interface AnimationSupplier {
		AssetAccessor<? extends StaticAnimation> get();
	}

}
