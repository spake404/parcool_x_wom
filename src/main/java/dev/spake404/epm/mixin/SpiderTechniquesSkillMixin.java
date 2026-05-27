package dev.spake404.epm.mixin;

import dev.spake404.epm.ModCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import reascer.wom.skill.mover.SpiderTechniquesSkill;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = SpiderTechniquesSkill.class, remap = false)
public abstract class SpiderTechniquesSkillMixin {
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
