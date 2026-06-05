package dev.spake404.epm.mixin;

import dev.spake404.epm.aqua.AquaManeuvreFastSwimHandler;
import dev.spake404.epm.EPM;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reascer.wom.skill.WOMSkillDataKeys;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.ServerAnimator;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ActionEvent;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = "reascer.wom.skill.mover.AquaManeuvreSkill", remap = false)
public abstract class AquaManeuvreSkillMixin {
	private static final ThreadLocal<Player> parcoolxwom$currentAquaPlayer = new ThreadLocal<>();
	private static final ThreadLocal<SkillContainer> parcoolxwom$currentAquaContainer = new ThreadLocal<>();
	private static final ThreadLocal<Input> parcoolxwom$currentMovementInput = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> parcoolxwom$currentMermaidRequest = new ThreadLocal<>();
	private static final ThreadLocal<AquaActionSnapshot> parcoolxwom$currentActionSnapshot = new ThreadLocal<>();
	private static final ConcurrentHashMap<UUID, Boolean> parcoolxwom$lastRequestByPlayerId = new ConcurrentHashMap<>();

	@Inject(method = "lambda$onInitiate$1", at = @At("HEAD"), require = 0)
	private static void parcoolxwom$enterAquaInputTick(SkillContainer container, MovementInputEvent event, CallbackInfo callback) {
		PlayerPatch<?> playerPatch = event == null ? null : event.getPlayerPatch();
		Entity entity = playerPatch == null ? null : playerPatch.getOriginal();
		if (entity instanceof Player player) {
			parcoolxwom$currentAquaPlayer.set(player);
			parcoolxwom$currentAquaContainer.set(container);
			parcoolxwom$currentMovementInput.set(event == null ? null : event.getMovementInput());
			parcoolxwom$logInputHead(container, event, playerPatch, player);
		} else {
			parcoolxwom$currentAquaPlayer.remove();
			parcoolxwom$currentAquaContainer.remove();
			parcoolxwom$currentMovementInput.remove();
		}
	}

	@Inject(method = "lambda$onInitiate$1", at = @At("RETURN"), require = 0)
	private static void parcoolxwom$exitAquaInputTick(SkillContainer container, MovementInputEvent event, CallbackInfo callback) {
		try {
			parcoolxwom$repairDashTail(container, event);
		} finally {
			parcoolxwom$currentAquaPlayer.remove();
			parcoolxwom$currentAquaContainer.remove();
			parcoolxwom$currentMovementInput.remove();
			parcoolxwom$currentMermaidRequest.remove();
		}
	}

	@Inject(method = "lambda$onInitiate$0", at = @At("HEAD"), require = 0)
	private static void parcoolxwom$enterAquaAction(SkillContainer container, ActionEvent<?> event, CallbackInfo callback) {
		if (!AquaManeuvreFastSwimHandler.diagnosticsEnabled()) {
			return;
		}

		parcoolxwom$currentActionSnapshot.set(new AquaActionSnapshot(
				parcoolxwom$readAquaBooleanText(container, WOMSkillDataKeys.MERMAID_MOVEMENT),
				parcoolxwom$readAquaBooleanText(container, WOMSkillDataKeys.WATER_DASHING)
		));
	}

	@Inject(method = "lambda$onInitiate$0", at = @At("RETURN"), require = 0)
	private static void parcoolxwom$exitAquaAction(SkillContainer container, ActionEvent<?> event, CallbackInfo callback) {
		try {
			if (!AquaManeuvreFastSwimHandler.diagnosticsEnabled()) {
				return;
			}

			PlayerPatch<?> playerPatch = container == null ? null : container.getExecutor();
			Player player = playerPatch == null ? null : playerPatch.getOriginal();
			AquaActionSnapshot snapshot = parcoolxwom$currentActionSnapshot.get();
			String beforeMermaid = snapshot == null ? "unknown" : snapshot.beforeMermaid();
			String beforeWaterDashing = snapshot == null ? "unknown" : snapshot.beforeWaterDashing();
			String afterMermaid = parcoolxwom$readAquaBooleanText(container, WOMSkillDataKeys.MERMAID_MOVEMENT);
			String afterWaterDashing = parcoolxwom$readAquaBooleanText(container, WOMSkillDataKeys.WATER_DASHING);
			Boolean lastRequest = player == null ? null : parcoolxwom$lastRequestByPlayerId.get(player.getUUID());

			if (parcoolxwom$shouldLogAction(player, beforeMermaid, beforeWaterDashing, afterMermaid, afterWaterDashing, event)) {
				EPM.LOGGER.info(
						"[EPM/Aqua][ACTION] tick={} eventAnimation={} before.mermaid={} before.waterDashing={} after.mermaid={} after.waterDashing={} request={} currentAnimation={}",
						parcoolxwom$tick(player),
						parcoolxwom$assetName(event == null ? null : event.getAnimation()),
						beforeMermaid,
						beforeWaterDashing,
						afterMermaid,
						afterWaterDashing,
						lastRequest == null ? "unknown" : lastRequest.toString(),
						parcoolxwom$currentAnimationName(playerPatch)
				);
			}
		} finally {
			parcoolxwom$currentActionSnapshot.remove();
		}
	}

