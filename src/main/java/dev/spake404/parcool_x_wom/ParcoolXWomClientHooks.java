package dev.spake404.parcool_x_wom;

import java.nio.ByteBuffer;
import java.util.WeakHashMap;

import com.alrex.parcool.client.animation.impl.FastRunningAnimator;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.impl.FastRun;
import com.alrex.parcool.common.capability.Animation;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.api.client.input.PlayerInputState;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

public final class ParcoolXWomClientHooks {
	private static final WeakHashMap<Player, Integer> NATURAL_SPRINTER_CAT_LEAP_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntityPatch<?>, Boolean> NATURAL_SPRINTER_CAT_LEAP_PATCHES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PHANTOM_ASCENT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, PhantomAscentPrimeSource> PHANTOM_ASCENT_SOURCES = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> PHANTOM_ASCENT_USED_AIRBORNE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Integer> PENDING_FORCED_PHANTOM_ASCENT_TICKS = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_FAST_RUN_RESTORE = new WeakHashMap<>();
	private static final WeakHashMap<Player, Boolean> VAULT_STARTED_FROM_FAST_RUN = new WeakHashMap<>();
	private static final WeakHashMap<PlayerPatch<?>, AssetAccessor<? extends StaticAnimation>> PENDING_FAST_RUN_DASHES = new WeakHashMap<>();
	private static volatile boolean jumpSpeedModifierInstalled;
	private static boolean phantomJumpWasDown;

