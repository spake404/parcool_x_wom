package dev.spake404.epm.skill.sandevistan.client;

import dev.spake404.epm.config.EPMConfig;
import java.util.Locale;

import yesman.epicfight.client.gui.ScreenCalculations;

public final class SandevistanHudPosition {
	public static final int PREVIEW_CELL_COUNT = 10;
	public static final int BASE_CELL_WIDTH = 6;
	public static final int BASE_CELL_HEIGHT = 10;
	private static final int REFERENCE_SCREEN_WIDTH = 480;
	private static final int REFERENCE_SCREEN_HEIGHT = 270;
	private static final float MIN_SCALE = 0.65F;
	private static final float MAX_SCALE = 1.35F;

	private SandevistanHudPosition() {
	}

	public static ScreenCalculations.HorizontalBasis horizontalBasis() {
		try {
			return ScreenCalculations.HorizontalBasis.valueOf(
					EPMConfig.sandevistanHudHorizontalBasis().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException ignored) {
			return ScreenCalculations.HorizontalBasis.CENTER;
		}
	}

	public static ScreenCalculations.VerticalBasis verticalBasis() {
		try {
			return ScreenCalculations.VerticalBasis.valueOf(
					EPMConfig.sandevistanHudVerticalBasis().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException ignored) {
			return ScreenCalculations.VerticalBasis.BOTTOM;
		}
	}

	public static HudMetrics metrics(int screenWidth, int screenHeight) {
		float widthScale = screenWidth / (float)REFERENCE_SCREEN_WIDTH;
		float heightScale = screenHeight / (float)REFERENCE_SCREEN_HEIGHT;
		float scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(widthScale, heightScale)));
		int cellWidth = Math.max(1, Math.round(BASE_CELL_WIDTH * scale));
		int cellHeight = Math.max(1, Math.round(BASE_CELL_HEIGHT * scale));
		return new HudMetrics(cellWidth, cellHeight, PREVIEW_CELL_COUNT * cellWidth);
	}

	public static int screenX(int screenWidth, int contentWidth, HudMetrics metrics) {
		ScreenCalculations.HorizontalBasis basis = horizontalBasis();
		if (basis == ScreenCalculations.HorizontalBasis.CENTER) {
			return screenWidth / 2 + EPMConfig.sandevistanHudX() - contentWidth / 2;
		}
		int previewX = basis.positionGetter.apply(screenWidth, EPMConfig.sandevistanHudX());
		return switch (basis) {
			case RIGHT -> previewX + metrics.previewWidth() - contentWidth;
			case CENTER -> throw new IllegalStateException("CENTER basis handled before switch");
			case LEFT -> previewX;
		};
	}

	public static int screenY(int screenHeight, int contentHeight, HudMetrics metrics) {
		ScreenCalculations.VerticalBasis basis = verticalBasis();
		int previewY = basis.positionGetter.apply(screenHeight, EPMConfig.sandevistanHudY());
		return switch (basis) {
			case BOTTOM -> previewY + metrics.cellHeight() - contentHeight;
			case CENTER -> previewY + (metrics.cellHeight() - contentHeight) / 2;
			case TOP -> previewY;
		};
	}

	public static int setupXCoordinate(HudMetrics metrics) {
		if (horizontalBasis() == ScreenCalculations.HorizontalBasis.CENTER) {
			return EPMConfig.sandevistanHudX() - metrics.previewWidth() / 2;
		}
		return EPMConfig.sandevistanHudX();
	}

	public static void setXFromSetup(
			int value,
			ScreenCalculations.HorizontalBasis basis,
			HudMetrics metrics) {
		setX(basis == ScreenCalculations.HorizontalBasis.CENTER
				? value + metrics.previewWidth() / 2
				: value);
	}

	public static void setX(int value) {
		EPMConfig.setSandevistanHudX(value);
	}

	public static void setY(int value) {
		EPMConfig.setSandevistanHudY(value);
	}

	public static void setHorizontalBasis(ScreenCalculations.HorizontalBasis value) {
		if (value != null) {
			EPMConfig.setSandevistanHudHorizontalBasis(value.name());
		}
	}

	public static void setVerticalBasis(ScreenCalculations.VerticalBasis value) {
		if (value != null) {
			EPMConfig.setSandevistanHudVerticalBasis(value.name());
		}
	}

	public record HudMetrics(int cellWidth, int cellHeight, int previewWidth) {
		public int barWidth(int cellCount) {
			return Math.max(0, cellCount) * this.cellWidth;
		}
	}
}