	@Redirect(
			method = "updateContainer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"
			),
			require = 0
	)
	private void parcoolxwom$suppressManualAquaSprintingDuringInaction(Player player, boolean sprinting) {
		if (sprinting && AquaManeuvreFastSwimHandler.shouldSuppressManualAquaSprintDuringInaction(player)) {
			AquaManeuvreFastSwimHandler.logSuppressedManualAquaSprint(player);
			return;
		}

		player.setSprinting(sprinting);
	}

	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z",
					ordinal = 1
			),
			require = 0
	)
	private static boolean parcoolxwom$routeMermaidSprintingCheck(LocalPlayer player) {
		if (AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)) {
			return parcoolxwom$mermaidFastSwimRequested(player);
		}
		return player.isSprinting();
	}

	@Redirect(
			method = "lambda$onInitiate$1",
			at = @At(
					value = "INVOKE",
					target = "Lyesman/epicfight/api/client/input/InputManager;isActionActive(Lyesman/epicfight/api/client/input/action/InputAction;)Z",
					ordinal = 1
			),
			require = 0
	)
	private static boolean parcoolxwom$routeMermaidSprintInput(InputAction action) {
		boolean original = InputManager.isActionActive(action);
		if (action == MinecraftInputAction.SPRINT) {
			Player player = parcoolxwom$currentAquaPlayer.get();
			if (AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)) {
				return parcoolxwom$mermaidFastSwimRequested(player);
			}
		}

		return original;
	}

	private static boolean parcoolxwom$mermaidFastSwimRequested(Player player) {
		Boolean cached = parcoolxwom$currentMermaidRequest.get();
		if (cached != null) {
			return cached.booleanValue();
		}

		SkillContainer container = parcoolxwom$currentAquaContainer.get();
		boolean requested = AquaManeuvreFastSwimHandler.parCoolControlRequestsAquaFastSwim(
				player,
				parcoolxwom$currentMovementInput.get(),
				Boolean.TRUE.equals(parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.MERMAID_MOVEMENT)),
				Boolean.TRUE.equals(parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.WATER_DASHING))
		);
		parcoolxwom$currentMermaidRequest.set(Boolean.valueOf(requested));
		if (player != null) {
			parcoolxwom$lastRequestByPlayerId.put(player.getUUID(), Boolean.valueOf(requested));
		}
		return requested;
	}

	private static void parcoolxwom$repairDashTail(SkillContainer container, MovementInputEvent event) {
		PlayerPatch<?> playerPatch = event == null ? null : event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)) {
			return;
		}

		boolean request = parcoolxwom$currentMermaidRequest.get() != null
				? parcoolxwom$currentMermaidRequest.get().booleanValue()
				: AquaManeuvreFastSwimHandler.peekParCoolControlRequestsAquaFastSwim(
				player,
				event == null ? null : event.getMovementInput(),
				Boolean.TRUE.equals(parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.MERMAID_MOVEMENT)),
				Boolean.TRUE.equals(parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.WATER_DASHING))
		);
		if (!request || playerPatch.getEntityState() == null || playerPatch.getEntityState().inaction()) {
			return;
		}

		Boolean mermaid = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.MERMAID_MOVEMENT);
		Boolean waterDashing = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.WATER_DASHING);
		if (!Boolean.TRUE.equals(mermaid) || !Boolean.TRUE.equals(waterDashing)) {
			return;
		}

		String currentAnimation = parcoolxwom$currentAnimationName(playerPatch);
		if (parcoolxwom$isTritonVortex(currentAnimation)) {
			return;
		}

		boolean clearedWaterDash = parcoolxwom$setAquaBoolean(container, WOMSkillDataKeys.WATER_DASHING, false);
		boolean keptMermaid = parcoolxwom$setAquaBoolean(container, WOMSkillDataKeys.MERMAID_MOVEMENT, true);
		if (AquaManeuvreFastSwimHandler.diagnosticsEnabled()) {
			EPM.LOGGER.info(
					"[EPM/Aqua][DASH_TAIL] tick={} request=true before.mermaid=true before.waterDashing=true after.mermaid={} after.waterDashing={} currentAnimation={} reason=triton-ended",
					parcoolxwom$tick(player),
					Boolean.toString(Boolean.TRUE.equals(parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.MERMAID_MOVEMENT))),
					Boolean.toString(Boolean.TRUE.equals(parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.WATER_DASHING))),
					currentAnimation
			);
			if (!clearedWaterDash || !keptMermaid) {
				EPM.LOGGER.info(
						"[EPM/Aqua][DASH_TAIL_FAIL] tick={} clearedWaterDash={} keptMermaid={} currentAnimation={}",
						parcoolxwom$tick(player),
						Boolean.valueOf(clearedWaterDash),
						Boolean.valueOf(keptMermaid),
						currentAnimation
				);
			}
		}
	}

	private static void parcoolxwom$logInputHead(SkillContainer container, MovementInputEvent event, PlayerPatch<?> playerPatch, Player player) {
		Boolean mermaid = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.MERMAID_MOVEMENT);
		Boolean crawling = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.CRAWLING);
		AquaManeuvreFastSwimHandler.rememberWomAquaState(
				player,
				Boolean.TRUE.equals(mermaid),
				Boolean.TRUE.equals(crawling)
		);
		if (!AquaManeuvreFastSwimHandler.diagnosticsEnabled()) {
			return;
		}

		Boolean waterDashing = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.WATER_DASHING);
		Boolean diving = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.DIVING);
		Boolean jumpKeyUp = parcoolxwom$readAquaBoolean(container, WOMSkillDataKeys.JUMP_KEY_UP);
		Input movementInput = event == null ? null : event.getMovementInput();
		boolean request = AquaManeuvreFastSwimHandler.peekParCoolControlRequestsAquaFastSwim(
				player,
				movementInput,
				Boolean.TRUE.equals(mermaid),
				Boolean.TRUE.equals(waterDashing)
		);
		boolean available = AquaManeuvreFastSwimHandler.isAquaFastSwimAvailable(player);
		parcoolxwom$lastRequestByPlayerId.put(player.getUUID(), Boolean.valueOf(request));

		if (!parcoolxwom$shouldLogInput(player, request, available, mermaid, waterDashing, crawling, diving)) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/Aqua][INPUT_HEAD] tick={} mode={} request={} available={} player.swimming={} player.sprinting={} player.inWater={} player.inWaterOrBubble={} player.underWater={} keyDown={} keyPressed={} movement.up={} movement.down={} movement.left={} movement.right={} movement.jumping={} wom.mermaid={} wom.waterDashing={} wom.crawling={} wom.diving={} wom.jumpKeyUp={} entityState.inaction={} currentAnimation={}",
				parcoolxwom$tick(player),
				AquaManeuvreFastSwimHandler.fastRunControlModeName(),
				request,
				available,
				player.isSwimming(),
				player.isSprinting(),
				player.isInWater(),
				player.isInWaterOrBubble(),
				player.isUnderWater(),
				AquaManeuvreFastSwimHandler.fastRunKeyDownForDiagnostics(),
				AquaManeuvreFastSwimHandler.fastRunKeyPressedForDiagnostics(),
				movementInput != null && movementInput.up,
				movementInput != null && movementInput.down,
				movementInput != null && movementInput.left,
				movementInput != null && movementInput.right,
				movementInput != null && movementInput.jumping,
				parcoolxwom$booleanText(mermaid),
				parcoolxwom$booleanText(waterDashing),
				parcoolxwom$booleanText(crawling),
				parcoolxwom$booleanText(diving),
				parcoolxwom$booleanText(jumpKeyUp),
				playerPatch != null && playerPatch.getEntityState() != null && playerPatch.getEntityState().inaction(),
				parcoolxwom$currentAnimationName(playerPatch)
		);
	}

	private static boolean parcoolxwom$shouldLogInput(Player player, boolean request, boolean available, Boolean mermaid, Boolean waterDashing, Boolean crawling, Boolean diving) {
		return AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)
				&& (request
				|| available
				|| player.isInWaterOrBubble()
				|| Boolean.TRUE.equals(mermaid)
				|| Boolean.TRUE.equals(waterDashing)
				|| Boolean.TRUE.equals(crawling)
				|| Boolean.TRUE.equals(diving));
	}

	private static boolean parcoolxwom$shouldLogAction(Player player, String beforeMermaid, String beforeWaterDashing, String afterMermaid, String afterWaterDashing, ActionEvent<?> event) {
		String eventAnimation = parcoolxwom$assetName(event == null ? null : event.getAnimation());
		return AquaManeuvreFastSwimHandler.shouldLetWomOwnAquaFastSwim(player)
				&& ("true".equals(beforeMermaid)
				|| "true".equals(beforeWaterDashing)
				|| "true".equals(afterMermaid)
				|| "true".equals(afterWaterDashing)
				|| eventAnimation.contains("triton")
				|| eventAnimation.contains("swim")
				|| eventAnimation.contains("mermaid")
				|| eventAnimation.contains("dive"));
	}

	private static Boolean parcoolxwom$readAquaBoolean(SkillContainer container, RegistryObject<SkillDataKey<Boolean>> keyObject) {
		if (container == null || keyObject == null) {
			return null;
		}

		try {
			SkillDataManager dataManager = container.getDataManager();
			SkillDataKey<Boolean> key = keyObject.get();
			return dataManager == null || key == null ? null : dataManager.getDataValue(key);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean parcoolxwom$setAquaBoolean(SkillContainer container, RegistryObject<SkillDataKey<Boolean>> keyObject, boolean value) {
		if (container == null || keyObject == null) {
			return false;
		}

		try {
			SkillDataManager dataManager = container.getDataManager();
			SkillDataKey<Boolean> key = keyObject.get();
			if (dataManager == null || key == null) {
				return false;
			}

			dataManager.setDataSync(key, Boolean.valueOf(value));
			return true;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static String parcoolxwom$readAquaBooleanText(SkillContainer container, RegistryObject<SkillDataKey<Boolean>> keyObject) {
		return parcoolxwom$booleanText(parcoolxwom$readAquaBoolean(container, keyObject));
	}

	private static String parcoolxwom$booleanText(Boolean value) {
		return value == null ? "null" : value.toString();
	}

	private static long parcoolxwom$tick(Player player) {
		return player == null || player.level() == null ? -1L : player.level().getGameTime();
	}

	private static String parcoolxwom$currentAnimationName(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return "null";
		}

		try {
			Animator animator = playerPatch.getAnimator();
			AnimationPlayer animationPlayer = null;
			if (animator instanceof ClientAnimator clientAnimator) {
				animationPlayer = clientAnimator.baseLayer.animationPlayer;
			} else if (animator instanceof ServerAnimator serverAnimator) {
				animationPlayer = serverAnimator.animationPlayer;
			} else if (animator != null) {
				animationPlayer = animator.getPlayerFor(null);
			}

			if (animationPlayer == null) {
				return "null";
			}

			String realAnimation = parcoolxwom$assetName(animationPlayer.getRealAnimation());
			if (!"null".equals(realAnimation)) {
				return realAnimation;
			}

			return parcoolxwom$assetName(animationPlayer.getAnimation());
		} catch (RuntimeException | LinkageError ignored) {
			return "unknown";
		}
	}

	private static String parcoolxwom$assetName(AssetAccessor<?> animation) {
		if (animation == null) {
			return "null";
		}

		try {
			return animation.registryName() == null ? animation.toString() : animation.registryName().toString();
		} catch (RuntimeException | LinkageError ignored) {
			return animation.toString();
		}
	}

	private static boolean parcoolxwom$isTritonVortex(String animationName) {
		return animationName != null && animationName.toLowerCase().contains("triton_vortex");
	}

	private record AquaActionSnapshot(String beforeMermaid, String beforeWaterDashing) {
	}
}
