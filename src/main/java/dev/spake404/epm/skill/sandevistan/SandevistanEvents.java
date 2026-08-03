package dev.spake404.epm.skill.sandevistan;

import dev.spake404.epm.EPM;
import dev.spake404.epm.skill.sandevistan.network.SandevistanNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EPM.MODID)
public final class SandevistanEvents {
	private SandevistanEvents() {
	}

	@SubscribeEvent
	public static void tickServer(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			SandevistanManager.tick();
		}
	}

	@SubscribeEvent
	public static void livingHurt(LivingHurtEvent event) {
		if (event.getSource().getEntity() instanceof ServerPlayer player) {
			event.setAmount((float)(event.getAmount() * SandevistanManager.outgoingDamageMultiplier(player)));
		}
		if (event.getEntity() instanceof ServerPlayer player) {
			event.setAmount((float)(event.getAmount()
					* SandevistanManager.incomingDamageMultiplier(player, event.getSource())));
		}

	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void livingDamage(LivingDamageEvent event) {
		Entity attacker = event.getSource().getEntity();
		if (event.getAmount() > 0.0F && attacker != null) {
			if (attacker instanceof ServerPlayer player && event.getEntity() != player) {
				SandevistanNetwork.sendCombatActivity(player);
			}
			if (event.getEntity() instanceof ServerPlayer player && attacker != player) {
				SandevistanNetwork.sendCombatActivity(player);
			}
		}
	}

	@SubscribeEvent
	public static void playerDeath(LivingDeathEvent event) {
		if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() != killer) {
			SandevistanManager.rewardKill(killer);
		}
		if (event.getEntity() instanceof ServerPlayer player) {
			SandevistanManager.stop(player, SandevistanStopReason.DEATH);
		}
	}

	@SubscribeEvent
	public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SandevistanManager.stop(player, SandevistanStopReason.LOGOUT);
		}
	}

	@SubscribeEvent
	public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SandevistanManager.stop(player, SandevistanStopReason.DIMENSION_CHANGE);
		}
	}

	@SubscribeEvent
	public static void startTracking(PlayerEvent.StartTracking event) {
		if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer target) {
			SandevistanNetwork.sendSnapshot(observer, target);
		}
	}
}
