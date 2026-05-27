package dev.spake404.epm;

import java.util.List;
import java.util.Locale;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EPMConfig {
	public static final ForgeConfigSpec SPEC;
	private static final ForgeConfigSpec.BooleanValue NATURAL_SPRINTER_ANIMATIONS;
	private static final ForgeConfigSpec.BooleanValue NATURAL_SPRINTER_MANUAL_STEP;
	private static final ForgeConfigSpec.BooleanValue AUTO_FAST_RUN_DASH;
	private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_BAREHAND_SPRINT_TYPES;
	private static final ForgeConfigSpec.BooleanValue CAT_LEAP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue WALL_JUMP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.DoubleValue PHANTOM_ASCENT_FALL_PROTECTION_DAMAGE_THRESHOLD;
	private static final ForgeConfigSpec.BooleanValue AUTO_SPRINT_AFTER_WALL_JUMP;
	private static final ForgeConfigSpec.BooleanValue WALL_JUMP_PRIMES_AIR_ATTACK;
	private static final ForgeConfigSpec.BooleanValue TACZ_SHOOT_DURING_WALL_JUMP;
	private static final ForgeConfigSpec.DoubleValue WALL_JUMP_AIR_ATTACK_FALL_PROTECTION_DAMAGE_THRESHOLD;
	private static final ForgeConfigSpec.BooleanValue SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES;
	private static final ForgeConfigSpec.BooleanValue DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE;
	private static final ForgeConfigSpec.DoubleValue VAULT_HEIGHT_SCALE;
	private static final ForgeConfigSpec.DoubleValue EPIC_PARCOOL_CLIMB_UP_VERTICAL_VELOCITY;
	private static final ForgeConfigSpec.DoubleValue EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_VELOCITY;
	private static final ForgeConfigSpec.IntValue EPIC_PARCOOL_CLIMB_UP_LATERAL_AIR_CONTROL_TICKS;

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
		AUTO_FAST_RUN_DASH = builder
				.translation("epic_parcool_momentum.configuration.autoFastRunDash")
				.comment(
						"true: auto trigger dash when entering EpicParCool FastRun.",
						"false: only trigger dash when pressing ParCool's FastRun key.")
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
		DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES = builder
				.translation("epic_parcool_momentum.configuration.disableVerticalWallRunWithSpiderTechniques")
				.comment("true: disables ParCool VerticalWallRun after learning WOM Spider Techniques.")
				.define("disableVerticalWallRunWithSpiderTechniques", true);
		DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE = builder
				.translation("epic_parcool_momentum.configuration.debugSpiderTechniquesAttackState")
				.comment("Temporary debug option. true: logs Spider Techniques state when attack executability is checked.")
				.define("debugSpiderTechniquesAttackState", false);
		builder.pop();

		builder.push("Vault");
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

	public static boolean disableVerticalWallRunWithSpiderTechniques() {
		return DISABLE_VERTICAL_WALL_RUN_WITH_SPIDER_TECHNIQUES.get();
	}

	public static boolean debugSpiderTechniquesAttackState() {
		return DEBUG_SPIDER_TECHNIQUES_ATTACK_STATE.get();
	}

	public static double vaultHeightScale() {
		return VAULT_HEIGHT_SCALE.get();
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

	private static boolean isStringValue(Object value) {
		return value instanceof String;
	}

	private static String normalizeType(String type) {
		return type.trim().toLowerCase(Locale.ROOT);
	}
}
