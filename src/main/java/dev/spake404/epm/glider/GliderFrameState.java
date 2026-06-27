package dev.spake404.epm.glider;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.animation.WomAnimationRefs;
import dev.spake404.epm.compat.WomCompatBridge;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMClientHooks;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class GliderFrameState {
	private static final WeakHashMap<Player, Snapshot> SNAPSHOTS = new WeakHashMap<>();

	private GliderFrameState() {
	}

	public static Snapshot snapshot(Player player) {
		if (player == null || !player.isLocalPlayer()) {
			return Snapshot.empty();
		}

		Snapshot cached = SNAPSHOTS.get(player);
		if (cached != null && cached.tick() == player.tickCount) {
			return cached;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		boolean jumpDown = isPhysicalJumpKeyDown();
		boolean attackDown = isPhysicalAttackKeyDown();
		boolean fastRunDown = EPMClientHooks.isFastRunControlKeyDown();
		Snapshot snapshot = new Snapshot(
				player.tickCount,
				playerPatch,
				animation,
				animationId,
				GliderCompat.isGlidingWithActiveGlider(player),
				player.onGround(),
				player.isInWater(),
				player.isSprinting(),
				jumpDown,
				attackDown,
				fastRunDown,
				isWallMovementAnimation(animation),
				isPhantomAscentAnimation(animation),
				isParCoolWallJumpAnimation(animation),
				isWomBackflipAnimation(animation),
				animationId == null ? String.valueOf(animation) : animationId.toString(),
				describeWomState(playerPatch)
		);
		SNAPSHOTS.put(player, snapshot);
		return snapshot;
	}

	public static void invalidate(Player player) {
		if (player != null) {
			SNAPSHOTS.remove(player);
		}
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

	private static boolean isPhysicalAttackKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null && minecraft.options.keyAttack.isDown();
	}

	private static boolean isWallMovementAnimation(AssetAccessor<?> animation) {
		return animation != null && WomAnimationRefs.isAny(animation,
				WomAnimationRefs.wallRunning(),
				WomAnimationRefs.wallRunLeftSide(),
				WomAnimationRefs.wallRunRightSide(),
				WomAnimationRefs.wallGlide());
	}

	private static boolean isPhantomAscentAnimation(AssetAccessor<?> animation) {
		return animation != null && WomAnimationRefs.isAny(animation, WomAnimationRefs.bipedPhantomAscentForward(), WomAnimationRefs.bipedPhantomAscentBackward());
	}

	private static boolean isParCoolWallJumpAnimation(AssetAccessor<?> animation) {
		return animation != null && WomAnimationRefs.isAny(
				animation,
				WomAnimationRefs.epicParCoolWallJumpLeftStart(),
				WomAnimationRefs.epicParCoolWallJumpRightStart(),
				WomAnimationRefs.epicParCoolWallJumpLeft(),
				WomAnimationRefs.epicParCoolWallJumpRight()
		);
	}

	private static boolean isWomBackflipAnimation(AssetAccessor<?> animation) {
		return animation != null && WomAnimationRefs.isAny(animation, WomAnimationRefs.wallBackflip());
	}

	private static String describeWomState(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return "null";
		}

		try {
			return WomCompatBridge.instance().describeSpiderTechniquesState(playerPatch);
		} catch (RuntimeException | LinkageError ignored) {
			return "unavailable";
		}
	}
	public record Snapshot(
			int tick,
			PlayerPatch<?> playerPatch,
			AssetAccessor<?> animation,
			ResourceLocation animationId,
			boolean gliderActive,
			boolean onGround,
			boolean inWater,
			boolean sprinting,
			boolean jumpDown,
			boolean attackDown,
			boolean fastRunDown,
			boolean wallMovementAnimationActive,
			boolean phantomAnimation,
			boolean parCoolWallJumpAnimation,
			boolean womBackflipAnimation,
			String animationName,
			String womState) {
		private static Snapshot empty() {
			return new Snapshot(
					-1,
					null,
					null,
					null,
					false,
					false,
					false,
					false,
					false,
					false,
					false,
					false,
					false,
					false,
					false,
					"null",
					"null");
		}
	}
}
