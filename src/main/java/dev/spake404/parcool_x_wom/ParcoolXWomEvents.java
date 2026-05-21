package dev.spake404.parcool_x_wom;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class ParcoolXWomEvents {
	private ParcoolXWomEvents() {
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

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
		ParcoolXWomClientHooks.markCatLeapForPhantomAscent(event.getPlayer());

		if (playerPatch != null && playerPatch.isLogicalClient() && playerPatch.isEpicFightMode() && NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			ParcoolXWomClientHooks.reduceNaturalSprinterCatLeapMotion(event.getPlayer());
			ParcoolXWomClientHooks.startNaturalSprinterCatLeap(event.getPlayer());

			AssetAccessor<? extends StaticAnimation> sprintJump = WomAnimationRefs.bipedSprintJump();
			if (sprintJump != null) {
				playerPatch.playAnimationSynchronized(sprintJump, 0.0F);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void markEpicParCoolWallJumpForPhantomAscent(ParCoolActionEvent.StartEvent event) {
		if (ParcoolXWomConfig.wallJumpPrimesPhantomAscent() && event.getAction() instanceof WallJump) {
			ParcoolXWomClientHooks.markWallJumpForPhantomAscent(event.getPlayer());
		}
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
			ParcoolXWomClientHooks.markVaultStartedFromFastRun(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void clearFastRunHoldAfterVault(ParCoolActionEvent.Finish.Post event) {
		if (event.getPlayer().isLocalPlayer() && event.getAction() instanceof Vault) {
			ParcoolXWomClientHooks.clearVaultFastRunHold(event.getPlayer());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void tickLocalPlayer(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		if (PhantomAscentAirAttackState.hasTrackedState()) {
			PhantomAscentAirAttackState.tick(event);
		}

		if (event.player.level().isClientSide()) {
			ParcoolXWomClientHooks.tickLocalPlayer(event);
		}
	}

	private static void syncAndSuppressNaturalSprinter(ParCoolActionEvent event) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
		if (playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			NaturalSprinterState.suppress(playerPatch);
		}
	}
}
