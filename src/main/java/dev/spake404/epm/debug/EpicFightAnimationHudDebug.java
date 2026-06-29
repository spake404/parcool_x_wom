package dev.spake404.epm.debug;

import dev.spake404.epm.animation.AnimationQuery;
import dev.spake404.epm.config.EPMConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public final class EpicFightAnimationHudDebug {
	private static boolean copyKeyDown;

	private EpicFightAnimationHudDebug() {
	}

	public static void tickCopyShortcut() {
		if (!EPMConfig.debugEpicFightAnimationHud()) {
			copyKeyDown = false;
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null) {
			copyKeyDown = false;
			return;
		}

		long window = minecraft.getWindow().getWindow();
		boolean pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_0) == GLFW.GLFW_PRESS;
		if (!pressed) {
			copyKeyDown = false;
			return;
		}
		if (copyKeyDown) {
			return;
		}

		copyKeyDown = true;
		String animationName = currentAnimationName(minecraft.player);
		if (animationName == null) {
			return;
		}

		GLFW.glfwSetClipboardString(window, animationName);
		minecraft.player.displayClientMessage(Component.literal("Copied EF animation: " + animationName), true);
	}

	public static void render(RenderGuiOverlayEvent.Post event) {
		if (!EPMConfig.debugEpicFightAnimationHud()
				|| event == null
				|| event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.options.hideGui) {
			return;
		}

		String animationName = currentAnimationName(player);
		if (animationName == null) {
			animationName = "none";
		}
		PlayerPatch<?> playerPatch = playerPatch(player);
		float elapsed = AnimationQuery.currentElapsedTime(playerPatch);
		float total = AnimationQuery.currentAnimationTotalTime(playerPatch);
		String text = "EF animation: " + animationName + timeSuffix(elapsed, total);

		renderCenteredAboveHotbar(event.getGuiGraphics(), minecraft.font, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), text);
	}

	private static String currentAnimationName(LocalPlayer player) {
		PlayerPatch<?> playerPatch = playerPatch(player);
		AssetAccessor<?> animation = AnimationQuery.currentAnimation(playerPatch);
		ResourceLocation animationId = AnimationQuery.safeRegistryName(animation);
		return animationId == null ? null : animationId.toString();
	}

	private static PlayerPatch<?> playerPatch(LocalPlayer player) {
		try {
			return EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static String timeSuffix(float elapsed, float total) {
		if (elapsed < 0.0F) {
			return "";
		}
		if (total > 0.0F) {
			return String.format(java.util.Locale.ROOT, " %.2f/%.2f", Float.valueOf(elapsed), Float.valueOf(total));
		}
		return String.format(java.util.Locale.ROOT, " %.2f", Float.valueOf(elapsed));
	}

	private static void renderCenteredAboveHotbar(GuiGraphics graphics, Font font, int screenWidth, int screenHeight, String text) {
		int textWidth = font.width(text);
		int x = (screenWidth - textWidth) / 2;
		int y = screenHeight - 52;
		int padding = 3;
		graphics.fill(x - padding, y - padding, x + textWidth + padding, y + font.lineHeight + padding, 0x88000000);
		graphics.drawString(font, text, x, y, 0xE6FFFFFF, true);
	}
}
