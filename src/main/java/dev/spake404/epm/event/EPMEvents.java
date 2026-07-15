package dev.spake404.epm.event;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.WallJump;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.compat.ModCompat;
import dev.spake404.epm.naturalsprinter.NaturalSprinterFastRunAnimationOverrides;
import dev.spake404.epm.naturalsprinter.NaturalSprinterState;
import dev.spake404.epm.network.EPMNetwork;
import dev.spake404.epm.phantom.MomentumAirAttackWindowState;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EPMEvents {
	private EPMEvents() {
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
	public static void markWallJumpAirAttackWindow(ParCoolActionEvent.StartEvent event) {
		if (!EPMParCoolGate.allowCrossModSkillCompat()) {
			return;
		}

		if (!(event.getAction() instanceof WallJump)) {
			return;
		}

		MomentumAirAttackWindowState.markWallJump(event.getPlayer());
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
	public static void tickMomentumAirAttackWindows(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		if (MomentumAirAttackWindowState.hasTrackedState()) {
			MomentumAirAttackWindowState.tick(event);
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
