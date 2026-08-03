package dev.spake404.epm.skill.sandevistan.type;

import dev.spake404.epm.EPM;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public final class SandevistanProfiles {
	public static final SandevistanProfile DINARA_MK4 = profile("dinara_mk4")
			.timeScale(0.50D)
			.damageMultiplier(1.10D)
			.reactionSecondsPerMaxStamina(0.10D)
			.cooldownSeconds(40.0D)
			.build();
	public static final SandevistanProfile QIANT_WARP_DANCER_MK5 = profile("qiant_warp_dancer_mk5")
			.timeScale(0.80D)
			.damageMultiplier(1.30D)
			.incomingDamageMultiplier(0.75D)
			.fireWitherDamageMultiplier(0.25D)
			.reactionSecondsPerMaxStamina(0.10D)
			.cooldownSeconds(60.0D)
			.build();
	public static final SandevistanProfile MILITECH_FALCON = profile("militech_falcon")
			.timeScale(0.30D)
			.damageMultiplier(1.10D)
			.reactionSecondsPerMaxStamina(0.10D)
			.killRewards(0.05D, 0.10D, 0.0D)
			.cooldownSeconds(35.0D)
			.manualCooldownRefundEnabled(false)
			.partialChargeActivation(true)
			.build();
	public static final SandevistanProfile MILITECH_APOGEE = profile("militech_apogee")
			.timeScale(0.15D)
			.damageMultiplier(1.10D)
			.reactionSecondsPerMaxStamina(0.10D)
			.killRewards(0.10D, 0.0D, 0.22D)
			.cooldownSeconds(30.0D)
			.manualCooldownRefundEnabled(false)
			.partialChargeActivation(true)
			.build();
	public static final SandevistanProfile ZETATECH = profile("zetatech")
			.timeScales(0.70D, 0.40D)
			.damageMultipliers(1.0D, 1.50D)
			.fallDamageMultiplier(0.50D)
			.reactionSecondsPerMaxStamina(0.10D)
			.killRewards(0.10D, 0.0D, 0.22D)
			.cooldownSeconds(30.0D)
			.build();

	private static final Map<ResourceLocation, SandevistanProfile> PROFILES = Map.of(
			DINARA_MK4.id(), DINARA_MK4,
			QIANT_WARP_DANCER_MK5.id(), QIANT_WARP_DANCER_MK5,
			MILITECH_FALCON.id(), MILITECH_FALCON,
			MILITECH_APOGEE.id(), MILITECH_APOGEE,
			ZETATECH.id(), ZETATECH);

	private SandevistanProfiles() {
	}

	public static SandevistanProfile get(ResourceLocation id) {
		return id == null ? null : PROFILES.get(id);
	}

	private static SandevistanProfile.Builder profile(String path) {
		return SandevistanProfile.builder(ResourceLocation.fromNamespaceAndPath(EPM.MODID, path))
				.radius(40.0D)
				.playerSpeedMultiplier(1.0D)
				.afterimageIntervalTicks(1);
	}
}
