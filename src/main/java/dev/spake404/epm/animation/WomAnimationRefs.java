package dev.spake404.epm.animation;

import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.EPM;
import com.yesman.epicparcool.animations.ParCoolAnimations;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;

public final class WomAnimationRefs {
	private WomAnimationRefs() {
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprint() {
		return WomCompatBridge.instance().bipedSprint();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintBarehand() {
		AssetAccessor<? extends StaticAnimation> animation = WomCompatBridge.instance().bipedSprintBarehand();
		return animation != null ? animation : epicParCoolFastRun();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintStop() {
		return WomCompatBridge.instance().bipedSprintStop();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintRightStep() {
		return WomCompatBridge.instance().bipedSprintRightStep();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep() {
		return WomCompatBridge.instance().bipedSprintLeftStep();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand() {
		return WomCompatBridge.instance().bipedSprintRightStepBarehand();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand() {
		return WomCompatBridge.instance().bipedSprintLeftStepBarehand();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintSlide() {
		return WomCompatBridge.instance().bipedSprintSlide();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSprintJump() {
		return WomCompatBridge.instance().bipedSprintJump();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedSwimCrawl() {
		return WomCompatBridge.instance().bipedSwimCrawl();
	}

	public static AssetAccessor<? extends StaticAnimation> bipedIdle() {
		return safe(() -> Animations.BIPED_IDLE);
	}

	public static AssetAccessor<? extends StaticAnimation> bipedPhantomAscentForward() {
		return safe(() -> Animations.BIPED_PHANTOM_ASCENT_FORWARD);
	}

	public static AssetAccessor<? extends StaticAnimation> bipedPhantomAscentBackward() {
		return safe(() -> Animations.BIPED_PHANTOM_ASCENT_BACKWARD);
	}

	public static AssetAccessor<? extends StaticAnimation> wallBackflip() {
		return WomCompatBridge.instance().wallBackflip();
	}

	public static AssetAccessor<? extends StaticAnimation> wallRunning() {
		return WomCompatBridge.instance().wallRunning();
	}

	public static AssetAccessor<? extends StaticAnimation> wallRunLeftSide() {
		return WomCompatBridge.instance().wallRunLeftSide();
	}

	public static AssetAccessor<? extends StaticAnimation> wallRunRightSide() {
		return WomCompatBridge.instance().wallRunRightSide();
	}

	public static AssetAccessor<? extends StaticAnimation> wallGlide() {
		return WomCompatBridge.instance().wallGlide();
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolFastRun() {
		return safe(() -> ParCoolAnimations.BIPED_FAST_RUN);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolCatLeap() {
		return safe(() -> ParCoolAnimations.BIPED_CAT_LEAP);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolCatLeapPreparation() {
		return safe(() -> ParCoolAnimations.BIPED_CAT_LEAP_PREPARATION);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolFlipForward() {
		return safe(() -> ParCoolAnimations.BIPED_FLIP_FOWARD);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolClimbUp() {
		return safe(() -> ParCoolAnimations.BIPED_CLIMB_UP);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolClimbUpNoAction() {
		return safe(() -> ParCoolAnimations.BIPED_CLIMB_UP_NO_ACTION);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeftStart() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_LEFT_START);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRightStart() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_RIGHT_START);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeft() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_LEFT);
	}

	public static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRight() {
		return safe(() -> ParCoolAnimations.BIPED_WALL_JUMP_RIGHT);
	}

	public static boolean isMoonlessCollider(Object collider) {
		return WomCompatBridge.instance().isMoonlessCollider(collider);
	}

	public static boolean isAny(AssetAccessor<?> current, AssetAccessor<?>... candidates) {
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
