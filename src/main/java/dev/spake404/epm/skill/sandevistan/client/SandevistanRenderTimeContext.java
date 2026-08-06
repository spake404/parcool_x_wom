package dev.spake404.epm.skill.sandevistan.client;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SandevistanRenderTimeContext {
	private static final ThreadLocal<Deque<Float>> LOCAL_PARTIAL_TICKS =
			ThreadLocal.withInitial(ArrayDeque::new);

	private SandevistanRenderTimeContext() {
	}

	public static void push(float partialTick) {
		LOCAL_PARTIAL_TICKS.get().push(partialTick);
	}

	public static void pop() {
		Deque<Float> partialTicks = LOCAL_PARTIAL_TICKS.get();
		if (!partialTicks.isEmpty()) {
			partialTicks.pop();
		}
		if (partialTicks.isEmpty()) {
			LOCAL_PARTIAL_TICKS.remove();
		}
	}

	public static Float currentPartialTick() {
		Deque<Float> partialTicks = LOCAL_PARTIAL_TICKS.get();
		return partialTicks.isEmpty() ? null : partialTicks.peek();
	}
}
