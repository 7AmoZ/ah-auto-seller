 
package com.ahseller.core;

import net.minecraft.entity.player.PlayerEntity;

public class MovementTracker {
    private double lastX, lastZ;
    private int standTicks = 0;
    private boolean initialized = false;

    public void update(PlayerEntity player) {
        if (!initialized) {
            lastX = player.getX(); lastZ = player.getZ();
            initialized = true; return;
        }
        double dx = player.getX() - lastX;
        double dz = player.getZ() - lastZ;
        if (Math.sqrt(dx*dx + dz*dz) > 0.01) {
            standTicks = 0;
        } else {
            standTicks++;
        }
        lastX = player.getX(); lastZ = player.getZ();
    }

    public void reset() { standTicks = 0; initialized = false; }
    public int getStandTicks() { return standTicks; }
}