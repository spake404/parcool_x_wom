package dev.spake404.epm;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EPMEvents {
	private EPMEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void primeEpicParCoolFastRun(InitAnimatorEvent event) {
		NaturalSprinterFastRunHandler.registerFastRunAnimation(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void chooseFastRunAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		NaturalSprinterFastRunHandler.chooseFastRunAnimation(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void handleEpicParCoolCatLeap(ParCoolActionEvent.StartEvent event) {
		if (!(event.getAction() instanceof CatLeap)) {
			return;
		}

		EPMClientHooks.markCatLeapForPhantomAscent(event.getPlayer());
		if (!event.getPlayer().isLocalPlayer() || !ModCompat.isWomLoaded() || !EPMConfig.naturalSprinterAnimations()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
		if (playerPatch != null && playerPatch.isLogicalClient() && playerPatch.isEpicFightMode() && NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			EPMClientHooks.reduceNaturalSprinterCatLeapMotion(event.getPlayer());
			EPMClientHooks.startNaturalSprinterCatLeap(event.getPlayer());

			AssetAccessor<? extends StaticAnimation> sprintJump = WomAnimationRefs.bipedSprintJump();
			if (sprintJump != null) {
				playerPatch.playAnimationSynchronized(sprintJump, 0.0F);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void markEpicParCoolWallJumpForPhantomAscent(ParCoolActionEvent.StartEvent event) {
		if (!(event.getAction() instanceof WallJump)) {
			return;
		}

		if (EPMConfig.wallJumpPrimesPhantomAscent()) {
			EPMClientHooks.markWallJumpForPhantomAscent(event.getPlayer());
		}

		MomentumAirAttackWindowState.markWallJump(event.getPlayer());
		EPMClientHooks.markWallJumpForTaczShootCancel(event.getPlayer());
		EPMClientHooks.markAutoSprintAfterWallJump(event.getPlayer());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void suppressNaturalSprinterStart(ParCoolActionEvent.Start.Post event) {
		if (event.getAction() instanceof FastRun) {
			syncAndSuppressNaturalSprinter(event);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void suppressNaturalSprinterTick(ParCoolActionEvent.Tick.Post event) {
		if (event.getAction() instanceof FastRun) {
			syncAndSuppressNaturalSprinter(event);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void suppressNaturalSprinterLegacyStart(ParCoolActionEvent.StartEvent event) {
		if (event.getAction() instanceof FastRun) {
			syncAndSuppressNaturalSprinter(event);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void rememberFastRunBeforeVault(ParCoolActionEvent.Start.Pre event) {
		if (event.getPlayer().isLocalPlayer() && event.getAction() instanceof Vault) {
			EPMClientHooks.markVaultStartedFromFastRun(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void clearFastRunHoldAfterVault(ParCoolActionEvent.Finish.Post event) {
		if (event.getPlayer().isLocalPlayer() && event.getAction() instanceof Vault) {
			EPMClientHooks.clearVaultFastRunHold(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void allowClimbUpFromEpicParCoolClingMove(ParCoolActionEvent.TryToStartEvent event) {
		if (event.getAction() instanceof ClimbUp && EPMClientHooks.shouldAllowClimbUpFromEpicParCoolClingMove(event.getPlayer())) {
			EPM.LOGGER.info("[ClingToCliffDebug] allowClimbUpFromEpicParCoolClingMove tick={}", Integer.valueOf(event.getPlayer().tickCount));
			EPMClientHooks.markClimbUpFromEpicParCoolClingMove(event.getPlayer());
			event.setCanceled(false);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void logClingToCliffStart(ParCoolActionEvent.Start.Pre event) {
		if (event.getAction() instanceof ClingToCliff || event.getAction() instanceof ClimbUp) {
			ClingToCliffDebug.logActionEvent("start_pre", event.getPlayer(), event.getAction());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void logClingToCliffStarted(ParCoolActionEvent.Start.Post event) {
		if (event.getAction() instanceof ClingToCliff || event.getAction() instanceof ClimbUp) {
			if (event.getAction() instanceof ClimbUp) {
				EPMClientHooks.compensateEpicParCoolClimbUp(event.getPlayer());
			}
			ClingToCliffDebug.logActionEvent("start_post", event.getPlayer(), event.getAction());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void logClingToCliffFinish(ParCoolActionEvent.Finish.Pre event) {
		if (event.getAction() instanceof ClingToCliff || event.getAction() instanceof ClimbUp) {
			ClingToCliffDebug.logActionEvent("finish_pre", event.getPlayer(), event.getAction());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void logClingToCliffFinished(ParCoolActionEvent.Finish.Post event) {
		if (event.getAction() instanceof ClingToCliff || event.getAction() instanceof ClimbUp) {
			ClingToCliffDebug.logActionEvent("finish_post", event.getPlayer(), event.getAction());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void tickLocalPlayer(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		if (MomentumAirAttackWindowState.hasTrackedState()) {
			MomentumAirAttackWindowState.tick(event);
		}

		if (event.player.level().isClientSide()) {
			EPMClientHooks.tickLocalPlayer(event);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void protectWallJumpAirAttackFall(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.IS_FALL)) {
			return;
		}

		if (MomentumAirAttackWindowState.shouldProtectWallJumpFall(player, event.getAmount())) {
			event.setCanceled(true);
		}
	}

	private static void syncAndSuppressNaturalSprinter(ParCoolActionEvent event) {
		if (!ModCompat.isWomLoaded()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
		if (playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			NaturalSprinterState.suppress(playerPatch);
		}
	}
}
