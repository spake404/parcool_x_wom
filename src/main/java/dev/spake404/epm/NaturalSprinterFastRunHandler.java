package dev.spake404.epm;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.WeakHashMap;

import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Parkourability;
import com.yesman.epicparcool.ParcoolLivingMotions;
import dev.spake404.epm.mixin.AnimatorAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

final class NaturalSprinterFastRunHandler {
	private static final WeakHashMap<PlayerPatch<?>, Boolean> FAST_RUN_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> MANUAL_FAST_RUN_KEY_CONSUMED = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, Boolean> NEXT_FAST_RUN_STEP_RIGHT = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> MANUAL_FAST_RUN_STEP_KEY_HELD = new WeakHashMap<>();
	private static TaczGunTypeResolver taczGunTypeResolver;
	private static TaczReloadStateResolver taczReloadStateResolver;

	private NaturalSprinterFastRunHandler() {
	}

	static void registerFastRunAnimation(InitAnimatorEvent event) {
		if (!ModCompat.isWomLoaded() || !EPMConfig.naturalSprinterAnimations()) {
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
		if (!NaturalSprinterState.hasNaturalSprinter(playerPatch) || !EPMConfig.naturalSprinterAnimations()) {
			clearFastRunState(playerPatch);
			event.setMotion(LivingMotions.RUN);
			return;
		}

		triggerNaturalSprinterDashOnFastRunStart(playerPatch);
		applyFastRunAnimation(playerPatch);
	}

	static boolean tryManualFastRunStep(Player player) {
		if (!ModCompat.isWomLoaded()
				|| !EPMConfig.naturalSprinterAnimations()
				|| !EPMConfig.naturalSprinterManualStep()
				|| player == null
				|| !player.isLocalPlayer()
				|| !canManualFastRunStep(player)
				|| isTaczReloading(player)
				|| !isParCoolFastRunDoing(player)) {
			return false;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !playerPatch.isLogicalClient() || !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return false;
		}

		AssetAccessor<? extends StaticAnimation> stepAnimation = currentSprintStepAnimation(playerPatch);
		if (stepAnimation == null || !NaturalSprinterState.consumeStep(playerPatch)) {
			return false;
		}

		advanceSprintStep(playerPatch);
		EPMClientHooks.queueNaturalSprinterFastRunDash(playerPatch, stepAnimation);
		return true;
	}

	static void tickManualFastRunStepKey(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		if (!ModCompat.isWomLoaded()
				|| !EPMConfig.naturalSprinterAnimations()
				|| !EPMConfig.naturalSprinterManualStep()) {
			MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player);
			return;
		}

		if (EPMKeyMappings.isNaturalSprinterStepDown()) {
			if (!MANUAL_FAST_RUN_STEP_KEY_HELD.containsKey(player)) {
				MANUAL_FAST_RUN_STEP_KEY_HELD.put(player, Boolean.TRUE);
			}
			return;
		}

		if (Boolean.TRUE.equals(MANUAL_FAST_RUN_STEP_KEY_HELD.remove(player))) {
			tryManualFastRunStep(player);
		}
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
		if (!EPMConfig.naturalSprinterAnimations()) {
			clearFastRunState(playerPatch);
			return;
		}

		boolean wasFastRunActive = Boolean.TRUE.equals(FAST_RUN_ACTIVE.put(playerPatch, Boolean.TRUE));
		if (!shouldTriggerNaturalSprinterDash(playerPatch, wasFastRunActive) || !playerPatch.hasStamina(2.0F)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> stepAnimation = nextSprintStepAnimation(playerPatch);
		if (stepAnimation != null) {
			EPMClientHooks.queueNaturalSprinterFastRunDash(playerPatch, stepAnimation);
		}
	}

	private static boolean shouldTriggerNaturalSprinterDash(PlayerPatch<?> playerPatch, boolean wasFastRunActive) {
		if (!EPMConfig.naturalSprinterAnimations()) {
			return false;
		}

		if (!EPMConfig.autoFastRunDash()) {
			return shouldTriggerManualFastRunDash(playerPatch);
		}

		return !wasFastRunActive && !EPMClientHooks.shouldSuppressAutoFastRunDashForTacz(playerPatch);
	}

	private static boolean shouldTriggerManualFastRunDash(PlayerPatch<?> playerPatch) {
		if (!EPMClientHooks.isFastRunKeyDown()) {
			MANUAL_FAST_RUN_KEY_CONSUMED.remove(playerPatch);
			return false;
		}

		if (!EPMClientHooks.isFastRunKeyRecentlyPressed()) {
			return false;
		}

		return !Boolean.TRUE.equals(MANUAL_FAST_RUN_KEY_CONSUMED.put(playerPatch, Boolean.TRUE));
	}

	private static boolean canManualFastRunStep(Player player) {
		return player.onGround()
				&& !player.isSpectator()
				&& !player.isDeadOrDying()
				&& !player.isShiftKeyDown()
				&& !player.isInWaterOrBubble()
				&& !player.isFallFlying()
				&& player.getVehicle() == null;
	}

	private static boolean isParCoolFastRunDoing(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static AssetAccessor<? extends StaticAnimation> nextSprintStepAnimation(PlayerPatch<?> playerPatch) {
		AssetAccessor<? extends StaticAnimation> animation = currentSprintStepAnimation(playerPatch);
		advanceSprintStep(playerPatch);
		return animation;
	}

	private static AssetAccessor<? extends StaticAnimation> currentSprintStepAnimation(PlayerPatch<?> playerPatch) {
		boolean barehand = chooseSprintFamily(playerPatch) == SprintFamily.BAREHAND;
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));

		if (barehand) {
			return rightStep ? WomAnimationRefs.bipedSprintRightStepBarehand() : WomAnimationRefs.bipedSprintLeftStepBarehand();
		}

		return rightStep ? WomAnimationRefs.bipedSprintRightStep() : WomAnimationRefs.bipedSprintLeftStep();
	}

