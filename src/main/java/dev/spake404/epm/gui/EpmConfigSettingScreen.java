package dev.spake404.epm.gui;

import com.alrex.parcool.client.gui.ColorTheme;
import com.alrex.parcool.client.gui.ParCoolSettingScreen;
import com.alrex.parcool.common.info.ActionInfo;
import dev.spake404.epm.EPM;
import dev.spake404.epm.EPMConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EpmConfigSettingScreen extends ParCoolSettingScreen {

	private static final List<Row> ROWS = new ArrayList<>();

	static {
		// Natural Sprinter
		addHeader("Natural Sprinter");
		addBool("naturalSprinterAnimations", "NATURAL_SPRINTER_ANIMATIONS");
		addBool("naturalSprinterManualStep", "NATURAL_SPRINTER_MANUAL_STEP");
		addEnum("naturalSprinterStepDodgeConflictMode", "NATURAL_SPRINTER_STEP_DODGE_CONFLICT_MODE",
				new String[]{"DISABLED", "SHORT_DODGE_LONG_STEP", "SHORT_STEP_HOLD_DODGE", "SINGLE_STEP_DOUBLE_DODGE"});
		addInt("naturalSprinterStepDodgeLongPressTicks", "NATURAL_SPRINTER_STEP_DODGE_LONG_PRESS_TICKS", 1, 40, 1);
		addInt("naturalSprinterStepDodgeDoubleTapGapTicks", "NATURAL_SPRINTER_STEP_DODGE_DOUBLE_TAP_GAP_TICKS", 1, 20, 1);
		addInt("naturalSprinterStepDodgeFirstTapMaxTicks", "NATURAL_SPRINTER_STEP_DODGE_FIRST_TAP_MAX_TICKS", 1, 40, 1);
		addBool("fastRunStartStepAnimation", "FAST_RUN_START_STEP_ANIMATION");
		addBool("autoFastRunDash", "AUTO_FAST_RUN_DASH");

		// Phantom Ascent
		addHeader("Phantom Ascent");
		addBool("catLeapPrimesPhantomAscent", "CAT_LEAP_PRIMES_PHANTOM_ASCENT");
		addBool("wallJumpPrimesPhantomAscent", "WALL_JUMP_PRIMES_PHANTOM_ASCENT");
		addBool("spiderWallJumpPrimesPhantomAscent", "SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT");
		addBool("disablePhantomAscentUnderwaterSwimming", "DISABLE_PHANTOM_ASCENT_UNDERWATER_SWIMMING");
		addDouble("phantomAscentFallProtectionDamageThreshold", "PHANTOM_ASCENT_FALL_PROTECTION_DAMAGE_THRESHOLD", 0.0, 100.0, 0.5);

		// Demolition Leap
		addHeader("Demolition Leap");
		addBool("demolitionLeapShiftSpaceReplacement", "DEMOLITION_LEAP_SHIFT_SPACE_REPLACEMENT");
		addBool("demolitionLeapAirDoubleJump", "DEMOLITION_LEAP_AIR_DOUBLE_JUMP");
		addBool("demolitionLeapChargeJumpAnimation", "DEMOLITION_LEAP_CHARGE_JUMP_ANIMATION");

		// WallJump
		addHeader("WallJump");
		addBool("autoSprintAfterWallJump", "AUTO_SPRINT_AFTER_WALL_JUMP");
		addBool("wallJumpPrimesAirAttack", "WALL_JUMP_PRIMES_AIR_ATTACK");
		addBool("taczShootDuringWallJump", "TACZ_SHOOT_DURING_WALL_JUMP");
		addDouble("wallJumpAirAttackFallProtectionDamageThreshold", "WALL_JUMP_AIR_ATTACK_FALL_PROTECTION_DAMAGE_THRESHOLD", 0.0, 100.0, 0.5);

		// Spider Techniques
		addHeader("Spider Techniques");
		addBool("disableVerticalWallRunWithSpiderTechniques", "DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES");
		addEnum("spiderTechniquesWallRunMode", "SPIDER_TECHNIQUES_WALL_RUN_MODE",
				new String[]{"DEFAULT", "PARCOOL", "WOM"});
		addDouble("parCoolWallRunAnimationTransition", "PARCOOL_WALL_RUN_ANIMATION_TRANSITION", 0.0, 0.5, 0.02);

		// Aqua Maneuvre
		addHeader("Aqua Maneuvre");
		addBool("aquaManeuvreFastSwimAnimation", "AQUA_MANEUVRE_FAST_SWIM_ANIMATION");

		// EpicFightX
		addHeader("EpicFightX");
		addBool("epicFightXCombatMasteryFastRunControlCompatibility", "EPICFIGHTX_COMBAT_MASTERY_FAST_RUN_CONTROL_COMPATIBILITY");
		addEnum("epicFightXCombatMasterySprintTriggerMode", "EPICFIGHTX_COMBAT_MASTERY_SPRINT_TRIGGER_MODE",
				new String[]{"Toggle", "Auto", "PressKey"});

		// Vault
		addHeader("Vault");
		addBool("fastRunVaultChainFix", "FAST_RUN_VAULT_CHAIN_FIX");
		addDouble("vaultHeightScale", "VAULT_HEIGHT_SCALE", 0.86, 2.0, 0.1);

		// Climb
		addHeader("Climb");
		addDouble("epicParCoolClimbUpVerticalVelocity", "EPIC_PARCOOL_CLIMB_UP_VERTICAL_VELOCITY", 0.6, 0.8, 0.05);
		addDouble("epicParCoolClimbUpLateralAirControlVelocity", "EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_VELOCITY", 0.0, 0.05, 0.01);
		addInt("epicParCoolClimbUpLateralAirControlTicks", "EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_TICKS", 0, 20, 1);

		// Debug
		addHeader("Debug");
		addBool("debugSpiderTechniquesAttackState", "DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE");
		addBool("debugSpiderWallRunState", "DEBUG_SPIDER_WALL_RUN_STATE");
		addBool("debugAquaManeuvreFastSwimState", "DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE");
		addBool("debugEpicFightXCombatMasterySprintState", "DEBUG_EPICFIGHTX_COMBAT_MASTERY_SPRINT_STATE");
		addBool("debugVaultState", "DEBUG_VAULT_STATE");
		addBool("debugGliderState", "DEBUG_GLIDER_STATE");
		addBool("debugNaturalSprinterFastRunStepState", "DEBUG_NATURAL_SPRINTER_FAST_RUN_STEP_STATE");
		addBool("debugCameraEventState", "DEBUG_CAMERA_EVENT_STATE");
	}

	private static void addHeader(String title) {
		ROWS.add(new Row(title));
	}

	private static void addBool(String key, String fieldName) {
		ROWS.add(new Row(key, fieldName));
	}

	private static void addDouble(String key, String fieldName, double min, double max, double step) {
		ROWS.add(new Row(key, fieldName, min, max, step));
	}

	private static void addInt(String key, String fieldName, int min, int max, int step) {
		ROWS.add(new Row(key, fieldName, min, max, step));
	}

	private static void addEnum(String key, String fieldName, String[] values) {
		ROWS.add(new Row(key, fieldName, values));
	}

	private final Checkbox[] boolCheckboxes;
	private int lastContentTop;

	public EpmConfigSettingScreen(Component title, ActionInfo info, ColorTheme theme) {
		super(title, info, theme);
		this.currentScreen = 4;

		// Count bool rows for checkbox array
		int boolCount = 0;
		for (Row row : ROWS) {
			if (row.kind == Kind.BOOL) boolCount++;
		}
		boolCheckboxes = new Checkbox[boolCount];

		int cbIdx = 0;
		for (int i = 0; i < ROWS.size(); i++) {
			Row row = ROWS.get(i);
			if (row.kind == Kind.BOOL) {
				row.widgetIndex = cbIdx;
				String name = Component.translatable("epic_parcool_momentum.configuration." + row.key).getString();
				boolCheckboxes[cbIdx] = new Checkbox(0, 0, 0, 21, Component.literal(name), readBoolField(row.fieldName));
				cbIdx++;
			} else if (row.kind == Kind.DOUBLE || row.kind == Kind.INT || row.kind == Kind.ENUM) {
				row.leftBtn = net.minecraft.client.gui.components.Button
						.builder(Component.literal("<"), btn -> row.decrement(EpmConfigSettingScreen.this))
						.pos(0, 0).size(15, 20).build();
				row.valueBtn = net.minecraft.client.gui.components.Button
						.builder(Component.literal(""), btn -> {})
						.pos(0, 0).size(60, 20).build();
				row.rightBtn = net.minecraft.client.gui.components.Button
						.builder(Component.literal(">"), btn -> row.increment(EpmConfigSettingScreen.this))
						.pos(0, 0).size(15, 20).build();
			}
		}
	}

	// ---- Reflection helpers ----

	private boolean readBoolField(String fieldName) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object v = f.get(null);
			if (v instanceof net.minecraftforge.common.ForgeConfigSpec.BooleanValue bv) return bv.get();
		} catch (Exception ignored) {}
		return false;
	}

	private double readDoubleField(String fieldName) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object v = f.get(null);
			if (v instanceof net.minecraftforge.common.ForgeConfigSpec.DoubleValue dv) return dv.get();
		} catch (Exception ignored) {}
		return 0.0;
	}

	private int readIntField(String fieldName) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object v = f.get(null);
			if (v instanceof net.minecraftforge.common.ForgeConfigSpec.IntValue iv) return iv.get();
		} catch (Exception ignored) {}
		return 0;
	}

	private String readEnumField(String fieldName) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object v = f.get(null);
			if (v instanceof net.minecraftforge.common.ForgeConfigSpec.EnumValue<?> ev) return String.valueOf(ev.get());
		} catch (Exception ignored) {}
		return "";
	}

	private void writeDoubleField(String fieldName, double value) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object obj = f.get(null);
			if (obj instanceof net.minecraftforge.common.ForgeConfigSpec.DoubleValue dv) dv.set(value);
		} catch (Exception e) {
			EPM.LOGGER.error("[EPM] Failed to write double field: {}", fieldName, e);
		}
	}

	private void writeIntField(String fieldName, int value) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object obj = f.get(null);
			if (obj instanceof net.minecraftforge.common.ForgeConfigSpec.IntValue iv) iv.set(value);
		} catch (Exception e) {
			EPM.LOGGER.error("[EPM] Failed to write int field: {}", fieldName, e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void writeEnumField(String fieldName, String value) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object obj = f.get(null);
			if (obj instanceof net.minecraftforge.common.ForgeConfigSpec.EnumValue ev) {
				for (Enum<?> e : ((Class<Enum>) ev.get().getClass()).getEnumConstants()) {
					if (e.name().equals(value)) {
						ev.set(e);
						return;
					}
				}
			}
		} catch (Exception e) {
			EPM.LOGGER.error("[EPM] Failed to write enum field: {}", fieldName, e);
		}
	}

	// ---- Scroll ----

	@Override
	protected boolean isDownScrollable() {
		return topIndex + viewableItemCount < ROWS.size();
	}

	// ---- Render ----

	@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTick, int topOffset, int bottomOffset) {
		lastContentTop = topOffset;
		int contentWidth = width - 80;
		int contentHeight = height - topOffset - bottomOffset;
		viewableItemCount = contentHeight / 21;

		for (Checkbox cb : boolCheckboxes) {
			cb.setWidth(0);
		}

		int visibleEnd = Math.min(topIndex + viewableItemCount, ROWS.size());
		for (int i = topIndex; i < visibleEnd; i++) {
			Row row = ROWS.get(i);
			int y = topOffset + 21 * (i - topIndex);

			if (row.kind == Kind.HEADER) {
				graphics.drawString(font, Component.literal(row.title), 43, y + 6, color.getStrongText());
				graphics.fill(40, y + 20, width - 40, y + 21, color.getStrongText());
				continue;
			}

					if (row.kind == Kind.BOOL) {
				// Checkbox renders its own label
				Checkbox cb = boolCheckboxes[row.widgetIndex];
				cb.setX(41);
				cb.setY(y);
				cb.setWidth(contentWidth);
				cb.setHeight(20);
				cb.render(graphics, mouseX, mouseY, partialTick);
			} else {
				// Label for numeric/enum (not auto-rendered by a widget)
				String label = Component.translatable("epic_parcool_momentum.configuration." + row.key).getString();
				graphics.drawString(font, label, 43, y + 6, color.getText());
				// Numeric/enum: value display + arrow buttons
				String valStr = row.getValueString(this);
				int arrowW = 15;
				int valW = font.width(valStr) + 16;
				int totalW = arrowW + valW + arrowW;
				int startX = width - 45 - totalW;

				// Left arrow
				row.leftBtn.setX(startX);
				row.leftBtn.setY(y);
				row.leftBtn.setWidth(arrowW);
				row.leftBtn.setHeight(20);
				row.leftBtn.render(graphics, mouseX, mouseY, partialTick);

				// Value
				row.valueBtn.setMessage(Component.literal(valStr));
				row.valueBtn.setX(startX + arrowW);
				row.valueBtn.setY(y);
				row.valueBtn.setWidth(valW);
				row.valueBtn.setHeight(20);
				row.valueBtn.active = true;
				row.valueBtn.render(graphics, mouseX, mouseY, partialTick);

				// Right arrow
				row.rightBtn.setX(startX + arrowW + valW);
				row.rightBtn.setY(y);
				row.rightBtn.setWidth(arrowW);
				row.rightBtn.setHeight(20);
				row.rightBtn.render(graphics, mouseX, mouseY, partialTick);
			}

			graphics.fill(40, y + 20, width - 40, y + 21, color.getSubSeparator());
		}

		graphics.fill(width - 40, topOffset, width - 40 + 1, topOffset + contentHeight, color.getSeparator());
		graphics.fill(40, topOffset, 41, topOffset + contentHeight, color.getSeparator());
	}

	// ---- Input ----

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int type) {
		int contentWidth = width - 80;
		int visibleEnd = Math.min(topIndex + viewableItemCount, ROWS.size());
		for (int i = topIndex; i < visibleEnd; i++) {
			Row row = ROWS.get(i);
			if (row.kind == Kind.HEADER) continue;

			if (row.kind == Kind.BOOL) {
				int y = lastContentTop + (i - topIndex) * 21;
				Checkbox cb = boolCheckboxes[row.widgetIndex];
				cb.setX(41);
				cb.setY(y);
				cb.setWidth(contentWidth);
				cb.setHeight(20);
				if (cb.mouseClicked(mouseX, mouseY, type)) return true;
			} else {
				// Numeric/enum: check arrow button clicks
				if (row.leftBtn != null && row.leftBtn.mouseClicked(mouseX, mouseY, type)) return true;
				if (row.rightBtn != null && row.rightBtn.mouseClicked(mouseX, mouseY, type)) return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, type);
	}

	// ---- Save ----

	@Override
	protected void save() {
		for (Row row : ROWS) {
			if (row.kind == Kind.BOOL) {
				writeBoolField(row.fieldName, boolCheckboxes[row.widgetIndex].selected());
			}
		}
		EPMConfig.SPEC.save();
		EPM.LOGGER.info("[EPM] Config saved from GUI");
	}

	private void writeBoolField(String fieldName, boolean value) {
		try {
			Field f = EPMConfig.class.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object obj = f.get(null);
			if (obj instanceof net.minecraftforge.common.ForgeConfigSpec.BooleanValue bv) {
				bv.set(value);
			}
		} catch (Exception e) {
			EPM.LOGGER.error("[EPM] Failed to write bool field: {}", fieldName, e);
		}
	}

	// ---- Row model ----

	private enum Kind {HEADER, BOOL, DOUBLE, INT, ENUM}

	private static class Row {
		final Kind kind;
		final String title;          // HEADER
		final String key;            // FIELD
		final String fieldName;      // FIELD
		final double dMin, dMax, dStep; // DOUBLE
		final int iMin, iMax, iStep;    // INT
		final String[] enumValues;       // ENUM
		int widgetIndex = -1;            // index into boolCheckboxes or -1
		net.minecraft.client.gui.components.Button leftBtn, valueBtn, rightBtn;

		// Header
		Row(String title) {
			this.kind = Kind.HEADER;
			this.title = title;
			this.key = null; this.fieldName = null;
			this.dMin = 0; this.dMax = 0; this.dStep = 0;
			this.iMin = 0; this.iMax = 0; this.iStep = 0;
			this.enumValues = null;
		}

		// Bool
		Row(String key, String fieldName) {
			this.kind = Kind.BOOL;
			this.title = null;
			this.key = key; this.fieldName = fieldName;
			this.dMin = 0; this.dMax = 0; this.dStep = 0;
			this.iMin = 0; this.iMax = 0; this.iStep = 0;
			this.enumValues = null;
		}

		// Double
		Row(String key, String fieldName, double min, double max, double step) {
			this.kind = Kind.DOUBLE;
			this.title = null;
			this.key = key; this.fieldName = fieldName;
			this.dMin = min; this.dMax = max; this.dStep = step;
			this.iMin = 0; this.iMax = 0; this.iStep = 0;
			this.enumValues = null;
		}

		// Int
		Row(String key, String fieldName, int min, int max, int step) {
			this.kind = Kind.INT;
			this.title = null;
			this.key = key; this.fieldName = fieldName;
			this.dMin = 0; this.dMax = 0; this.dStep = 0;
			this.iMin = min; this.iMax = max; this.iStep = step;
			this.enumValues = null;
		}

		// Enum
		Row(String key, String fieldName, String[] values) {
			this.kind = Kind.ENUM;
			this.title = null;
			this.key = key; this.fieldName = fieldName;
			this.dMin = 0; this.dMax = 0; this.dStep = 0;
			this.iMin = 0; this.iMax = 0; this.iStep = 0;
			this.enumValues = values;
		}

		String getValueString(EpmConfigSettingScreen s) {
			return switch (kind) {
				case DOUBLE -> String.format("%.2f", s.readDoubleField(fieldName));
				case INT -> String.valueOf(s.readIntField(fieldName));
				case ENUM -> translatedEnumValue(s.readEnumField(fieldName));
				default -> "";
			};
		}

		private String translatedEnumValue(String value) {
			String translationKey = "epic_parcool_momentum.configuration." + key + "." + value.toLowerCase(Locale.ROOT);
			String translated = Component.translatable(translationKey).getString();
			return translationKey.equals(translated) ? value : translated;
		}

		void increment(EpmConfigSettingScreen s) {
			switch (kind) {
				case DOUBLE -> {
					double v = s.readDoubleField(fieldName) + dStep;
					if (v > dMax) v = dMax;
					s.writeDoubleField(fieldName, v);
				}
				case INT -> {
					int v = s.readIntField(fieldName) + iStep;
					if (v > iMax) v = iMax;
					s.writeIntField(fieldName, v);
				}
				case ENUM -> {
					String cur = s.readEnumField(fieldName);
					for (int idx = 0; idx < enumValues.length; idx++) {
						if (enumValues[idx].equals(cur)) {
							s.writeEnumField(fieldName, enumValues[(idx + 1) % enumValues.length]);
							return;
						}
					}
				}
			}
			EPMConfig.SPEC.save();
		}

		void decrement(EpmConfigSettingScreen s) {
			switch (kind) {
				case DOUBLE -> {
					double v = s.readDoubleField(fieldName) - dStep;
					if (v < dMin) v = dMin;
					s.writeDoubleField(fieldName, v);
				}
				case INT -> {
					int v = s.readIntField(fieldName) - iStep;
					if (v < iMin) v = iMin;
					s.writeIntField(fieldName, v);
				}
				case ENUM -> {
					String cur = s.readEnumField(fieldName);
					for (int idx = 0; idx < enumValues.length; idx++) {
						if (enumValues[idx].equals(cur)) {
							int prev = (idx - 1 + enumValues.length) % enumValues.length;
							s.writeEnumField(fieldName, enumValues[prev]);
							return;
						}
					}
				}
			}
			EPMConfig.SPEC.save();
		}
	}
}