	private ParcoolXWomClientHooks() {
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
		if (player != null && player.isLocalPlayer() && ParcoolXWomConfig.catLeapPrimesPhantomAscent()) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.CAT_LEAP);
		}
	}

	public static void markWallJumpForPhantomAscent(Player player) {
		if (player != null && player.isLocalPlayer() && ParcoolXWomConfig.wallJumpPrimesPhantomAscent()) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.WALL_JUMP);
		}
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
		KeyRecorder.KeyState keyState = fastRunKeyState();
		if (keyState == null) {
			return false;
		}

		int ticksDown = keyState.getTickKeyDown();
		return keyState.isPressed() || (ticksDown > 0 && ticksDown <= 4);
	}

	public static boolean isFastRunKeyDown() {
		KeyRecorder.KeyState keyState = fastRunKeyState();
		return keyState != null && keyState.getTickKeyDown() > 0;
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
		if (isPhantomAscentAirborneLocked(player)) {
			VAULT_FAST_RUN_RESTORE.remove(player);
			return false;
		}

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

		clearAirbornePhantomAscentLockIfLanded(event.player);
		tickPendingForcedPhantomAscent(event.player);
		tickNaturalSprinterCatLeap(event.player);
		markSpiderWallJumpForPhantomAscent(event.player);
		tickPhantomAscent(event.player);
		playPendingFastRunDash(event.player);

		if (Boolean.TRUE.equals(VAULT_FAST_RUN_RESTORE.get(event.player)) && !canRestoreFastRun(event.player)) {
			VAULT_FAST_RUN_RESTORE.remove(event.player);
		}
	}

	private static void playPendingFastRunDash(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (isPhantomAscentAirborneLocked(player)) {
			if (playerPatch != null) {
				PENDING_FAST_RUN_DASHES.remove(playerPatch);
			}
			return;
		}

		if (playerPatch == null || !NaturalSprinterState.hasNaturalSprinter(playerPatch)) {
			return;
		}

		AssetAccessor<? extends StaticAnimation> animation = PENDING_FAST_RUN_DASHES.remove(playerPatch);
		if (animation != null) {
			playerPatch.playAnimationInClientSide(animation, 0.0F);
		}
	}

	private static void markForPhantomAscent(Player player, PhantomAscentPrimeSource source) {
		if (Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player))) {
			return;
		}

		PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(0));
		PHANTOM_ASCENT_SOURCES.put(player, source);
		phantomJumpWasDown = isPhysicalJumpKeyDown();
	}

	private static void tickPhantomAscent(Player player) {
		Integer ticks = PHANTOM_ASCENT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		int nextTick = ticks.intValue() + 1;
		if (!isPhantomAscentPrimeEnabled(player) || nextTick > 80) {
			clearPhantomAscent(player);
			return;
		}

		if (isJumpKeyRecentlyPressed()) {
			primePhantomAscentForNextInput(player);
			clearPhantomAscent(player);
			return;
		}

		PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(nextTick));
	}

	private static void clearPhantomAscent(Player player) {
		PHANTOM_ASCENT_TICKS.remove(player);
		PHANTOM_ASCENT_SOURCES.remove(player);
		phantomJumpWasDown = false;
	}

	private static void markSpiderWallJumpForPhantomAscent(Player player) {
		if (player == null || !player.isLocalPlayer() || !ParcoolXWomConfig.spiderWallJumpPrimesPhantomAscent() || PHANTOM_ASCENT_TICKS.containsKey(player)) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch != null && WomAnimationRefs.isAny(currentBaseAnimation(playerPatch), WomAnimationRefs.wallBackflip())) {
			markForPhantomAscent(player, PhantomAscentPrimeSource.SPIDER_WALL_JUMP);
		}
	}

	private static void primePhantomAscentForNextInput(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch == null) {
			return;
		}

		SkillContainer phantomAscent = findPhantomAscent(playerPatch);
		if (phantomAscent != null && phantomAscent.getDataManager() != null) {
			phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.TRUE);
			phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_COUNT.get(), Integer.valueOf(999));
			phantomAscent.setResource(0.0F);
		}

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			cancelCurrentActionBeforeNativePhantomAscent(player, localPlayerPatch);
			PENDING_FORCED_PHANTOM_ASCENT_TICKS.put(player, Integer.valueOf(1));
		}
	}

	private static void tickPendingForcedPhantomAscent(Player player) {
		Integer ticks = PENDING_FORCED_PHANTOM_ASCENT_TICKS.get(player);
		if (ticks == null) {
			return;
		}

		if (ticks.intValue() > 3) {
			PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
			return;
		}

		PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			triggerNativePhantomAscent(player, localPlayerPatch);
		}
	}

	private static void cancelCurrentActionBeforeNativePhantomAscent(Player player, LocalPlayerPatch playerPatch) {
		PHANTOM_ASCENT_USED_AIRBORNE.put(player, Boolean.TRUE);

		stopPlaying(playerPatch,
				WomAnimationRefs.bipedSprintJump(),
				WomAnimationRefs.wallBackflip(),
				WomAnimationRefs.epicParCoolCatLeap(),
				WomAnimationRefs.epicParCoolCatLeapPreparation(),
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight());

		NATURAL_SPRINTER_CAT_LEAP_TICKS.remove(player);
		NATURAL_SPRINTER_CAT_LEAP_PATCHES.remove(playerPatch);
		NaturalSprinterState.suppress(playerPatch);
		clearParCoolAnimator(player);

		try {
			playerPatch.getClientAnimator().offAllLayers();
			playerPatch.getClientAnimator().resetMotion(true);
			playerPatch.getClientAnimator().resetCompositeMotion();
			playerPatch.setModelYRot(player.getYRot(), true);
		} catch (RuntimeException | LinkageError ignored) {
		}

	}

	private static void triggerNativePhantomAscent(Player player, LocalPlayerPatch playerPatch) {
		SkillContainer phantomAscent = findPhantomAscent(playerPatch);
		if (phantomAscent == null || phantomAscent.getDataManager() == null || !(player instanceof LocalPlayer localPlayer)) {
			return;
		}

		phantomAscent.setResource(0.0F);
		if (phantomAscent.getStack() < 1) {
			phantomAscent.setStack(1);
		}
		phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_KEY_PRESSED_LAST_TICK.get(), Boolean.FALSE);
		phantomAscent.getDataManager().setData(SkillDataKeys.JUMP_COUNT.get(), Integer.valueOf(1));

		try {
			PlayerInputState inputState = PlayerInputState.fromVanillaInput(localPlayer.input).withJumping(true);
			MovementInputEvent movementInputEvent = new MovementInputEvent(playerPatch, inputState);
			playerPatch.getEventListener().triggerEvents(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, movementInputEvent);
		} catch (RuntimeException | LinkageError ignored) {
		}
	}

	private static void clearAirbornePhantomAscentLockIfLanded(Player player) {
		if (!player.onGround()) {
			return;
		}

		PHANTOM_ASCENT_USED_AIRBORNE.remove(player);
		PENDING_FORCED_PHANTOM_ASCENT_TICKS.remove(player);
	}

	private static SkillContainer findPhantomAscent(PlayerPatch<?> playerPatch) {
		if (playerPatch.getSkillCapability() == null) {
			return null;
		}

		try (var containers = playerPatch.getSkillCapability().listSkillContainers()) {
			return containers
					.filter(ParcoolXWomClientHooks::isPhantomAscentContainer)
					.findFirst()
					.orElse(null);
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

	@SafeVarargs
	private static void stopPlaying(LocalPlayerPatch playerPatch, AssetAccessor<? extends StaticAnimation>... animations) {
		for (AssetAccessor<? extends StaticAnimation> animation : animations) {
			if (animation == null) {
				continue;
			}

			try {
				playerPatch.stopPlaying(animation);
			} catch (RuntimeException | LinkageError ignored) {
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

		if (playerPatch instanceof LocalPlayerPatch localPlayerPatch) {
			stopPlaying(localPlayerPatch, WomAnimationRefs.bipedSprintJump());
		}

		try {
			AssetAccessor<? extends StaticAnimation> idle = WomAnimationRefs.bipedIdle();
			if (idle != null) {
				playerPatch.playAnimationInClientSide(idle, 0.0F);
			} else {
				playerPatch.getClientAnimator().resetMotion(true);
			}
			playerPatch.getClientAnimator().resetCompositeMotion();
		} catch (RuntimeException | LinkageError ignored) {
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

		if (!player.onGround() || isPhantomAscentAirborneLocked(player)
				|| player.isShiftKeyDown() || player.isInWaterOrBubble() || player.isFallFlying() || player.getVehicle() != null) {
			return false;
		}

		Vec3 movement = player.getDeltaMovement();
		return movement.x() * movement.x() + movement.z() * movement.z() > 0.0025D;
	}

	private static AssetAccessor<?> currentBaseAnimation(PlayerPatch<?> playerPatch) {
		try {
			return playerPatch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean hasNaturalSprinter(Player player) {
		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		return playerPatch != null && NaturalSprinterState.hasNaturalSprinter(playerPatch);
	}

	private static boolean isPhantomAscentAirborneLocked(Player player) {
		return player != null && Boolean.TRUE.equals(PHANTOM_ASCENT_USED_AIRBORNE.get(player)) && !player.onGround();
	}

	private static boolean isPhantomAscentPrimeEnabled(Player player) {
		PhantomAscentPrimeSource source = PHANTOM_ASCENT_SOURCES.get(player);
		return source != null && source.enabled();
	}

	private static boolean isJumpKeyRecentlyPressed() {
		boolean down = isPhysicalJumpKeyDown();
		boolean pressed = down && !phantomJumpWasDown;
		phantomJumpWasDown = down;
		return pressed;
	}

	private static boolean isPhysicalJumpKeyDown() {
		try {
			if (InputManager.isActionActive(MinecraftInputAction.JUMP)) {
				return true;
			}
		} catch (RuntimeException | LinkageError ignored) {
		}

		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyJump.isDown();
	}

	private static KeyRecorder.KeyState fastRunKeyState() {
		try {
			return KeyRecorder.keyFastRunning;
		} catch (LinkageError ignored) {
			return null;
		}
	}

	private enum PhantomAscentPrimeSource {
		CAT_LEAP {
			@Override
			boolean enabled() {
				return ParcoolXWomConfig.catLeapPrimesPhantomAscent();
			}
		},
		WALL_JUMP {
			@Override
			boolean enabled() {
				return ParcoolXWomConfig.wallJumpPrimesPhantomAscent();
			}
		},
		SPIDER_WALL_JUMP {
			@Override
			boolean enabled() {
				return ParcoolXWomConfig.spiderWallJumpPrimesPhantomAscent();
			}
		};

		abstract boolean enabled();
	}
}
