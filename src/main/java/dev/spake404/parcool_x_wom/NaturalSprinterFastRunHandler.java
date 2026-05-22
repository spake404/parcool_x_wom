package dev.spake404.parcool_x_wom;

import java.util.WeakHashMap;

import com.yesman.epicparcool.ParcoolLivingMotions;
import dev.spake404.parcool_x_wom.mixin.AnimatorAccessor;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

final class NaturalSprinterFastRunHandler {
	private static final WeakHashMap<PlayerPatch<?>, Boolean> FAST_RUN_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> MANUAL_FAST_RUN_KEY_CONSUMED = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> NEXT_FAST_RUN_STEP_RIGHT = new WeakHashMap<>();

	private NaturalSprinterFastRunHandler() {
	}

	static void registerFastRunAnimation(InitAnimatorEvent event) {
		if (!ModCompat.isWomLoaded()) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> barehandSprint = WomAnimationRefs.bipedSprintBarehand();
		if (barehandSprint != null) {
			event.getAnimator().addLivingAnimation(ParcoolLivingMotions.FAST_RUN, barehandSprint);
		}
	}

	static void chooseFastRunAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		if (!ModCompat.isWomLoaded()) {
			return;
		}

		if (event.getMotion() != ParcoolLivingMotions.FAST_RUN) {
			clearFastRunState(event.getPlayerPatch());
			return;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		if (!NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			clearFastRunState(playerPatch);
			event.setMotion(LivingMotions.RUN);
			return;
		}

		triggerNaturalSprinterDashOnFastRunStart(playerPatch);
		applyFastRunAnimation(playerPatch);
	}

	private static void clearFastRunState(PlayerPatch<?> playerPatch) {
		FAST_RUN_ACTIVE.remove(playerPatch);
		MANUAL_FAST_RUN_KEY_CONSUMED.remove(playerPatch);
	}

	private static void applyFastRunAnimation(PlayerPatch<?> playerPatch) {
		SprintFamily family = chooseSprintFamily(playerPatch);
		AssetAccessor<? extends StaticAnimation> animation = family.animation();
		if (animation == null) {
			return;
		}

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (family.acceptsCurrentAnimation(currentAnimation)) {
			return;
		}

		putLivingAnimationSilently(playerPatch.getAnimator(), ParcoolLivingMotions.FAST_RUN, animation);
		swapPrimarySprintAnimationPreservingTime(playerPatch, family, animation, currentAnimation);
	}

	private static void triggerNaturalSprinterDashOnFastRunStart(PlayerPatch<?> playerPatch) {
		boolean wasFastRunActive = Boolean.TRUE.equals(FAST_RUN_ACTIVE.put(playerPatch, Boolean.TRUE));
		if (!shouldTriggerNaturalSprinterDash(playerPatch, wasFastRunActive) || !playerPatch.hasStamina(2.0F)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> stepAnimation = nextSprintStepAnimation(playerPatch);
		if (stepAnimation != null) {
			ParcoolXWomClientHooks.queueNaturalSprinterFastRunDash(playerPatch, stepAnimation);
		}
	}

	private static boolean shouldTriggerNaturalSprinterDash(PlayerPatch<?> playerPatch, boolean wasFastRunActive) {
		return ParcoolXWomConfig.autoFastRunDash() ? !wasFastRunActive : shouldTriggerManualFastRunDash(playerPatch);
	}

	private static boolean shouldTriggerManualFastRunDash(PlayerPatch<?> playerPatch) {
		if (!ParcoolXWomClientHooks.isFastRunKeyDown()) {
			MANUAL_FAST_RUN_KEY_CONSUMED.remove(playerPatch);
			return false;
		}

		if (!ParcoolXWomClientHooks.isFastRunKeyRecentlyPressed()) {
			return false;
		}

		return !Boolean.TRUE.equals(MANUAL_FAST_RUN_KEY_CONSUMED.put(playerPatch, Boolean.TRUE));
	}

	private static AssetAccessor<? extends StaticAnimation> nextSprintStepAnimation(PlayerPatch<?> playerPatch) {
		boolean barehand = chooseSprintFamily(playerPatch) == SprintFamily.BAREHAND;
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));
		NEXT_FAST_RUN_STEP_RIGHT.put(playerPatch, Boolean.valueOf(!rightStep));

		if (barehand) {
			return rightStep ? WomAnimationRefs.bipedSprintRightStepBarehand() : WomAnimationRefs.bipedSprintLeftStepBarehand();
		}

