package dev.spake404.epm.skill.sandevistan.mixin;

import dev.spake404.epm.skill.sandevistan.client.SandevistanClientTickClock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class SandevistanLevelRendererMixin {
	@Redirect(
			method = "renderEntity",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
			require = 0)
	private void epm$renderWithSandevistanLocalPartialTick(
			EntityRenderDispatcher dispatcher,
			Entity entity,
			double renderX,
			double renderY,
			double renderZ,
			float renderedYaw,
			float globalPartialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight) {
		float localPartialTick = SandevistanClientTickClock.localPartialTick(entity, globalPartialTick);
		if (localPartialTick != globalPartialTick) {
			renderX += Mth.lerp(localPartialTick, entity.xOld, entity.getX())
					- Mth.lerp(globalPartialTick, entity.xOld, entity.getX());
			renderY += Mth.lerp(localPartialTick, entity.yOld, entity.getY())
					- Mth.lerp(globalPartialTick, entity.yOld, entity.getY());
			renderZ += Mth.lerp(localPartialTick, entity.zOld, entity.getZ())
					- Mth.lerp(globalPartialTick, entity.zOld, entity.getZ());
			renderedYaw = Mth.rotLerp(localPartialTick, entity.yRotO, entity.getYRot());
			packedLight = dispatcher.getPackedLightCoords(entity, localPartialTick);
		}
		dispatcher.render(
				entity,
				renderX,
				renderY,
				renderZ,
				renderedYaw,
				localPartialTick,
				poseStack,
				bufferSource,
				packedLight);
	}
}
