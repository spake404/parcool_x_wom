package dev.spake404.parcoolxwom;

import java.nio.ByteBuffer;
import java.util.WeakHashMap;

import com.alrex.parcool.client.animation.impl.FastRunningAnimator;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

public final class ParCoolXWomClientHooks {
	private static final WeakHashMap<Player, Integer> NATURAL_SPRINTER_CAT_LEAP_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntityPatch<?>, Boolean> NATURAL_SPRINTER_CAT_LEAP_PATCHES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> CAT_LEAP_PHANTOM_ASCENT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, PhantomAscentPrimeSource> PHANTOM_ASCENT_PRIME_SOURCES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_FAST_RUN_RESTORE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_STARTED_FROM_FAST_RUN = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, AssetAccessor<? extends StaticAnimation>> PENDING_FAST_RUN_DASHES = new WeakHashMap<>();
	private static volatile boolean jumpSpeedModifierInstalled;
	private static boolean catLeapPhantomJumpWasDown;

	private ParCoolXWomClientHooks() {
	}

	public static void startNaturalSprinterCatLeap(Player player) {
		if (player == null || !player.isLocalPlayer() || !hasNaturalSprinter(player)) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		installJumpSpeedModifier();
		NATURAL_SPRINTER_CAT_LEAP_TICKS.put(player, Integer.valueOf(0));
		NATURAL_SPRINTER_CAT_LEAP_PATCHES.put(playerPatch, Boolean.TRUE);
	}

