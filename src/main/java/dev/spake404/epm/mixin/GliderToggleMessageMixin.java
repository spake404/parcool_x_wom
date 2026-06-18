package dev.spake404.epm.mixin;

import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.WomSpiderWallRunHandler;
import dev.spake404.epm.WomSpiderWallSlideHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.threetag.palladiumcore.network.MessageC2S;
import net.venturecraft.gliders.network.MessageToggleGlide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MessageC2S.class, remap = false)
public abstract class GliderToggleMessageMixin {
	@Inject(method = "send", at = @At("HEAD"), cancellable = true, require = 0)
	private void parcoolxwom$blockGliderToggleDuringPhantomAscentPriority(CallbackInfo callback) {
		if (!((Object) this instanceof MessageToggleGlide)) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft == null ? null : minecraft.player;
		if (EPMClientHooks.consumeGliderDisableRequest(player)) {
			return;
		}
		if (EPMClientHooks.consumeGliderReplayRequest(player)) {
			return;
		}
		if (EPMClientHooks.shouldDeferGliderToggleForInputArbitration(player)) {
			callback.cancel();
			return;
		}
		if (EPMClientHooks.shouldHardBlockGliderToggleForWallRunToParCoolWallJump(player)) {
			callback.cancel();
			return;
		}
		if (EPMClientHooks.shouldBlockGliderToggleForWomBackflip(player)) {
			callback.cancel();
			return;
		}

		boolean wallRunActive = player != null && (WomSpiderWallRunHandler.isWallRunActive(player)
				|| WomSpiderWallSlideHandler.shouldOwnWallState(player)
				|| EPMClientHooks.isWallMovementAnimationActiveForGlider(player));
		if (wallRunActive) {
			EPMClientHooks.logGliderOpeningDelayProbe(player, "toggle_block_wall_movement", false, true);
			callback.cancel();
			return;
		}

		if (EPMClientHooks.shouldBlockGliderToggleForPhantomAscent(player)) {
			callback.cancel();
			return;
		}

	}
}
