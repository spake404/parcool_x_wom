package dev.spake404.epm;

import java.nio.ByteBuffer;

import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.HangDown;
import com.alrex.parcool.common.action.impl.JumpFromBar;
import com.alrex.parcool.common.action.impl.RideZipline;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.world.entity.player.Player;

public final class ParCoolRightClickActionPriority {
	private static final ThreadLocal<ByteBuffer> START_INFO_BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(128));
	private static final ActionProbe<?>[] RIGHT_CLICK_ACTIONS = {
			new ActionProbe<>(ClingToCliff.class, "ClingToCliff"),
			new ActionProbe<>(HangDown.class, "HangDown"),
			new ActionProbe<>(RideZipline.class, "RideZipline"),
			new ActionProbe<>(Vault.class, "Vault"),
			new ActionProbe<>(JumpFromBar.class, "JumpFromBar"),
			new ActionProbe<>(ClimbUp.class, "ClimbUp")
	};

	private ParCoolRightClickActionPriority() {
	}

	public static String blockingAction(Player player, boolean probeStarts) {
		if (player == null) {
			return null;
		}

		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) {
			return null;
		}

		for (ActionProbe<?> probe : RIGHT_CLICK_ACTIONS) {
			Action action = probe.action(parkourability);
			if (isDoing(action)) {
				return probe.name();
			}
		}

		if (!probeStarts) {
			return null;
		}

		IStamina stamina = IStamina.get(player);
		if (stamina == null) {
			return null;
		}

		ByteBuffer startInfo = START_INFO_BUFFER.get();
		for (ActionProbe<?> probe : RIGHT_CLICK_ACTIONS) {
			Action action = probe.action(parkourability);
			if (canStart(player, parkourability, stamina, startInfo, action, probe.actionClass())) {
				return probe.name();
			}
		}
		return null;
	}

	private static boolean canStart(Player player, Parkourability parkourability, IStamina stamina, ByteBuffer startInfo, Action action, Class<? extends Action> actionClass) {
		if (action == null) {
			return false;
		}

		try {
			if (parkourability.getActionInfo() == null || !parkourability.getActionInfo().can(actionClass)) {
				return false;
			}
			startInfo.clear();
			return action.canStart(player, parkourability, stamina, startInfo);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isDoing(Action action) {
		try {
			return action != null && action.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private record ActionProbe<T extends Action>(Class<T> actionClass, String name) {
		private T action(Parkourability parkourability) {
			try {
				return parkourability.get(actionClass);
			} catch (RuntimeException | LinkageError ignored) {
				return null;
			}
		}
	}
}
