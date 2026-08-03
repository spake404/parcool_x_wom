package dev.spake404.epm.config;

import dev.spake404.epm.EPM;
import java.util.List;
import java.util.Locale;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EPMConfig {
	public static final ForgeConfigSpec SPEC;
	private static final ForgeConfigSpec.BooleanValue CUSTOM_FAST_RUN_ANIMATIONS;
	private static final ForgeConfigSpec.BooleanValue USE_EPIC_PARCOOL_DEFAULT_CAT_LEAP_ANIMATION;
	private static final ForgeConfigSpec.BooleanValue NATURAL_SPRINTER_MANUAL_STEP;
	private static final ForgeConfigSpec.BooleanValue AUTO_GENERATE_FAST_RUN_FROM_CURRENT_WEAPON;
	private static final ForgeConfigSpec.IntValue GENERIC_FAST_RUN_STEP_COOLDOWN_TICKS;
	private static final ForgeConfigSpec.EnumValue<StepDodgeConflictMode> NATURAL_SPRINTER_STEP_DODGE_CONFLICT_MODE;
	private static final ForgeConfigSpec.IntValue NATURAL_SPRINTER_STEP_DODGE_LONG_PRESS_TICKS;
	private static final ForgeConfigSpec.IntValue NATURAL_SPRINTER_STEP_DODGE_DOUBLE_TAP_GAP_TICKS;
	private static final ForgeConfigSpec.IntValue NATURAL_SPRINTER_STEP_DODGE_FIRST_TAP_MAX_TICKS;
	private static final ForgeConfigSpec.BooleanValue FAST_RUN_START_STEP_ANIMATION;
	private static final ForgeConfigSpec.BooleanValue AUTO_FAST_RUN_DASH;
	private static final ForgeConfigSpec.DoubleValue MOVEMENT_ANIMATION_SPEED_CAP_MULTIPLIER;
	private static final ForgeConfigSpec.DoubleValue MOVEMENT_ANIMATION_MAX_PLAY_SPEED;
	private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_BAREHAND_SPRINT_TYPES;
	private static final ForgeConfigSpec.BooleanValue CAT_LEAP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue WALL_JUMP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue CLIMB_UP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue REPLACE_PHANTOM_ASCENT_DOUBLE_JUMP_ANIMATIONS;
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
	private static final ForgeConfigSpec.DoubleValue SPIDER_WALL_JUMP_WOM_FRONT_ANGLE;
	private static final ForgeConfigSpec.BooleanValue DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES;
	private static final ForgeConfigSpec.BooleanValue DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_SPIDER_WALL_RUN_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_DEMOLITION_LEAP_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_ACTION_ARBITRATION_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_CLING_TO_CLIFF_STATE;
	private static final ForgeConfigSpec.BooleanValue FAST_RUN_VAULT_CHAIN_FIX;
	private static final ForgeConfigSpec.BooleanValue VAULT_START_FAST_RUN_GRACE;
	private static final ForgeConfigSpec.DoubleValue VAULT_HEIGHT_SCALE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_VAULT_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_GLIDER_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_NATURAL_SPRINTER_FAST_RUN_STEP_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_EXHAUSTION_POSE_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_EPIC_FIGHT_ANIMATION_HUD;
	private static final ForgeConfigSpec.BooleanValue AQUA_MANEUVRE_FAST_SWIM_ANIMATION;
	private static final ForgeConfigSpec.BooleanValue DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE;
	private static final ForgeConfigSpec.BooleanValue EPICFIGHTX_COMBAT_MASTERY_FAST_RUN_CONTROL_COMPATIBILITY;
	private static final ForgeConfigSpec.EnumValue<CombatMasterySprintTriggerMode> EPICFIGHTX_COMBAT_MASTERY_SPRINT_TRIGGER_MODE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_EPICFIGHTX_COMBAT_MASTERY_SPRINT_STATE;
	private static final ForgeConfigSpec.DoubleValue EPIC_PARCOOL_CLIMB_UP_VERTICAL_VELOCITY;
	private static final ForgeConfigSpec.DoubleValue EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_VELOCITY;
	private static final ForgeConfigSpec.IntValue EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_TICKS;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_ENABLED;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_HUD_X;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_HUD_Y;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_HUD_HORIZONTAL_BASIS;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_HUD_VERTICAL_BASIS;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_AFTERIMAGE_MAX_COUNT;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_REMOTE_AFTERIMAGE_MAX_COUNT;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_AFTERIMAGE_LIFETIME_TICKS;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_AFTERIMAGE_MIN_DISTANCE;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_INTERVAL_TICKS;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_DISPLAY_DELAY_TICKS;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_LIFETIME_TICKS;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_ALPHA;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_END_ALPHA;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_COLOR;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_AFTERIMAGE_ALPHA;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_AFTERIMAGE_START_COLOR;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_AFTERIMAGE_MIDDLE_COLOR;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_AFTERIMAGE_END_COLOR;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_FILTER_ENABLED;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_FILTER_INTENSITY;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_FILTER_FADE_IN_TICKS;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_FILTER_FADE_OUT_TICKS;
	private static final ForgeConfigSpec.ConfigValue<String> SANDEVISTAN_FILTER_COLOR;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_FILTER_DEBUG_GREEN_SCREEN;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_EDGE_BLUR_ENABLED;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_BLUR_INTENSITY;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_BLUR_STRENGTH;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_EDGE_BLUR_SAMPLES;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_BLUR_START;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_BLUR_FULL;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_EDGE_WARP_ENABLED;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_WARP_STRENGTH;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_EDGE_WARP_DURATION_TICKS;
	private static final ForgeConfigSpec.IntValue SANDEVISTAN_EDGE_WARP_RISE_TICKS;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_WARP_START;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_EDGE_WARP_FULL;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_CHROMATIC_ABERRATION_ENABLED;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_CHROMATIC_ABERRATION_STRENGTH;
	private static final ForgeConfigSpec.BooleanValue SANDEVISTAN_ACTIVATION_FLASH_ENABLED;
	private static final ForgeConfigSpec.DoubleValue SANDEVISTAN_ACTIVATION_FLASH_STRENGTH;
	private static final ForgeConfigSpec.BooleanValue DEBUG_CAMERA_EVENT_STATE;
	private static final ForgeConfigSpec.BooleanValue DEBUG_SANDEVISTAN_PERFORMANCE;
	private static final ForgeConfigSpec.IntValue DEBUG_SANDEVISTAN_PERFORMANCE_THRESHOLD_MS;
	private static final ForgeConfigSpec.BooleanValue PARCOOL_DODGE_DEFAULT_MIGRATION_APPLIED;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("FastRun");
		CUSTOM_FAST_RUN_ANIMATIONS = builder
				.translation("epic_parcool_momentum.configuration.customFastRunAnimations")
				.comment(
						"true: enables this mod's custom FastRun animation replacement and related step/jump visuals.",
						"false: keeps EpicParCool's default FastRun animation behavior and disables the R-key FastRun step.")
				.define("customFastRunAnimations", true);
		USE_EPIC_PARCOOL_DEFAULT_CAT_LEAP_ANIMATION = builder
				.translation("epic_parcool_momentum.configuration.useEpicParCoolDefaultCatLeapAnimation")
				.comment(
						"Restores EpicParCool's default CatLeap preparation and airborne animations.",
						"false: WOM Natural Sprinter replaces CatLeap with its sprint-jump animation when the compatibility conditions are met.",
						"true: skips the Natural Sprinter CatLeap animation and motion overrides while preserving CatLeap gameplay and Phantom Ascent priming.")
				.define("useEpicParCoolDefaultCatLeapAnimation", false);
		NATURAL_SPRINTER_MANUAL_STEP = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterManualStep")
				.comment(
						"true: pressing the configured FastRun Step key can trigger a step during EpicParCool FastRun.",
						"When WOM Natural Sprinter is available, its step resource is consumed. Otherwise this mod uses a generic cooldown.",
						"This only works when customFastRunAnimations is also true.")
				.define("naturalSprinterManualStep", true);
		AUTO_GENERATE_FAST_RUN_FROM_CURRENT_WEAPON = builder
				.translation("epic_parcool_momentum.configuration.autoGenerateFastRunFromCurrentWeapon")
				.comment(
						"true: when WOM is not installed and no datapack FastRun rule matches the current weapon, automatically generates FastRun from the weapon's Epic Fight RUN animation.",
						"false: unmatched no-WOM weapons use EpicParCool's default FastRun animation.",
						"If the weapon RUN animation cannot be resolved, this option falls back to EpicParCool's default FastRun animation.")
				.define("autoGenerateFastRunFromCurrentWeapon", true);
		GENERIC_FAST_RUN_STEP_COOLDOWN_TICKS = builder
				.translation("epic_parcool_momentum.configuration.genericFastRunStepCooldownTicks")
				.comment(
						"Cooldown, in ticks, for FastRun step when WOM Natural Sprinter is unavailable.",
						"Natural Sprinter's own step resource is still used when available. 10 ticks is 0.5 seconds at 20 TPS.")
				.defineInRange("genericFastRunStepCooldownTicks", 10, 1, 100);
		NATURAL_SPRINTER_STEP_DODGE_CONFLICT_MODE = builder
				.translation("epic_parcool_momentum.configuration.naturalSprinterStepDodgeConflictMode")
				.comment(
						"Controls how FastRun Step and ParCool Dodge share one key.",
						"When ParCool Dodge is disabled in ParCool settings, this option is ignored and the shared key behaves as FastRun Step.",
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
		MOVEMENT_ANIMATION_SPEED_CAP_MULTIPLIER = builder
				.translation("epic_parcool_momentum.configuration.movementAnimationSpeedCapMultiplier")
				.comment(
						"Player speed, relative to vanilla sprint, required for Epic Fight MovementAnimation to reach the extended maximum playback speed.",
						"1.6 means the animation reaches its maximum at 160% of vanilla sprint speed.")
				.defineInRange("movementAnimationSpeedCapMultiplier", 1.6D, 1.0D, 3.0D);
		MOVEMENT_ANIMATION_MAX_PLAY_SPEED = builder
				.translation("epic_parcool_momentum.configuration.movementAnimationMaxPlaySpeed")
				.comment(
						"Maximum playback multiplier for player Epic Fight MovementAnimation above vanilla sprint speed.",
						"The default 1.856 is 1.16 multiplied by 1.6.")
				.defineInRange("movementAnimationMaxPlaySpeed", 1.856D, 1.16D, 4.0D);
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
		CLIMB_UP_PRIMES_PHANTOM_ASCENT = builder
				.translation("epic_parcool_momentum.configuration.climbUpPrimesPhantomAscent")
				.comment("true: pressing jump after ParCool ClimbUp can trigger Epic Fight Phantom Ascent before glider deployment.")
				.define("climbUpPrimesPhantomAscent", true);
		SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT = builder
				.translation("epic_parcool_momentum.configuration.spiderWallJumpPrimesPhantomAscent")
				.comment("true: pressing jump after Spider Techniques wall jump can trigger Epic Fight Phantom Ascent.")
				.define("spiderWallJumpPrimesPhantomAscent", true);
		REPLACE_PHANTOM_ASCENT_DOUBLE_JUMP_ANIMATIONS = builder
				.translation("epic_parcool_momentum.configuration.replacePhantomAscentDoubleJumpAnimations")
				.comment(
						"true: replaces Epic Fight Phantom Ascent jump, fall, and landing animations with this mod's double-jump animation set.",
						"false: keeps Epic Fight's original Phantom Ascent jump, fall, and landing animation behavior.")
				.define("replacePhantomAscentDoubleJumpAnimations", false);
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
				.comment("true: before learning Demolition Leap, ParCool Charge Jump charging uses Demolition Leap's charging animation in Epic Fight mode. After learning Demolition Leap, Shift alone shows no charging animation - only Shift+Space triggers the skill.")
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
		SPIDER_WALL_JUMP_WOM_FRONT_ANGLE = builder
				.translation("epic_parcool_momentum.configuration.spiderWallJumpWomFrontAngle")
				.comment(
						"Maximum horizontal look angle, in degrees, for WOM Spider Techniques wall jump to keep priority over ParCool WallJump.",
						"0 means the player must look straight into the wall. 90 means WOM keeps priority from any side-facing angle.",
						"Default 50 lets direct wall-facing input use WOM, while side-facing wall-run input prefers ParCool WallJump.")
				.defineInRange("spiderWallJumpWomFrontAngle", 50.0D, 0.0D, 90.0D);
		DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES = builder
				.translation("epic_parcool_momentum.configuration.disableVerticalWallRunWithSpiderTechniques")
				.comment("true: disables ParCool VerticalWallRun after learning WOM Spider Techniques.")
				.define("disableVerticalWallRunWithSpiderTechniques", true);
		builder.pop();

		builder.push("Aqua Maneuvre");
		AQUA_MANEUVRE_FAST_SWIM_ANIMATION = builder
				.translation("epic_parcool_momentum.configuration.aquaManeuvreFastSwimAnimation")
				.comment(
						"true: without Aqua Maneuvre, maps ParCool FastSwimAnimator to WOM's Mermaid animation in Epic Fight mode.",
						"After learning Aqua Maneuvre, ParCool's FastRun control mode only drives WOM's original Mermaid fast-swim input; WOM owns movement, speed, animation start, and animation exit.")
				.define("aquaManeuvreFastSwimAnimation", true);
		builder.pop();

		builder.push("EpicFightX");
		EPICFIGHTX_COMBAT_MASTERY_FAST_RUN_CONTROL_COMPATIBILITY = builder
				.translation("epic_parcool_momentum.configuration.epicFightXCombatMasteryFastRunControlCompatibility")
				.comment(
						"true: after learning EpicFightX Combat Mastery II, post-dodge sprint follows this mod's Combat Mastery sprint trigger mode.",
						"false: keeps EpicFightX's original Combat Mastery II sprint trigger behavior.")
				.define("epicFightXCombatMasteryFastRunControlCompatibility", true);
		EPICFIGHTX_COMBAT_MASTERY_SPRINT_TRIGGER_MODE = builder
				.translation("epic_parcool_momentum.configuration.epicFightXCombatMasterySprintTriggerMode")
				.comment(
						"Controls how this mod enters vanilla sprint after an EpicFightX Combat Mastery II dodge window.",
						"Toggle: press the FastRun key again after the dodge, then hold forward to enter vanilla sprint.",
						"Auto: automatically enters vanilla sprint after the dodge when forward input is held.",
						"PressKey: hold the FastRun key and forward input to enter and keep vanilla sprint.")
				.defineEnum("epicFightXCombatMasterySprintTriggerMode", CombatMasterySprintTriggerMode.Toggle);
		builder.pop();

		builder.push("Vault");
		FAST_RUN_VAULT_CHAIN_FIX = builder
				.translation("epic_parcool_momentum.configuration.fastRunVaultChainFix")
				.comment(
						"true: when Vault starts from FastRun, releases ParCool's Vault action at 6 ticks and keeps a short FastRun grace window so close obstacles can chain through ParCool's original Vault detection.",
						"false: keeps ParCool's original Vault action timing and disables this mod's FastRun Vault chaining assist.")
				.define("fastRunVaultChainFix", true);
		VAULT_START_FAST_RUN_GRACE = builder
				.translation("epic_parcool_momentum.configuration.vaultStartFastRunGrace")
				.comment(
						"true: lets Vault accept a short recent-FastRun window when FastRun drops immediately before ParCool Vault can start.",
						"false: Vault start requires ParCool FastRun to still be actively doing, except for the existing post-Vault chain grace.")
				.define("vaultStartFastRunGrace", true);
		VAULT_HEIGHT_SCALE = builder
				.translation("epic_parcool_momentum.configuration.vaultHeightScale")
				.comment(
						"Changes ParCool Vault's maximum detected obstacle height.",
						"ParCool original default is 0.86. This compatibility mod defaults to 1.5 for stable three-block air vaults.",
						"Lower values require more precise jump timing.")
				.defineInRange("vaultHeightScale", 1.5D, 0.86D, 2.0D);
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

		builder.push("Sandevistan");
		SANDEVISTAN_ENABLED = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEnabled")
				.comment("Enables the Sandevistan time-dilation skill.")
				.define("sandevistanEnabled", true);
		SANDEVISTAN_HUD_X = builder
				.comment("Independent Sandevistan HUD horizontal coordinate used by Epic Fight's HUD setup screen.")
				.defineInRange("sandevistanHudX", 0, -10000, 10000);
		SANDEVISTAN_HUD_Y = builder
				.comment("Independent Sandevistan HUD vertical coordinate used by Epic Fight's HUD setup screen.")
				.defineInRange("sandevistanHudY", 70, -10000, 10000);
		SANDEVISTAN_HUD_HORIZONTAL_BASIS = builder
				.comment("Horizontal screen basis of the independent Sandevistan HUD: LEFT, RIGHT, or CENTER.")
				.define("sandevistanHudHorizontalBasis", "CENTER", EPMConfig::isHorizontalBasis);
		SANDEVISTAN_HUD_VERTICAL_BASIS = builder
				.comment("Vertical screen basis of the independent Sandevistan HUD: TOP, BOTTOM, or CENTER.")
				.define("sandevistanHudVerticalBasis", "BOTTOM", EPMConfig::isVerticalBasis);
		SANDEVISTAN_AFTERIMAGE_MAX_COUNT = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageMaxCount")
				.comment("Maximum number of Sandevistan afterimages retained for the local player.")
				.defineInRange("sandevistanAfterimageMaxCount", 24, 1, 64);
		SANDEVISTAN_REMOTE_AFTERIMAGE_MAX_COUNT = builder
				.translation("epic_parcool_momentum.configuration.sandevistanRemoteAfterimageMaxCount")
				.comment("Maximum number of Sandevistan afterimages retained for each remote player.")
				.defineInRange("sandevistanRemoteAfterimageMaxCount", 16, 1, 64);
		SANDEVISTAN_AFTERIMAGE_LIFETIME_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageLifetimeTicks")
				.comment("Lifetime in ticks of each Sandevistan afterimage.")
				.defineInRange("sandevistanAfterimageLifetimeTicks", 24, 1, 100);
		SANDEVISTAN_AFTERIMAGE_MIN_DISTANCE = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageMinDistance")
				.comment("Minimum movement distance in blocks before another afterimage can be captured.")
				.defineInRange("sandevistanAfterimageMinDistance", 0.2D, 0.0D, 2.0D);
		SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_INTERVAL_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanStationaryActionAfterimageIntervalTicks")
				.comment("Ticks between afterimage captures while performing an Epic Fight action without moving.")
				.defineInRange("sandevistanStationaryActionAfterimageIntervalTicks", 2, 1, 40);
		SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_DISPLAY_DELAY_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanStationaryActionAfterimageDisplayDelayTicks")
				.comment("Client ticks between capturing a stationary action pose and displaying its afterimage.")
				.defineInRange("sandevistanStationaryActionAfterimageDisplayDelayTicks", 2, 0, 40);
		SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_LIFETIME_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanStationaryActionAfterimageLifetimeTicks")
				.comment("Lifetime in ticks of afterimages captured from stationary Epic Fight actions.")
				.defineInRange("sandevistanStationaryActionAfterimageLifetimeTicks", 15, 1, 100);
		SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_ALPHA = builder
				.translation("epic_parcool_momentum.configuration.sandevistanStationaryActionAfterimageAlpha")
				.comment("Starting opacity of stationary Epic Fight action afterimages.")
				.defineInRange("sandevistanStationaryActionAfterimageAlpha", 0.85D, 0.05D, 1.0D);
		SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_END_ALPHA = builder
				.translation("epic_parcool_momentum.configuration.sandevistanStationaryActionAfterimageEndAlpha")
				.comment("Opacity reached by stationary Epic Fight action afterimages at the end of their lifetime.")
				.defineInRange("sandevistanStationaryActionAfterimageEndAlpha", 0.45D, 0.0D, 1.0D);
		SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_COLOR = builder
				.translation("epic_parcool_momentum.configuration.sandevistanStationaryActionAfterimageColor")
				.comment("Constant hex RGB tint of stationary Epic Fight action afterimages.")
				.define("sandevistanStationaryActionAfterimageColor", "#33E6FF", EPMConfig::isHexColor);
		SANDEVISTAN_AFTERIMAGE_ALPHA = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageAlpha")
				.comment("Initial opacity of textured Sandevistan afterimages.")
				.defineInRange("sandevistanAfterimageAlpha", 0.86D, 0.05D, 1.0D);
		SANDEVISTAN_AFTERIMAGE_START_COLOR = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageStartColor")
				.comment("Hex RGB tint used by newly captured afterimages.")
				.define("sandevistanAfterimageStartColor", "#33E6FF", EPMConfig::isHexColor);
		SANDEVISTAN_AFTERIMAGE_MIDDLE_COLOR = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageMiddleColor")
				.comment("Hex RGB tint used midway through an afterimage's lifetime.")
				.define("sandevistanAfterimageMiddleColor", "#AD4DFF", EPMConfig::isHexColor);
		SANDEVISTAN_AFTERIMAGE_END_COLOR = builder
				.translation("epic_parcool_momentum.configuration.sandevistanAfterimageEndColor")
				.comment("Hex RGB tint used by fading afterimages.")
				.define("sandevistanAfterimageEndColor", "#FF941F", EPMConfig::isHexColor);
		SANDEVISTAN_FILTER_ENABLED = builder
				.translation("epic_parcool_momentum.configuration.sandevistanFilterEnabled")
				.comment("Enables the low-cost green Sandevistan screen filter.")
				.define("sandevistanFilterEnabled", true);
		SANDEVISTAN_FILTER_INTENSITY = builder
				.translation("epic_parcool_momentum.configuration.sandevistanFilterIntensity")
				.comment("Strength of the green Sandevistan screen filter.")
				.defineInRange("sandevistanFilterIntensity", 1.0D, 0.0D, 1.0D);
		SANDEVISTAN_FILTER_FADE_IN_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanFilterFadeInTicks")
				.comment("Ticks used to fade in the Sandevistan screen filter.")
				.defineInRange("sandevistanFilterFadeInTicks", 6, 1, 40);
		SANDEVISTAN_FILTER_FADE_OUT_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanFilterFadeOutTicks")
				.comment("Ticks used to fade out the Sandevistan screen filter.")
				.defineInRange("sandevistanFilterFadeOutTicks", 8, 1, 40);
		SANDEVISTAN_FILTER_COLOR = builder
				.translation("epic_parcool_momentum.configuration.sandevistanFilterColor")
				.comment("Hex RGB channel multiplier used by the low-cost Sandevistan color grade.")
				.define("sandevistanFilterColor", "#3CFF48", EPMConfig::isHexColor);
		SANDEVISTAN_FILTER_DEBUG_GREEN_SCREEN = builder
				.translation("epic_parcool_momentum.configuration.sandevistanFilterDebugGreenScreen")
				.comment("Debug only. Replaces the world with the solid filter color to inspect stencil exclusions.")
				.define("sandevistanFilterDebugGreenScreen", false);
		SANDEVISTAN_EDGE_BLUR_ENABLED = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeBlurEnabled")
				.comment("Enables the low-cost radial blur around the screen edges while Sandevistan is active.")
				.define("sandevistanEdgeBlurEnabled", true);
		SANDEVISTAN_EDGE_BLUR_INTENSITY = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeBlurIntensity")
				.comment("Overall opacity of the Sandevistan edge blur.")
				.defineInRange("sandevistanEdgeBlurIntensity", 1.0D, 0.0D, 1.0D);
		SANDEVISTAN_EDGE_BLUR_STRENGTH = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeBlurStrength")
				.comment("Distance sampled toward the screen center. Higher values create longer blur trails.")
				.defineInRange("sandevistanEdgeBlurStrength", 0.3D, 0.0D, 0.6D);
		SANDEVISTAN_EDGE_BLUR_SAMPLES = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeBlurSamples")
				.comment("Texture samples used by the edge blur. Higher values look smoother but cost more GPU time.")
				.defineInRange("sandevistanEdgeBlurSamples", 10, 1, 12);
		SANDEVISTAN_EDGE_BLUR_START = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeBlurStart")
				.comment(
						"Position where edge blur begins. 0 is the screen center and 1 is the screen border.",
						"Increase this value to restrict blur to a thinner outer edge.")
				.defineInRange("sandevistanEdgeBlurStart", 0.6D, 0.0D, 0.99D);
		SANDEVISTAN_EDGE_BLUR_FULL = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeBlurFull")
				.comment(
						"Position where edge blur reaches full strength. 0 is the screen center and 1 is the screen border.",
						"This should normally be greater than sandevistanEdgeBlurStart.")
				.defineInRange("sandevistanEdgeBlurFull", 0.8D, 0.01D, 1.0D);
		SANDEVISTAN_EDGE_WARP_ENABLED = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeWarpEnabled")
				.comment("Enables the radial screen-edge distortion pulse when Sandevistan activates.")
				.define("sandevistanEdgeWarpEnabled", true);
		SANDEVISTAN_EDGE_WARP_STRENGTH = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeWarpStrength")
				.comment("Maximum UV displacement used by the Sandevistan activation distortion pulse.")
				.defineInRange("sandevistanEdgeWarpStrength", 0.2D, 0.0D, 0.35D);
		SANDEVISTAN_EDGE_WARP_DURATION_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeWarpDurationTicks")
				.comment("Total duration in ticks of the Sandevistan activation distortion pulse.")
				.defineInRange("sandevistanEdgeWarpDurationTicks", 12, 1, 40);
		SANDEVISTAN_EDGE_WARP_RISE_TICKS = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeWarpRiseTicks")
				.comment("Ticks used for the activation distortion pulse to reach full strength before fading out.")
				.defineInRange("sandevistanEdgeWarpRiseTicks", 5, 0, 10);
		SANDEVISTAN_EDGE_WARP_START = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeWarpStart")
				.comment(
						"Position where activation distortion begins. 0 is the screen center and 1 is the screen border.",
						"Increase this value to preserve a larger undistorted center area.")
				.defineInRange("sandevistanEdgeWarpStart", 0.45D, 0.0D, 0.99D);
		SANDEVISTAN_EDGE_WARP_FULL = builder
				.translation("epic_parcool_momentum.configuration.sandevistanEdgeWarpFull")
				.comment(
						"Position where activation distortion reaches full strength. 0 is the screen center and 1 is the screen border.",
						"This should normally be greater than sandevistanEdgeWarpStart.")
				.defineInRange("sandevistanEdgeWarpFull", 0.6D, 0.01D, 1.0D);
		SANDEVISTAN_CHROMATIC_ABERRATION_ENABLED = builder
				.translation("epic_parcool_momentum.configuration.sandevistanChromaticAberrationEnabled")
				.comment("Enables radial RGB channel separation during the Sandevistan activation pulse.")
				.define("sandevistanChromaticAberrationEnabled", true);
		SANDEVISTAN_CHROMATIC_ABERRATION_STRENGTH = builder
				.translation("epic_parcool_momentum.configuration.sandevistanChromaticAberrationStrength")
				.comment("Maximum UV offset between red and blue channels during the activation pulse.")
				.defineInRange("sandevistanChromaticAberrationStrength", 0.012D, 0.0D, 0.04D);
		SANDEVISTAN_ACTIVATION_FLASH_ENABLED = builder
				.translation("epic_parcool_momentum.configuration.sandevistanActivationFlashEnabled")
				.comment("Enables the short white exposure flash at the peak of Sandevistan activation.")
				.define("sandevistanActivationFlashEnabled", true);
		SANDEVISTAN_ACTIVATION_FLASH_STRENGTH = builder
				.translation("epic_parcool_momentum.configuration.sandevistanActivationFlashStrength")
				.comment("Maximum white exposure mixed into the screen at the activation peak.")
				.defineInRange("sandevistanActivationFlashStrength", 0.72D, 0.0D, 1.0D);
		builder.pop();

		builder.push("Debug");
		DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugSpiderTechniquesAttackState")
				.comment("Temporary debug option. true: logs Spider Techniques state when attack executability is checked.")
				.define("debugSpiderTechniquesAttackState", false);
		DEBUG_SPIDER_WALL_RUN_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugSpiderWallRunState")
				.comment("Temporary debug option. true: logs detailed Spider Techniques wall-run state.")
				.define("debugSpiderWallRunState", false);
		DEBUG_DEMOLITION_LEAP_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugDemolitionLeapState")
				.comment("Temporary debug option. true: logs detailed Demolition Leap compatibility state.")
				.define("debugDemolitionLeapState", false);
		DEBUG_ACTION_ARBITRATION_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugActionArbitrationState")
				.comment("Temporary debug option. true: logs jump/action arbitration and handoff state.")
				.define("debugActionArbitrationState", false);
		DEBUG_CLING_TO_CLIFF_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugClingToCliffState")
				.comment("Temporary debug option. true: logs detailed ClingToCliff and ClimbUp diagnostics.")
				.define("debugClingToCliffState", false);
		DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugAquaManeuvreFastSwimState")
				.comment("Temporary debug option. true: logs detailed Aqua Maneuvre fast-swim state.")
				.define("debugAquaManeuvreFastSwimState", false);
		DEBUG_EPICFIGHTX_COMBAT_MASTERY_SPRINT_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugEpicFightXCombatMasterySprintState")
				.comment("Temporary debug option. true: logs detailed EpicFightX Combat Mastery II sprint trigger state.")
				.define("debugEpicFightXCombatMasterySprintState", false);
		DEBUG_VAULT_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugVaultState")
				.comment("Temporary debug option. true: logs detailed ParCool Vault canStart, movement, and post-vault state.")
				.define("debugVaultState", false);
		DEBUG_GLIDER_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugGliderState")
				.comment("Temporary debug option. true: logs detailed Glider/FastRun diagnostic state.")
				.define("debugGliderState", false);
		DEBUG_NATURAL_SPRINTER_FAST_RUN_STEP_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugNaturalSprinterFastRunStepState")
				.comment("Temporary debug option. true: logs FastRun startup step and procedural pulse state.")
				.define("debugNaturalSprinterFastRunStepState", false);
		DEBUG_EXHAUSTION_POSE_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugExhaustionPoseState")
				.comment("Temporary debug option. true: logs low-stamina exhaustion pose diagnostics.")
				.define("debugExhaustionPoseState", false);
		DEBUG_EPIC_FIGHT_ANIMATION_HUD = builder
				.translation("epic_parcool_momentum.configuration.debugEpicFightAnimationHud")
				.comment("Temporary debug option. true: renders the current Epic Fight base animation above the hotbar.")
				.define("debugEpicFightAnimationHud", false);
		DEBUG_CAMERA_EVENT_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugCameraEventState")
				.comment("Temporary debug option. true: logs camera angle event diagnostics for intermittent camera shake investigation.")
				.define("debugCameraEventState", false);
		DEBUG_SANDEVISTAN_PERFORMANCE = builder
				.translation("epic_parcool_momentum.configuration.debugSandevistanPerformance")
				.comment("Logs Sandevistan rendering costs when a client frame exceeds the configured hitch threshold.")
				.define("debugSandevistanPerformance", false);
		DEBUG_SANDEVISTAN_PERFORMANCE_THRESHOLD_MS = builder
				.translation("epic_parcool_momentum.configuration.debugSandevistanPerformanceThresholdMs")
				.comment("Client frame time in milliseconds that triggers a Sandevistan performance diagnostic log.")
				.defineInRange("debugSandevistanPerformanceThresholdMs", 33, 16, 250);
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

	public static double movementAnimationSpeedCapMultiplier() {
		return MOVEMENT_ANIMATION_SPEED_CAP_MULTIPLIER.get();
	}

	public static float movementAnimationMaxPlaySpeed() {
		return MOVEMENT_ANIMATION_MAX_PLAY_SPEED.get().floatValue();
	}

	public static boolean customFastRunAnimations() {
		return CUSTOM_FAST_RUN_ANIMATIONS.get();
	}

	public static boolean useEpicParCoolDefaultCatLeapAnimation() {
		return USE_EPIC_PARCOOL_DEFAULT_CAT_LEAP_ANIMATION.get();
	}

	public static boolean naturalSprinterManualStep() {
		return NATURAL_SPRINTER_MANUAL_STEP.get();
	}

	public static boolean autoGenerateFastRunFromCurrentWeapon() {
		return AUTO_GENERATE_FAST_RUN_FROM_CURRENT_WEAPON.get();
	}

	public static int genericFastRunStepCooldownTicks() {
		return GENERIC_FAST_RUN_STEP_COOLDOWN_TICKS.get();
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

	public static boolean climbUpPrimesPhantomAscent() {
		return CLIMB_UP_PRIMES_PHANTOM_ASCENT.get();
	}

	public static float phantomAscentFallProtectionDamageThreshold() {
		return PHANTOM_ASCENT_FALL_PROTECTION_DAMAGE_THRESHOLD.get().floatValue();
	}

	public static boolean disablePhantomAscentUnderwaterSwimming() {
		return DISABLE_PHANTOM_ASCENT_UNDERWATER_SWIMMING.get();
	}

	public static boolean replacePhantomAscentDoubleJumpAnimations() {
		return REPLACE_PHANTOM_ASCENT_DOUBLE_JUMP_ANIMATIONS.get();
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

	public static double spiderWallJumpWomFrontAngle() {
		return SPIDER_WALL_JUMP_WOM_FRONT_ANGLE.get();
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

	public static boolean debugDemolitionLeapState() {
		return DEBUG_DEMOLITION_LEAP_STATE.get();
	}

	public static boolean debugActionArbitrationState() {
		return DEBUG_ACTION_ARBITRATION_STATE.get();
	}

	public static boolean debugClingToCliffState() {
		return DEBUG_CLING_TO_CLIFF_STATE.get();
	}

	public static boolean aquaManeuvreFastSwimAnimation() {
		return AQUA_MANEUVRE_FAST_SWIM_ANIMATION.get();
	}

	public static boolean debugAquaManeuvreFastSwimState() {
		return DEBUG_AQUA_MANEUVRE_FAST_SWIM_STATE.get();
	}

	public static boolean epicFightXCombatMasteryFastRunControlCompatibility() {
		return EPICFIGHTX_COMBAT_MASTERY_FAST_RUN_CONTROL_COMPATIBILITY.get();
	}

	public static CombatMasterySprintTriggerMode epicFightXCombatMasterySprintTriggerMode() {
		return EPICFIGHTX_COMBAT_MASTERY_SPRINT_TRIGGER_MODE.get();
	}

	public static boolean debugEpicFightXCombatMasterySprintState() {
		return DEBUG_EPICFIGHTX_COMBAT_MASTERY_SPRINT_STATE.get();
	}

	public static double vaultHeightScale() {
		return VAULT_HEIGHT_SCALE.get();
	}

	public static boolean fastRunVaultChainFix() {
		return FAST_RUN_VAULT_CHAIN_FIX.get();
	}

	public static boolean vaultStartFastRunGrace() {
		return VAULT_START_FAST_RUN_GRACE.get();
	}

	public static boolean debugVaultState() {
		return DEBUG_VAULT_STATE.get();
	}

	public static boolean debugGliderState() {
		return DEBUG_GLIDER_STATE.get();
	}

	public static boolean debugNaturalSprinterFastRunStepState() {
		return DEBUG_NATURAL_SPRINTER_FAST_RUN_STEP_STATE.get();
	}

	public static boolean debugExhaustionPoseState() {
		return DEBUG_EXHAUSTION_POSE_STATE.get();
	}

	public static boolean debugEpicFightAnimationHud() {
		try {
			return DEBUG_EPIC_FIGHT_ANIMATION_HUD.get();
		} catch (IllegalStateException ignored) {
			return false;
		}
	}

	public static boolean debugCameraEventState() {
		try {
			return DEBUG_CAMERA_EVENT_STATE.get();
		} catch (IllegalStateException ignored) {
			return false;
		}
	}

	public static boolean debugSandevistanPerformance() {
		return DEBUG_SANDEVISTAN_PERFORMANCE.get();
	}

	public static int debugSandevistanPerformanceThresholdMs() {
		return DEBUG_SANDEVISTAN_PERFORMANCE_THRESHOLD_MS.get();
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

	public static boolean sandevistanEnabled() {
		return SANDEVISTAN_ENABLED.get();
	}

	public static int sandevistanHudX() {
		return SANDEVISTAN_HUD_X.get();
	}

	public static int sandevistanHudY() {
		return SANDEVISTAN_HUD_Y.get();
	}

	public static String sandevistanHudHorizontalBasis() {
		return SANDEVISTAN_HUD_HORIZONTAL_BASIS.get();
	}

	public static String sandevistanHudVerticalBasis() {
		return SANDEVISTAN_HUD_VERTICAL_BASIS.get();
	}

	public static void setSandevistanHudX(int value) {
		SANDEVISTAN_HUD_X.set(value);
		SPEC.save();
	}

	public static void setSandevistanHudY(int value) {
		SANDEVISTAN_HUD_Y.set(value);
		SPEC.save();
	}

	public static void setSandevistanHudHorizontalBasis(String value) {
		if (isHorizontalBasis(value)) {
			SANDEVISTAN_HUD_HORIZONTAL_BASIS.set(value);
			SPEC.save();
		}
	}

	public static void setSandevistanHudVerticalBasis(String value) {
		if (isVerticalBasis(value)) {
			SANDEVISTAN_HUD_VERTICAL_BASIS.set(value);
			SPEC.save();
		}
	}

	public static int sandevistanAfterimageMaxCount() {
		return SANDEVISTAN_AFTERIMAGE_MAX_COUNT.get();
	}

	public static int sandevistanRemoteAfterimageMaxCount() {
		return SANDEVISTAN_REMOTE_AFTERIMAGE_MAX_COUNT.get();
	}

	public static int sandevistanAfterimageLifetimeTicks() {
		return SANDEVISTAN_AFTERIMAGE_LIFETIME_TICKS.get();
	}

	public static double sandevistanAfterimageMinDistance() {
		return SANDEVISTAN_AFTERIMAGE_MIN_DISTANCE.get();
	}

	public static int sandevistanStationaryActionAfterimageIntervalTicks() {
		return SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_INTERVAL_TICKS.get();
	}

	public static int sandevistanStationaryActionAfterimageDisplayDelayTicks() {
		return SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_DISPLAY_DELAY_TICKS.get();
	}

	public static int sandevistanStationaryActionAfterimageLifetimeTicks() {
		return SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_LIFETIME_TICKS.get();
	}

	public static float sandevistanStationaryActionAfterimageAlpha() {
		return SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_ALPHA.get().floatValue();
	}

	public static float sandevistanStationaryActionAfterimageEndAlpha() {
		return SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_END_ALPHA.get().floatValue();
	}

	public static int sandevistanStationaryActionAfterimageColor() {
		return parseHexColor(SANDEVISTAN_STATIONARY_ACTION_AFTERIMAGE_COLOR.get(), 0x33E6FF);
	}

	public static float sandevistanAfterimageAlpha() {
		return SANDEVISTAN_AFTERIMAGE_ALPHA.get().floatValue();
	}

	public static int sandevistanAfterimageStartColor() {
		return parseHexColor(SANDEVISTAN_AFTERIMAGE_START_COLOR.get(), 0x33E6FF);
	}

	public static int sandevistanAfterimageMiddleColor() {
		return parseHexColor(SANDEVISTAN_AFTERIMAGE_MIDDLE_COLOR.get(), 0xAD4DFF);
	}

	public static int sandevistanAfterimageEndColor() {
		return parseHexColor(SANDEVISTAN_AFTERIMAGE_END_COLOR.get(), 0xFF941F);
	}

	public static boolean sandevistanFilterEnabled() {
		return SANDEVISTAN_FILTER_ENABLED.get();
	}

	public static float sandevistanFilterIntensity() {
		return SANDEVISTAN_FILTER_INTENSITY.get().floatValue();
	}

	public static int sandevistanFilterFadeInTicks() {
		return SANDEVISTAN_FILTER_FADE_IN_TICKS.get();
	}

	public static int sandevistanFilterFadeOutTicks() {
		return SANDEVISTAN_FILTER_FADE_OUT_TICKS.get();
	}

	public static int sandevistanFilterColor() {
		return parseHexColor(SANDEVISTAN_FILTER_COLOR.get(), 0x3CFF48);
	}

	public static boolean sandevistanFilterDebugGreenScreen() {
		return SANDEVISTAN_FILTER_DEBUG_GREEN_SCREEN.get();
	}

	public static boolean sandevistanEdgeBlurEnabled() {
		return SANDEVISTAN_EDGE_BLUR_ENABLED.get();
	}

	public static float sandevistanEdgeBlurIntensity() {
		return SANDEVISTAN_EDGE_BLUR_INTENSITY.get().floatValue();
	}

	public static float sandevistanEdgeBlurStrength() {
		return SANDEVISTAN_EDGE_BLUR_STRENGTH.get().floatValue();
	}

	public static int sandevistanEdgeBlurSamples() {
		return SANDEVISTAN_EDGE_BLUR_SAMPLES.get();
	}

	public static float sandevistanEdgeBlurStart() {
		return SANDEVISTAN_EDGE_BLUR_START.get().floatValue();
	}

	public static float sandevistanEdgeBlurFull() {
		return SANDEVISTAN_EDGE_BLUR_FULL.get().floatValue();
	}

	public static boolean sandevistanEdgeWarpEnabled() {
		return SANDEVISTAN_EDGE_WARP_ENABLED.get();
	}

	public static float sandevistanEdgeWarpStrength() {
		return SANDEVISTAN_EDGE_WARP_STRENGTH.get().floatValue();
	}

	public static int sandevistanEdgeWarpDurationTicks() {
		return SANDEVISTAN_EDGE_WARP_DURATION_TICKS.get();
	}

	public static int sandevistanEdgeWarpRiseTicks() {
		return SANDEVISTAN_EDGE_WARP_RISE_TICKS.get();
	}

	public static float sandevistanEdgeWarpStart() {
		return SANDEVISTAN_EDGE_WARP_START.get().floatValue();
	}

	public static float sandevistanEdgeWarpFull() {
		return SANDEVISTAN_EDGE_WARP_FULL.get().floatValue();
	}

	public static boolean sandevistanChromaticAberrationEnabled() {
		return SANDEVISTAN_CHROMATIC_ABERRATION_ENABLED.get();
	}

	public static float sandevistanChromaticAberrationStrength() {
		return SANDEVISTAN_CHROMATIC_ABERRATION_STRENGTH.get().floatValue();
	}

	public static boolean sandevistanActivationFlashEnabled() {
		return SANDEVISTAN_ACTIVATION_FLASH_ENABLED.get();
	}

	public static float sandevistanActivationFlashStrength() {
		return SANDEVISTAN_ACTIVATION_FLASH_STRENGTH.get().floatValue();
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

	private static boolean isHorizontalBasis(Object value) {
		return value instanceof String string
				&& ("LEFT".equals(string) || "RIGHT".equals(string) || "CENTER".equals(string));
	}

	private static boolean isVerticalBasis(Object value) {
		return value instanceof String string
				&& ("TOP".equals(string) || "BOTTOM".equals(string) || "CENTER".equals(string));
	}

	private static boolean isHexColor(Object value) {
		return value instanceof String string && string.matches("#?[0-9a-fA-F]{6}");
	}

	private static int parseHexColor(String value, int fallback) {
		if (!isHexColor(value)) {
			return fallback;
		}

		String normalized = value.charAt(0) == '#' ? value.substring(1) : value;
		try {
			return Integer.parseInt(normalized, 16);
		} catch (NumberFormatException ignored) {
			return fallback;
		}
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

	public enum CombatMasterySprintTriggerMode {
		Toggle,
		Auto,
		PressKey
	}
}
