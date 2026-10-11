package appeng.parts;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.parts.IPartHost;
import appeng.util.Platform;

/**
 * Utility class to cache an API that is adjacent to a part.
 */
public class PartAdjacentApi<T> {
    private final AEBasePart part;
    private final BlockApiLookup<T, Direction> lookup;
    private BlockApiCache<T, Direction> cache;

    public PartAdjacentApi(AEBasePart part, BlockApiLookup<T, Direction> lookup) {
        this.lookup = lookup;
        this.part = part;
    }

    @Nullable
    public T find() {
        if (!(part.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (!isPartValid(part)) {
            return null;
        }

        var host = part.getHost().getBlockEntity();
        var attachedSide = part.getSide();
        var targetPos = host.getBlockPos().relative(attachedSide);

        if (!Platform.areBlockEntitiesTicking(serverLevel, targetPos)) {
            return null;
        }

        if (cache == null || cache.getLevel() != serverLevel) {
            cache = BlockApiCache.create(lookup, serverLevel, targetPos);
        }

        return cache.find(attachedSide.getOpposite());
    }

    public static boolean isPartValid(AEBasePart part) {
        var be = part.getBlockEntity();
        return be instanceof IPartHost host && host.getPart(part.getSide()) == part && !be.isRemoved();
    }
}
