package dev.spake404.epm.vault;

import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class VaultStartFastRunGrace {
	private static final int RECENT_FAST_RUN_GRACE_TICKS = 6;
	private static final WeakHashMap<Player, Integer> RECENT_FAST_RUN_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LOG_TICKS = new WeakHashMap<>();

	private VaultStartFastRunGrace() {
	}

	public static void rememberRecentFastRun(Player player) {
		if (!isEnabledFor(player)) {
			return;
		}

		RECENT_FAST_RUN_TICKS.put(player, Integer.valueOf(player.tickCount));
	}

	public static boolean shouldAllow(Player player) {
		if (!isEnabledFor(player)) {
			clear(player);
			return false;
		}

		Integer lastTick = RECENT_FAST_RUN_TICKS.get(player);
		if (lastTick == null || player.tickCount - lastTick.intValue() > RECENT_FAST_RUN_GRACE_TICKS) {
			RECENT_FAST_RUN_TICKS.remove(player);
			return false;
		}

		if (isParCoolVaultDoing(player)
				|| EPMClientHooks.hasHardVaultFastRunBlockerForVaultStartGrace(player)
				|| !hasMovementInput()
				|| (EPMClientHooks.isFastRunPressKeyControl() && !EPMClientHooks.isFastRunControlKeyDown())
				|| !hasVaultStartGeometry(player)) {
			return false;
		}

		if (EPMConfig.debugVaultState() && shouldLogTick(player)) {
			Vec3 delta = player.getDeltaMovement();
			EPM.LOGGER.info(
					"[EPM/VaultDebug] phase=vault_start_fast_run_recent_grace tick={} elapsed={} lastFastRunTick={} onGround={} sprinting={} fastRunKeyDown={} fastRunControlKeyDown={} pos=({}, {}, {}) delta=({}, {}, {})",
					Integer.valueOf(player.tickCount),
					Integer.valueOf(player.tickCount - lastTick.intValue()),
					lastTick,
					Boolean.valueOf(player.onGround()),
					Boolean.valueOf(player.isSprinting()),
					Boolean.valueOf(EPMClientHooks.isFastRunKeyDown()),
					Boolean.valueOf(EPMClientHooks.isFastRunControlKeyDown()),
					Double.valueOf(player.getX()),
					Double.valueOf(player.getY()),
					Double.valueOf(player.getZ()),
					Double.valueOf(delta.x()),
					Double.valueOf(delta.y()),
					Double.valueOf(delta.z()));
		}
		return true;
	}

	public static boolean hasRecent(Player player) {
		if (!isEnabledFor(player)) {
			return false;
		}

		Integer lastTick = RECENT_FAST_RUN_TICKS.get(player);
		if (lastTick == null) {
			return false;
		}

		if (player.tickCount - lastTick.intValue() > RECENT_FAST_RUN_GRACE_TICKS) {
			RECENT_FAST_RUN_TICKS.remove(player);
			return false;
		}
		return true;
	}

	public static void clear(Player player) {
		if (player == null) {
			return;
		}

		RECENT_FAST_RUN_TICKS.remove(player);
		LOG_TICKS.remove(player);
	}

	private static boolean isEnabledFor(Player player) {
		return EPMParCoolGate.allowCrossModSkillCompat()
				&& EPMConfig.fastRunVaultChainFix()
				&& EPMConfig.vaultStartFastRunGrace()
				&& player != null
				&& player.isLocalPlayer()
				&& player.level().isClientSide();
	}

	private static boolean isParCoolVaultDoing(Player player) {
		try {
			Parkourability parkourability = Parkourability.get(player);
			Vault vault = parkourability == null ? null : parkourability.get(Vault.class);
			return vault != null && vault.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasMovementInput() {
		try {
			Vec3 moveVector = KeyBindings.getCurrentMoveVector();
			return moveVector != null && moveVector.lengthSqr() > 1.0E-6D;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean hasVaultStartGeometry(Player player) {
		try {
			Vec3 step = WorldUtil.getVaultableStep(player);
			if (step == null || WorldUtil.getWallHeight(player) <= player.getBbHeight() * 0.44D) {
				return false;
			}

			Vec3 look = player.getLookAngle();
			look = new Vec3(look.x(), 0.0D, look.z()).normalize();
			Vec3 normalizedStep = step.normalize();
			Vec3 angle = new Vec3(
					look.x() * normalizedStep.x() + look.z() * normalizedStep.z(),
					0.0D,
					-look.x() * normalizedStep.z() + look.z() * normalizedStep.x()).normalize();
			return angle.x() >= 0.707106D;
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean shouldLogTick(Player player) {
		Integer lastTick = LOG_TICKS.get(player);
		if (lastTick != null && lastTick.intValue() == player.tickCount) {
			return false;
		}

		LOG_TICKS.put(player, Integer.valueOf(player.tickCount));
		return true;
	}
}
