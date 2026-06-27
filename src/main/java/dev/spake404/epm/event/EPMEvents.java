package dev.spake404.epm.event;

import dev.spake404.epm.animation.WomAnimationRefs;
import dev.spake404.epm.aqua.AquaManeuvreFastSwimHandler;
import dev.spake404.epm.climb.ClingToCliffDebug;
import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.demolition.DemolitionLeapAirJumpHandler;
import dev.spake404.epm.demolition.DemolitionLeapCatJumpHandler;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.naturalsprinter.NaturalSprinterDodgeStepArbiter;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunAnimationOverrides;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunHandler;
import dev.spake404.epm.naturalsprinter.NaturalSprinterState;
import dev.spake404.epm.network.EPMNetwork;
import dev.spake404.epm.phantom.MomentumAirAttackWindowState;
import dev.spake404.epm.vault.VaultCameraAnimationSmoother;
import dev.spake404.epm.wom.spider.WomSpiderWallRunHandler;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
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
		AquaManeuvreFastSwimHandler.registerFastSwimAnimation(event);
		DemolitionLeapCatJumpHandler.registerChargeJumpAnimation(event);
		EPMClientHooks.registerDoubleJumpFallAnimation(event);
	}

	@SubscribeEvent
	public static void addReloadListeners(AddReloadListenerEvent event) {
		event.addListener(NaturalSprinterFastRunAnimationOverrides.reloadListener());
	}

	@SubscribeEvent
	public static void syncDatapackData(OnDatapackSyncEvent event) {
		for (net.minecraft.server.level.ServerPlayer player : event.getPlayers()) {
			EPMNetwork.sendNaturalSprinterFastRunAnimationOverrides(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void chooseFastRunAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		NaturalSprinterFastRunHandler.chooseFastRunAnimation(event);
		AquaManeuvreFastSwimHandler.chooseFastSwimAnimation(event);
		DemolitionLeapCatJumpHandler.chooseChargeJumpAnimation(event);
		EPMClientHooks.chooseDoubleJumpFallAnimation(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void handleEpicParCoolCatLeap(ParCoolActionEvent.StartEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

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
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (!(event.getAction() instanceof WallJump)) {
			return;
		}

		EPMClientHooks.markParCoolWallJumpHandoffStarted(event.getPlayer());
		if (EPMConfig.wallJumpPrimesPhantomAscent()) {
			EPMClientHooks.markWallJumpForPhantomAscent(event.getPlayer());
		}

		MomentumAirAttackWindowState.markWallJump(event.getPlayer());
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
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getPlayer().isLocalPlayer() && event.getAction() instanceof Vault) {
			EPMClientHooks.markVaultStartedFromFastRun(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void rememberVaultCameraAnimation(ParCoolActionEvent.Start.Post event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getPlayer().isLocalPlayer() && event.getAction() instanceof Vault vault) {
			VaultCameraAnimationSmoother.markStart(event.getPlayer(), vault);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void clearFastRunHoldAfterVault(ParCoolActionEvent.Finish.Post event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getPlayer().isLocalPlayer() && event.getAction() instanceof Vault) {
			EPMClientHooks.clearVaultFastRunHold(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void arbitrateDodgeStepConflict(ParCoolActionEvent.TryToStartEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getAction() instanceof Dodge && NaturalSprinterDodgeStepArbiter.shouldCancelDodgeStart(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void arbitrateDodgeStepConflictModern(ParCoolActionEvent.TryToStart event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getAction() instanceof Dodge && NaturalSprinterDodgeStepArbiter.shouldCancelDodgeStart(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void rememberDodgeStartedForStepConflict(ParCoolActionEvent.Start.Post event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getAction() instanceof Dodge) {
			NaturalSprinterDodgeStepArbiter.markDodgeStarted(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void blockClimbUpDuringSpiderWallRun(ParCoolActionEvent.TryToStartEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (!(event.getAction() instanceof ClimbUp)) {
			return;
		}

		if (WomSpiderWallRunHandler.shouldBlockParCoolClimbUp(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void allowClimbUpFromEpicParCoolClingMove(ParCoolActionEvent.TryToStartEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (event.getAction() instanceof ClimbUp && WomSpiderWallRunHandler.shouldBlockParCoolClimbUp(event.getPlayer())) {
			return;
		}

		if (event.getAction() instanceof ClimbUp && EPMClientHooks.shouldAllowClimbUpFromEpicParCoolClingMove(event.getPlayer())) {
			if (EPMConfig.debugClingToCliffState() && EPM.LOGGER.isDebugEnabled()) {
				EPM.LOGGER.debug("[ClingToCliffDebug] allowClimbUpFromEpicParCoolClingMove tick={}", Integer.valueOf(event.getPlayer().tickCount));
			}
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
			if (event.getAction() instanceof ClimbUp && EPMParCoolGate.allowCrossModSkillCompat()) {
				EPMClientHooks.compensateEpicParCoolClimbUp(event.getPlayer());
				EPMClientHooks.markClimbUpForPhantomAscent(event.getPlayer());
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
			DemolitionLeapCatJumpHandler.tickLocalPlayer(event);
			DemolitionLeapAirJumpHandler.tickLocalPlayer(event);
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
		if (!EPMParCoolGate.allowCrossModSkillCompat() || !ModCompat.isWomLoaded()) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
		if (playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			NaturalSprinterState.suppress(playerPatch);
		}
	}
}
