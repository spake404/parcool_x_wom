package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.config.EPMConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.EntitySnapshot;
import yesman.epicfight.client.particle.EntityAfterimageParticle;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

final class SandevistanAfterimageParticle extends EntityAfterimageParticle {
	private final int lifetimeTicks;
	private final float initialAlpha;
	private final float endAlpha;
	private final int startColor;
	private final int middleColor;
	private final int endColor;
	private final boolean animatedVisuals;
	private final int displayDelayTicks;
	private final AABB renderBounds;
	private boolean readyForRendering;
	private int pendingTicks;
	private int visualAge;

	private SandevistanAfterimageParticle(
			ClientLevel level,
			EntitySnapshot<?> snapshot,
			int lifetimeTicks,
			float initialAlpha,
			float endAlpha,
			int startColor,
			int middleColor,
			int endColor,
			boolean animatedVisuals,
			int displayDelayTicks) {
		super(
				level,
				snapshot.getPosition().x,
				snapshot.getPosition().y,
				snapshot.getPosition().z,
				0.0D,
				0.0D,
				0.0D,
				snapshot,
				ignored -> {
				});
		this.lifetimeTicks = Math.max(1, lifetimeTicks);
		this.initialAlpha = initialAlpha;
		this.endAlpha = endAlpha;
		this.startColor = startColor;
		this.middleColor = middleColor;
		this.endColor = endColor;
		this.animatedVisuals = animatedVisuals;
		this.displayDelayTicks = Math.max(0, displayDelayTicks);
		this.readyForRendering = this.displayDelayTicks == 0;
		Vec3 position = snapshot.getPosition();
		renderBounds = new AABB(
				position.x - 0.9D,
				position.y - 0.25D,
				position.z - 0.9D,
				position.x + 0.9D,
				position.y + 2.75D,
				position.z + 0.9D);
		setLifetime(this.lifetimeTicks);
		applyVisuals(0.0F);
	}

	static SandevistanAfterimageParticle captureMovement(Player player) {
		return capture(
				player,
				EPMConfig.sandevistanAfterimageLifetimeTicks(),
				EPMConfig.sandevistanAfterimageAlpha(),
				0.0F,
				EPMConfig.sandevistanAfterimageStartColor(),
				EPMConfig.sandevistanAfterimageMiddleColor(),
				EPMConfig.sandevistanAfterimageEndColor(),
				true,
				1);
	}

	static SandevistanAfterimageParticle captureStationaryAction(Player player) {
		int color = EPMConfig.sandevistanStationaryActionAfterimageColor();
		return capture(
				player,
				EPMConfig.sandevistanStationaryActionAfterimageLifetimeTicks(),
				EPMConfig.sandevistanStationaryActionAfterimageAlpha(),
				EPMConfig.sandevistanStationaryActionAfterimageEndAlpha(),
				color,
				color,
				color,
				false,
				EPMConfig.sandevistanStationaryActionAfterimageDisplayDelayTicks());
	}

	private static SandevistanAfterimageParticle capture(
			Player player,
			int lifetimeTicks,
			float initialAlpha,
			float endAlpha,
			int startColor,
			int middleColor,
			int endColor,
			boolean animatedVisuals,
			int displayDelayTicks) {
		if (player == null || !(player.level() instanceof ClientLevel level)) {
			return null;
		}

		LivingEntityPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
		EntitySnapshot<?> snapshot = playerPatch == null ? null : EntitySnapshot.captureLivingEntity(playerPatch);
		return snapshot == null ? null : new SandevistanAfterimageParticle(
				level,
				snapshot,
				lifetimeTicks,
				initialAlpha,
				endAlpha,
				startColor,
				middleColor,
				endColor,
				animatedVisuals,
				displayDelayTicks);
	}

	void renderAfterimage(Camera camera, float partialTick) {
		super.render(null, camera, partialTick);
	}

	boolean isVisible(Frustum frustum) {
		return frustum == null || frustum.isVisible(renderBounds);
	}

	boolean isReadyForRendering() {
		return readyForRendering && isAlive();
	}

	void advanceDisplayDelay() {
		if (!readyForRendering && ++pendingTicks >= displayDelayTicks) {
			readyForRendering = true;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!isAlive()) {
			return;
		}

		visualAge++;
		applyVisuals(Math.min(1.0F, visualAge / (float)lifetimeTicks));
	}

	private void applyVisuals(float progress) {
		if (!animatedVisuals) {
			setAlpha(lerp(initialAlpha, endAlpha, easeIn(progress)));
			setColor(channel(startColor, 16), channel(startColor, 8), channel(startColor, 0));
			return;
		}

		setAlpha(alpha(progress));
		float[] color = color(progress);
		setColor(color[0], color[1], color[2]);
	}

	private float alpha(float progress) {
		if (progress <= 0.55F) {
			return lerp(initialAlpha, initialAlpha * 0.79F, progress / 0.55F);
		}

		float fade = smoothstep((progress - 0.55F) / 0.45F);
		return initialAlpha * 0.79F * (1.0F - fade);
	}

	private float[] color(float progress) {
		if (progress <= 0.45F) {
			float blend = smoothstep(progress / 0.45F);
			return mix(startColor, middleColor, blend);
		}

		float blend = smoothstep((progress - 0.45F) / 0.55F);
		return mix(middleColor, endColor, blend);
	}

	private static float[] mix(int startColor, int endColor, float blend) {
		return new float[] {
				lerp(channel(startColor, 16), channel(endColor, 16), blend),
				lerp(channel(startColor, 8), channel(endColor, 8), blend),
				lerp(channel(startColor, 0), channel(endColor, 0), blend)
		};
	}

	private static float channel(int color, int shift) {
		return (color >> shift & 0xFF) / 255.0F;
	}

	private static float lerp(float start, float end, float blend) {
		return start + (end - start) * blend;
	}

	private static float smoothstep(float value) {
		float clamped = Math.max(0.0F, Math.min(1.0F, value));
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static float easeIn(float value) {
		float clamped = Math.max(0.0F, Math.min(1.0F, value));
		return (float)Math.pow(clamped, 2.2D);
	}
}
