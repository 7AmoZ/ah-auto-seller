package com.ahseller.core;

import com.ahseller.network.SocketClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import java.util.concurrent.ThreadLocalRandom;

public class AutomationExecutor {
    private final StateManager manager;
    private enum Phase { WAIT_STAND, CHECK_PLAYERS, SEND_COMMAND, WAIT_DELAY, CLICK_CONFIRM, SCAN }
    private Phase phase = Phase.WAIT_STAND;
    private int delayTicks = 0;
    private int targetDelayTicks = 0;
    private int lastSuccessfulMethodIndex = -1; // تتبع آخر طريقة نجحت

    public AutomationExecutor(StateManager manager) { 
        this.manager = manager; 
    }

    public void tick(MinecraftClient client) {
        // ✅ التحقق: المود لا يعمل إلا لو البرنامج مفتوح
        if (!SocketClient.isConnected()) {
            if (phase != Phase.WAIT_STAND) {
                log(client, "Disconnected from Python Controller. Stopping automation.");
                phase = Phase.WAIT_STAND;
            }
            return;
        }

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
                if (client.player != null) {
                    sendCommandSafe(client);
                }
                log(client, "Sell command sent.");
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
                    clickSlotSafe(client, screen, manager.getConfig().confirmSlot);
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

    // جرب جميع طرق إرسال الأمر
    private void sendCommandSafe(MinecraftClient client) {
        String command = "ah sell " + manager.getConfig().price;
        
        // لو معروفة آخر طريقة نجحت، ابدأ بيها
        if (lastSuccessfulMethodIndex >= 0) {
            if (tryMethod(client, command, lastSuccessfulMethodIndex)) {
                return;
            }
        }
        
        // جرب جميع الطرق بالترتيب
        for (int i = 0; i < 3; i++) {
            if (i == lastSuccessfulMethodIndex) continue; // تخطي الطريقة المعروفة
            if (tryMethod(client, command, i)) {
                lastSuccessfulMethodIndex = i;
                return;
            }
        }
        
        log(client, "Failed to send command with all methods!");
    }

    private boolean tryMethod(MinecraftClient client, String command, int methodIndex) {
        try {
            switch (methodIndex) {
                case 0: // الطريقة الأولى: sendChatMessage مع /
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatMessage("/" + command);
                        log(client, "Using method 1: sendChatMessage");
                        return true;
                    }
                    return false;
                    
                case 1: // الطريقة الثانية: onSlotClick (لو كان GUI مفتوح)
                    if (client.currentScreen instanceof GenericContainerScreen screen) {
                        var handler = screen.getScreenHandler();
                        handler.onSlotClick(0, 0, SlotActionType.PICKUP, client.player);
                        log(client, "Using method 2: onSlotClick");
                        return true;
                    }
                    return false;
                    
                case 2: // الطريقة الثالثة: محاولة استدعاء الأمر عبر NetworkHandler
                    if (client.getNetworkHandler() != null) {
                        // إرسال الأمر بطريقة آمنة
                        client.getNetworkHandler().sendChatMessage("/" + command);
                        log(client, "Using method 3: fallback sendChatMessage");
                        return true;
                    }
                    return false;
            }
        } catch (Exception e) {
            log(client, "Method " + (methodIndex + 1) + " failed: " + e.getMessage());
            return false;
        }
        return false;
    }

    // جرب جميع طرق الكليك
    private void clickSlotSafe(MinecraftClient client, GenericContainerScreen screen, int slotIndex) {
        // لو معروفة آخر طريقة نجحت، ابدأ بيها
        if (lastSuccessfulMethodIndex >= 0) {
            if (tryClickMethod(client, screen, slotIndex, lastSuccessfulMethodIndex)) {
                return;
            }
        }
        
        // جرب جميع الطرق بالترتيب
        for (int i = 0; i < 3; i++) {
            if (i == lastSuccessfulMethodIndex) continue;
            if (tryClickMethod(client, screen, slotIndex, i)) {
                lastSuccessfulMethodIndex = i;
                return;
            }
        }
        
        log(client, "Failed to click slot with all methods!");
    }

    private boolean tryClickMethod(MinecraftClient client, GenericContainerScreen screen, int slotIndex, int methodIndex) {
        try {
            var handler = screen.getScreenHandler();
            if (slotIndex < 0 || slotIndex >= handler.slots.size()) {
                return false;
            }

            switch (methodIndex) {
                case 0: // الطريقة الأولى: onSlotClick
                    try {
                        handler.onSlotClick(slotIndex, 0, SlotActionType.PICKUP, client.player);
                        log(client, "Slot click method 1: onSlotClick");
                        return true;
                    } catch (NoSuchMethodError e) {
                        return false;
                    }
                    
                case 1: // الطريقة الثانية: clickSlot
                    try {
                        if (client.interactionManager != null) {
                            client.interactionManager.clickSlot(
                                handler.syncId,
                                slotIndex,
                                0,
                                SlotActionType.PICKUP,
                                client.player
                            );
                            log(client, "Slot click method 2: clickSlot");
                            return true;
                        }
                        return false;
                    } catch (NoSuchMethodError e) {
                        return false;
                    }
                    
                case 2: // الطريقة الثالثة: إغلاق الشاشة
                    if (client.player != null) {
                        client.player.closeHandledScreen();
                        log(client, "Slot click method 3: closeHandledScreen");
                        return true;
                    }
                    return false;
            }
        } catch (Exception e) {
            log(client, "Click method " + (methodIndex + 1) + " failed: " + e.getMessage());
            return false;
        }
        return false;
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
        lastSuccessfulMethodIndex = -1;
    }

    public void reset() { 
        phase = Phase.WAIT_STAND; 
        delayTicks = 0;
        lastSuccessfulMethodIndex = -1;
    }

    private void log(MinecraftClient client, String msg) {
        SocketClient.sendEvent("{\"type\":\"LOG\",\"msg\":\"" + msg.replace("\"", "\\\"") + "\"}");
    }
}
