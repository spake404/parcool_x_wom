package com.alrex.parcool.common.action;

import java.nio.ByteBuffer;

import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;

import net.minecraft.world.entity.player.Player;

public class Action {
	public boolean isDoing() {
		return false;
	}

	public void start(Player player, Parkourability parkourability, ByteBuffer startInfo, IStamina stamina) {
	}
}
