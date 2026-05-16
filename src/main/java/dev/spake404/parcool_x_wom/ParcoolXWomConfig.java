package dev.spake404.parcool_x_wom;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ParcoolXWomConfig {
	public static final ForgeConfigSpec SPEC;
	private static final ForgeConfigSpec.BooleanValue AUTO_FAST_RUN_DASH;
	private static final ForgeConfigSpec.BooleanValue CAT_LEAP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue WALL_JUMP_PRIMES_PHANTOM_ASCENT;
	private static final ForgeConfigSpec.BooleanValue SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("Natural Sprinter");
		AUTO_FAST_RUN_DASH = builder
				.translation("parcool_x_wom.configuration.autoFastRunDash")
				.comment(
						"true: auto trigger dash when entering EpicParCool FastRun.",
						"false: only trigger dash when pressing ParCool's FastRun key.")
				.define("autoFastRunDash", true);
		builder.pop();

		builder.push("Phantom Ascent");
		CAT_LEAP_PRIMES_PHANTOM_ASCENT = builder
				.translation("parcool_x_wom.configuration.catLeapPrimesPhantomAscent")
				.comment("true: pressing jump after CatLeap can trigger Epic Fight Phantom Ascent.")
				.define("catLeapPrimesPhantomAscent", true);
		WALL_JUMP_PRIMES_PHANTOM_ASCENT = builder
				.translation("parcool_x_wom.configuration.wallJumpPrimesPhantomAscent")
				.comment("true: pressing jump after ParCool WallJump can trigger Epic Fight Phantom Ascent.")
				.define("wallJumpPrimesPhantomAscent", true);
		SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT = builder
				.translation("parcool_x_wom.configuration.spiderWallJumpPrimesPhantomAscent")
				.comment("true: pressing jump after Spider Techniques wall jump can trigger Epic Fight Phantom Ascent.")
				.define("spiderWallJumpPrimesPhantomAscent", true);
		builder.pop();
		SPEC = builder.build();
	}

	private ParcoolXWomConfig() {
	}

	public static boolean autoFastRunDash() {
		return AUTO_FAST_RUN_DASH.get();
	}

	public static boolean catLeapPrimesPhantomAscent() {
		return CAT_LEAP_PRIMES_PHANTOM_ASCENT.get();
	}

	public static boolean wallJumpPrimesPhantomAscent() {
		return WALL_JUMP_PRIMES_PHANTOM_ASCENT.get();
	}

	public static boolean spiderWallJumpPrimesPhantomAscent() {
		return SPIDER_WALL_JUMP_PRIMES_PHANTOM_ASCENT.get();
	}
}
