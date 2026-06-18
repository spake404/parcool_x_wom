package dev.spake404.epm;

import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;

public final class JumpInputConsumptionState {
	private static final WeakHashMap<Player, State> STATES = new WeakHashMap<>();
	private static final int CONSUMED_PRESS_MAX_TICKS = 40;

	private JumpInputConsumptionState() {
	}

	public static void tick(Player player, boolean jumpDown) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		State state = STATES.get(player);
		if (state == null) {
			if (!jumpDown) {
				return;
			}
			state = new State();
			STATES.put(player, state);
			beginPress(state, player.tickCount);
		} else if (jumpDown && !state.jumpDown) {
			beginPress(state, player.tickCount);
		} else if (!jumpDown && state.jumpDown) {
			state.jumpDown = false;
		}

		clearExpiredConsumedPress(player, state);
		if (!state.jumpDown && !state.hasConsumedPress()) {
			STATES.remove(player);
		}
	}

	public static void markConsumed(Player player, Consumer consumer, String reason, boolean jumpDown) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		tick(player, jumpDown);
		State state = STATES.get(player);
		if (state == null) {
			state = new State();
			STATES.put(player, state);
			beginPress(state, player.tickCount);
			state.jumpDown = jumpDown;
		}

		state.consumedPressId = state.currentPressId;
		state.consumedPressStartTick = state.currentPressStartTick;
		state.consumedTick = player.tickCount;
		state.consumer = consumer == null ? Consumer.UNKNOWN : consumer;
		state.reason = reason == null ? "unknown" : reason;
	}

	public static boolean isCurrentPressConsumed(Player player) {
		State state = STATES.get(player);
		return player != null
				&& player.isLocalPlayer()
				&& state != null
				&& state.jumpDown
				&& state.hasConsumedPress()
				&& state.consumedPressId == state.currentPressId;
	}

	public static boolean shouldDropQueuedPress(Player player, int queuedTick) {
		State state = STATES.get(player);
		if (player == null || !player.isLocalPlayer() || state == null || !state.hasConsumedPress()) {
			return false;
		}

		int elapsed = player.tickCount - state.consumedTick;
		return elapsed >= 0
				&& elapsed <= CONSUMED_PRESS_MAX_TICKS
				&& queuedTick <= state.consumedTick;
	}

	public static boolean hasTrackedState(Player player) {
		return player != null && STATES.containsKey(player);
	}

	public static void clear(Player player) {
		if (player != null) {
			STATES.remove(player);
		}
	}

	public static Snapshot snapshot(Player player) {
		State state = player == null ? null : STATES.get(player);
		if (player == null || state == null) {
			return Snapshot.empty();
		}

		int pressElapsed = state.currentPressStartTick < 0 ? -1 : player.tickCount - state.currentPressStartTick;
		int consumedElapsed = state.consumedTick < 0 ? -1 : player.tickCount - state.consumedTick;
		boolean consumedActive = state.hasConsumedPress()
				&& consumedElapsed >= 0
				&& consumedElapsed <= CONSUMED_PRESS_MAX_TICKS;
		boolean consumedHeld = consumedActive
				&& state.jumpDown
				&& state.consumedPressId == state.currentPressId;
		return new Snapshot(
				true,
				state.jumpDown,
				state.currentPressId,
				state.currentPressStartTick,
				pressElapsed,
				consumedActive,
				consumedHeld,
				state.consumedPressId,
				state.consumedPressStartTick,
				state.consumedTick,
				consumedElapsed,
				state.consumer,
				state.reason
		);
	}

	private static void beginPress(State state, int tick) {
		state.currentPressId++;
		state.currentPressStartTick = tick;
		state.jumpDown = true;
	}

	private static void clearExpiredConsumedPress(Player player, State state) {
		if (!state.hasConsumedPress()) {
			return;
		}

		int elapsed = player.tickCount - state.consumedTick;
		if (elapsed < 0 || elapsed > CONSUMED_PRESS_MAX_TICKS) {
			state.consumedPressId = -1;
			state.consumedPressStartTick = -1;
			state.consumedTick = -1;
			state.consumer = Consumer.NONE;
			state.reason = "none";
		}
	}

	public enum Consumer {
		NONE,
		PHANTOM_ASCENT,
		DEMOLITION_LEAP_DOUBLE_JUMP,
		GLIDER_TOGGLE,
		PARCOOL_WALL_JUMP,
		WOM_BACKFLIP,
		UNKNOWN
	}

	public record Snapshot(
			boolean tracked,
			boolean jumpDown,
			int currentPressId,
			int currentPressStartTick,
			int currentPressElapsed,
			boolean consumedActive,
			boolean consumedHeld,
			int consumedPressId,
			int consumedPressStartTick,
			int consumedTick,
			int consumedElapsed,
			Consumer consumer,
			String reason) {
		private static Snapshot empty() {
			return new Snapshot(false, false, -1, -1, -1, false, false, -1, -1, -1, -1, Consumer.NONE, "none");
		}
	}

	private static final class State {
		private boolean jumpDown;
		private int currentPressId;
		private int currentPressStartTick = -1;
		private int consumedPressId = -1;
		private int consumedPressStartTick = -1;
		private int consumedTick = -1;
		private Consumer consumer = Consumer.NONE;
		private String reason = "none";

		private boolean hasConsumedPress() {
			return consumedPressId >= 0 && consumedTick >= 0;
		}
	}
}
