package com.ahseller.core;

import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;

public class InventoryScanner {
    public int countEnderPearls(PlayerEntity player) {
        if (player == null) return 0;
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.ENDER_PEARL)) {
                count += inv.getStack(i).getCount();
            }
        }
        return count;
    }
}
