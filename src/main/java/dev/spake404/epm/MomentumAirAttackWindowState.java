package dev.spake404.epm;

import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class MomentumAirAttackWindowState {
	private static final WeakHashMap<Player, Integer> WALL_JUMP_AIR_ATTACK_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> WALL_JUMP_AIR_ATTACK_CONSUMED = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> WALL_JUMP_FALL_PROTECTION = new WeakHashMap<>();
	private static final int WALL_JUMP_AIR_ATTACK_DURATION_TICKS = 12;

	private MomentumAirAttackWindowState() {
	}

	public static void markWallJump(Player player) {
		if (player == null || !EPMConfig.wallJumpPrimesAirAttack() || player.onGround() || player.isSpectator() || player.isInWater()) {
			return;
		}

		WALL_JUMP_AIR_ATTACK_TICKS.put(player, Integer.valueOf(WALL_JUMP_AIR_ATTACK_DURATION_TICKS));
		WALL_JUMP_AIR_ATTACK_CONSUMED.remove(player);
		WALL_JUMP_FALL_PROTECTION.put(player, Boolean.TRUE);
	}

	public static boolean hasTrackedState() {
		return !WALL_JUMP_AIR_ATTACK_TICKS.isEmpty()
				|| !WALL_JUMP_AIR_ATTACK_CONSUMED.isEmpty()
				|| !WALL_JUMP_FALL_PROTECTION.isEmpty()
				|| PhantomAscentAirAttackState.hasTrackedState();
	}

	public static void tick(TickEvent.PlayerTickEvent event) {
		Player player = event.player;
		PhantomAscentAirAttackState.tick(event);

		Integer ticks = WALL_JUMP_AIR_ATTACK_TICKS.get(player);
		if (ticks == null) {
			if (player.onGround()) {
				WALL_JUMP_AIR_ATTACK_CONSUMED.remove(player);
				WALL_JUMP_FALL_PROTECTION.remove(player);
			}
			return;
		}

		if (ticks.intValue() <= 0 || player.onGround() || player.isSpectator() || player.isInWater() || !EPMConfig.wallJumpPrimesAirAttack()) {
			clearWallJump(player);
			return;
		}

		WALL_JUMP_AIR_ATTACK_TICKS.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	public static boolean canUseBasicAttack(PlayerPatch<?> playerPatch) {
		return isInAirAttackWindow(playerPatch);
	}

	public static boolean isInAirAttackWindow(PlayerPatch<?> playerPatch) {
		return PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(playerPatch)
				|| isInWallJumpAirAttackWindow(playerPatch);
	}

	public static boolean isInWallJumpWindow(Player player) {
		return isInWallJumpAirAttackWindow(player);
	}

	public static void clearWallJumpWindow(Player player) {
		clearWallJump(player);
	}

	public static double adjustAirAttackYVelocity(PlayerPatch<?> playerPatch, double yVelocity) {
		return PhantomAscentAirAttackState.adjustAirAttackYVelocity(playerPatch, yVelocity);
	}

	public static void consume(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return;
		}

		Player player = playerPatch.getOriginal();
		if (PhantomAscentAirAttackState.isInPhantomAscentAirAttackWindow(playerPatch)) {
			PhantomAscentAirAttackState.consume(player);
			return;
		}

		if (isInWallJumpAirAttackWindow(player)) {
			WALL_JUMP_AIR_ATTACK_TICKS.remove(player);
			WALL_JUMP_AIR_ATTACK_CONSUMED.put(player, Boolean.TRUE);
		}
	}

	public static boolean shouldProtectWallJumpFall(Player player, float damage) {
		if (player == null || !Boolean.TRUE.equals(WALL_JUMP_FALL_PROTECTION.remove(player))) {
			return false;
		}

		return damage < EPMConfig.wallJumpAirAttackFallProtectionDamageThreshold();
	}

	private static boolean isInWallJumpAirAttackWindow(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		return isInWallJumpAirAttackWindow(playerPatch.getOriginal());
	}

	private static boolean isInWallJumpAirAttackWindow(Player player) {
		return player != null
				&& WALL_JUMP_AIR_ATTACK_TICKS.containsKey(player)
				&& !Boolean.TRUE.equals(WALL_JUMP_AIR_ATTACK_CONSUMED.get(player))
				&& !player.onGround()
				&& !player.isSpectator()
				&& !player.isInWater();
	}

	private static void clearWallJump(Player player) {
		WALL_JUMP_AIR_ATTACK_TICKS.remove(player);
		WALL_JUMP_AIR_ATTACK_CONSUMED.remove(player);
		WALL_JUMP_FALL_PROTECTION.remove(player);
	}
}
