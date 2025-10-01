package com.tuempresa.rogue.util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

/**
 * Utilidades para trabajar con volúmenes axis-aligned.
 */
public final class AABBUtil {
    private AABBUtil() {
    }

    public static AABB fromInts(int[] min, int[] max) {
        if (min.length != 3 || max.length != 3) {
            throw new IllegalArgumentException("Se requieren tres componentes para min y max");
        }
        return new AABB(min[0], min[1], min[2], max[0] + 1, max[1] + 1, max[2] + 1);
    }

    public static BlockPos randomPosInside(AABB box, RandomSource random) {
        int x = (int) Math.floor(random.nextDouble() * (box.maxX - box.minX)) + (int) box.minX;
        int y = (int) Math.floor(random.nextDouble() * (box.maxY - box.minY)) + (int) box.minY;
        int z = (int) Math.floor(random.nextDouble() * (box.maxZ - box.minZ)) + (int) box.minZ;
        return new BlockPos(x, y, z);
    }
}
