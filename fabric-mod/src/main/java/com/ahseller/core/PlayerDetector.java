package com.ahseller.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PlayerDetector {
    public boolean isPlayerNearby(MinecraftClient client, double radius) {
        if (client.player == null || client.world == null) return false;
        
        // استخدام Vec3d مباشرة لتجنب مشاكل التوافق مع 1.21.11
        Vec3d pos = client.player.getPos();
        Box box = new Box(
            pos.x - radius, pos.y - radius, pos.z - radius,
            pos.x + radius, pos.y + radius, pos.z + radius
        );
        
        return !client.world.getEntitiesByClass(PlayerEntity.class, box, p -> p != client.player).isEmpty();
    }
}                                            
