package dev.spake404.epm;

public final class WomCompatBridge {
	private static final WomCompat INSTANCE = create();

	private WomCompatBridge() {
	}

	static WomCompat instance() {
		return INSTANCE;
	}

	private static WomCompat create() {
		if (!ModCompat.isWomLoaded()) {
			return new NoopWomCompat();
		}

		try {
			Class<?> compatClass = Class.forName("dev.spake404.epm.LoadedWomCompat");
			return (WomCompat) compatClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			return new NoopWomCompat();
		}
	}
}
