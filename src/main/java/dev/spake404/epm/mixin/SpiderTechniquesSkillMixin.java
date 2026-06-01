package dev.spake404.epm.mixin;

import dev.spake404.epm.ModCompat;
import dev.spake404.epm.WomSpiderWallRunHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
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
		if (WomSpiderWallRunHandler.handleMovementInput(event)) {
			ci.cancel();
		}
	}

	@Redirect(method = "lambda$onInitiate$1", at = @At(value = "INVOKE", target = "Lyesman/epicfight/api/client/input/InputManager;isActionActive(Lyesman/epicfight/api/client/input/action/InputAction;)Z"), require = 0)
	private boolean parcoolxwom$disableOriginalSprintWallRunTrigger(InputAction action, SkillContainer container, MovementInputEvent event) {
		if (action == MinecraftInputAction.SPRINT && WomSpiderWallRunHandler.shouldDisableOriginalWomSprintTrigger(event.getPlayerPatch())) {
			return false;
		}
		return InputManager.isActionActive(action);
	}

	@Redirect(method = "lambda$onInitiate$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getViewYRot(F)F"), require = 0)
	private float parcoolxwom$useModelYawForEpicArsenalGunWallRun(Player player, float partialTick) {
		if (!ModCompat.isEpicArsenalLoaded() || !isHoldingTaczGun(player)) {
			return player.getViewYRot(partialTick);
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			return localPlayerPatch.getModelYRot();
		}
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
