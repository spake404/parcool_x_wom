package yesman.epicfight.world.entity.eventlistener;

import java.util.UUID;
import java.util.function.Consumer;

public class PlayerEventListener {
	public <T> void addEventListener(EventType<T> eventType, UUID uuid, Consumer<T> function, int priority) {
	}

	public <T> boolean triggerEvents(EventType<T> eventType, T event) {
		return false;
	}

	public static class EventType<T> {
		public static final EventType<MovementInputEvent> MOVEMENT_INPUT_EVENT = new EventType<>();
	}
}
