package net.minecraft.world.phys;

public class Vec3 {
	public static final Vec3 ZERO = new Vec3(0.0D, 0.0D, 0.0D);

	private final double x;
	private final double y;
	private final double z;

	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}
}
