package com.alrex.parcool.common.capability;

import com.alrex.parcool.common.action.Action;

import net.minecraft.world.entity.player.Player;

public class Parkourability {
	public static Parkourability get(Player player) {
		return null;
	}

	public <T extends Action> T get(Class<T> type) {
		return null;
	}
}
