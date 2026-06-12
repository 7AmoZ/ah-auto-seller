package com.ahseller.core;

import com.ahseller.config.ModConfig;
import net.minecraft.client.MinecraftClient;

public class StateManager {
    public enum Status { STOPPED, RUNNING, OUT_OF_ITEMS }
    private Status status = Status.STOPPED;
    private final ModConfig config = new ModConfig();
    private final MovementTracker movementTracker = new MovementTracker();
    private final PlayerDetector playerDetector = new PlayerDetector();
    private final InventoryScanner inventoryScanner = new InventoryScanner();
    private final AutomationExecutor executor = new AutomationExecutor(this);

    public void tick(MinecraftClient client) {
        if (status != Status.RUNNING) return;
        movementTracker.update(client.player);
        executor.tick(client);
    }

    public void start() { status = Status.RUNNING; movementTracker.reset(); executor.reset(); }
    public void stop() { status = Status.STOPPED; executor.cleanup(); }
    public void setOutOfItems() { status = Status.OUT_OF_ITEMS; executor.cleanup(); }

    public Status getStatus() { return status; }
    public ModConfig getConfig() { return config; }
    public MovementTracker getMovementTracker() { return movementTracker; }
    public PlayerDetector getPlayerDetector() { return playerDetector; }
    public InventoryScanner getInventoryScanner() { return inventoryScanner; }
}