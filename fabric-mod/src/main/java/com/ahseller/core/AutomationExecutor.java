package com.ahseller.core;

import com.ahseller.network.SocketClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import java.util.concurrent.ThreadLocalRandom;

public class AutomationExecutor {
    private final StateManager manager;
    private enum Phase { WAIT_STAND, CHECK_PLAYERS, SEND_COMMAND, WAIT_DELAY, CLICK_CONFIRM, SCAN }
    private Phase phase = Phase.WAIT_STAND;
    private int delayTicks = 0;
    private int targetDelayTicks = 0;

    public AutomationExecutor(StateManager manager) { 
        this.manager = manager; 
    }

    public void tick(MinecraftClient client) {
        switch (phase) {
            case WAIT_STAND -> {
                int required = manager.getConfig().standTime * 20;
                if (manager.getMovementTracker().getStandTicks() >= required) {
                    log(client, "Player stopped moving. Checking area...");
                    phase = Phase.CHECK_PLAYERS;
                }
            }
            case CHECK_PLAYERS -> {
                if (manager.getPlayerDetector().isPlayerNearby(client, manager.getConfig().playerRadius)) {
                    log(client, "Nearby player detected! Stopping automation.");
                    SocketClient.sendEvent("{\"type\":\"PLAYER_DETECTED\"}");
                    manager.stop();
                    return;
                }
                phase = Phase.SEND_COMMAND;
            }
            case SEND_COMMAND -> {
                if (client.player != null && client.getNetworkHandler() != null) {
                    // في إصدار 1.21.1 بنبعت الأمر من غير علامة الـ "/" في الأول
                    String commandText = "ah sell " + manager.getConfig().price;
                    
                    // تنفيذ الأمر على الـ Main Thread لضمان عدم حدوث تداخل في الـ Packets
                    client.execute(() -> {
                        client.getNetworkHandler().sendCommand(commandText);
                    });
                    
                    log(client, "Sell command sent: " + commandText);
                }
                
                double min = manager.getConfig().minDelay;
                double max = manager.getConfig().maxDelay;
                targetDelayTicks = (int) ((ThreadLocalRandom.current().nextDouble(min, max)) * 20);
                delayTicks = 0;
                phase = Phase.WAIT_DELAY;
            }
            case WAIT_DELAY -> {
                delayTicks++;
                if (delayTicks >= targetDelayTicks) phase = Phase.CLICK_CONFIRM;
            }
            case CLICK_CONFIRM -> {
                if (client.currentScreen instanceof GenericContainerScreen screen) {
                    clickSlot(client, screen, manager.getConfig().confirmSlot);
                    log(client, "Confirm clicked (slot " + manager.getConfig().confirmSlot + ")");
                } else {
                    log(client, "No container screen open.");
                }
                phase = Phase.SCAN;
                delayTicks = 0;
                targetDelayTicks = 10;
            }
            case SCAN -> {
                delayTicks++;
                if (delayTicks >= targetDelayTicks) {
                    int count = manager.getInventoryScanner().countEnderPearls(client.player);
                    SocketClient.sendEvent("{\"type\":\"ITEM_COUNT\",\"count\":" + count + "}");
                    if (count == 0) {
                        log(client, "Out of Ender Pearls.");
                        manager.setOutOfItems();
                        SocketClient.sendEvent("{\"type\":\"STATUS\",\"state\":\"OUT_OF_ITEMS\"}");
                        return;
                    }
                    phase = Phase.WAIT_STAND;
                    manager.getMovementTracker().reset();
                }
            }
        }
    }

    private void clickSlot(MinecraftClient client, GenericContainerScreen screen, int slotIndex) {
        try {
            var handler = screen.getScreenHandler();
            if (slotIndex < 0 || slotIndex >= handler.slots.size()) return;
            
            client.interactionManager.clickSlot(
                handler.syncId,
                slotIndex,
                0,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                client.player
            );
        } catch (Exception e) {
            log(MinecraftClient.getInstance(), "Slot click failed: " + e.getMessage());
        }
    }

    public void cleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        client.executeSync(() -> {
            if (client.currentScreen instanceof ChatScreen || client.currentScreen instanceof GenericContainerScreen) {
                client.setScreen(null);
            }
            if (client.player != null) {
                client.player.closeHandledScreen();
            }
        });
        phase = Phase.WAIT_STAND;
    }

    public void reset() { 
        phase = Phase.WAIT_STAND; 
        delayTicks = 0; 
    }

    private void log(MinecraftClient client, String msg) {
        SocketClient.sendEvent("{\"type\":\"LOG\",\"msg\":\"" + msg.replace("\"", "\\\"") + "\"}");
    }
}
