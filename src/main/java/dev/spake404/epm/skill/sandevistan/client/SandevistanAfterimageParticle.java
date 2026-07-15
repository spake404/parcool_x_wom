package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.config.EPMConfig;
import dev.spake404.epm.skill.sandevistan.client.filter.SandevistanFilterRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import yesman.epicfight.api.utils.EntitySnapshot;
import yesman.epicfight.client.particle.EntityAfterimageParticle;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

final class SandevistanAfterimageParticle extends EntityAfterimageParticle {
	private final int lifetimeTicks;
	private final float initialAlpha;
	private final int startColor;
	private final int middleColor;
	private final int endColor;
	private final UUID ownerId;
	private int visualAge;

	private SandevistanAfterimageParticle(ClientLevel level, EntitySnapshot<?> snapshot, UUID ownerId) {
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
		lifetimeTicks = EPMConfig.sandevistanAfterimageLifetimeTicks();
		initialAlpha = EPMConfig.sandevistanAfterimageAlpha();
		startColor = EPMConfig.sandevistanAfterimageStartColor();
		middleColor = EPMConfig.sandevistanAfterimageMiddleColor();
		endColor = EPMConfig.sandevistanAfterimageEndColor();
		this.ownerId = ownerId;
		setLifetime(lifetimeTicks);
		applyVisuals(0.0F);
	}

	static SandevistanAfterimageParticle capture(Player player) {
		if (player == null || !(player.level() instanceof ClientLevel level)) {
			return null;
		}

		LivingEntityPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
		EntitySnapshot<?> snapshot = playerPatch == null ? null : playerPatch.captureEntitySnapshot();
		return snapshot == null ? null : new SandevistanAfterimageParticle(level, snapshot, player.getUUID());
	}

	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
		long startedNanos = System.nanoTime();
		boolean masking = SandevistanFilterRenderer.beginAfterimageMask(ownerId);
		try {
			super.render(vertexConsumer, camera, partialTick);
		} finally {
			if (masking) {
				SandevistanFilterRenderer.endMask();
			}
			SandevistanPerformanceDiagnostics.recordAfterimage(System.nanoTime() - startedNanos);
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
}
