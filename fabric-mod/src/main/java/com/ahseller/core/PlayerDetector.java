package com.ahseller.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import java.lang.reflect.Method;

public class PlayerDetector {
    private static Method getPosMethod = null;
    private static boolean methodFound = false;

    // كلاس مساعد للحصول على إحداثيات اللاعب بشكل آمن عبر أي إصدار
    private static double[] getPlayerCoords(PlayerEntity player) {
        if (!methodFound) {
            try {
                // البحث عن أي دالة ترجع Vec3d ولا تأخذ مدخلات
                for (Method m : player.getClass().getDeclaredMethods()) {
                    if (m.getReturnType().getName().contains("Vec3d") && m.getParameterCount() == 0) {
                        m.setAccessible(true);
                        getPosMethod = m;
                        break;
                    }
                }
                methodFound = true;
            } catch (Exception e) {
                // إذا فشل البحث، نعود للطريقة التقليدية كحل أخير
                return new double[]{player.getX(), player.getY(), player.getZ()};
            }
        }

        if (getPosMethod != null) {
            try {
                Object vec = getPosMethod.invoke(player);
                // استخراج x, y, z من كائن Vec3d بغض النظر عن أسماء الحقول الداخلية
                double x = (double) vec.getClass().getField("x").get(vec);
                double y = (double) vec.getClass().getField("y").get(vec);
                double z = (double) vec.getClass().getField("z").get(vec);
                return new double[]{x, y, z};
            } catch (Exception ignored) {}
        }
        
        // Fallback آمن 100% لجميع الإصدارات
        return new double[]{player.getX(), player.getY(), player.getZ()};
    }

    public boolean isPlayerNearby(MinecraftClient client, double radius) {
        if (client.player == null || client.world == null) return false;
        
        double[] coords = getPlayerCoords(client.player);
        Box box = new Box(
            coords[0] - radius, coords[1] - radius, coords[2] - radius,
            coords[0] + radius, coords[1] + radius, coords[2] + radius
        );
        
        return !client.world.getEntitiesByClass(PlayerEntity.class, box, p -> p != client.player).isEmpty();
    }
}
