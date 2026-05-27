package dev.spake404.epm;

import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class PhantomAscentAirAttackState {
	private static final WeakHashMap<Player, Boolean> AIR_ATTACK_SIGNALS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> AIR_ATTACK_CONSUMED = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PROTECT_NEXT_FALL_ACTIVE = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, SkillContainer> PHANTOM_ASCENT_CACHE = new WeakHashMap<>();

	private PhantomAscentAirAttackState() {
	}

	public static void mark(Player player) {
		if (player != null) {
			AIR_ATTACK_SIGNALS.put(player, Boolean.TRUE);
			if (player.onGround()) {
				AIR_ATTACK_CONSUMED.remove(player);
			}
		}
	}

	public static void consume(Player player) {
		if (player != null) {
			AIR_ATTACK_SIGNALS.remove(player);
			AIR_ATTACK_CONSUMED.put(player, Boolean.TRUE);
		}
	}

	public static void clear(Player player) {
		if (player != null) {
			AIR_ATTACK_SIGNALS.remove(player);
			AIR_ATTACK_CONSUMED.remove(player);
			PROTECT_NEXT_FALL_ACTIVE.remove(player);
		}
	}

	public static boolean hasAirAttackSignal(Player player) {
		return Boolean.TRUE.equals(AIR_ATTACK_SIGNALS.get(player));
	}

	public static boolean hasTrackedState() {
		return !AIR_ATTACK_SIGNALS.isEmpty()
				|| !AIR_ATTACK_CONSUMED.isEmpty()
				|| !PROTECT_NEXT_FALL_ACTIVE.isEmpty();
	}

	public static void tick(TickEvent.PlayerTickEvent event) {
		Player player = event.player;
		if (player.onGround()) {
			clear(player);
		}
	}

	public static boolean canUseBasicAttackAfterPhantomAscent(PlayerPatch<?> playerPatch) {
		return isActivePhantomAscentAirAttackWindow(playerPatch);
	}

	public static boolean isActivePhantomAscentAirAttackWindow(PlayerPatch<?> playerPatch) {
		return isInPhantomAscentAirAttackWindow(playerPatch)
				&& !playerPatch.getOriginal().onGround()
				&& isPlayingPhantomAscent(playerPatch);
	}

	public static boolean isInPhantomAscentAirAttackWindow(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		return hasUnconsumedSignal(playerPatch)
				&& !player.onGround()
				&& !player.isSpectator()
				&& !player.isInWater();
	}

	public static double adjustAirAttackYVelocity(PlayerPatch<?> playerPatch, double yVelocity) {
		return isInPhantomAscentAirAttackWindow(playerPatch) ? 0.0D : yVelocity;
	}

	public static boolean hasUnconsumedSignal(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		if (player.onGround()) {
			clear(player);
			return false;
		}

		if (Boolean.TRUE.equals(AIR_ATTACK_CONSUMED.get(player))) {
			return false;
		}

		return hasAirAttackSignal(player) || refreshProtectNextFallState(playerPatch);
	}

	public static boolean hasProtectNextFall(PlayerPatch<?> playerPatch) {
		return refreshProtectNextFallState(playerPatch);
	}

	public static boolean isAirAttackConsumed(Player player) {
		return Boolean.TRUE.equals(AIR_ATTACK_CONSUMED.get(player));
	}

	private static boolean refreshProtectNextFallState(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return false;
		}

		Player player = playerPatch.getOriginal();
		if (player.onGround()) {
			clear(player);
			return false;
		}

		boolean protectNextFall = readProtectNextFall(playerPatch);
		if (protectNextFall && !Boolean.TRUE.equals(PROTECT_NEXT_FALL_ACTIVE.get(player))) {
			AIR_ATTACK_CONSUMED.remove(player);
			AIR_ATTACK_SIGNALS.put(player, Boolean.TRUE);
			PROTECT_NEXT_FALL_ACTIVE.put(player, Boolean.TRUE);
		} else if (!protectNextFall) {
			PROTECT_NEXT_FALL_ACTIVE.remove(player);
		}

		return protectNextFall;
	}

	private static boolean readProtectNextFall(PlayerPatch<?> playerPatch) {
		SkillContainer phantomAscent = findPhantomAscent(playerPatch);
		if (phantomAscent == null || phantomAscent.getDataManager() == null) {
			return false;
		}

		Object value = phantomAscent.getDataManager().getDataValue(SkillDataKeys.PROTECT_NEXT_FALL.get());
		return Boolean.TRUE.equals(value);
	}

	private static boolean isPlayingPhantomAscent(PlayerPatch<?> playerPatch) {
		try {
			AnimationPlayer animationPlayer = playerPatch.getAnimator().getPlayerFor(null);
			AssetAccessor<?> animation = animationPlayer == null ? null : animationPlayer.getRealAnimation();
			return WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedPhantomAscentForward(), WomAnimationRefs.bipedPhantomAscentBackward());
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static SkillContainer findPhantomAscent(PlayerPatch<?> playerPatch) {
		if (playerPatch.getSkillCapability() == null) {
			return null;
		}

		SkillContainer cached = PHANTOM_ASCENT_CACHE.get(playerPatch);
		if (isPhantomAscentContainer(cached)) {
			return cached;
		}

		try (var containers = playerPatch.getSkillCapability().listSkillContainers()) {
			SkillContainer found = containers
					.filter(PhantomAscentAirAttackState::isPhantomAscentContainer)
					.findFirst()
					.orElse(null);
			if (found != null) {
				PHANTOM_ASCENT_CACHE.put(playerPatch, found);
			}
			return found;
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isPhantomAscentContainer(SkillContainer container) {
		if (container == null) {
			return false;
		}

		Skill skill = container.getSkill();
		return skill instanceof PhantomAscentSkill
				|| skill != null && "epicfight:phantom_ascent".equals(String.valueOf(skill.getRegistryName()));
	}

}
