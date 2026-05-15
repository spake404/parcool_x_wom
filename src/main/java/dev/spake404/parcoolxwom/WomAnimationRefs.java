package dev.spake404.parcoolxwom;

import java.lang.reflect.Field;

import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;

final class WomAnimationRefs {
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprint;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintBarehand;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintRightStep;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintSlide;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintJump;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedSprintStop;
	private static volatile AssetAccessor<? extends StaticAnimation> bipedIdle;
	private static volatile AssetAccessor<? extends StaticAnimation> wallBackflip;
	private static volatile AssetAccessor<? extends StaticAnimation> epicParCoolFastRun;
	private static volatile AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeftStart;
	private static volatile AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRightStart;
	private static volatile AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeft;
	private static volatile AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRight;
	private static volatile Object moonlessCollider;

	private WomAnimationRefs() {
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprint() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprint;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT");
			bipedSprint = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintJump() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintJump;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_JUMP");
			bipedSprintJump = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStep() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintLeftStep;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_LEFT_STEP");
			bipedSprintLeftStep = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintRightStep() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintRightStep;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_RIGHT_STEP");
			bipedSprintRightStep = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintLeftStepBarehand() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintLeftStepBarehand;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_LEFT_STEP_BAREHAND");
			bipedSprintLeftStepBarehand = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintRightStepBarehand() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintRightStepBarehand;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_RIGHT_STEP_BAREHAND");
			bipedSprintRightStepBarehand = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintSlide() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintSlide;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_SLIDE");
			bipedSprintSlide = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintStop() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintStop;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_STOP");
			bipedSprintStop = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedSprintBarehand() {
		AssetAccessor<? extends StaticAnimation> animation = bipedSprintBarehand;
		if (animation == null) {
			animation = readAnimation("BIPED_SPRINT_BAREHAND");
			if (animation == null) {
				animation = epicParCoolFastRun();
			}
			bipedSprintBarehand = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> bipedIdle() {
		AssetAccessor<? extends StaticAnimation> animation = bipedIdle;
		if (animation == null) {
			animation = readAnimation("yesman.epicfight.gameasset.Animations", "BIPED_IDLE");
			bipedIdle = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> wallBackflip() {
		AssetAccessor<? extends StaticAnimation> animation = wallBackflip;
		if (animation == null) {
			animation = readAnimation("WALL_BACKFLIP");
			wallBackflip = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolFastRun() {
		AssetAccessor<? extends StaticAnimation> animation = epicParCoolFastRun;
		if (animation == null) {
			animation = readAnimation("com.yesman.epicparcool.animations.ParCoolAnimations", "BIPED_FAST_RUN");
			epicParCoolFastRun = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeftStart() {
		AssetAccessor<? extends StaticAnimation> animation = epicParCoolWallJumpLeftStart;
		if (animation == null) {
			animation = readAnimation("com.yesman.epicparcool.animations.ParCoolAnimations", "BIPED_WALL_JUMP_LEFT_START");
			epicParCoolWallJumpLeftStart = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRightStart() {
		AssetAccessor<? extends StaticAnimation> animation = epicParCoolWallJumpRightStart;
		if (animation == null) {
			animation = readAnimation("com.yesman.epicparcool.animations.ParCoolAnimations", "BIPED_WALL_JUMP_RIGHT_START");
			epicParCoolWallJumpRightStart = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpLeft() {
		AssetAccessor<? extends StaticAnimation> animation = epicParCoolWallJumpLeft;
		if (animation == null) {
			animation = readAnimation("com.yesman.epicparcool.animations.ParCoolAnimations", "BIPED_WALL_JUMP_LEFT");
			epicParCoolWallJumpLeft = animation;
		}
		return animation;
	}

	static AssetAccessor<? extends StaticAnimation> epicParCoolWallJumpRight() {
		AssetAccessor<? extends StaticAnimation> animation = epicParCoolWallJumpRight;
		if (animation == null) {
			animation = readAnimation("com.yesman.epicparcool.animations.ParCoolAnimations", "BIPED_WALL_JUMP_RIGHT");
			epicParCoolWallJumpRight = animation;
		}
		return animation;
	}

	static boolean isMoonlessCollider(Object collider) {
		Object moonless = moonlessCollider;
		if (moonless == null) {
			moonless = readStaticField("reascer.wom.gameasset.colliders.WOMWeaponColliders", "MOONLESS");
			moonlessCollider = moonless;
		}

		return moonless != null && moonless == collider;
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

	@SuppressWarnings("unchecked")
	private static AssetAccessor<? extends StaticAnimation> readAnimation(String fieldName) {
		return readAnimation("reascer.wom.gameasset.WOMAnimations", fieldName);
	}

	@SuppressWarnings("unchecked")
	private static AssetAccessor<? extends StaticAnimation> readAnimation(String className, String fieldName) {
		try {
			Object value = readStaticField(className, fieldName);
			if (value instanceof AssetAccessor<?> accessor) {
				return (AssetAccessor<? extends StaticAnimation>) accessor;
			}
		} catch (Throwable ignored) {
		}

		return null;
	}

	private static Object readStaticField(String className, String fieldName) {
		try {
			Class<?> ownerClass = Class.forName(className);
			Field field = ownerClass.getField(fieldName);
			return field.get(null);
		} catch (Throwable ignored) {
			return null;
		}
	}
}
