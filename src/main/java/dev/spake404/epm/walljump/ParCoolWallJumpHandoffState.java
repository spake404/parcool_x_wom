package dev.spake404.epm.walljump;

import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.config.EPMConfig;
import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;

public final class ParCoolWallJumpHandoffState {
	private static final WeakHashMap<Player, State> STATES = new WeakHashMap<>();
	private static final int HANDOFF_WINDOW_TICKS = 20;
	private static final int FRESH_PRESS_MAX_TICKS = 3;

	private ParCoolWallJumpHandoffState() {
	}

	public static void markStarted(Player player, Source source) {
		if (!EPMParCoolGate.allowCrossModSkillCompat() || player == null || !player.isLocalPlayer()) {
			return;
		}

		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		int initialPressSequence = jump.tracked() ? jump.pressSequence() : -1;
		STATES.put(player, new State(player.tickCount, initialPressSequence, source == null ? Source.PARCOOL : source));
		log(player, "started", Consumer.NONE, STATES.get(player));
	}

	public static boolean isActive(Player player) {
		State state = state(player);
		return state != null && !state.consumed && elapsed(player, state) <= HANDOFF_WINDOW_TICKS;
	}

	public static boolean isSamePress(Player player) {
		State state = state(player);
		if (state == null) {
			return false;
		}

		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		return jump.tracked()
				&& jump.jumpDown()
				&& state.initialPressSequence >= 0
				&& jump.pressSequence() == state.initialPressSequence;
	}

	public static boolean isFreshPress(Player player) {
		State state = state(player);
		if (state == null || state.consumed || elapsed(player, state) > HANDOFF_WINDOW_TICKS) {
			return false;
		}

		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		return jump.tracked()
				&& jump.jumpDown()
				&& jump.pressSequence() != state.initialPressSequence
				&& jump.pressElapsed() >= 0
				&& jump.pressElapsed() <= FRESH_PRESS_MAX_TICKS;
	}

	public static boolean shouldAllowPhantom(Player player) {
		boolean allow = isFreshPress(player);
		if (allow) {
			log(player, "allow_phantom", Consumer.PHANTOM, state(player));
		}
		return allow;
	}

	public static boolean shouldAllowGlider(Player player) {
		boolean allow = isFreshPress(player);
		if (allow) {
			log(player, "allow_glider", Consumer.GLIDER, state(player));
		}
		return allow;
	}

	public static boolean shouldBlockInitialPress(Player player) {
		boolean block = isActive(player) && isSamePress(player);
		if (block) {
			log(player, "block_initial_press", Consumer.NONE, state(player));
		}
		return block;
	}

	public static boolean canAttackHandoff(Player player) {
		boolean allow = isActive(player);
		if (allow) {
			log(player, "allow_attack", Consumer.ATTACK, state(player));
		}
		return allow;
	}

	public static Integer startedTick(Player player) {
		State state = state(player);
		return state == null ? null : Integer.valueOf(state.startedTick);
	}

	public static void consume(Player player, Consumer consumer) {
		State state = state(player);
		if (state == null) {
			return;
		}

		state.consumed = true;
		log(player, "consume", consumer == null ? Consumer.NONE : consumer, state);
		STATES.remove(player);
	}

	public static void clear(Player player) {
		if (player != null) {
			STATES.remove(player);
		}
	}

	private static State state(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return null;
		}

		State state = STATES.get(player);
		if (state == null) {
			return null;
		}

		int elapsed = elapsed(player, state);
		if (elapsed < 0 || elapsed > HANDOFF_WINDOW_TICKS || player.onGround() || player.isInWater()) {
			STATES.remove(player);
			log(player, "expired", Consumer.NONE, state);
			return null;
		}
		return state;
	}

	private static int elapsed(Player player, State state) {
		return player.tickCount - state.startedTick;
	}

	private static void log(Player player, String phase, Consumer consumer, State state) {
		if (!EPMConfig.debugActionArbitrationState() || player == null || !player.isLocalPlayer()) {
			return;
		}

		JumpActionArbiter.Snapshot jump = JumpActionArbiter.snapshot(player);
		EPM.LOGGER.info(
				"[EPM/WallJumpHandoff] phase={} tick={} consumer={} active={} source={} elapsed={} startedTick={} initialPressSeq={} jumpDown={} pressSeq={} pressElapsed={} winner={} winnerReason={} onGround={} delta={}",
				phase,
				Integer.valueOf(player.tickCount),
				consumer,
				Boolean.valueOf(state != null && !state.consumed),
				state == null ? "none" : state.source.name(),
				Integer.valueOf(state == null ? -1 : elapsed(player, state)),
				Integer.valueOf(state == null ? -1 : state.startedTick),
				Integer.valueOf(state == null ? -1 : state.initialPressSequence),
				Boolean.valueOf(jump.jumpDown()),
				Integer.valueOf(jump.pressSequence()),
				Integer.valueOf(jump.pressElapsed()),
				jump.winner(),
				jump.reason(),
				Boolean.valueOf(player.onGround()),
				player.getDeltaMovement());
	}

	public enum Source {
		PARCOOL,
		WOM_WALLRUN
	}

	public enum Consumer {
		NONE,
		PHANTOM,
		GLIDER,
		ATTACK
	}

	private static final class State {
		private final int startedTick;
		private final int initialPressSequence;
		private final Source source;
		private boolean consumed;

		private State(int startedTick, int initialPressSequence, Source source) {
			this.startedTick = startedTick;
			this.initialPressSequence = initialPressSequence;
			this.source = source;
		}
	}
}
