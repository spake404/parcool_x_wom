package dev.spake404.epm.vault;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import java.util.Locale;
import java.util.WeakHashMap;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class VaultDebug {
	private static final int CAN_START_PERIODIC_TICKS = 5;
	private static final int POST_VAULT_TRACE_TICKS = 12;
	private static final double TOP_CLEARANCE_SCAN_DISTANCE = 1.5D;
	private static final double ANGLE_THRESHOLD = 0.707106D;
	private static final WeakHashMap<Player, Integer> CAN_START_ATTEMPTS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> POST_VAULT_TRACE = new WeakHashMap<>();

	private VaultDebug() {
	}

	public static void logCanStart(Player player, Parkourability parkourability, IStamina stamina, boolean result) {
		if (!isEnabledFor(player)) {
			return;
		}

		VaultScan scan = scanVaultableStep(player);
		Vec3 actualStep = safeVaultableStep(player);
		VaultAngle angle = computeAngle(player, actualStep);
		double wallHeight = safeWallHeight(player);
		boolean exhausted = isExhausted(stamina);
		boolean inWater = player.isInWater();
		boolean keyNeeded = safeVaultKeyNeeded();
		boolean keyDown = isVaultKeyDown();
		boolean fastRunCanAct = canFastRunActWithRunning(parkourability, player);
		boolean fastRunDoing = isFastRunDoing(parkourability);
		boolean enableInAir = safeEnableVaultInAir();
		boolean onGround = player.onGround();
		boolean wallHeightPass = wallHeight > player.getBbHeight() * 0.44D;
		String reason = canStartReason(scan, actualStep, angle, exhausted, inWater, keyNeeded, keyDown,
				fastRunCanAct, onGround, enableInAir, wallHeightPass);
		String failedChecks = failedChecks(scan, actualStep, angle, exhausted, inWater, keyNeeded, keyDown,
				fastRunCanAct, onGround, enableInAir, wallHeightPass);

		boolean interest = result
				|| keyDown
				|| actualStep != null
				|| scan.hasLowerBlocker()
				|| isVaultDoing(parkourability)
				|| POST_VAULT_TRACE.containsKey(player);
		if (!interest) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		String animation = animationName(AnimationQuery.currentAnimation(playerPatch));
		Vec3 pos = player.position();
		Vec3 movement = player.getDeltaMovement();
		Vault vault = safeVault(parkourability);
		EPM.LOGGER.info(
				"[EPM/VaultDebug] canStart attempt={} tick={} result={} reason={} failedChecks={} scanReason={} actualStep={} scanStep={} angleForward={} angleSide={} anglePass={} wallHeight={} wallHeightPass={} exhausted={} inWater={} keyNeeded={} keyDown={} fastRunCanAct={} fastRunDoing={} fastRunKeyDown={} fastRunControlKeyDown={} vaultFastRunHold={} vaultFastRunGraceTicks={} onGround={} enableInAir={} shiftDown={} inWaterOrBubble={} fallFlying={} vehicle={} vaultDoing={} vaultTick={} vaultAnimation={} efMode={} inaction={} baseAnimation={} movementKeys={} pos=({}, {}, {}) delta=({}, {}, {}) scanXp={} scanXn={} scanZp={} scanZn={} target={}",
				Integer.valueOf(nextCanStartAttempt(player)),
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(result),
				reason,
				failedChecks,
				scan.reason(),
				formatVec(actualStep),
				formatVec(scan.step()),
				formatDouble(angle.forward()),
				formatDouble(angle.side()),
				Boolean.valueOf(angle.pass()),
				formatDouble(wallHeight),
				Boolean.valueOf(wallHeightPass),
				Boolean.valueOf(exhausted),
				Boolean.valueOf(inWater),
				Boolean.valueOf(keyNeeded),
				Boolean.valueOf(keyDown),
				Boolean.valueOf(fastRunCanAct),
				Boolean.valueOf(fastRunDoing),
				Boolean.valueOf(isParCoolFastRunKeyDown()),
				Boolean.valueOf(isParCoolFastRunControlKeyDown()),
				Boolean.valueOf(EPMClientHooks.vaultFastRunHoldForDebug(player)),
				Integer.valueOf(EPMClientHooks.vaultFastRunGraceTicksForDebug(player)),
				Boolean.valueOf(onGround),
				Boolean.valueOf(enableInAir),
				Boolean.valueOf(player.isShiftKeyDown()),
				Boolean.valueOf(player.isInWaterOrBubble()),
				Boolean.valueOf(player.isFallFlying()),
				Boolean.valueOf(player.getVehicle() != null),
				Boolean.valueOf(vault != null && vault.isDoing()),
				Integer.valueOf(vault == null ? -1 : vault.getDoingTick()),
				vault == null ? "none" : String.valueOf(vault.getCurrentAnimation()),
				Boolean.valueOf(playerPatch != null && playerPatch.isEpicFightMode()),
				Boolean.valueOf(entityStateInaction(playerPatch)),
				animation,
				movementKeys(),
				formatDouble(pos.x()),
				formatDouble(pos.y()),
				formatDouble(pos.z()),
				formatDouble(movement.x()),
				formatDouble(movement.y()),
				formatDouble(movement.z()),
				scan.xPositive().format(),
				scan.xNegative().format(),
				scan.zPositive().format(),
				scan.zNegative().format(),
				scan.target().format());
	}

	public static void logStart(String phase, Vault vault, Player player, Parkourability parkourability, IStamina stamina) {
		if (!isEnabledFor(player)) {
			return;
		}

		logVaultState(phase, vault, player, parkourability, stamina);
	}

	public static void logWorkingTick(Vault vault, Player player, Parkourability parkourability, IStamina stamina) {
		if (!isEnabledFor(player)) {
			return;
		}

		logVaultState("working_tick", vault, player, parkourability, stamina);
	}

	public static void logCanContinue(Vault vault, Player player, Parkourability parkourability, IStamina stamina, boolean result) {
		if (!isEnabledFor(player) || result && player.tickCount % CAN_START_PERIODIC_TICKS != 0) {
			return;
		}

		logVaultState("canContinue_" + result, vault, player, parkourability, stamina);
	}

	public static void logStop(Vault vault, Player player) {
		if (!isEnabledFor(player)) {
			return;
		}

		logVaultState("stop_local", vault, player, Parkourability.get(player), null);
		POST_VAULT_TRACE.put(player, Integer.valueOf(POST_VAULT_TRACE_TICKS));
	}

	public static void tickPostVaultTrace(Player player) {
		if (!isEnabledFor(player)) {
			POST_VAULT_TRACE.remove(player);
			return;
		}

		Integer ticks = POST_VAULT_TRACE.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() <= 0) {
			POST_VAULT_TRACE.remove(player);
			return;
		}

		logVaultState("post_tick", null, player, Parkourability.get(player), null);
		POST_VAULT_TRACE.put(player, Integer.valueOf(ticks.intValue() - 1));
	}

	private static void logVaultState(String phase, Vault vault, Player player, Parkourability parkourability, IStamina stamina) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> currentAnimation = AnimationQuery.currentAnimation(playerPatch);
		Vault currentVault = vault != null ? vault : safeVault(parkourability);
		Vec3 pos = player.position();
		Vec3 movement = player.getDeltaMovement();
		VaultScan scan = scanVaultableStep(player);
		Vec3 actualStep = safeVaultableStep(player);
		double wallHeight = safeWallHeight(player);
		boolean fastRunCanAct = canFastRunActWithRunning(parkourability, player);
		boolean fastRunDoing = isFastRunDoing(parkourability);
		boolean exhausted = stamina != null && isExhausted(stamina);
		EPM.LOGGER.info(
				"[EPM/VaultDebug] phase={} tick={} vaultDoing={} vaultTick={} vaultAnimation={} actualStep={} scanStep={} scanReason={} wallHeight={} fastRunCanAct={} fastRunDoing={} fastRunKeyDown={} fastRunControlKeyDown={} vaultFastRunHold={} vaultFastRunGraceTicks={} exhausted={} sprinting={} onGround={} inWater={} shiftDown={} inWaterOrBubble={} fallFlying={} vehicle={} keyDown={} efMode={} inaction={} baseAnimation={} movementKeys={} pos=({}, {}, {}) delta=({}, {}, {}) scanXp={} scanXn={} scanZp={} scanZn={} target={}",
				phase,
				Integer.valueOf(player.tickCount),
				Boolean.valueOf(currentVault != null && currentVault.isDoing()),
				Integer.valueOf(currentVault == null ? -1 : currentVault.getDoingTick()),
				currentVault == null ? "none" : String.valueOf(currentVault.getCurrentAnimation()),
				formatVec(actualStep),
				formatVec(scan.step()),
				scan.reason(),
				formatDouble(wallHeight),
				Boolean.valueOf(fastRunCanAct),
				Boolean.valueOf(fastRunDoing),
				Boolean.valueOf(isParCoolFastRunKeyDown()),
				Boolean.valueOf(isParCoolFastRunControlKeyDown()),
				Boolean.valueOf(EPMClientHooks.vaultFastRunHoldForDebug(player)),
				Integer.valueOf(EPMClientHooks.vaultFastRunGraceTicksForDebug(player)),
				Boolean.valueOf(exhausted),
				Boolean.valueOf(player.isSprinting()),
				Boolean.valueOf(player.onGround()),
				Boolean.valueOf(player.isInWater()),
				Boolean.valueOf(player.isShiftKeyDown()),
				Boolean.valueOf(player.isInWaterOrBubble()),
				Boolean.valueOf(player.isFallFlying()),
				Boolean.valueOf(player.getVehicle() != null),
				Boolean.valueOf(isVaultKeyDown()),
				Boolean.valueOf(playerPatch != null && playerPatch.isEpicFightMode()),
				Boolean.valueOf(entityStateInaction(playerPatch)),
				animationName(currentAnimation),
				movementKeys(),
				formatDouble(pos.x()),
				formatDouble(pos.y()),
				formatDouble(pos.z()),
				formatDouble(movement.x()),
				formatDouble(movement.y()),
				formatDouble(movement.z()),
				scan.xPositive().format(),
				scan.xNegative().format(),
				scan.zPositive().format(),
				scan.zNegative().format(),
				scan.target().format());
	}

	private static String canStartReason(VaultScan scan, Vec3 actualStep, VaultAngle angle, boolean exhausted,
			boolean inWater, boolean keyNeeded, boolean keyDown, boolean fastRunCanAct, boolean onGround,
			boolean enableInAir, boolean wallHeightPass) {
		if (actualStep == null) {
			return scan.step() == null ? scan.reason() : "world_util_step_null_scan_mismatch";
		}
		if (!angle.pass()) {
			return "look_angle";
		}
		if (exhausted) {
			return "stamina_exhausted";
		}
		if (inWater) {
			return "in_water";
		}
		if (keyNeeded && !keyDown) {
			return "vault_key_not_down";
		}
		if (!fastRunCanAct) {
			return "fast_run_can_act_false";
		}
		if (!onGround && !enableInAir) {
			return "air_disabled";
		}
		if (!wallHeightPass) {
			return "wall_height_too_low";
		}
		return "ok";
	}

	private static String failedChecks(VaultScan scan, Vec3 actualStep, VaultAngle angle, boolean exhausted,
			boolean inWater, boolean keyNeeded, boolean keyDown, boolean fastRunCanAct, boolean onGround,
			boolean enableInAir, boolean wallHeightPass) {
		StringBuilder checks = new StringBuilder();
		appendFailedCheck(checks, actualStep == null, scan.step() == null ? scan.reason() : "world_util_step_null_scan_mismatch");
		appendFailedCheck(checks, actualStep != null && !angle.pass(), "look_angle");
		appendFailedCheck(checks, exhausted, "stamina_exhausted");
		appendFailedCheck(checks, inWater, "in_water");
		appendFailedCheck(checks, keyNeeded && !keyDown, "vault_key_not_down");
		appendFailedCheck(checks, !fastRunCanAct, "fast_run_can_act_false");
		appendFailedCheck(checks, !onGround && !enableInAir, "air_disabled");
		appendFailedCheck(checks, !wallHeightPass, "wall_height_too_low");
		return checks.length() == 0 ? "none" : checks.toString();
	}

	private static void appendFailedCheck(StringBuilder checks, boolean failed, String check) {
		if (!failed) {
			return;
		}

		if (checks.length() > 0) {
			checks.append(',');
		}
		checks.append(check);
	}

	private static VaultScan scanVaultableStep(LivingEntity entity) {
		try {
			double halfWidth = entity.getBbWidth() * 0.5D;
			Level level = entity.level();
			double distance = entity.getBbWidth() / 2.0F;
			double wallHeight = safeWallHeight(entity);
			double baseLine = Math.min(entity.getBbHeight() * EPMConfig.vaultHeightScale(), wallHeight);
			Vec3 pos = entity.position();
			AABB baseBoxBottom = new AABB(
					pos.x() - halfWidth,
					pos.y(),
					pos.z() - halfWidth,
					pos.x() + halfWidth,
					pos.y() + baseLine,
					pos.z() + halfWidth);
			AABB baseBoxTop = new AABB(
					pos.x() - halfWidth,
					pos.y() + baseLine,
					pos.z() - halfWidth,
					pos.x() + halfWidth,
					pos.y() + baseLine + entity.getBbHeight(),
					pos.z() + halfWidth);

			DirectionScan xPositive = directionScan(entity, level, baseBoxBottom, baseBoxTop, distance, distance + TOP_CLEARANCE_SCAN_DISTANCE, 0.0D, 0.0D);
			DirectionScan xNegative = directionScan(entity, level, baseBoxBottom, baseBoxTop, -distance, -(distance + TOP_CLEARANCE_SCAN_DISTANCE), 0.0D, 0.0D);
			DirectionScan zPositive = directionScan(entity, level, baseBoxBottom, baseBoxTop, 0.0D, 0.0D, distance, distance + TOP_CLEARANCE_SCAN_DISTANCE);
			DirectionScan zNegative = directionScan(entity, level, baseBoxBottom, baseBoxTop, 0.0D, 0.0D, -distance, -(distance + TOP_CLEARANCE_SCAN_DISTANCE));

			double stepX = 0.0D;
			double stepZ = 0.0D;
			if (xPositive.candidate()) {
				stepX++;
			}
			if (xNegative.candidate()) {
				stepX--;
			}
			if (zPositive.candidate()) {
				stepZ++;
			}
			if (zNegative.candidate()) {
				stepZ--;
			}

			TargetScan target = TargetScan.none();
			if (stepX == 0.0D && stepZ == 0.0D) {
				return new VaultScan(null, "no_step_candidate", xPositive, xNegative, zPositive, zNegative, target);
			}

			Vec3 step = new Vec3(stepX, 0.0D, stepZ);
			if (stepX == 0.0D || stepZ == 0.0D) {
				target = targetScan(entity, level, step);
				if (!target.loaded()) {
					return new VaultScan(null, "target_not_loaded", xPositive, xNegative, zPositive, zNegative, target);
				}
				if (target.stairBlocked()) {
					return new VaultScan(null, "stair_direction_blocked", xPositive, xNegative, zPositive, zNegative, target);
				}
			}

			return new VaultScan(step, "ok", xPositive, xNegative, zPositive, zNegative, target);
		} catch (RuntimeException | LinkageError exception) {
			DirectionScan failed = DirectionScan.failed(exception);
			return new VaultScan(null, "scan_exception_" + exception.getClass().getSimpleName(), failed, failed, failed, failed, TargetScan.none());
		}
	}

	private static DirectionScan directionScan(LivingEntity entity, Level level, AABB bottom, AABB top,
			double bottomX, double topX, double bottomZ, double topZ) {
		boolean lowerBlocked = !level.noCollision(entity, bottom.move(bottomX, 0.0D, bottomZ));
		boolean topClear = level.noCollision(entity, top.move(topX, 0.0D, topZ));
		return new DirectionScan(lowerBlocked, topClear, lowerBlocked && topClear, "ok");
	}

	private static TargetScan targetScan(LivingEntity entity, Level level, Vec3 step) {
		Vec3 blockPosition = entity.position().add(step).add(0.0D, 0.5D, 0.0D);
		BlockPos target = new BlockPos(Mth.floor(blockPosition.x()), Mth.floor(blockPosition.y()), Mth.floor(blockPosition.z()));
		boolean loaded = level.isLoaded(target);
		if (!loaded) {
			return new TargetScan(target, false, "unloaded", false);
		}

		BlockState state = level.getBlockState(target);
		boolean stairBlocked = false;
		if (state.getBlock() instanceof StairBlock) {
			Half half = state.getValue(StairBlock.HALF);
			if (half == Half.BOTTOM) {
				Direction direction = state.getValue(StairBlock.FACING);
				stairBlocked = step.z() > 0.0D && direction == Direction.SOUTH
						|| step.z() < 0.0D && direction == Direction.NORTH
						|| step.x() > 0.0D && direction == Direction.EAST
						|| step.x() < 0.0D && direction == Direction.WEST;
			}
		}

		return new TargetScan(target, true, blockName(state), stairBlocked);
	}

	private static VaultAngle computeAngle(Player player, Vec3 step) {
		if (step == null) {
			return new VaultAngle(0.0D, 0.0D, false);
		}

		Vec3 look = player.getLookAngle();
		look = new Vec3(look.x(), 0.0D, look.z()).normalize();
		Vec3 normalizedStep = step.normalize();
		Vec3 divided = new Vec3(
				look.x() * normalizedStep.x() + look.z() * normalizedStep.z(),
				0.0D,
				-look.x() * normalizedStep.z() + look.z() * normalizedStep.x()).normalize();
		return new VaultAngle(divided.x(), divided.z(), divided.x() >= ANGLE_THRESHOLD);
	}

	private static Vec3 safeVaultableStep(Player player) {
		try {
			return WorldUtil.getVaultableStep(player);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static double safeWallHeight(LivingEntity entity) {
		try {
			return WorldUtil.getWallHeight(entity);
		} catch (RuntimeException | LinkageError ignored) {
			return 0.0D;
		}
	}

	private static boolean canFastRunActWithRunning(Parkourability parkourability, Player player) {
		try {
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.canActWithRunning(player);
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isFastRunDoing(Parkourability parkourability) {
		try {
			FastRun fastRun = parkourability == null ? null : parkourability.get(FastRun.class);
			return fastRun != null && fastRun.isDoing();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static Vault safeVault(Parkourability parkourability) {
		try {
			return parkourability == null ? null : parkourability.get(Vault.class);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean isVaultDoing(Parkourability parkourability) {
		Vault vault = safeVault(parkourability);
		return vault != null && vault.isDoing();
	}

	private static boolean isExhausted(IStamina stamina) {
		try {
			return stamina != null && stamina.isExhausted();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean safeVaultKeyNeeded() {
		try {
			return ParCoolConfig.Client.Booleans.VaultKeyPressedNeeded.get();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean safeEnableVaultInAir() {
		try {
			return ParCoolConfig.Client.Booleans.EnableVaultInAir.get();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isVaultKeyDown() {
		try {
			return KeyBindings.getKeyVault().isDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolFastRunKeyDown() {
		try {
			return EPMClientHooks.isFastRunKeyDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isParCoolFastRunControlKeyDown() {
		try {
			return EPMClientHooks.isFastRunControlKeyDown();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static int nextCanStartAttempt(Player player) {
		Integer previous = CAN_START_ATTEMPTS.get(player);
		int next = previous == null ? 1 : previous.intValue() + 1;
		CAN_START_ATTEMPTS.put(player, Integer.valueOf(next));
		return next;
	}

	private static String movementKeys() {
		try {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft == null || minecraft.options == null) {
				return "unknown";
			}

			StringBuilder keys = new StringBuilder();
			appendKey(keys, minecraft.options.keyUp.isDown(), "forward");
			appendKey(keys, minecraft.options.keyDown.isDown(), "back");
			appendKey(keys, minecraft.options.keyLeft.isDown(), "left");
			appendKey(keys, minecraft.options.keyRight.isDown(), "right");
			appendKey(keys, minecraft.options.keyJump.isDown(), "jump");
			appendKey(keys, minecraft.options.keyShift.isDown(), "shift");
			return keys.length() == 0 ? "none" : keys.toString();
		} catch (RuntimeException | LinkageError ignored) {
			return "unknown";
		}
	}

	private static void appendKey(StringBuilder keys, boolean down, String key) {
		if (!down) {
			return;
		}

		if (keys.length() > 0) {
			keys.append(',');
		}
		keys.append(key);
	}

	private static boolean entityStateInaction(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch != null && playerPatch.getEntityState() != null && playerPatch.getEntityState().inaction();
		} catch (RuntimeException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean isEnabledFor(Player player) {
		return EPMConfig.debugVaultState()
				&& player != null
				&& player.isLocalPlayer()
				&& player.level().isClientSide();
	}

	private static String animationName(AssetAccessor<?> animation) {
		ResourceLocation registryName = AnimationQuery.safeRegistryName(animation);
		return registryName == null ? String.valueOf(animation) : registryName.toString();
	}

	private static String blockName(BlockState state) {
		ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(state.getBlock());
		return registryName == null ? String.valueOf(state.getBlock()) : registryName.toString();
	}

	private static String formatVec(Vec3 vec) {
		return vec == null ? "null" : "(" + formatDouble(vec.x()) + "," + formatDouble(vec.y()) + "," + formatDouble(vec.z()) + ")";
	}

	private static String formatDouble(double value) {
		return String.format(Locale.ROOT, "%.4f", Double.valueOf(value));
	}

	private record VaultAngle(double forward, double side, boolean pass) {
	}

	private record DirectionScan(boolean lowerBlocked, boolean topClear, boolean candidate, String reason) {
		static DirectionScan failed(Throwable throwable) {
			return new DirectionScan(false, false, false, throwable.getClass().getSimpleName());
		}

		String format() {
			return "lower=" + lowerBlocked + ",topClear=" + topClear + ",candidate=" + candidate + ",reason=" + reason;
		}
	}

	private record TargetScan(BlockPos pos, boolean loaded, String block, boolean stairBlocked) {
		static TargetScan none() {
			return new TargetScan(null, true, "none", false);
		}

		String format() {
			return "pos=" + (pos == null ? "none" : pos.toShortString())
					+ ",loaded=" + loaded
					+ ",block=" + block
					+ ",stairBlocked=" + stairBlocked;
		}
	}

	private record VaultScan(
			Vec3 step,
			String reason,
			DirectionScan xPositive,
			DirectionScan xNegative,
			DirectionScan zPositive,
			DirectionScan zNegative,
			TargetScan target) {
		boolean hasLowerBlocker() {
			return xPositive.lowerBlocked()
					|| xNegative.lowerBlocked()
					|| zPositive.lowerBlocked()
					|| zNegative.lowerBlocked();
		}
	}

}
