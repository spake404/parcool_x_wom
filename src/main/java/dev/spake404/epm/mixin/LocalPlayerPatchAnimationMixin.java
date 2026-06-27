package dev.spake404.epm.mixin;

import dev.spake404.epm.EPMClientHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class LocalPlayerPatchAnimationMixin {
	@ModifyVariable(
			method = "playAnimationInClientSide",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 0,
			require = 0
	)
	private AssetAccessor<? extends StaticAnimation> parcoolxwom$replaceDoubleJumpAnimation(
			AssetAccessor<? extends StaticAnimation> animation) {
		Player player = ((LocalPlayerPatch) (Object) this).getOriginal();
		return EPMClientHooks.replaceDoubleJumpAnimation(player, animation);
	}
}
