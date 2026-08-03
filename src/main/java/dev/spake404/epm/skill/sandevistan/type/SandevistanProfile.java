package dev.spake404.epm.skill.sandevistan.type;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class SandevistanProfile {
	private final ResourceLocation id;
	private final double groundTimeScale;
	private final double airTimeScale;
	private final double radius;
	private final double playerSpeedMultiplier;
	private final double groundDamageMultiplier;
	private final double airDamageMultiplier;
	private final double incomingDamageMultiplier;
	private final double fireWitherDamageMultiplier;
	private final double fallDamageMultiplier;
	private final double reactionSecondsPerMaxStamina;
	private final double killDurationRestoreFraction;
	private final double killHealthRestoreFraction;
	private final double killStaminaRestoreFraction;
	private final double cooldownSeconds;
	private final boolean manualCooldownRefundEnabled;
	private final boolean partialChargeActivation;
	private final int afterimageIntervalTicks;

	private SandevistanProfile(Builder builder) {
		this.id = builder.id;
		this.groundTimeScale = clampTimeScale(builder.groundTimeScale);
		this.airTimeScale = clampTimeScale(builder.airTimeScale);
		this.radius = Math.max(0.0D, builder.radius);
		this.playerSpeedMultiplier = Math.max(0.0D, builder.playerSpeedMultiplier);
		this.groundDamageMultiplier = Math.max(0.0D, builder.groundDamageMultiplier);
		this.airDamageMultiplier = Math.max(0.0D, builder.airDamageMultiplier);
		this.incomingDamageMultiplier = Math.max(0.0D, builder.incomingDamageMultiplier);
		this.fireWitherDamageMultiplier = Math.max(0.0D, builder.fireWitherDamageMultiplier);
		this.fallDamageMultiplier = Math.max(0.0D, builder.fallDamageMultiplier);
		this.reactionSecondsPerMaxStamina = Math.max(0.0D, builder.reactionSecondsPerMaxStamina);
		this.killDurationRestoreFraction = Math.max(0.0D, builder.killDurationRestoreFraction);
		this.killHealthRestoreFraction = Math.max(0.0D, builder.killHealthRestoreFraction);
		this.killStaminaRestoreFraction = Math.max(0.0D, builder.killStaminaRestoreFraction);
		this.cooldownSeconds = Math.max(0.05D, builder.cooldownSeconds);
		this.manualCooldownRefundEnabled = builder.manualCooldownRefundEnabled;
		this.partialChargeActivation = builder.partialChargeActivation;
		this.afterimageIntervalTicks = Math.max(1, builder.afterimageIntervalTicks);
	}

	public static Builder builder(ResourceLocation id) {
		return new Builder(id);
	}

	public ResourceLocation id() {
		return id;
	}

	public double timeScale(Player player) {
		return player != null && !player.onGround() ? airTimeScale : groundTimeScale;
	}

	public double groundTimeScale() {
		return groundTimeScale;
	}

	public double airTimeScale() {
		return airTimeScale;
	}

	public double radius() {
		return radius;
	}

	public double playerSpeedMultiplier() {
		return playerSpeedMultiplier;
	}

	public double outgoingDamageMultiplier(Player player) {
		return player != null && !player.onGround() ? airDamageMultiplier : groundDamageMultiplier;
	}

	public double groundDamageMultiplier() {
		return groundDamageMultiplier;
	}

	public double airDamageMultiplier() {
		return airDamageMultiplier;
	}

	public double incomingDamageMultiplier() {
		return incomingDamageMultiplier;
	}

	public double fireWitherDamageMultiplier() {
		return fireWitherDamageMultiplier;
	}

	public double fallDamageMultiplier() {
		return fallDamageMultiplier;
	}

	public double reactionSecondsPerMaxStamina() {
		return reactionSecondsPerMaxStamina;
	}

	public double killDurationRestoreFraction() {
		return killDurationRestoreFraction;
	}

	public double killHealthRestoreFraction() {
		return killHealthRestoreFraction;
	}

	public double killStaminaRestoreFraction() {
		return killStaminaRestoreFraction;
	}

	public double cooldownSeconds() {
		return cooldownSeconds;
	}

	public boolean manualCooldownRefundEnabled() {
		return manualCooldownRefundEnabled;
	}

	public boolean partialChargeActivation() {
		return partialChargeActivation;
	}

	public int afterimageIntervalTicks() {
		return afterimageIntervalTicks;
	}

	private static double clampTimeScale(double value) {
		return Math.max(0.05D, Math.min(1.0D, value));
	}

	public static final class Builder {
		private final ResourceLocation id;
		private double groundTimeScale = 1.0D;
		private double airTimeScale = 1.0D;
		private double radius = 40.0D;
		private double playerSpeedMultiplier = 1.0D;
		private double groundDamageMultiplier = 1.0D;
		private double airDamageMultiplier = 1.0D;
		private double incomingDamageMultiplier = 1.0D;
		private double fireWitherDamageMultiplier = 1.0D;
		private double fallDamageMultiplier = 1.0D;
		private double reactionSecondsPerMaxStamina;
		private double killDurationRestoreFraction;
		private double killHealthRestoreFraction;
		private double killStaminaRestoreFraction;
		private double cooldownSeconds = 30.0D;
		private boolean manualCooldownRefundEnabled = true;
		private boolean partialChargeActivation;
		private int afterimageIntervalTicks = 1;

		private Builder(ResourceLocation id) {
			this.id = id;
		}

		public Builder timeScale(double value) {
			this.groundTimeScale = value;
			this.airTimeScale = value;
			return this;
		}

		public Builder timeScales(double ground, double air) {
			this.groundTimeScale = ground;
			this.airTimeScale = air;
			return this;
		}

		public Builder radius(double value) {
			this.radius = value;
			return this;
		}

		public Builder playerSpeedMultiplier(double value) {
			this.playerSpeedMultiplier = value;
			return this;
		}

		public Builder damageMultiplier(double value) {
			this.groundDamageMultiplier = value;
			this.airDamageMultiplier = value;
			return this;
		}

		public Builder damageMultipliers(double ground, double air) {
			this.groundDamageMultiplier = ground;
			this.airDamageMultiplier = air;
			return this;
		}

		public Builder incomingDamageMultiplier(double value) {
			this.incomingDamageMultiplier = value;
			return this;
		}

		public Builder fireWitherDamageMultiplier(double value) {
			this.fireWitherDamageMultiplier = value;
			return this;
		}

		public Builder fallDamageMultiplier(double value) {
			this.fallDamageMultiplier = value;
			return this;
		}

		public Builder reactionSecondsPerMaxStamina(double value) {
			this.reactionSecondsPerMaxStamina = value;
			return this;
		}

		public Builder killRewards(double durationFraction, double healthFraction, double staminaFraction) {
			this.killDurationRestoreFraction = durationFraction;
			this.killHealthRestoreFraction = healthFraction;
			this.killStaminaRestoreFraction = staminaFraction;
			return this;
		}

		public Builder cooldownSeconds(double value) {
			this.cooldownSeconds = value;
			return this;
		}

		public Builder manualCooldownRefundEnabled(boolean value) {
			this.manualCooldownRefundEnabled = value;
			return this;
		}

		public Builder partialChargeActivation(boolean value) {
			this.partialChargeActivation = value;
			return this;
		}

		public Builder afterimageIntervalTicks(int value) {
			this.afterimageIntervalTicks = value;
			return this;
		}

		public SandevistanProfile build() {
			return new SandevistanProfile(this);
		}
	}
}
