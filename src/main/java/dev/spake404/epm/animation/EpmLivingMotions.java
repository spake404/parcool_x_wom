package dev.spake404.epm.animation;

import yesman.epicfight.api.animation.LivingMotion;

public enum EpmLivingMotions implements LivingMotion {
	FAST_SWIM,
	SURFACE_FAST_SWIM,
	DEMOLITION_LEAP_CHARGING;

	private final int id = LivingMotion.ENUM_MANAGER.assign(this);

	@Override
	public int universalOrdinal() {
		return id;
	}
}
