package net.minecraft.world.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Player extends Entity {
	public boolean isLocalPlayer() {
		return false;
	}

	public Vec3 getDeltaMovement() {
		return Vec3.ZERO;
	}

	public void setDeltaMovement(double x, double y, double z) {
	}

	public void setSprinting(boolean sprinting) {
	}

	public boolean isSprinting() {
		return false;
	}

	public boolean onGround() {
		return false;
	}

	public boolean isShiftKeyDown() {
		return false;
	}

	public boolean isInWaterOrBubble() {
		return false;
	}

	public boolean isFallFlying() {
		return false;
	}

	public Object getVehicle() {
		return null;
	}
}
