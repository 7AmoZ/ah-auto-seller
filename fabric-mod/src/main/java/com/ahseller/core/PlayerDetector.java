package com.ahseller.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class PlayerDetector {
    public boolean isPlayerNearby(MinecraftClient client, double radius) {
        if (client.player == null || client.world == null) return false;
        Box box = Box.from(client.player.getPos()).expand(radius);
        return !client.world.getEntitiesByClass(PlayerEntity.class, box, p -> p != client.player).isEmpty();
    }
}