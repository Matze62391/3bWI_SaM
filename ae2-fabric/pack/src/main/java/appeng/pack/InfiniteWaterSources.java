package appeng.pack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

/**
 * Blocks in the {@code ae2_fabric_pack:infinite_water_sources} tag (by default the kitchen sinks of Macaw's Furniture)
 * offer an endless amount of water through the Fabric transfer API. An AE2 storage bus on such a block shows the water
 * in the terminal, and the network can take as much as it needs without filling the block first.
 */
public class InfiniteWaterSources implements ModInitializer {
    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath("ae2_fabric_pack", "infinite_water_sources"));

    /**
     * The amount offered to other mods. Large, but far enough from {@link Long#MAX_VALUE} that adding it to other water
     * does not overflow. AE2's storage bus shows at most 2^42 droplets (~54 million buckets) of it.
     */
    public static final long DISPLAYED_AMOUNT = 1_000_000_000L * FluidConstants.BUCKET;

    @Override
    public void onInitialize() {
        // A fallback, so it does not depend on the order in which mods register their blocks, and a block that gets
        // its own fluid storage from its mod keeps it.
        FluidStorage.SIDED.registerFallback((level, pos, state, blockEntity, side) -> state.is(TAG)
                ? InfiniteWaterStorage.INSTANCE
                : null);
    }

    public static final class InfiniteWaterStorage implements ExtractionOnlyStorage<FluidVariant>,
            SingleSlotStorage<FluidVariant> {
        public static final InfiniteWaterStorage INSTANCE = new InfiniteWaterStorage();
        private static final FluidVariant WATER = FluidVariant.of(Fluids.WATER);

        private InfiniteWaterStorage() {
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return resource.equals(WATER) ? maxAmount : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return false;
        }

        @Override
        public FluidVariant getResource() {
            return WATER;
        }

        @Override
        public long getAmount() {
            return DISPLAYED_AMOUNT;
        }

        @Override
        public long getCapacity() {
            return DISPLAYED_AMOUNT;
        }
    }
}
