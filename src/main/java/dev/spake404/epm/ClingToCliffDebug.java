package dev.spake404.epm;

import java.util.Locale;
import java.util.WeakHashMap;

import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.HorizontalWallRun;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class ClingToCliffDebug {
	private static final WeakHashMap<Player, Integer> LAST_CAN_START_LOG_TICK = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_CAN_CONTINUE_LOG_TICK = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> LAST_CLIMB_UP_CAN_START_LOG_TICK = new WeakHashMap<>();
	private static final int CAN_START_LOG_INTERVAL_TICKS = 5;
	private static final int CAN_CONTINUE_LOG_INTERVAL_TICKS = 10;
	private static final int CLIMB_UP_CAN_START_LOG_INTERVAL_TICKS = 2;

	private ClingToCliffDebug() {
	}

	public static void logActionEvent(String phase, Player player, Action action) {
		if (!shouldInspect(player) || !isTrackedAction(action)) {
			return;
		}

		EPM.LOGGER.info("[ClingToCliffDebug] action={}, phase={}, tick={}, {}",
				actionName(action),
				phase,
				Integer.valueOf(player.tickCount),
				context(player, Parkourability.get(player), null, false));
	}

	public static void logCanStart(Player player, Parkourability parkourability, IStamina stamina, boolean grabDown, boolean result) {
		if (!shouldInspect(player) || (!grabDown && !result)) {
			return;
		}

		if (!shouldLog(player, LAST_CAN_START_LOG_TICK, CAN_START_LOG_INTERVAL_TICKS, result)) {
			return;
		}

		EPM.LOGGER.info("[ClingToCliffDebug] canStart={}, tick={}, {}",
				Boolean.valueOf(result),
				Integer.valueOf(player.tickCount),
				context(player, parkourability, stamina, grabDown));
		logGrabbableWallProbe(player, "canStart");
	}

	public static void logCanContinue(Player player, Parkourability parkourability, IStamina stamina, boolean grabDown, boolean result) {
		if (!shouldInspect(player)) {
			return;
		}

		if (!shouldLog(player, LAST_CAN_CONTINUE_LOG_TICK, CAN_CONTINUE_LOG_INTERVAL_TICKS, !result)) {
			return;
		}

		EPM.LOGGER.info("[ClingToCliffDebug] canContinue={}, tick={}, {}",
				Boolean.valueOf(result),
				Integer.valueOf(player.tickCount),
				context(player, parkourability, stamina, grabDown));
	}

	public static void logClimbUpCanStart(Player player, Parkourability parkourability, IStamina stamina, boolean result) {
		if (!shouldInspect(player)) {
			return;
		}

		boolean clingDoing = isDoing(parkourability, ClingToCliff.class);
		if (!result && !clingDoing && !isJumpDown()) {
			return;
		}

		if (!shouldLog(player, LAST_CLIMB_UP_CAN_START_LOG_TICK, CLIMB_UP_CAN_START_LOG_INTERVAL_TICKS, result || isJumpDown())) {
			return;
		}

		EPM.LOGGER.info("[ClingToCliffDebug] climbUpCanStart={}, tick={}, {}",
				Boolean.valueOf(result),
				Integer.valueOf(player.tickCount),
				context(player, parkourability, stamina, false));
		logClimbUpDecision(player, parkourability, result);
	}

	public static void logClingInputTick(Player player) {
		if (!shouldInspect(player)) {
			return;
		}

		Parkourability parkourability = Parkourability.get(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> animation = currentAnimation(playerPatch);
		ResourceLocation animationName = safeRegistryName(animation);
		String path = animationName == null ? "" : animationName.getPath();
		boolean epicParCoolAnimation = animationName != null && "epicparcool".equals(animationName.getNamespace());
		boolean clingMoveAnimation = epicParCoolAnimation && path.startsWith("biped/cling_move_");
		boolean clingPoseAnimation = epicParCoolAnimation && path.startsWith("biped/cling_to_cliff");
		boolean wallSlideAnimation = epicParCoolAnimation && path.startsWith("biped/wall_slide_");
		boolean clingDoing = isDoing(parkourability, ClingToCliff.class);
		boolean jumpPressed = isJumpPressed();
		boolean jumpDown = isJumpDown();
		boolean grabDown = isGrabWallDown();
		boolean leftDown = bool(KeyBindings.isKeyLeftDown());
		boolean rightDown = bool(KeyBindings.isKeyRightDown());
		boolean forwardDown = bool(KeyBindings.isKeyForwardDown());
		boolean backDown = bool(KeyBindings.isKeyBackDown());
		boolean relevant = clingDoing
				|| clingMoveAnimation
				|| clingPoseAnimation
				|| wallSlideAnimation
				|| ((jumpPressed || jumpDown || leftDown || rightDown) && grabDown);
		if (!relevant) {
			return;
		}

		EPM.LOGGER.info("[ClingToCliffDebug] clingInput tick={}, clingDoing={}, clingTick={}, facing={}, climbDoing={}, jumpPressed={}, jumpDown={}, jumpTickDown={}, grabDown={}, grabTickDown={}, leftDown={}, rightDown={}, forwardDown={}, backDown={}, mcLeft={}, mcRight={}, mcForward={}, mcBack={}, efMode={}, efInaction={}, animation={}, clingMoveAnimation={}, clingPoseAnimation={}, wallSlideAnimation={}",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(clingDoing),
				Integer.valueOf(doingTick(parkourability, ClingToCliff.class)),
				clingFacing(parkourability),
				Boolean.valueOf(isDoing(parkourability, ClimbUp.class)),
				Boolean.valueOf(jumpPressed),
				Boolean.valueOf(jumpDown),
				Integer.valueOf(keyTickDown(() -> KeyRecorder.keyJumpState)),
				Boolean.valueOf(grabDown),
				Integer.valueOf(keyTickDown(() -> KeyRecorder.keyGrabWall)),
				Boolean.valueOf(leftDown),
				Boolean.valueOf(rightDown),
				Boolean.valueOf(forwardDown),
				Boolean.valueOf(backDown),
				Boolean.valueOf(minecraftKeyDown("left")),
				Boolean.valueOf(minecraftKeyDown("right")),
				Boolean.valueOf(minecraftKeyDown("forward")),
				Boolean.valueOf(minecraftKeyDown("back")),
				Boolean.valueOf(playerPatch != null && playerPatch.isEpicFightMode()),
				Boolean.valueOf(isEpicFightInaction(playerPatch)),
				animationName == null ? "null" : animationName,
				Boolean.valueOf(clingMoveAnimation),
				Boolean.valueOf(clingPoseAnimation),
				Boolean.valueOf(wallSlideAnimation));
	}

	private static boolean shouldInspect(Player player) {
		return player != null && player.isLocalPlayer() && player.level().isClientSide();
	}

	private static boolean shouldLog(Player player, WeakHashMap<Player, Integer> lastLogTicks, int intervalTicks, boolean force) {
		Integer lastTick = lastLogTicks.get(player);
		if (!force && lastTick != null && player.tickCount - lastTick.intValue() < intervalTicks) {
			return false;
		}

		lastLogTicks.put(player, Integer.valueOf(player.tickCount));
		return true;
	}

	private static boolean isTrackedAction(Action action) {
		return action instanceof ClingToCliff || action instanceof ClimbUp;
	}

	private static String actionName(Action action) {
		return action == null ? "null" : action.getClass().getSimpleName();
	}

	private static String context(Player player, Parkourability parkourability, IStamina stamina, boolean grabDown) {
		Vec3 wall = safeGrabbableWall(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return String.format(Locale.ROOT,
				"grabDown=%s, jumpPressed=%s, jumpDown=%s, exhausted=%s, yVel=%.3f, wall=%s, clingDoing=%s, clingTick=%d, clingFacing=%s, climbDoing=%s, climbTick=%d, hWallRun=%s, efMode=%s, efInaction=%s, efAnimation=%s",
				Boolean.valueOf(grabDown),
				Boolean.valueOf(isJumpPressed()),
				Boolean.valueOf(isJumpDown()),
				Boolean.valueOf(stamina != null && stamina.isExhausted()),
				Double.valueOf(player.getDeltaMovement().y()),
				formatWall(wall),
				Boolean.valueOf(isDoing(parkourability, ClingToCliff.class)),
				Integer.valueOf(doingTick(parkourability, ClingToCliff.class)),
				clingFacing(parkourability),
				Boolean.valueOf(isDoing(parkourability, ClimbUp.class)),
				Integer.valueOf(doingTick(parkourability, ClimbUp.class)),
				Boolean.valueOf(isDoing(parkourability, HorizontalWallRun.class)),
				Boolean.valueOf(playerPatch != null && playerPatch.isEpicFightMode()),
				Boolean.valueOf(isEpicFightInaction(playerPatch)),
				currentAnimationName(playerPatch));
	}

	private static Vec3 safeGrabbableWall(Player player) {
		try {
			return WorldUtil.getGrabbableWall(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isJumpPressed() {
		try {
			return KeyRecorder.keyJumpState.isPressed();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isJumpDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
	}

	private static boolean isGrabWallDown() {
		try {
			return KeyBindings.getKeyGrabWall().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean bool(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	private static int keyTickDown(KeyStateSupplier supplier) {
		try {
			KeyRecorder.KeyState keyState = supplier.get();
			return keyState == null ? -1 : keyState.getTickKeyDown();
		} catch (RuntimeException | LinkageError ignored) {
			return -1;
		}
	}

	private static boolean minecraftKeyDown(String key) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null) {
			return false;
		}

		return switch (key) {
			case "left" -> minecraft.options.keyLeft.isDown();
			case "right" -> minecraft.options.keyRight.isDown();
			case "forward" -> minecraft.options.keyUp.isDown();
			case "back" -> minecraft.options.keyDown.isDown();
			default -> false;
		};
	}

	@FunctionalInterface
	private interface KeyStateSupplier {
		KeyRecorder.KeyState get();
	}

	private static void logClimbUpDecision(Player player, Parkourability parkourability, boolean originalResult) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> animation = currentAnimation(playerPatch);
		ResourceLocation animationName = safeRegistryName(animation);
		String path = animationName == null ? "" : animationName.getPath();
		boolean epicParCoolAnimation = animationName != null && "epicparcool".equals(animationName.getNamespace());
		boolean clingMoveAnimation = epicParCoolAnimation && path.startsWith("biped/cling_move_");
		boolean sideOrCornerClingPose = epicParCoolAnimation && isEpicParCoolSideOrCornerClingPose(path);
		boolean anyEpicParCoolSideCling = clingMoveAnimation || sideOrCornerClingPose;
		boolean clingDoing = isDoing(parkourability, ClingToCliff.class);
		int clingTick = doingTick(parkourability, ClingToCliff.class);
		String facing = clingFacing(parkourability);
		boolean facingToWall = "ToWall".equals(facing);
		boolean jumpPressed = isJumpPressed();
		boolean jumpDown = isJumpDown();
		boolean originalExpected = clingDoing && clingTick > 2 && facingToWall && jumpPressed;
		boolean sideClingCandidate = clingDoing && clingTick > 2 && (jumpPressed || jumpDown) && anyEpicParCoolSideCling;

		EPM.LOGGER.info("[ClingToCliffDebug] climbUpDecision tick={}, originalResult={}, originalExpected={}, originalBlocker={}, sideClingCandidate={}, clingDoing={}, clingTick={}, facing={}, jumpPressed={}, jumpDown={}, efMode={}, efInaction={}, animation={}, epicParCoolSideCling={}, clingMoveAnimation={}, sideOrCornerClingPose={}",
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(originalResult),
				Boolean.valueOf(originalExpected),
				climbUpOriginalBlocker(clingDoing, clingTick, facingToWall, jumpPressed),
				Boolean.valueOf(sideClingCandidate),
				Boolean.valueOf(clingDoing),
				Integer.valueOf(clingTick),
				facing,
				Boolean.valueOf(jumpPressed),
				Boolean.valueOf(jumpDown),
				Boolean.valueOf(playerPatch != null && playerPatch.isEpicFightMode()),
				Boolean.valueOf(isEpicFightInaction(playerPatch)),
				animationName == null ? "null" : animationName,
				Boolean.valueOf(anyEpicParCoolSideCling),
				Boolean.valueOf(clingMoveAnimation),
				Boolean.valueOf(sideOrCornerClingPose));
	}

	private static String climbUpOriginalBlocker(boolean clingDoing, int clingTick, boolean facingToWall, boolean jumpPressed) {
		if (!clingDoing) {
			return "clingNotDoing";
		}
		if (clingTick <= 2) {
			return "clingTickTooLow";
		}
		if (!facingToWall) {
			return "facingNotToWall";
		}
		if (!jumpPressed) {
			return "jumpNotPressed";
		}
		return "none";
	}

	private static boolean isEpicParCoolSideOrCornerClingPose(String path) {
		return "biped/cling_to_cliff_left".equals(path)
				|| "biped/cling_to_cliff_right".equals(path)
				|| "biped/cling_to_cliff_inner_corner".equals(path)
				|| "biped/cling_to_cliff_outer_corner".equals(path);
	}

	private static void logGrabbableWallProbe(Player player, String source) {
		try {
			double distance = player.getBbWidth() / 2.0D;
			double firstYOffset = player.getEyeHeight() + (player.getBbHeight() - player.getEyeHeight()) / 2.0D;
			double secondYOffset = player.getBbHeight() + (player.getBbHeight() - player.getEyeHeight()) / 2.0D;
			AABB box = player.getBoundingBox();
			Vec3 pos = player.position();
			EPM.LOGGER.info("[ClingToCliffDebug] wallProbe source={}, tick={}, pos=({},{},{}), bbMinY={}, bbMaxY={}, width={}, height={}, eye={}, distance={}, firstYOffset={}, secondYOffset={}",
					source,
					Integer.valueOf(player.tickCount),
					formatDouble(pos.x()),
					formatDouble(pos.y()),
					formatDouble(pos.z()),
					formatDouble(box.minY),
					formatDouble(box.maxY),
					formatDouble(player.getBbWidth()),
					formatDouble(player.getBbHeight()),
					formatDouble(player.getEyeHeight()),
					formatDouble(distance),
					formatDouble(firstYOffset),
					formatDouble(secondYOffset));
			logGrabbableWallProbeAt(player, distance, firstYOffset, "first");
			logGrabbableWallProbeAt(player, distance, secondYOffset, "second");
		} catch (RuntimeException | LinkageError exception) {
			EPM.LOGGER.info("[ClingToCliffDebug] wallProbe source={}, tick={}, failed={}",
					source,
					Integer.valueOf(player.tickCount),
					exception.getClass().getSimpleName());
		}
	}

	private static void logGrabbableWallProbeAt(LivingEntity entity, double distance, double yOffset, String pass) {
		double margin = entity.getBbWidth() * 0.49D;
		Level level = entity.level();
		Vec3 pos = entity.position();
		AABB lower = new AABB(
				pos.x() - margin,
				pos.y() + yOffset - entity.getBbHeight() / 6.0D,
				pos.z() - margin,
				pos.x() + margin,
				pos.y() + yOffset,
				pos.z() + margin);
		AABB upper = new AABB(
				pos.x() - margin,
				pos.y() + yOffset,
				pos.z() - margin,
				pos.x() + margin,
				pos.y() + entity.getBbHeight(),
				pos.z() + margin);
		int xResult = 0;
		int zResult = 0;
		xResult += logDirectionProbe(entity, level, lower, upper, distance, yOffset, pass, 1, 0) ? 1 : 0;
		xResult -= logDirectionProbe(entity, level, lower, upper, distance, yOffset, pass, -1, 0) ? 1 : 0;
		zResult += logDirectionProbe(entity, level, lower, upper, distance, yOffset, pass, 0, 1) ? 1 : 0;
		zResult -= logDirectionProbe(entity, level, lower, upper, distance, yOffset, pass, 0, -1) ? 1 : 0;

		if (xResult == 0 && zResult == 0) {
			EPM.LOGGER.info("[ClingToCliffDebug] wallProbe pass={}, yOffset={}, result=null, reason=noDirectionMatched",
					pass,
					formatDouble(yOffset));
			return;
		}

		Float friction = resolveProbeFriction(entity, level, yOffset, xResult, zResult);
		EPM.LOGGER.info("[ClingToCliffDebug] wallProbe pass={}, yOffset={}, result=({},{}), friction={}, accepted={}",
				pass,
				formatDouble(yOffset),
				Integer.valueOf(xResult),
				Integer.valueOf(zResult),
				friction == null ? "unloaded" : formatDouble(friction.doubleValue()),
				Boolean.valueOf(friction != null && friction.floatValue() <= 0.9F));
	}

	private static boolean logDirectionProbe(LivingEntity entity, Level level, AABB lower, AABB upper, double distance, double yOffset, String pass, int xDirection, int zDirection) {
		AABB lowerExpanded = lower.expandTowards(distance * xDirection, 0.0D, distance * zDirection);
		AABB upperExpanded = upper.expandTowards(distance * xDirection, 0.0D, distance * zDirection);
		boolean lowerHitsWall = !level.noCollision(entity, lowerExpanded);
		boolean upperClear = level.noCollision(entity, upperExpanded);
		boolean matched = lowerHitsWall && upperClear;
		BlockPos samplePos = sampleBlockPos(entity, yOffset, xDirection, zDirection);
		EPM.LOGGER.info("[ClingToCliffDebug] wallProbe pass={}, dir=({},{}), yOffset={}, lowerHitsWall={}, upperClear={}, matched={}, samplePos={}, sampleBlock={}",
				pass,
				Integer.valueOf(xDirection),
				Integer.valueOf(zDirection),
				formatDouble(yOffset),
				Boolean.valueOf(lowerHitsWall),
				Boolean.valueOf(upperClear),
				Boolean.valueOf(matched),
				formatBlockPos(samplePos),
				formatBlockState(level, samplePos));
		return matched;
	}

	private static Float resolveProbeFriction(LivingEntity entity, Level level, double yOffset, int xResult, int zResult) {
		if (xResult != 0 && zResult != 0) {
			BlockPos xPos = new BlockPos(
					Mth.floor(entity.getX() + xResult),
					Mth.floor(entity.getBoundingBox().minY + yOffset - 0.3D),
					Mth.floor(entity.getZ()));
			BlockPos zPos = new BlockPos(
					Mth.floor(entity.getX()),
					Mth.floor(entity.getBoundingBox().minY + yOffset - 0.3D),
					Mth.floor(entity.getZ() + zResult));
			if (!level.isLoaded(xPos) || !level.isLoaded(zPos)) {
				return null;
			}

			float xFriction = level.getBlockState(xPos).getFriction(level, xPos, entity);
			float zFriction = level.getBlockState(zPos).getFriction(level, zPos, entity);
			return Float.valueOf(Math.min(xFriction, zFriction));
		}

		BlockPos samplePos = sampleBlockPos(entity, yOffset, xResult, zResult);
		if (!level.isLoaded(samplePos)) {
			return null;
		}

		return Float.valueOf(level.getBlockState(samplePos).getFriction(level, samplePos, entity));
	}

	private static BlockPos sampleBlockPos(LivingEntity entity, double yOffset, int xResult, int zResult) {
		double x = entity.getX() + xResult;
		double z = entity.getZ() + zResult;
		int y = Mth.floor(entity.getBoundingBox().minY + yOffset - 0.3D);
		BlockPos pos = new BlockPos(Mth.floor(x), y, Mth.floor(z));
		if (xResult == 0 || zResult == 0) {
			Level level = entity.level();
			if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.AIR)) {
				if (xResult != 0) {
					z += Math.signum(z - Math.floor(z) - 0.5D);
				} else {
					x += Math.signum(x - Math.floor(x) - 0.5D);
				}

				pos = new BlockPos(Mth.floor(x), y, Mth.floor(z));
			}
		}

		return pos;
	}

	private static String formatBlockPos(BlockPos pos) {
		return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
	}

	private static String formatBlockState(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return "unloaded";
		}

		BlockState state = level.getBlockState(pos);
		return String.valueOf(state.getBlock());
	}

	private static String formatWall(Vec3 wall) {
		if (wall == null) {
			return "null";
		}

		return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", wall.x(), wall.y(), wall.z());
	}

	private static String formatDouble(double value) {
		return String.format(Locale.ROOT, "%.3f", Double.valueOf(value));
	}

	private static <T extends Action> boolean isDoing(Parkourability parkourability, Class<T> actionClass) {
		Action action = action(parkourability, actionClass);
		return action != null && action.isDoing();
	}

	private static <T extends Action> int doingTick(Parkourability parkourability, Class<T> actionClass) {
		Action action = action(parkourability, actionClass);
		return action == null ? -1 : action.getDoingTick();
	}

	private static String clingFacing(Parkourability parkourability) {
		Action action = action(parkourability, ClingToCliff.class);
		if (action instanceof ClingToCliff clingToCliff) {
			try {
				return String.valueOf(clingToCliff.getFacingDirection());
			} catch (RuntimeException | LinkageError ignored) {
				return "unavailable";
			}
		}

		return "null";
	}

	private static <T extends Action> Action action(Parkourability parkourability, Class<T> actionClass) {
		try {
			return parkourability == null ? null : parkourability.get(actionClass);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isEpicFightInaction(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch != null && playerPatch.getEntityState().inaction();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static String currentAnimationName(PlayerPatch<?> playerPatch) {
		AssetAccessor<?> animation = currentAnimation(playerPatch);
		ResourceLocation registryName = safeRegistryName(animation);
		return registryName == null ? "null" : String.valueOf(registryName);
	}

	private static AssetAccessor<?> currentAnimation(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return null;
		}

		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static ResourceLocation safeRegistryName(AssetAccessor<?> animation) {
		if (animation == null) {
			return null;
		}

		try {
			return animation.registryName();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}
}
