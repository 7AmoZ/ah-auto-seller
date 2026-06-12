package com.ahseller.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ModConfig {
    public int price = 2000;
    public double minDelay = 0.2;
    public double maxDelay = 1.0;
    public double playerRadius = 20.0;
    public int standTime = 10;
    public int clickX = 960;
    public int clickY = 540;
    public int confirmSlot = 16;

    public void updateFromJson(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("price")) price = obj.get("price").getAsInt();
            if (obj.has("min_delay")) minDelay = obj.get("min_delay").getAsDouble();
            if (obj.has("max_delay")) maxDelay = obj.get("max_delay").getAsDouble();
            if (obj.has("player_radius")) playerRadius = obj.get("player_radius").getAsDouble();
            if (obj.has("stand_time")) standTime = obj.get("stand_time").getAsInt();
            if (obj.has("click_x")) clickX = obj.get("click_x").getAsInt();
            if (obj.has("click_y")) clickY = obj.get("click_y").getAsInt();
            if (obj.has("confirm_slot")) confirmSlot = obj.get("confirm_slot").getAsInt();
        } catch (Exception e) {
            com.ahseller.AHSellerMod.LOGGER.warn("Failed to parse config: {}", e.getMessage());
        }
    }
}