		return rightStep ? WomAnimationRefs.bipedSprintRightStep() : WomAnimationRefs.bipedSprintLeftStep();
	}

	private static void putLivingAnimationSilently(Animator animator, LivingMotion motion, AssetAccessor<? extends StaticAnimation> animation) {
		if (animator instanceof AnimatorAccessor accessor) {
			accessor.parcoolxwom$livingAnimations().put(motion, animation);
		}
	}

	@SuppressWarnings("unchecked")
	private static void swapPrimarySprintAnimationPreservingTime(PlayerPatch<?> playerPatch, SprintFamily family, AssetAccessor<? extends StaticAnimation> animation, AssetAccessor<?> currentAnimation) {
		if (!SprintFamily.isAnyPrimary(currentAnimation) || family.isPrimaryAnimation(currentAnimation)) {
			return;
		}

		try {
			AnimationPlayer animationPlayer = playerPatch.getClientAnimator().baseLayer.animationPlayer;
			float previousTime = animationPlayer.getPrevElapsedTime();
			float elapsedTime = animationPlayer.getElapsedTime();
			animationPlayer.setPlayAnimation((AssetAccessor<? extends DynamicAnimation>) animation);
			animationPlayer.setElapsedTime(previousTime, elapsedTime);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static SprintFamily chooseSprintFamily(PlayerPatch<?> playerPatch) {
		CapabilityItem mainHand = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		Object category = mainHand.getWeaponCategory();
		String categoryName = categoryName(category);

		if ("FIST".equals(categoryName)
				|| "DAGGER".equals(categoryName)
				|| "ENDERBLASTER".equals(categoryName)
				|| "HOE".equals(categoryName)
				|| "AXE".equals(categoryName)
				|| "PICKAXE".equals(categoryName)
				|| "SHOVEL".equals(categoryName)
				|| "NOT_WEAPON".equals(categoryName)) {
			return SprintFamily.BAREHAND;
		}

		return WomAnimationRefs.isMoonlessCollider(mainHand.getWeaponCollider()) ? SprintFamily.BAREHAND : SprintFamily.WEAPON;
	}

	private static String categoryName(Object category) {
		return category instanceof Enum<?> enumCategory ? enumCategory.name() : String.valueOf(category);
	}

	private enum SprintFamily {
		BAREHAND {
			@Override
			AssetAccessor<? extends StaticAnimation> animation() {
				return WomAnimationRefs.bipedSprintBarehand();
			}

			@Override
			boolean acceptsCurrentAnimation(AssetAccessor<?> animation) {
				return WomAnimationRefs.isAny(animation,
						WomAnimationRefs.bipedSprintBarehand(),
						WomAnimationRefs.bipedSprintLeftStep(),
						WomAnimationRefs.bipedSprintRightStep(),
						WomAnimationRefs.bipedSprintLeftStepBarehand(),
						WomAnimationRefs.bipedSprintRightStepBarehand(),
						WomAnimationRefs.bipedSprintSlide(),
						WomAnimationRefs.bipedSprintJump(),
						WomAnimationRefs.bipedSprintStop());
			}

			@Override
			boolean isPrimaryAnimation(AssetAccessor<?> animation) {
				return WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedSprintBarehand());
			}
		},
		WEAPON {
			@Override
			AssetAccessor<? extends StaticAnimation> animation() {
				return WomAnimationRefs.bipedSprint();
			}

			@Override
			boolean acceptsCurrentAnimation(AssetAccessor<?> animation) {
				return WomAnimationRefs.isAny(animation,
						WomAnimationRefs.bipedSprint(),
						WomAnimationRefs.bipedSprintLeftStep(),
						WomAnimationRefs.bipedSprintRightStep(),
						WomAnimationRefs.bipedSprintLeftStepBarehand(),
						WomAnimationRefs.bipedSprintRightStepBarehand(),
						WomAnimationRefs.bipedSprintSlide(),
						WomAnimationRefs.bipedSprintJump(),
						WomAnimationRefs.bipedSprintStop());
			}

			@Override
			boolean isPrimaryAnimation(AssetAccessor<?> animation) {
				return WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedSprint());
			}
		};

		abstract AssetAccessor<? extends StaticAnimation> animation();

		abstract boolean acceptsCurrentAnimation(AssetAccessor<?> animation);

		abstract boolean isPrimaryAnimation(AssetAccessor<?> animation);

		static boolean isAnyPrimary(AssetAccessor<?> animation) {
			return BAREHAND.isPrimaryAnimation(animation) || WEAPON.isPrimaryAnimation(animation);
		}
	}
}
