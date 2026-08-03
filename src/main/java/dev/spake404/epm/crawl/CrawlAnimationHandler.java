package dev.spake404.epm.crawl;

import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.capability.Parkourability;
import dev.spake404.epm.EPMParCoolGate;
import dev.spake404.epm.animation.EpmAnimations;
import dev.spake404.epm.animation.EpmLivingMotions;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.property.AnimationParameters;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.WeakHashMap;

public final class CrawlAnimationHandler {
	private static final double MOVEMENT_EPSILON_SQR = 1.0E-4D;
	private static final WeakHashMap<Player, CrawlState> STATES = new WeakHashMap<>();

	private CrawlAnimationHandler() {
	}

	public static void registerAnimations(InitAnimatorEvent event) {
		if (!(event.getEntityPatch() instanceof PlayerPatch<?>)) {
			return;
		}

		addLivingAnimation(event, EpmLivingMotions.CRAWL_ENTER, EpmAnimations.crawlEnter());
		addLivingAnimation(event, EpmLivingMotions.CRAWL_IDLE, EpmAnimations.crawlIdle());
		addLivingAnimation(event, EpmLivingMotions.CRAWL_MOVE_LEFT, EpmAnimations.crawlMoveLeft());
		addLivingAnimation(event, EpmLivingMotions.CRAWL_MOVE_RIGHT, EpmAnimations.crawlMoveRight());
		addLivingAnimation(event, EpmLivingMotions.CRAWL_EXIT, EpmAnimations.crawlExit());
	}

	public static void chooseAnimation(UpdatePlayerMotionEvent.BaseLayer event) {
		PlayerPatch<?> playerPatch = event.getPlayerPatch();
		Player player = playerPatch == null ? null : playerPatch.getOriginal();
		if (player == null) {
			return;
		}

		if (!EPMParCoolGate.allowCrossModSkillCompat() || !playerPatch.isEpicFightMode()) {
			STATES.remove(player);
			return;
		}

		boolean crawling = isCrawling(player);
		CrawlState state = STATES.get(player);

		if (!crawling) {
			if (state == null) {
				return;
			}

			if (event.inaction()) {
				STATES.remove(player);
				return;
			}

			state.phase = Phase.EXIT;
			event.setMotion(EpmLivingMotions.CRAWL_EXIT);
			return;
		}

		if (state == null || state.phase == Phase.EXIT) {
			state = new CrawlState();
			STATES.put(player, state);
		}

		if (event.inaction()) {
			return;
		}

		if (state.phase != Phase.ENTER) {
			boolean moving = isMoving(player);
			if (!moving) {
				state.phase = Phase.IDLE;
			} else if (state.phase == Phase.IDLE) {
				state.phase = state.nextMove;
			}
		}

		event.setMotion(motionFor(state.phase));
	}

	public static void onAnimationEnd(
			LivingEntityPatch<?> entityPatch,
			AssetAccessor<?> animation,
			AnimationParameters parameters) {
		if (entityPatch == null || !(entityPatch.getOriginal() instanceof Player player)) {
			return;
		}

		if (parameters == null || !Boolean.TRUE.equals(parameters.first())) {
			return;
		}

		CrawlState state = STATES.get(player);
		if (state == null) {
			return;
		}

		if (sameAnimation(animation, EpmAnimations.crawlExit())) {
			STATES.remove(player);
			return;
		}

		boolean crawling = isCrawling(player);
		if (!crawling) {
			state.phase = Phase.EXIT;
			return;
		}

		if (sameAnimation(animation, EpmAnimations.crawlEnter()) && state.phase == Phase.ENTER) {
			state.phase = isMoving(player) ? state.nextMove : Phase.IDLE;
			return;
		}

		if (sameAnimation(animation, EpmAnimations.crawlMoveLeft()) && state.phase == Phase.MOVE_LEFT) {
			state.nextMove = Phase.MOVE_RIGHT;
			state.phase = isMoving(player) ? Phase.MOVE_RIGHT : Phase.IDLE;
			return;
		}

		if (sameAnimation(animation, EpmAnimations.crawlMoveRight()) && state.phase == Phase.MOVE_RIGHT) {
			state.nextMove = Phase.MOVE_LEFT;
			state.phase = isMoving(player) ? Phase.MOVE_LEFT : Phase.IDLE;
		}
	}

	private static void addLivingAnimation(
			InitAnimatorEvent event,
			EpmLivingMotions motion,
			AssetAccessor<? extends StaticAnimation> animation) {
		if (animation != null) {
			event.getAnimator().addLivingAnimation(motion, animation);
		}
	}

	private static boolean isCrawling(Player player) {
		Parkourability parkourability = Parkourability.get(player);
		return parkourability != null && parkourability.get(Crawl.class).isDoing();
	}

	private static boolean isMoving(Player player) {
		if (player instanceof LocalPlayer localPlayer
				&& localPlayer.input != null
				&& (Math.abs(localPlayer.input.leftImpulse) > 0.01F
				|| Math.abs(localPlayer.input.forwardImpulse) > 0.01F)) {
			return true;
		}

		if (player.walkAnimation.speed() > 0.01F) {
			return true;
		}

		double deltaX = player.getX() - player.xOld;
		double deltaZ = player.getZ() - player.zOld;
		return deltaX * deltaX + deltaZ * deltaZ > MOVEMENT_EPSILON_SQR
				|| player.getDeltaMovement().horizontalDistanceSqr() > MOVEMENT_EPSILON_SQR;
	}

	private static EpmLivingMotions motionFor(Phase phase) {
		return switch (phase) {
			case ENTER -> EpmLivingMotions.CRAWL_ENTER;
			case IDLE -> EpmLivingMotions.CRAWL_IDLE;
			case MOVE_LEFT -> EpmLivingMotions.CRAWL_MOVE_LEFT;
			case MOVE_RIGHT -> EpmLivingMotions.CRAWL_MOVE_RIGHT;
			case EXIT -> EpmLivingMotions.CRAWL_EXIT;
		};
	}

	private static boolean sameAnimation(AssetAccessor<?> first, AssetAccessor<?> second) {
		return first == second || first != null && first.equals(second);
	}

	private enum Phase {
		ENTER,
		IDLE,
		MOVE_LEFT,
		MOVE_RIGHT,
		EXIT
	}

	private static final class CrawlState {
		private Phase phase = Phase.ENTER;
		private Phase nextMove = Phase.MOVE_LEFT;
	}
}
