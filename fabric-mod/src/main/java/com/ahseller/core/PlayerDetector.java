package com.ahseller.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class PlayerDetector {
    public boolean isPlayerNearby(MinecraftClient client, double radius) {
        if (client.player == null || client.world == null) return false;
        
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        
        Box box = new Box(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        return !client.world.getEntitiesByClass(PlayerEntity.class, box, p -> p != client.player).isEmpty();
    }
}
