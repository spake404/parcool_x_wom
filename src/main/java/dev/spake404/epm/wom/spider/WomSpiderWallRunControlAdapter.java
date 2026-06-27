package dev.spake404.epm.wom.spider;

import dev.spake404.epm.EPM;
import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;

public final class WomSpiderWallRunControlAdapter {
	private static final int LOG_INTERVAL_TICKS = 5;
	private static final WeakHashMap<Player, Integer> LAST_LOG_TICKS = new WeakHashMap<>();

	private WomSpiderWallRunControlAdapter() {
	}

	public static boolean shouldUseOriginalWomWallRunControls(Player player, PlayerPatch<?> playerPatch) {
		return WomSpiderWallRunModeGate.canUseParCoolOriginalAdapter(player, playerPatch);
	}

	public static boolean shouldTriggerOriginalWomSprintInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (!shouldUseOriginalWomWallRunControls(player, playerPatch)) {
			return false;
		}

		boolean wallRunKeyDown = isWallRunKeyDown();
		boolean forwardDown = isForwardDown(event.getMovementInput());
		boolean allow = wallRunKeyDown && forwardDown;
		logAdapterInput(player, allow, wallRunKeyDown, forwardDown);
		return allow;
	}

	public static boolean shouldOwnOriginalWomSprintInput(MovementInputEvent event) {
		if (event == null) {
			return false;
		}

		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		return shouldUseOriginalWomWallRunControls(player, playerPatch);
	}

	public static boolean isWallRunRequestActive(Player player) {
		PlayerPatch<?> playerPatch = player == null ? null : EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return shouldUseOriginalWomWallRunControls(player, playerPatch)
				&& isWallRunKeyDown()
				&& isForwardDown(null);
	}

	private static boolean isWallRunKeyDown() {
		try {
			return KeyBindings.getKeyHorizontalWallRun().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isForwardDown(Input input) {
		if (input != null && input.up) {
			return true;
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyUp.isDown();
	}

	private static void logAdapterInput(Player player, boolean allow, boolean wallRunKeyDown, boolean forwardDown) {
		if (!EPMConfig.debugSpiderWallRunState() || player == null || !player.isLocalPlayer()) {
			return;
		}
		Integer previousTick = LAST_LOG_TICKS.get(player);
		if (previousTick != null && player.tickCount - previousTick.intValue() < LOG_INTERVAL_TICKS && !allow) {
			return;
		}

		LAST_LOG_TICKS.put(player, Integer.valueOf(player.tickCount));
		EPM.LOGGER.info(
				"[EPM/WomWallRunControlAdapter] phase=sprint_input tick={} allow={} wallRunKeyDown={} forwardDown={} onGround={} delta={} yRot={} yHeadRot={} yBodyRot={}",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(allow),
				Boolean.valueOf(wallRunKeyDown),
				Boolean.valueOf(forwardDown),
				Boolean.valueOf(player.onGround()),
				player.getDeltaMovement(),
				Float.valueOf(player.getViewYRot(1.0F)),
				Float.valueOf(player.yHeadRot),
				Float.valueOf(player.yBodyRot));
	}
}
