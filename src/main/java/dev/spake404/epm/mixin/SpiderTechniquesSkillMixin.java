package dev.spake404.epm.mixin;

import dev.spake404.epm.ModCompat;
import dev.spake404.epm.WomOriginalSpiderWallRunDirectionFix;
import dev.spake404.epm.WomOriginalSpiderWallRunDiagnostics;
import dev.spake404.epm.WomSpiderWallRunHandler;
import dev.spake404.epm.WomSpiderWallRunModeGate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reascer.wom.skill.mover.SpiderTechniquesSkill;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

@Mixin(value = SpiderTechniquesSkill.class, remap = false)
public abstract class SpiderTechniquesSkillMixin {
	@Inject(method = "lambda$onInitiate$1", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$replaceWallRunInput(SkillContainer container, MovementInputEvent event, CallbackInfo ci) {
		if (WomSpiderWallRunHandler.handleMovementInput(event)
				|| WomOriginalSpiderWallRunDirectionFix.beforeOriginalInput(container, event)) {
			ci.cancel();
		}
	}

	@Inject(method = "lambda$onInitiate$1", at = @At("TAIL"), require = 0)
	private void parcoolxwom$stabilizeOriginalWomSideWallRun(SkillContainer container, MovementInputEvent event, CallbackInfo ci) {
		WomOriginalSpiderWallRunDiagnostics.logAfterOriginalInput(container, event);
	}

	@Redirect(method = "lambda$onInitiate$1", at = @At(value = "INVOKE", target = "Lyesman/epicfight/api/client/input/InputManager;isActionActive(Lyesman/epicfight/api/client/input/action/InputAction;)Z"), require = 0)
	private boolean parcoolxwom$disableOriginalSprintWallRunTrigger(InputAction action, SkillContainer container, MovementInputEvent event) {
		if (action == MinecraftInputAction.SPRINT && WomSpiderWallRunModeGate.shouldDisableOriginalWomSprintTrigger(event.getPlayerPatch())) {
			return false;
		}
		return InputManager.isActionActive(action);
	}

	@Redirect(method = "lambda$onInitiate$1", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;yHeadRot:F", opcode = Opcodes.GETFIELD), require = 0)
	private float parcoolxwom$useWallFacingYawForOriginalWallProbe(Player player) {
		return WomOriginalSpiderWallRunDirectionFix.wallProbeYaw(player);
	}

	@Redirect(method = "lambda$onInitiate$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getViewYRot(F)F"), require = 0)
	private float parcoolxwom$useModelYawForEpicArsenalGunWallRun(Player player, float partialTick) {
		float vanillaViewYaw = player.getViewYRot(partialTick);
		float wallRunYaw = WomOriginalSpiderWallRunDirectionFix.wallRunMovementYaw(player, partialTick, vanillaViewYaw);
		if (wallRunYaw != vanillaViewYaw) {
			WomOriginalSpiderWallRunDiagnostics.logViewYawRedirect(player, vanillaViewYaw, wallRunYaw);
			return wallRunYaw;
		}

		if (!ModCompat.isEpicArsenalLoaded() || !isHoldingTaczGun(player)) {
			return vanillaViewYaw;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			float modelYaw = localPlayerPatch.getModelYRot();
			WomOriginalSpiderWallRunDiagnostics.logViewYawRedirect(player, vanillaViewYaw, modelYaw);
			return modelYaw;
		}
		WomOriginalSpiderWallRunDiagnostics.logViewYawRedirect(player, vanillaViewYaw, player.yBodyRot);
		return player.yBodyRot;
	}

	private static boolean isHoldingTaczGun(Player player) {
		return player != null && isTaczItem(player.getMainHandItem());
	}

	private static boolean isTaczItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return itemId != null && ModCompat.TACZ.equals(itemId.getNamespace());
	}
}
