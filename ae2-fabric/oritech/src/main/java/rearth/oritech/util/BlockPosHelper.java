package rearth.oritech.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/** BlockPos helpers that were removed from vanilla in 26.3. */
public final class BlockPosHelper {
    private BlockPosHelper() {}

    public static BlockPos of(Vec3i vec) {
        return new BlockPos(vec.getX(), vec.getY(), vec.getZ());
    }

    /** All positions in the box around {@code center}, ordered by their manhattan distance to it. */
    public static Iterable<BlockPos> withinManhattan(BlockPos center, int rangeX, int rangeY, int rangeZ) {
        List<BlockPos> result = new ArrayList<>((2 * rangeX + 1) * (2 * rangeY + 1) * (2 * rangeZ + 1));
        for (int x = -rangeX; x <= rangeX; x++) {
            for (int y = -rangeY; y <= rangeY; y++) {
                for (int z = -rangeZ; z <= rangeZ; z++) {
                    result.add(center.offset(x, y, z));
                }
            }
        }
        result.sort(Comparator.comparingInt(pos -> pos.distManhattan(center)));
        return result;
    }
}