	private static void advanceSprintStep(PlayerPatch<?> playerPatch) {
		boolean rightStep = Boolean.TRUE.equals(NEXT_FAST_RUN_STEP_RIGHT.get(playerPatch));
		NEXT_FAST_RUN_STEP_RIGHT.put(playerPatch, Boolean.valueOf(!rightStep));
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
		SprintFamily taczFamily = chooseTaczSprintFamily(playerPatch);
		if (taczFamily != null) {
			return taczFamily;
		}

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

	private static SprintFamily chooseTaczSprintFamily(PlayerPatch<?> playerPatch) {
		if (!ModCompat.isTaczLoaded() || playerPatch == null || playerPatch.getOriginal() == null) {
			return null;
		}

		String gunType = taczGunType(playerPatch.getOriginal().getMainHandItem());
		if (gunType == null) {
			return null;
		}

		return EPMConfig.isTaczBarehandSprintType(gunType) ? SprintFamily.BAREHAND : SprintFamily.WEAPON;
	}

	private static String taczGunType(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}

		try {
			if (taczGunTypeResolver == null) {
				taczGunTypeResolver = new TaczGunTypeResolver();
			}
			return taczGunTypeResolver.type(stack);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isTaczReloading(Player player) {
		if (!ModCompat.isTaczLoaded() || player == null) {
			return false;
		}

		try {
			if (taczReloadStateResolver == null) {
				taczReloadStateResolver = new TaczReloadStateResolver();
			}
			return taczReloadStateResolver.isReloading(player);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static String categoryName(Object category) {
		return category instanceof Enum<?> enumCategory ? enumCategory.name() : String.valueOf(category);
	}

	private static final class TaczGunTypeResolver {
		private final Class<?> gunClass;
		private final Method getGunId;
		private final Method getCommonGunIndex;
		private final Method getType;

		private TaczGunTypeResolver() throws ClassNotFoundException, NoSuchMethodException {
			gunClass = Class.forName("com.tacz.guns.api.item.IGun");
			getGunId = gunClass.getMethod("getGunId", ItemStack.class);
			getCommonGunIndex = Class.forName("com.tacz.guns.api.TimelessAPI").getMethod("getCommonGunIndex", ResourceLocation.class);
			getType = Class.forName("com.tacz.guns.resource.index.CommonGunIndex").getMethod("getType");
		}

		private String type(ItemStack stack) throws ReflectiveOperationException {
			Object item = stack.getItem();
			if (!gunClass.isInstance(item)) {
				return null;
			}

			Object gunId = getGunId.invoke(item, stack);
			if (!(gunId instanceof ResourceLocation resourceLocation)) {
				return null;
			}

			Object optional = getCommonGunIndex.invoke(null, resourceLocation);
			if (!(optional instanceof Optional<?> gunIndexOptional)) {
				return null;
			}

			Object gunIndex = gunIndexOptional.orElse(null);
			if (gunIndex == null) {
				return null;
			}

			Object type = getType.invoke(gunIndex);
			return type instanceof String typeName ? typeName : null;
		}
	}

	private static final class TaczReloadStateResolver {
		private final Method fromLivingEntity;
		private final Method getSynReloadState;
		private final Method getStateType;
		private final Method isReloading;

		private TaczReloadStateResolver() throws ClassNotFoundException, NoSuchMethodException {
			Class<?> gunOperatorClass = Class.forName("com.tacz.guns.api.entity.IGunOperator");
			Class<?> reloadStateClass = Class.forName("com.tacz.guns.api.entity.ReloadState");
			Class<?> reloadStateTypeClass = Class.forName("com.tacz.guns.api.entity.ReloadState$StateType");
			fromLivingEntity = gunOperatorClass.getMethod("fromLivingEntity", LivingEntity.class);
			getSynReloadState = gunOperatorClass.getMethod("getSynReloadState");
			getStateType = reloadStateClass.getMethod("getStateType");
			isReloading = reloadStateTypeClass.getMethod("isReloading");
		}

		private boolean isReloading(Player player) throws ReflectiveOperationException {
			Object operator = fromLivingEntity.invoke(null, player);
			if (operator == null) {
				return false;
			}

			Object reloadState = getSynReloadState.invoke(operator);
			if (reloadState == null) {
				return false;
			}

			Object stateType = getStateType.invoke(reloadState);
			return stateType != null && Boolean.TRUE.equals(isReloading.invoke(stateType));
		}
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
