package com.alrex.parcool.api.unstable.action;

import com.alrex.parcool.common.action.Action;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class ParCoolActionEvent extends Event {
	public Player getPlayer() {
		return null;
	}

	public Action getAction() {
		return null;
	}

	public static class StartEvent extends ParCoolActionEvent {
	}

	public static class StopEvent extends ParCoolActionEvent {
	}

	public static class Start extends ParCoolActionEvent {
		public static class Post extends Start {
		}
	}

	public static class Finish extends ParCoolActionEvent {
		public static class Post extends Finish {
		}
	}

	public static class Tick extends ParCoolActionEvent {
		public static class Post extends Tick {
		}
	}
}
