package com.ahseller;

import com.ahseller.core.StateManager;
import com.ahseller.network.SocketClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AHSellerMod implements ClientModInitializer {
    public static final String MOD_ID = "ahseller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static SocketClient socketClient;
    private static StateManager stateManager;

    @Override
    public void onInitializeClient() {
        LOGGER.info("AH Seller Mod initialized.");
        stateManager = new StateManager();
        socketClient = new SocketClient("127.0.0.1", 25566, stateManager);
        socketClient.start();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            stateManager.tick(client);
        });
    }

    public static StateManager getStateManager() { return stateManager; }
    public static SocketClient getSocketClient() { return socketClient; }
}