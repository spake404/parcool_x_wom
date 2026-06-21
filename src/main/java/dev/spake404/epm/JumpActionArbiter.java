package dev.spake404.epm;

import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;

public final class JumpActionArbiter {
	private static final WeakHashMap<Player, State> STATES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PRESS_SEQUENCES = new WeakHashMap<>();
	private static final int WINNER_MAX_AGE_TICKS = 40;

	private JumpActionArbiter() {
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
			beginPress(player, state, player.tickCount);
			log(player, "press_begin", Winner.NONE, Winner.NONE, "tick", state);
			return;
		}

		if (jumpDown && !state.jumpDown) {
			beginPress(player, state, player.tickCount);
			log(player, "press_begin", Winner.NONE, Winner.NONE, "tick", state);
		} else if (!jumpDown && state.jumpDown) {
			state.jumpDown = false;
			state.winner = Winner.NONE;
			state.winnerTick = -1;
			state.winnerPressSequence = -1;
			state.reason = "none";
			log(player, "press_release", Winner.NONE, Winner.NONE, "tick", state);
		}

		clearExpiredWinner(player, state);
		if (!state.jumpDown && state.winner == Winner.NONE) {
			STATES.remove(player);
		}
	}

	public static boolean claim(Player player, Winner candidate, String reason, boolean jumpDown) {
		if (player == null || !player.isLocalPlayer() || candidate == null || candidate == Winner.NONE) {
			return false;
		}

		tick(player, jumpDown);
		State state = STATES.get(player);
		if (state == null || !state.jumpDown) {
			log(player, "claim_reject_no_press", candidate, Winner.NONE, reason, state);
			return false;
		}

		if (state.winner == Winner.NONE || state.winnerPressSequence != state.pressSequence) {
			state.winner = candidate;
			state.winnerTick = player.tickCount;
			state.winnerPressSequence = state.pressSequence;
			state.reason = reason == null ? "unknown" : reason;
			log(player, "claim_accept", candidate, state.winner, state.reason, state);
			return true;
		}

		if (state.winner == candidate) {
			log(player, "claim_keep_same", candidate, state.winner, reason, state);
			return true;
		}

		log(player, "claim_reject_taken", candidate, state.winner, reason, state);
		return false;
	}

	public static boolean isClaimedBy(Player player, Winner winner) {
		State state = STATES.get(player);
		return player != null
				&& player.isLocalPlayer()
				&& winner != null
				&& state != null
				&& state.jumpDown
				&& state.winner == winner
				&& state.winnerPressSequence == state.pressSequence
				&& isWinnerFresh(player, state);
	}

	public static boolean isClaimedByOther(Player player, Winner candidate) {
		State state = STATES.get(player);
		return player != null
				&& player.isLocalPlayer()
				&& candidate != null
				&& state != null
				&& state.jumpDown
				&& state.winner != Winner.NONE
				&& state.winner != candidate
				&& state.winnerPressSequence == state.pressSequence
				&& isWinnerFresh(player, state);
	}

	public static boolean isClaimedByHigherOrEqual(Player player, Winner candidate) {
		State state = STATES.get(player);
		return player != null
				&& player.isLocalPlayer()
				&& candidate != null
				&& state != null
				&& state.jumpDown
				&& state.winner != Winner.NONE
				&& state.winner != candidate
				&& state.winnerPressSequence == state.pressSequence
				&& state.winner.priority <= candidate.priority
				&& isWinnerFresh(player, state);
	}

	public static boolean shouldDropQueuedPress(Player player, int queuedTick) {
		State state = STATES.get(player);
		return player != null
				&& player.isLocalPlayer()
				&& state != null
				&& state.winner != Winner.NONE
				&& state.winnerTick >= 0
				&& queuedTick <= state.winnerTick
				&& isWinnerFresh(player, state);
	}

	public static Snapshot snapshot(Player player) {
		State state = player == null ? null : STATES.get(player);
		if (player == null || state == null) {
			return Snapshot.empty();
		}

		return new Snapshot(
				true,
				state.jumpDown,
				state.pressSequence,
				state.pressStartTick,
				state.pressStartTick < 0 ? -1 : player.tickCount - state.pressStartTick,
				state.winner,
				state.winnerPressSequence,
				state.winnerTick,
				state.winnerTick < 0 ? -1 : player.tickCount - state.winnerTick,
				state.reason);
	}

	public static void clear(Player player) {
		if (player != null) {
			STATES.remove(player);
		}
	}

	private static void beginPress(Player player, State state, int tick) {
		state.pressSequence = nextPressSequence(player);
		state.pressStartTick = tick;
		state.jumpDown = true;
		state.winner = Winner.NONE;
		state.winnerTick = -1;
		state.winnerPressSequence = -1;
		state.reason = "none";
	}

	private static int nextPressSequence(Player player) {
		int next = PRESS_SEQUENCES.getOrDefault(player, Integer.valueOf(0)).intValue() + 1;
		if (next < 0) {
			next = 1;
		}
		PRESS_SEQUENCES.put(player, Integer.valueOf(next));
		return next;
	}

	private static void clearExpiredWinner(Player player, State state) {
		if (state.winner == Winner.NONE || isWinnerFresh(player, state)) {
			return;
		}

		state.winner = Winner.NONE;
		state.winnerTick = -1;
		state.winnerPressSequence = -1;
		state.reason = "none";
	}

	private static boolean isWinnerFresh(Player player, State state) {
		int elapsed = player.tickCount - state.winnerTick;
		return elapsed >= 0 && elapsed <= WINNER_MAX_AGE_TICKS;
	}

	private static void log(Player player, String phase, Winner candidate, Winner winner, String reason, State state) {
		if (player == null || !player.isLocalPlayer()) {
			return;
		}

		EPM.LOGGER.info(
				"[EPM/JumpArbiter] phase={} tick={} candidate={} winner={} reason={} jumpDown={} pressSeq={} pressStart={} winnerPressSeq={} winnerTick={} winnerReason={} onGround={} delta={}",
				phase,
				Integer.valueOf(player.tickCount),
				candidate,
				winner,
				reason,
				Boolean.valueOf(state != null && state.jumpDown),
				Integer.valueOf(state == null ? -1 : state.pressSequence),
				Integer.valueOf(state == null ? -1 : state.pressStartTick),
				Integer.valueOf(state == null ? -1 : state.winnerPressSequence),
				Integer.valueOf(state == null ? -1 : state.winnerTick),
				state == null ? "none" : state.reason,
				Boolean.valueOf(player.onGround()),
				player.getDeltaMovement());
	}

	public enum Winner {
		NONE(1000),
		WOM_WALL_JUMP(10),
		PARCOOL_WALL_JUMP(20),
		PHANTOM_ASCENT(30),
		GLIDER_TOGGLE(40);

		private final int priority;

		Winner(int priority) {
			this.priority = priority;
		}
	}

	public record Snapshot(
			boolean tracked,
			boolean jumpDown,
			int pressSequence,
			int pressStartTick,
			int pressElapsed,
			Winner winner,
			int winnerPressSequence,
			int winnerTick,
			int winnerElapsed,
			String reason) {
		private static Snapshot empty() {
			return new Snapshot(false, false, -1, -1, -1, Winner.NONE, -1, -1, -1, "none");
		}
	}

	private static final class State {
		private boolean jumpDown;
		private int pressSequence = -1;
		private int pressStartTick = -1;
		private Winner winner = Winner.NONE;
		private int winnerPressSequence = -1;
		private int winnerTick = -1;
		private String reason = "none";
	}
}
