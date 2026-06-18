package dev.spake404.epm;

import java.util.List;
import java.util.Locale;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EPMConfig {
	public static final ForgeConfigSpec SPEC;
	private static final ForgeConfigSpec.BooleanValue NATURAL_SPRINTER_ANIMATIONS;
	private static final ForgeConfigSpec.BooleanValue NATURAL_SPRINTER_MANUAL_STEP;
	private static final ForgeConfigSpec.EnumValue<StepDodgeConflictMode> NATURAL_SPRINTER_STEP_DODGE_CONFLICT_MODE;
	private static final ForgeConfigSpec.IntValue NATURAL_SPRINTER_STEP_DODGE_LONG_PRESS_TICKS;
	private static final ForgeConfigSpec.IntValue NATURAL_SPRINTER_STEP_DODGE_DOUBLE_TAP_GAP_TICKS;
	private static final ForgeConfigSpec.IntValue NATURAL_SPRINTER_STEP_DODGE_FIRST_TAP_MAX_TICKS;
	private static final ForgeConfigSpec.BooleanValue FAST_RUN_START_STEP_ANIMATION;
	private static final ForgeConfigSpec.BooleanValue AUTO_FAST_RUN_DASH;
	private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_BAREHAND_SPRINT_TYPES;
	private static final ForgeConfigSpec.BooleanValue CAT_LEAP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue WALL_JUMP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.DoubleValue PHANTOM_ASCENT_FALL_PROTECTION_DAMAGE_THRESHOLD;
	private static final ForgeConfigSpec.BooleanValue DISABLE_PHANTOM_ASCENT_UNDERWATER_SWIMMING;
	private static final ForgeConfigSpec.BooleanValue DEMOLITION_LEAP_SHIFT_SPACE_REPLACEMENT;
	private static final ForgeConfigSpec.BooleanValue DEMOLITION_LEAP_AIR_DOUBLE_JUMP;
	private static final ForgeConfigSpec.BooleanValue DEMOLITION_LEAP_CHARGE_JUMP_ANIMATION;
	private static final ForgeConfigSpec.BooleanValue AUTO_SPRINT_AFTER_WALL_JUMP;
	private static final ForgeConfigSpec.BooleanValue WALL_JUMP_PRIMES_AIR_ATTACK;
	private static final ForgeConfigSpec.BooleanValue TACZ_SHOOT_DURING_WALL_JUMP;
	private static final ForgeConfigSpec.DoubleValue WALL_JUMP_AIR_ATTACK_FALL_PROTECTION_DAMAGE_THRESHOLD;
	private static final ForgeConfigSpec.BooleanValue SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.EnumValue<SpiderTechniquesWallRunMode> SPIDER_TECHNIQUES_WALL_RUN_MODE;
	private static final ForgeConfigSpec.DoubleValue PARCOOL_WALL_RUN_ANIMATION_TRANSITION;
	private static final ForgeConfigSpec.BooleanValue DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES;
	private static final ForgeConfigSpec.BooleanValue DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_SPIDER_WALL_RUN_STATE;
	private static final ForgeConfigSpec.BooleanValue FAST_RUN_VAULT_CHAIN_FIX;
	private static final ForgeConfigSpec.DoubleValue VAULT_HEIGHT_SCALE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_VAULT_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_GLIDER_STATE;
	private static final ForgeConfigSpec.BooleanValue AQUA_MANEUVRE_FAST_SWIM_ANIMATION;
	private static final ForgeConfigSpec.BooleanValue DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE;
	private static final ForgeConfigSpec.DoubleValue EPIC_PARCOOL_CLIMB_UP_VERTICAL_VELOCITY;
	private static final ForgeConfigSpec.DoubleValue EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_VELOCITY;
	private static final ForgeConfigSpec.IntValue EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_TICKS;
	private static final ForgeConfigSpec.BooleanValue PARCOOL_DODGE_DEFAULT_MIGRATION_APPLIED;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("Natural Sprinter");
		NATURAL_SPRINTER_ANIMATIONS = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterAnimations")
				.comment(
						"true: uses WOM Natural Sprinter animations for EpicParCool FastRun and related step/jump visuals.",
						"false: keeps this mod's Natural Sprinter compatibility logic, but shows normal run/EpicParCool animations and disables the R-key Natural Sprinter step.")
				.define("naturalSprinterAnimations", true);
		NATURAL_SPRINTER_MANUAL_STEP = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterManualStep")
				.comment(
						"true: pressing the configured Natural Sprinter Step key can trigger a step during EpicParCool FastRun.",
						"This only works when naturalSprinterAnimations is also true.")
				.define("naturalSprinterManualStep", true);
		NATURAL_SPRINTER_STEP_DODGE_CONFLICT_MODE = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterStepDodgeConflictMode")
				.comment(
						"Controls how Natural Sprinter Step and ParCool Dodge share one key.",
						"When ParCool Dodge is disabled in ParCool settings, this option is ignored and the shared key behaves as Natural Sprinter Step.",
						"disabled: do not arbitrate. Step and Dodge keep their normal behavior.",
						"short_dodge_long_step: short press triggers Dodge; holding then releasing triggers Step. This is the default.",
						"short_step_hold_dodge: short press triggers Step; holding triggers Dodge. Releasing after Dodge has fully ended can trigger Step.",
						"single_step_double_dodge: single tap triggers Step after the double-tap window; double tap triggers Dodge.")
				.defineEnum("naturalSprinterStepDodgeConflictMode", StepDodgeConflictMode.SHORT_DODGE_LONG_STEP);
		NATURAL_SPRINTER_STEP_DODGE_LONG_PRESS_TICKS = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterStepDodgeLongPressTicks")
				.comment("Ticks held before a shared Step/Dodge key counts as a long press. 6 ticks is about 300 ms at 20 TPS.")
				.defineInRange("naturalSprinterStepDodgeLongPressTicks", 6, 1, 40);
		NATURAL_SPRINTER_STEP_DODGE_DOUBLE_TAP_GAP_TICKS = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterStepDodgeDoubleTapGapTicks")
				.comment("Maximum ticks between first release and second press for single_step_double_dodge. Single-tap Step is delayed by this window. Default 2 matches ParCool's native movement-key double-tap window.")
				.defineInRange("naturalSprinterStepDodgeDoubleTapGapTicks", 2, 1, 20);
		NATURAL_SPRINTER_STEP_DODGE_FIRST_TAP_MAX_TICKS = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterStepDodgeFirstTapMaxTicks")
				.comment("Maximum held ticks for the first tap to be eligible for double-tap Dodge.")
				.defineInRange("naturalSprinterStepDodgeFirstTapMaxTicks", 5, 1, 40);
		FAST_RUN_START_STEP_ANIMATION = builder
				.translation("epic_parcool_momentum.configuration.fastRunStartStepAnimation")
				.comment(
						"true: allows one startup step when entering FastRun.",
						"false: entering FastRun never plays a startup step.")
				.define("fastRunStartStepAnimation", true);
		AUTO_FAST_RUN_DASH = builder
				.translation("epic_parcool_momentum.configuration.autoFastRunDash")
				.comment(
						"true: when FastRun is entered by a non-manual path, it can still auto-trigger one startup step.",
						"false: non-manual FastRun entry does not auto-trigger a startup step; only the manual FastRun key path can do it.")
				.define("autoFastRunDash", true);
		TACZ_BAREHAND_SPRINT_TYPES = builder
				.translation("epic_parcool_momentum.configuration.taczBarehandSprintTypes")
				.comment(
						"TaCZ gun index types that use WOM barehand sprint.",
						"Common types include: pistol, smg, rifle, shotgun, sniper, mg.",
						"Types not listed here use WOM weapon sprint. Set to [] to make all TaCZ gun types use WOM weapon sprint.")
				.defineList("taczBarehandSprintTypes", List.of("pistol"), EPMConfig::isStringValue);
		builder.pop();

		builder.push("Phantom Ascent");
		CAT_LEAP_PRIMES_PHANTOM_ASCENT = builder
				.translation("epic_parcool_momentum.configuration.catLeapPrimesPhantomAscent")
				.comment("true: pressing jump after CatLeap can trigger Epic Fight Phantom Ascent.")
				.define("catLeapPrimesPhantomAscent", true);
		WALL_JUMP_PRIMES_PHANTOM_ASCENT = builder
				.translation("epic_parcool_momentum.configuration.wallJumpPrimesPhantomAscent")
				.comment("true: pressing jump after ParCool WallJump can trigger Epic Fight Phantom Ascent.")
				.define("wallJumpPrimesPhantomAscent", true);
		SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT = builder
				.translation("epic_parcool_momentum.configuration.spiderWallJumpPrimesPhantomAscent")
				.comment("true: pressing jump after Spider Techniques wall jump can trigger Epic Fight Phantom Ascent.")
				.define("spiderWallJumpPrimesPhantomAscent", true);
		PHANTOM_ASCENT_FALL_PROTECTION_DAMAGE_THRESHOLD = builder
				.translation("epic_parcool_momentum.configuration.phantomAscentFallProtectionDamageThreshold")
				.comment("Maximum fall damage canceled by Phantom Ascent's next-fall protection. Epic Fight original default is 2.5.")
				.defineInRange("phantomAscentFallProtectionDamageThreshold", 2.5D, 0.0D, 100.0D);
		DISABLE_PHANTOM_ASCENT_UNDERWATER_SWIMMING = builder
				.translation("epic_parcool_momentum.configuration.disablePhantomAscentUnderwaterSwimming")
				.comment(
						"true: blocks Epic Fight Phantom Ascent while the player is swimming underwater.",
						"false: keeps Epic Fight's original Phantom Ascent behavior underwater.")
				.define("disablePhantomAscentUnderwaterSwimming", true);
		builder.pop();

		builder.push("Demolition Leap");
		DEMOLITION_LEAP_SHIFT_SPACE_REPLACEMENT = builder
				.translation("epic_parcool_momentum.configuration.demolitionLeapShiftSpaceReplacement")
				.comment(
						"true: after learning Epic Fight Demolition Leap, Shift+Jump starts Demolition Leap through Epic Fight's original hold/release chain and suppresses ParCool Cat Leap/Charge Jump on that input.",
						"false: keeps Epic Fight Demolition Leap and ParCool Cat Leap/Charge Jump trigger paths separate.")
				.define("demolitionLeapShiftSpaceReplacement", true);
		DEMOLITION_LEAP_AIR_DOUBLE_JUMP = builder
				.translation("epic_parcool_momentum.configuration.demolitionLeapAirDoubleJump")
				.comment("true: after Demolition Leap launches the player, the next new airborne jump press triggers Epic Fight Phantom Ascent through its native chain.")
				.define("demolitionLeapAirDoubleJump", true);
		DEMOLITION_LEAP_CHARGE_JUMP_ANIMATION = builder
				.translation("epic_parcool_momentum.configuration.demolitionLeapChargeJumpAnimation")
				.comment("true: before learning Demolition Leap, ParCool Charge Jump charging uses Demolition Leap's charging animation in Epic Fight mode. After learning Demolition Leap, Shift alone shows no charging animation — only Shift+Space triggers the skill.")
				.define("demolitionLeapChargeJumpAnimation", true);
		builder.pop();

		builder.push("WallJump");
		AUTO_SPRINT_AFTER_WALL_JUMP = builder
				.translation("epic_parcool_momentum.configuration.autoSprintAfterWallJump")
				.comment("true: automatically restores sprint shortly after ParCool WallJump.")
				.define("autoSprintAfterWallJump", true);
		WALL_JUMP_PRIMES_AIR_ATTACK = builder
				.translation("epic_parcool_momentum.configuration.wallJumpPrimesAirAttack")
				.comment("true: ParCool WallJump opens a short window for Epic Fight air attacks.")
				.define("wallJumpPrimesAirAttack", true);
		TACZ_SHOOT_DURING_WALL_JUMP = builder
				.translation("epic_parcool_momentum.configuration.taczShootDuringWallJump")
				.comment("true: TaCZ guns can cancel ParCool WallJump and fire immediately.")
				.define("taczShootDuringWallJump", true);
		WALL_JUMP_AIR_ATTACK_FALL_PROTECTION_DAMAGE_THRESHOLD = builder
				.translation("epic_parcool_momentum.configuration.wallJumpAirAttackFallProtectionDamageThreshold")
				.comment("Maximum fall damage canceled after a ParCool WallJump air-attack window. Matches Phantom Ascent's default value.")
				.defineInRange("wallJumpAirAttackFallProtectionDamageThreshold", 2.5D, 0.0D, 100.0D);
		builder.pop();

		builder.push("Spider Techniques");
		SPIDER_TECHNIQUES_WALL_RUN_MODE = builder
				.translation("epic_parcool_momentum.configuration.spiderTechniquesWallRunMode")
				.comment(
						"Wall-run mode used after learning WOM Spider Techniques.",
						"default: Default wall-run mode. This mod does not replace ParCool/WOM wall-run triggers, but keeps original WOM side wall-run direction and corner-transfer fixes.",
						"parcool: ParCool wall-run mode. ParCool's HorizontalWallRun key triggers this mod's WOM-style wall run replacement.",
						"wom: WOM wall-run mode. WOM's original sprint input owns Spider Techniques wall run, while ParCool HorizontalWallRun and its key input are disabled.",
						"Modes that need WOM do nothing when WOM is not installed or Spider Techniques is not learned.")
				.defineEnum("spiderTechniquesWallRunMode", SpiderTechniquesWallRunMode.PARCOOL);
		PARCOOL_WALL_RUN_ANIMATION_TRANSITION = builder
				.translation("epic_parcool_momentum.configuration.parCoolWallRunAnimationTransition")
				.comment(
						"Animation transition time used when ParCool wall-run mode switches WOM Spider Techniques wall-run animations.",
						"Higher values feel heavier but make direction changes slower.")
				.defineInRange("parCoolWallRunAnimationTransition", 0.12D, 0.0D, 0.5D);
		DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES = builder
				.translation("epic_parcool_momentum.configuration.disableVerticalWallRunWithSpiderTechniques")
				.comment("true: disables ParCool VerticalWallRun after learning WOM Spider Techniques.")
				.define("disableVerticalWallRunWithSpiderTechniques", true);
		DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugSpiderTechniquesAttackState")
				.comment("Temporary debug option. true: logs Spider Techniques state when attack executability is checked.")
				.define("debugSpiderTechniquesAttackState", false);
		DEBUG_SPIDER_WALL_RUN_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugSpiderWallRunState")
				.comment("Temporary debug option. true: logs detailed Spider Techniques wall-run state.")
				.define("debugSpiderWallRunState", false);
		builder.pop();

		builder.push("Aqua Maneuvre");
		AQUA_MANEUVRE_FAST_SWIM_ANIMATION = builder
				.translation("epic_parcool_momentum.configuration.aquaManeuvreFastSwimAnimation")
				.comment(
						"true: without Aqua Maneuvre, maps ParCool FastSwimAnimator to WOM's Mermaid animation in Epic Fight mode.",
						"After learning Aqua Maneuvre, ParCool's FastRun control mode only drives WOM's original Mermaid fast-swim input; WOM owns movement, speed, animation start, and animation exit.")
				.define("aquaManeuvreFastSwimAnimation", true);
		DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugAquaManeuvreFastSwimState")
				.comment("Temporary debug option. true: logs detailed Aqua Maneuvre fast-swim state.")
				.define("debugAquaManeuvreFastSwimState", false);
		builder.pop();

		builder.push("Vault");
		FAST_RUN_VAULT_CHAIN_FIX = builder
				.translation("epic_parcool_momentum.configuration.fastRunVaultChainFix")
				.comment(
						"true: when Vault starts from FastRun, releases ParCool's Vault action at 6 ticks and keeps a short FastRun grace window so close obstacles can chain through ParCool's original Vault detection.",
						"false: keeps ParCool's original Vault action timing and disables this mod's FastRun Vault chaining assist.")
				.define("fastRunVaultChainFix", true);
		VAULT_HEIGHT_SCALE = builder
				.translation("epic_parcool_momentum.configuration.vaultHeightScale")
				.comment(
						"Changes ParCool Vault's maximum detected obstacle height.",
						"ParCool original default is 0.86. This compatibility mod defaults to 1.5 for stable three-block air vaults.",
						"Lower values require more precise jump timing.")
				.defineInRange("vaultHeightScale", 1.5D, 0.86D, 2.0D);
		DEBUG_VAULT_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugVaultState")
				.comment("Temporary debug option. true: logs detailed ParCool Vault canStart, movement, and post-vault state.")
				.define("debugVaultState", false);
		builder.pop();

		builder.push("Debug");
		DEBUG_GLIDER_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugGliderState")
				.comment("Temporary debug option. true: logs detailed Glider/FastRun diagnostic state.")
				.define("debugGliderState", false);
		builder.pop();

		builder.push("Climb");
		EPIC_PARCOOL_CLIMB_UP_VERTICAL_VELOCITY = builder
				.translation("epic_parcool_momentum.configuration.epicParCoolClimbUpVerticalVelocity")
				.comment(
						"Vertical velocity applied when EpicFight mode starts ParCool ClimbUp.",
						"ParCool original value is 0.6. This defaults to 0.65 to better match EpicFightParCool's climb-up animation with re-grabbing full blocks.")
				.defineInRange("epicParCoolClimbUpVerticalVelocity", 0.65D, 0.6D, 0.8D);
		EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_VELOCITY = builder
				.translation("epic_parcool_momentum.configuration.epicParCoolClimbUpLateralAirControlVelocity")
				.comment(
						"Per-tick left/right air-control velocity applied during EpicParCool ClimbUp.",
						"Set to 0.0 to disable. This only affects left/right input and is intended to mimic ParCool's vanilla air movement.")
				.defineInRange("epicParCoolClimbUpLateralAirControlVelocity", 0.02D, 0.0D, 0.05D);
		EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_TICKS = builder
				.translation("epic_parcool_momentum.configuration.epicParCoolClimbUpLateralAirControlTicks")
				.comment(
						"How many ticks after EpicParCool ClimbUp starts left/right air-control compensation can be applied.",
						"Set to 0 to disable.")
				.defineInRange("epicParCoolClimbUpLateralAirControlTicks", 6, 0, 20);
		builder.pop();

		builder.push("Internal");
		PARCOOL_DODGE_DEFAULT_MIGRATION_APPLIED = builder
				.comment("Internal marker. Prevents Epic ParCool: Momentum from overriding a player-enabled ParCool Dodge setting after the default-off migration has run once.")
				.define("parCoolDodgeDefaultMigrationApplied", false);
		builder.pop();
		SPEC = builder.build();
	}

	private EPMConfig() {
	}

	public static boolean autoFastRunDash() {
		return AUTO_FAST_RUN_DASH.get();
	}

	public static boolean naturalSprinterAnimations() {
		return NATURAL_SPRINTER_ANIMATIONS.get();
	}

	public static boolean naturalSprinterManualStep() {
		return NATURAL_SPRINTER_MANUAL_STEP.get();
	}

	public static StepDodgeConflictMode naturalSprinterStepDodgeConflictMode() {
		return NATURAL_SPRINTER_STEP_DODGE_CONFLICT_MODE.get();
	}

	public static int naturalSprinterStepDodgeLongPressTicks() {
		return NATURAL_SPRINTER_STEP_DODGE_LONG_PRESS_TICKS.get();
	}

	public static int naturalSprinterStepDodgeDoubleTapGapTicks() {
		return NATURAL_SPRINTER_STEP_DODGE_DOUBLE_TAP_GAP_TICKS.get();
	}

	public static int naturalSprinterStepDodgeFirstTapMaxTicks() {
		return NATURAL_SPRINTER_STEP_DODGE_FIRST_TAP_MAX_TICKS.get();
	}

	public static boolean fastRunStartStepAnimation() {
		return FAST_RUN_START_STEP_ANIMATION.get();
	}

	public static boolean isTaczBarehandSprintType(String gunType) {
		if (gunType == null) {
			return false;
		}

		String normalizedGunType = normalizeType(gunType);
		for (String configuredType : TACZ_BAREHAND_SPRINT_TYPES.get()) {
			if (normalizedGunType.equals(normalizeType(configuredType))) {
				return true;
			}
		}
		return false;
	}

	public static boolean catLeapPrimesPhantomAscent() {
		return CAT_LEAP_PRIMES_PHANTOM_ASCENT.get();
	}

	public static boolean wallJumpPrimesPhantomAscent() {
		return WALL_JUMP_PRIMES_PHANTOM_ASCENT.get();
	}

	public static float phantomAscentFallProtectionDamageThreshold() {
		return PHANTOM_ASCENT_FALL_PROTECTION_DAMAGE_THRESHOLD.get().floatValue();
	}

	public static boolean disablePhantomAscentUnderwaterSwimming() {
		return DISABLE_PHANTOM_ASCENT_UNDERWATER_SWIMMING.get();
	}

	public static boolean demolitionLeapShiftSpaceReplacement() {
		return DEMOLITION_LEAP_SHIFT_SPACE_REPLACEMENT.get();
	}

	public static boolean demolitionLeapAirDoubleJump() {
		return DEMOLITION_LEAP_AIR_DOUBLE_JUMP.get();
	}

	public static boolean demolitionLeapChargeJumpAnimation() {
		return DEMOLITION_LEAP_CHARGE_JUMP_ANIMATION.get();
	}

	public static boolean autoSprintAfterWallJump() {
		return AUTO_SPRINT_AFTER_WALL_JUMP.get();
	}

	public static boolean wallJumpPrimesAirAttack() {
		return WALL_JUMP_PRIMES_AIR_ATTACK.get();
	}

	public static boolean taczShootDuringWallJump() {
		return TACZ_SHOOT_DURING_WALL_JUMP.get();
	}

	public static float wallJumpAirAttackFallProtectionDamageThreshold() {
		return WALL_JUMP_AIR_ATTACK_FALL_PROTECTION_DAMAGE_THRESHOLD.get().floatValue();
	}

	public static boolean spiderWallJumpPrimesPhantomAscent() {
		return SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT.get();
	}

	public static SpiderTechniquesWallRunMode spiderTechniquesWallRunMode() {
		return SPIDER_TECHNIQUES_WALL_RUN_MODE.get();
	}

	public static boolean parCoolSpiderWallRunMode() {
		return spiderTechniquesWallRunMode() == SpiderTechniquesWallRunMode.PARCOOL;
	}

	public static boolean defaultSpiderWallRunMode() {
		return spiderTechniquesWallRunMode() == SpiderTechniquesWallRunMode.DEFAULT;
	}

	public static boolean womSpiderWallRunMode() {
		return spiderTechniquesWallRunMode() == SpiderTechniquesWallRunMode.WOM;
	}

	public static float parCoolWallRunAnimationTransition() {
		return PARCOOL_WALL_RUN_ANIMATION_TRANSITION.get().floatValue();
	}

	public static boolean disableVerticalWallRunWithSpiderTechniques() {
		return DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES.get();
	}

	public static boolean debugSpiderTechniquesAttackState() {
		return DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE.get();
	}

	public static boolean debugSpiderWallRunState() {
		return DEBUG_SPIDER_WALL_RUN_STATE.get();
	}

	public static boolean aquaManeuvreFastSwimAnimation() {
		return AQUA_MANEUVRE_FAST_SWIM_ANIMATION.get();
	}

	public static boolean debugAquaManeuvreFastSwimState() {
		return DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE.get();
	}

	public static double vaultHeightScale() {
		return VAULT_HEIGHT_SCALE.get();
	}

	public static boolean fastRunVaultChainFix() {
		return FAST_RUN_VAULT_CHAIN_FIX.get();
	}

	public static boolean debugVaultState() {
		return DEBUG_VAULT_STATE.get();
	}

	public static boolean debugGliderState() {
		return DEBUG_GLIDER_STATE.get();
	}

	public static double epicParCoolClimbUpVerticalVelocity() {
		return EPIC_PARCOOL_CLIMB_UP_VERTICAL_VELOCITY.get();
	}

	public static double epicParCoolClimbUpLateralAirControlVelocity() {
		return EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_VELOCITY.get();
	}

	public static int epicParCoolClimbUpLateralAirControlTicks() {
		return EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_TICKS.get();
	}

	public static boolean parCoolDodgeDefaultMigrationApplied() {
		return PARCOOL_DODGE_DEFAULT_MIGRATION_APPLIED.get();
	}

	public static void markParCoolDodgeDefaultMigrationApplied() {
		PARCOOL_DODGE_DEFAULT_MIGRATION_APPLIED.set(true);
		SPEC.save();
	}

	private static boolean isStringValue(Object value) {
		return value instanceof String;
	}

	private static String normalizeType(String type) {
		return type.trim().toLowerCase(Locale.ROOT);
	}

	public enum SpiderTechniquesWallRunMode {
		DEFAULT,
		PARCOOL,
		WOM
	}

	public enum StepDodgeConflictMode {
		DISABLED,
		SHORT_DODGE_LONG_STEP,
		SHORT_STEP_HOLD_DODGE,
		SINGLE_STEP_DOUBLE_DODGE
	}
}
