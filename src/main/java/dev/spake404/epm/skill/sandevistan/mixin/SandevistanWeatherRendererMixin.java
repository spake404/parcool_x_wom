package dev.spake404.epm.skill.sandevistan.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.spake404.epm.skill.sandevistan.client.SandevistanWeatherClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SandevistanWeatherRendererMixin {
	@Unique
	private static final ResourceLocation EPM$RAIN_LOCATION = new ResourceLocation("textures/environment/rain.png");
	@Unique
	private static final ResourceLocation EPM$SNOW_LOCATION = new ResourceLocation("textures/environment/snow.png");

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private ClientLevel level;

	@Shadow
	private int ticks;

	@Shadow
	@Final
	private float[] rainSizeX;

	@Shadow
	@Final
	private float[] rainSizeZ;

	@Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true, require = 0)
	private void epm$renderSnowAndRain(
			LightTexture lightTexture,
			float partialTick,
			double cameraX,
			double cameraY,
			double cameraZ,
			CallbackInfo callbackInfo) {
		if (this.level == null || !SandevistanWeatherClock.shouldOverrideVanilla()) {
			return;
		}

		double globalTime = this.ticks + partialTick;
		SandevistanWeatherClock.beginFrame(this.level);
		try {
			epm$renderWeather(lightTexture, partialTick, cameraX, cameraY, cameraZ, globalTime);
		} finally {
			SandevistanWeatherClock.endFrame(globalTime);
		}
		callbackInfo.cancel();
	}

	@Unique
	private void epm$renderWeather(
			LightTexture lightTexture,
			float partialTick,
			double cameraX,
			double cameraY,
			double cameraZ,
			double globalTime) {
		double effectsTime = SandevistanWeatherClock.localTime(
				this.level,
				Mth.floor(cameraX),
				cameraY,
				Mth.floor(cameraZ),
				globalTime);
		int effectsTicks = Mth.floor(effectsTime);
		float effectsPartialTick = (float)(effectsTime - effectsTicks);
		if (this.level.effects().renderSnowAndRain(
				this.level,
				effectsTicks,
				effectsPartialTick,
				lightTexture,
				cameraX,
				cameraY,
				cameraZ)) {
			return;
		}

		float rainLevel = this.level.getRainLevel(partialTick);
		if (rainLevel <= 0.0F) {
			return;
		}

		lightTexture.turnOnLightLayer();
		Level renderLevel = this.level;
		int cameraBlockX = Mth.floor(cameraX);
		int cameraBlockY = Mth.floor(cameraY);
		int cameraBlockZ = Mth.floor(cameraZ);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		int radius = Minecraft.useFancyGraphics() ? 10 : 5;
		RenderSystem.depthMask(Minecraft.useShaderTransparency());
		int precipitationType = -1;
		RenderSystem.setShader(GameRenderer::getParticleShader);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

		for (int z = cameraBlockZ - radius; z <= cameraBlockZ + radius; z++) {
			for (int x = cameraBlockX - radius; x <= cameraBlockX + radius; x++) {
				int rainSizeIndex = (z - cameraBlockZ + 16) * 32 + x - cameraBlockX + 16;
				double rainX = this.rainSizeX[rainSizeIndex] * 0.5D;
				double rainZ = this.rainSizeZ[rainSizeIndex] * 0.5D;
				mutablePos.set(x, cameraY, z);
				Biome biome = renderLevel.getBiome(mutablePos).value();
				if (!biome.hasPrecipitation()) {
					continue;
				}

				int surfaceY = renderLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				int minY = Math.max(cameraBlockY - radius, surfaceY);
				int maxY = Math.max(cameraBlockY + radius, surfaceY);
				int lightY = Math.max(surfaceY, cameraBlockY);
				if (minY == maxY) {
					continue;
				}

				RandomSource random = RandomSource.create((long)(x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
				mutablePos.set(x, minY, z);
				Biome.Precipitation precipitation = biome.getPrecipitationAt(mutablePos);
				double localTime = SandevistanWeatherClock.localTime(
						this.level,
						x,
						cameraY,
						z,
						globalTime);
				int localTicks = Mth.floor(localTime);
				float localPartialTick = (float)(localTime - localTicks);
				if (precipitation == Biome.Precipitation.RAIN) {
					if (precipitationType != 0) {
						if (precipitationType >= 0) {
							tesselator.end();
						}
						precipitationType = 0;
						RenderSystem.setShaderTexture(0, EPM$RAIN_LOCATION);
						bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
					}
					epm$renderRainColumn(
						bufferBuilder,
						renderLevel,
						mutablePos,
						random,
						x,
						z,
						minY,
						maxY,
						lightY,
						cameraX,
						cameraY,
						cameraZ,
						rainX,
						rainZ,
						radius,
						rainLevel,
						localTicks,
						localPartialTick);
				} else if (precipitation == Biome.Precipitation.SNOW) {
					if (precipitationType != 1) {
						if (precipitationType >= 0) {
							tesselator.end();
						}
						precipitationType = 1;
						RenderSystem.setShaderTexture(0, EPM$SNOW_LOCATION);
						bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
					}
					epm$renderSnowColumn(
						bufferBuilder,
						renderLevel,
						mutablePos,
						random,
						x,
						z,
						minY,
						maxY,
						lightY,
						cameraX,
						cameraY,
						cameraZ,
						rainX,
						rainZ,
						radius,
						rainLevel,
						localTicks,
						localPartialTick,
						localTime);
				}
			}
		}

		if (precipitationType >= 0) {
			tesselator.end();
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		lightTexture.turnOffLightLayer();
	}

	@Unique
	private static void epm$renderRainColumn(
			BufferBuilder bufferBuilder,
			Level level,
			BlockPos.MutableBlockPos mutablePos,
			RandomSource random,
			int x,
			int z,
			int minY,
			int maxY,
			int lightY,
			double cameraX,
			double cameraY,
			double cameraZ,
			double rainX,
			double rainZ,
			int radius,
			float rainLevel,
			int localTicks,
			float localPartialTick) {
		int phaseTicks = localTicks + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 31;
		float textureOffset = -((float)phaseTicks + localPartialTick) / 32.0F * (3.0F + random.nextFloat());
		double distanceX = x + 0.5D - cameraX;
		double distanceZ = z + 0.5D - cameraZ;
		float distance = (float)Math.sqrt(distanceX * distanceX + distanceZ * distanceZ) / radius;
		float alpha = ((1.0F - distance * distance) * 0.5F + 0.5F) * rainLevel;
		mutablePos.set(x, lightY, z);
		int lightColor = LevelRenderer.getLightColor(level, mutablePos);
		bufferBuilder.vertex(x - cameraX - rainX + 0.5D, maxY - cameraY, z - cameraZ - rainZ + 0.5D)
				.uv(0.0F, minY * 0.25F + textureOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(lightColor)
				.endVertex();
		bufferBuilder.vertex(x - cameraX + rainX + 0.5D, maxY - cameraY, z - cameraZ + rainZ + 0.5D)
				.uv(1.0F, minY * 0.25F + textureOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(lightColor)
				.endVertex();
		bufferBuilder.vertex(x - cameraX + rainX + 0.5D, minY - cameraY, z - cameraZ + rainZ + 0.5D)
				.uv(1.0F, maxY * 0.25F + textureOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(lightColor)
				.endVertex();
		bufferBuilder.vertex(x - cameraX - rainX + 0.5D, minY - cameraY, z - cameraZ - rainZ + 0.5D)
				.uv(0.0F, maxY * 0.25F + textureOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(lightColor)
				.endVertex();
	}

	@Unique
	private static void epm$renderSnowColumn(
			BufferBuilder bufferBuilder,
			Level level,
			BlockPos.MutableBlockPos mutablePos,
			RandomSource random,
			int x,
			int z,
			int minY,
			int maxY,
			int lightY,
			double cameraX,
			double cameraY,
			double cameraZ,
			double rainX,
			double rainZ,
			int radius,
			float rainLevel,
			int localTicks,
			float localPartialTick,
			double localTime) {
		float verticalOffset = -((float)(localTicks & 511) + localPartialTick) / 512.0F;
		float horizontalOffset = (float)(random.nextDouble() + localTime * 0.01D * random.nextGaussian());
		float depthOffset = (float)(random.nextDouble() + localTime * random.nextGaussian() * 0.001D);
		double distanceX = x + 0.5D - cameraX;
		double distanceZ = z + 0.5D - cameraZ;
		float distance = (float)Math.sqrt(distanceX * distanceX + distanceZ * distanceZ) / radius;
		float alpha = ((1.0F - distance * distance) * 0.3F + 0.5F) * rainLevel;
		mutablePos.set(x, lightY, z);
		int lightColor = LevelRenderer.getLightColor(level, mutablePos);
		int blockLight = lightColor >> 16 & 65535;
		int skyLight = lightColor & 65535;
		int brightBlockLight = (blockLight * 3 + 240) / 4;
		int brightSkyLight = (skyLight * 3 + 240) / 4;
		bufferBuilder.vertex(x - cameraX - rainX + 0.5D, maxY - cameraY, z - cameraZ - rainZ + 0.5D)
				.uv(horizontalOffset, minY * 0.25F + verticalOffset + depthOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(brightSkyLight, brightBlockLight)
				.endVertex();
		bufferBuilder.vertex(x - cameraX + rainX + 0.5D, maxY - cameraY, z - cameraZ + rainZ + 0.5D)
				.uv(1.0F + horizontalOffset, minY * 0.25F + verticalOffset + depthOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(brightSkyLight, brightBlockLight)
				.endVertex();
		bufferBuilder.vertex(x - cameraX + rainX + 0.5D, minY - cameraY, z - cameraZ + rainZ + 0.5D)
				.uv(1.0F + horizontalOffset, maxY * 0.25F + verticalOffset + depthOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(brightSkyLight, brightBlockLight)
				.endVertex();
		bufferBuilder.vertex(x - cameraX - rainX + 0.5D, minY - cameraY, z - cameraZ - rainZ + 0.5D)
				.uv(horizontalOffset, maxY * 0.25F + verticalOffset + depthOffset)
				.color(1.0F, 1.0F, 1.0F, alpha)
				.uv2(brightSkyLight, brightBlockLight)
				.endVertex();
	}
}