	public static void markCatLeapForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer() || !ParCoolXWomConfig.catLeapPrimesPhantomAscent()) {
			return;
		}

		markForPhantomAscent(player, PhantomAscentPrimeSource.CAT_LEAP);
	}

	public static void markWallJumpForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer() || !ParCoolXWomConfig.wallJumpPrimesPhantomAscent()) {
			return;
		}

		markForPhantomAscent(player, PhantomAscentPrimeSource.WALL_JUMP);
	}

	private static void markSpiderWallJumpForPhantomAscentWindow(Player player) {
		if (player == null || !player.isLocalPlayer() || !ParCoolXWomConfig.spiderWallJumpPrimesPhantomAscent()) {
			return;
		}

		markForPhantomAscent(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
	}

	private static void markForPhantomAscent(Player player, PhantomAscentPrimeSource source) {
		CAT_LEAP_PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(0));
		PHANTOM_ASCENT_PRIME_SOURCES.put(player, source);
		catLeapPhantomJumpWasDown = isPhysicalJumpKeyDown();
	}

	public static void reduceNaturalSprinterCatLeapMotion(Player player) {
		if (player == null || !player.isLocalPlayer() || !hasNaturalSprinter(player)) {
			return;
		}

		Vec3 movement = player.getDeltaMovement();
		player.setDeltaMovement(movement.x() * 0.5D, movement.y() * 1.1D, movement.z() * 0.5D);
	}

	public static void queueNaturalSprinterFastRunDash(PlayerPatch<?> playerPatch, AssetAccessor<? extends StaticAnimation> animation) {
		if (playerPatch != null && animation != null) {
			PENDING_FAST_RUN_DASHES.put(playerPatch, animation);
		}
	}

	public static boolean isFastRunKeyRecentlyPressed() {
		Object keyState = parcoolKeyState("keyFastRunning");
		if (keyState == null) {
			return false;
		}

		int ticksDown = invokeInt(keyState, "getTickKeyDown", -1);
		return invokeBoolean(keyState, "isPressed") || (ticksDown > 0 && ticksDown <= 4);
	}

	public static boolean isFastRunKeyDown() {
		Object keyState = parcoolKeyState("keyFastRunning");
		return keyState != null && invokeInt(keyState, "getTickKeyDown", 0) > 0;
	}

	public static void markVaultStartedFromFastRun(Player player) {
		if (player != null && player.isLocalPlayer() && hasNaturalSprinter(player)) {
			VAULT_STARTED_FROM_FAST_RUN.put(player, Boolean.TRUE);
		}
	}

	public static void restoreFastRunAfterVault(Player player) {
		if (player == null || !player.isLocalPlayer() || !hasNaturalSprinter(player) || !Boolean.TRUE.equals(VAULT_STARTED_FROM_FAST_RUN.remove(player))) {
			return;
		}

		Parkourability parkourability = Parkourability.get(player);
		IStamina stamina = IStamina.get(player);
		if (parkourability == null || stamina == null) {
			return;
		}

		FastRun fastRun = parkourability.get(FastRun.class);
		if (fastRun == null) {
			return;
		}

		if (!fastRun.isDoing()) {
			fastRun.start(player, parkourability, ByteBuffer.allocate(0), stamina);
		}

		player.setSprinting(true);
		VAULT_FAST_RUN_RESTORE.put(player, Boolean.TRUE);
		Animation animation = Animation.get(player);
		if (animation != null && !animation.hasAnimator()) {
			animation.setAnimator(new FastRunningAnimator());
		}
	}

	public static boolean shouldKeepFastRunAfterVault(Player player) {
		if (!Boolean.TRUE.equals(VAULT_FAST_RUN_RESTORE.get(player)) || !canRestoreFastRun(player)) {
			VAULT_FAST_RUN_RESTORE.remove(player);
			return false;
		}

		player.setSprinting(true);
		return true;
	}

	public static void tickLocalPlayer(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !event.player.isLocalPlayer()) {
			return;
		}

		tickNaturalSprinterCatLeap(event.player);
		markSpiderWallJumpForPhantomAscent(event.player);
		tickCatLeapPhantomAscent(event.player);
		playPendingFastRunDash(event.player);

		if (Boolean.TRUE.equals(VAULT_FAST_RUN_RESTORE.get(event.player)) && !canRestoreFastRun(event.player)) {
			VAULT_FAST_RUN_RESTORE.remove(event.player);
		}
	}

	private static void playPendingFastRunDash(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null || !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = PENDING_FAST_RUN_DASHES.remove(playerPatch);
		if (animation != null) {
			playerPatch.playAnimationInClientSide(animation, 0.0F);
		}
	}

	private static void tickCatLeapPhantomAscent(Player player) {
		Integer ticks = CAT_LEAP_PHANTOM_ASCENT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		int nextTick = ticks.intValue() + 1;
		if (!isPhantomAscentPrimeEnabled(player) || nextTick > 80) {
			CAT_LEAP_PHANTOM_ASCENT_TICKS.remove(player);
			PHANTOM_ASCENT_PRIME_SOURCES.remove(player);
			catLeapPhantomJumpWasDown = false;
			return;
		}

		if (isJumpKeyRecentlyPressed()) {
			interruptNaturalSprinterCatLeap(player);
			CAT_LEAP_PHANTOM_ASCENT_TICKS.remove(player);
			PHANTOM_ASCENT_PRIME_SOURCES.remove(player);
			catLeapPhantomJumpWasDown = false;
			return;
		}

		CAT_LEAP_PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(nextTick));
	}

	private static void markSpiderWallJumpForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer() || !ParCoolXWomConfig.spiderWallJumpPrimesPhantomAscent()) {
			return;
		}

		if (CAT_LEAP_PHANTOM_ASCENT_TICKS.containsKey(player)) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		AssetAccessor<?> currentAnimation = currentBaseAnimation(playerPatch);
		if (WomAnimationRefs.isAny(currentAnimation, WomAnimationRefs.wallBackflip())) {
			markSpiderWallJumpForPhantomAscentWindow(player);
		}
	}

	private static void interruptNaturalSprinterCatLeap(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			interruptNaturalSprinterCatLeap(localPlayerPatch);
		}
	}

	private static void interruptNaturalSprinterCatLeap(LocalPlayerPatch playerPatch) {
		stopPlaying(playerPatch,
				WomAnimationRefs.bipedSprintJump(),
				WomAnimationRefs.wallBackflip(),
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight());

		try {
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (Throwable ignored) {
		}
	}

	@SafeVarargs
	private static void stopPlaying(LocalPlayerPatch playerPatch, AssetAccessor<? extends StaticAnimation>... animations) {
		for (AssetAccessor<? extends StaticAnimation> animation : animations) {
			if (animation == null) {
				continue;
			}

			try {
				playerPatch.stopPlaying(animation);
			} catch (Throwable ignored) {
			}
		}
	}

	private static void tickNaturalSprinterCatLeap(Player player) {
		Integer ticks = NATURAL_SPRINTER_CAT_LEAP_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		int nextTick = ticks.intValue() + 1;

		if ((nextTick > 2 && player.onGround()) || nextTick > 60) {
			NATURAL_SPRINTER_CAT_LEAP_TICKS.remove(player);
			resetBaseAnimation(player);
			return;
		}

		NATURAL_SPRINTER_CAT_LEAP_TICKS.put(player, Integer.valueOf(nextTick));
	}

	private static void installJumpSpeedModifier() {
		if (jumpSpeedModifierInstalled) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> sprintJump = WomAnimationRefs.bipedSprintJump();
		if (sprintJump == null) {
			return;
		}

		StaticAnimation animation = sprintJump.get();
		PlaybackSpeedModifier originalModifier = animation.getProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER).orElse(null);
		animation.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
			float modifiedSpeed = originalModifier == null ? speed : originalModifier.modify(self, entitypatch, speed, prevElapsedTime, elapsedTime);
			return isNaturalSprinterCatLeapPatch(entitypatch) ? modifiedSpeed * 0.5F : modifiedSpeed;
		});
		jumpSpeedModifierInstalled = true;
	}

	private static boolean isNaturalSprinterCatLeapPatch(LivingEntityPatch<?> entityPatch) {
		return entityPatch != null && Boolean.TRUE.equals(NATURAL_SPRINTER_CAT_LEAP_PATCHES.get(entityPatch));
	}

	private static void resetBaseAnimation(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		NATURAL_SPRINTER_CAT_LEAP_PATCHES.remove(playerPatch);
		NaturalSprinterState.suppress(playerPatch);
		player.setSprinting(false);
		clearParCoolAnimator(player);

		try {
			AssetAccessor<? extends StaticAnimation> idle = WomAnimationRefs.bipedIdle();
			if (idle != null) {
				playerPatch.playAnimationInClientSide(idle, 0.0F);
			} else {
				playerPatch.getClientAnimator().resetMotion(true);
			}
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (Throwable ignored) {
		}
	}

	private static void clearParCoolAnimator(Player player) {
		Animation animation = Animation.get(player);
		if (animation != null) {
			animation.removeAnimator();
		}
	}

	private static boolean canRestoreFastRun(Player player) {
		if (player == null || !player.isLocalPlayer() || !hasNaturalSprinter(player)) {
			return false;
		}

		if (player.isShiftKeyDown() || player.isInWaterOrBubble() || player.isFallFlying() || player.getVehicle() != null) {
			return false;
		}

		Vec3 movement = player.getDeltaMovement();
		return horizontalMovementSqr(movement) > 0.0025D;
	}

	private static double horizontalMovementSqr(Vec3 movement) {
		return movement.x() * movement.x() + movement.z() * movement.z();
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static boolean hasNaturalSprinter(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch);
	}

	private static boolean isPhantomAscentPrimeEnabled(Player player) {
		PhantomAscentPrimeSource source = PHANTOM_ASCENT_PRIME_SOURCES.get(player);
		return source != null && source.enabled();
	}

	private static boolean isJumpKeyRecentlyPressed() {
		boolean down = isPhysicalJumpKeyDown();
		boolean pressed = down && !catLeapPhantomJumpWasDown;
		catLeapPhantomJumpWasDown = down;
		return pressed;
	}

	private static boolean isPhysicalJumpKeyDown() {
		try {
			if (InputManager.isActionActive(MinecraftInputAction.JUMP)) {
				return true;
			}
		} catch (Throwable ignored) {
		}

		try {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static Object parcoolKeyState(String fieldName) {
		try {
			Class<?> keyRecorder = Class.forName("com.alrex.parcool.client.input.KeyRecorder");
			return keyRecorder.getField(fieldName).get(null);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static boolean invokeBoolean(Object target, String methodName) {
		try {
			Object value = target.getClass().getMethod(methodName).invoke(target);
			return value instanceof Boolean booleanValue && booleanValue.booleanValue();
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static int invokeInt(Object target, String methodName, int fallback) {
		try {
			Object value = target.getClass().getMethod(methodName).invoke(target);
			return value instanceof Number number ? number.intValue() : fallback;
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private enum PhantomAscentPrimeSource {
		CAT_LEAP {
			@Override
			boolean enabled() {
				return ParCoolXWomConfig.catLeapPrimesPhantomAscent();
			}
		},
		WALL_JUMP {
			@Override
			boolean enabled() {
				return ParCoolXWomConfig.wallJumpPrimesPhantomAscent();
			}
		},
		SPIDER_WALL_JUMP {
			@Override
			boolean enabled() {
				return ParCoolXWomConfig.spiderWallJumpPrimesPhantomAscent();
			}
		};

		abstract boolean enabled();
	}
}
