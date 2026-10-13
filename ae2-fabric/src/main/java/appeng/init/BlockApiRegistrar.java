package appeng.init;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Collects block API providers and registers them with Fabric's API lookups at the end.
 * <p>
 * NeoForge allows multiple capability providers per block (the first non-null result wins), while Fabric only accepts
 * one provider per lookup and block. This class combines all providers for the same lookup and block into one, in
 * registration order.
 */
final class BlockApiRegistrar {
    private final Map<BlockApiLookup<?, ?>, Map<Block, List<BlockApiLookup.BlockApiProvider<?, ?>>>> providers = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    <A, C, T extends BlockEntity> void registerBlockEntity(BlockApiLookup<A, C> lookup, BlockEntityType<T> type,
            BiFunction<? super T, C, @Nullable A> provider) {
        registerBlockEntityUnchecked(lookup, type, provider);
    }

    @SuppressWarnings("unchecked")
    <A, C, T extends BlockEntity> void registerBlockEntityUnchecked(BlockApiLookup<A, C> lookup,
            BlockEntityType<?> type, BiFunction<? super T, C, @Nullable A> provider) {
        BlockApiLookup.BlockApiProvider<A, C> blockProvider = (level, pos, state, blockEntity, context) -> {
            if (blockEntity == null || blockEntity.getType() != type) {
                return null;
            }
            return provider.apply((T) blockEntity, context);
        };
        for (var block : type.validBlocks) {
            registerBlock(lookup, blockProvider, block);
        }
    }

    <A, C> void registerBlock(BlockApiLookup<A, C> lookup, BlockApiLookup.BlockApiProvider<A, C> provider,
            Block block) {
        providers.computeIfAbsent(lookup, l -> new LinkedHashMap<>())
                .computeIfAbsent(block, b -> new ArrayList<>())
                .add(provider);
    }

    boolean isBlockRegistered(BlockApiLookup<?, ?> lookup, Block block) {
        var forLookup = providers.get(lookup);
        return forLookup != null && forLookup.containsKey(block);
    }

    void registerAll() {
        for (var entry : providers.entrySet()) {
            registerAll(entry.getKey(), entry.getValue());
        }
        providers.clear();
    }

    @SuppressWarnings("unchecked")
    private static <A, C> void registerAll(BlockApiLookup<A, C> lookup,
            Map<Block, List<BlockApiLookup.BlockApiProvider<?, ?>>> blockProviders) {
        for (var entry : blockProviders.entrySet()) {
            var list = (List<BlockApiLookup.BlockApiProvider<A, C>>) (List<?>) entry.getValue();
            BlockApiLookup.BlockApiProvider<A, C> combined;
            if (list.size() == 1) {
                combined = list.getFirst();
            } else {
                combined = (level, pos, state, blockEntity, context) -> {
                    for (var provider : list) {
                        var result = provider.find(level, pos, state, blockEntity, context);
                        if (result != null) {
                            return result;
                        }
                    }
                    return null;
                };
            }
            lookup.registerForBlocks(combined, entry.getKey());
        }
    }
}
