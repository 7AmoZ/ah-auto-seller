package com.ahseller.network;

import com.ahseller.AHSellerMod;
import com.ahseller.core.StateManager;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketClient {
    private final String host;
    private final int port;
    private final StateManager manager;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public SocketClient(String host, int port, StateManager manager) {
        this.host = host; this.port = port; this.manager = manager;
    }

    public void start() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    socket = new Socket(host, port);
                    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    AHSellerMod.LOGGER.info("Connected to Python Controller.");
                    sendEvent("{\"type\":\"STATUS\",\"state\":\"STOPPED\"}");

                    String line;
                    while ((line = in.readLine()) != null) {
                        handleCommand(line);
                    }
                } catch (Exception e) {
                    AHSellerMod.LOGGER.warn("Socket disconnected. Retrying in 3s...");
                    manager.stop();
                    try { Thread.sleep(3000); } catch (InterruptedException ie) { break; }
                }
            }
        }, "AH-Socket-Thread").start();
    }

    private void handleCommand(String json) {
        MinecraftClient.getInstance().executeSync(() -> {
            try {
                var obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                String type = obj.get("type").getAsString();
                switch (type) {
                    case "START" -> { manager.start(); sendEvent("{\"type\":\"STATUS\",\"state\":\"RUNNING\"}"); }
                    case "STOP" -> { manager.stop(); sendEvent("{\"type\":\"STATUS\",\"state\":\"STOPPED\"}"); }
                    case "CONFIG" -> manager.getConfig().updateFromJson(obj.get("data").toString());
                }
            } catch (Exception e) {
                AHSellerMod.LOGGER.error("Invalid command: {}", json);
            }
        });
    }

    public static synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && out != null;
    }

    public static synchronized void sendEvent(String json) {
        if (out != null) {
            out.println(json);
            out.flush();
        }
    }
}
