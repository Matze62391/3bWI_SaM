package com.glodblock.github.glodium.registry;

import appeng.core.registries.DeferredBlock;
import appeng.core.registries.DeferredItem;
import appeng.core.registries.DeferredRegister;
import com.glodblock.github.glodium.registry.defer.DeferredDataComponentRegister;
import com.glodblock.github.glodium.registry.defer.DeferredDataComponentType;
import com.glodblock.github.glodium.registry.defer.DeferredTileEntityType;
import com.glodblock.github.glodium.registry.defer.DeferredTileTypeRegister;
import com.glodblock.github.glodium.registry.token.TileToken;
import com.glodblock.github.glodium.xmod.XModManager;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects a mod's blocks, items, block entity types, data components and capabilities. On NeoForge, the registers are
 * attached to the mod event bus. On Fabric, the mod calls {@link #register()} once everything is declared.
 */
public class RegistryHandler {

    protected final String id;
    protected final DeferredRegister.Items items;
    protected final DeferredRegister.Blocks blocks;
    protected final DeferredTileTypeRegister tiles;
    protected final DeferredDataComponentRegister components;
    protected final List<Pair<TileToken, Set<Block>>> tileBind = new ArrayList<>();
    protected final List<TileToken> tileTypes = new ArrayList<>();
    protected final List<TileCapabilityMap<?, ?, ?>> tileCaps = new ArrayList<>();
    protected final List<ItemCapabilityMap<?, ?, ?>> itemCaps = new ArrayList<>();

    public RegistryHandler(String modid) {
        this.id = modid;
        this.items = DeferredRegister.createItems(modid);
        this.blocks = DeferredRegister.createBlocks(modid);
        this.tiles = new DeferredTileTypeRegister(modid);
        this.components = new DeferredDataComponentRegister(modid);
    }

    /**
     * Registers everything with the vanilla registries, then the capabilities, then lets the mod's integrations add
     * their own content.
     */
    public void register() {
        this.components.register();
        this.blocks.register();
        this.items.register();
        this.tiles.register();
        XModManager.register(this.id, this);
        this.registerCapabilities();
    }

    protected void registerCapabilities() {
        // Fabric only uses the first provider registered for a block entity type, so the providers of one lookup are
        // combined: the first that returns something wins.
        var blockProviders = new LinkedHashMap<Pair<BlockApiLookup<?, ?>, TileToken>, List<TileCapabilityMap<?, ?, ?>>>();
        for (var token : this.tileTypes) {
            for (var tcm : this.tileCaps) {
                if (tcm.capInterface().isAssignableFrom(token.token())) {
                    blockProviders.computeIfAbsent(Pair.of(tcm.cap(), token), k -> new ArrayList<>()).add(tcm);
                }
            }
        }
        blockProviders.forEach((key, maps) -> registerBlockProviders(key.getRight(), maps));

        for (var item : this.items.getEntries()) {
            for (var icm : this.itemCaps) {
                icm.register(item.get());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerBlockProviders(TileToken token, List<TileCapabilityMap<?, ?, ?>> maps) {
        BlockApiLookup lookup = maps.getFirst().cap();
        lookup.registerForBlockEntity((be, context) -> {
            for (TileCapabilityMap map : maps) {
                var result = map.map().apply(be, context);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }, token.type().get());
    }

    public <T extends Block> DeferredBlock<T> block(String name, Function<BlockBehaviour.Properties, T> builder) {
        return this.block(name, builder, BlockBehaviour.Properties.of(), BlockItem::new, new Item.Properties().useBlockDescriptionPrefix());
    }

    public <T extends Block> DeferredBlock<T> block(String name, Function<BlockBehaviour.Properties, T> builder, BlockBehaviour.Properties properties) {
        return this.block(name, builder, properties, BlockItem::new, new Item.Properties().useBlockDescriptionPrefix());
    }

    public <T extends Block> DeferredBlock<T> block(String name, Function<BlockBehaviour.Properties, T> builder, BlockBehaviour.Properties properties, Item.Properties itemProperties) {
        return this.block(name, builder, properties, BlockItem::new, itemProperties);
    }

    public <T extends Block> DeferredBlock<T> block(String name, Function<BlockBehaviour.Properties, T> builder, BlockBehaviour.Properties properties, BiFunction<Block, Item.Properties, Item> itemWrapper) {
        return this.block(name, builder, properties, itemWrapper, new Item.Properties().useBlockDescriptionPrefix());
    }

    public <T extends Block> DeferredBlock<T> block(String name, Function<BlockBehaviour.Properties, T> builder, BlockBehaviour.Properties properties, BiFunction<Block, Item.Properties, Item> itemWrapper, Item.Properties itemProperties) {
        DeferredBlock<T> block = this.blocks.register(name, key -> builder.apply(properties.setId(ResourceKey.create(Registries.BLOCK, key))));
        this.item(name, prop -> itemWrapper.apply(block.get(), prop), itemProperties);
        return block;
    }

    public <T extends Item> DeferredItem<T> item(String name, Function<Item.Properties, T> builder) {
        return this.item(name, builder, new Item.Properties());
    }

    public DeferredItem<Item> item(String name, Item.Properties properties) {
        return this.item(name, Item::new, properties);
    }

    public <T extends Item> DeferredItem<T> item(String name, Function<Item.Properties, T> builder, Item.Properties properties) {
        return this.items.register(name, key -> builder.apply(properties.setId(ResourceKey.create(Registries.ITEM, key))));
    }

    @SafeVarargs
    public final <T extends BlockEntity> DeferredTileEntityType<T> tile(String name, Class<T> tileClass, BlockEntityType.BlockEntitySupplier<T> factory, DeferredBlock<? extends Block>... blocks) {
        var blockHolders = List.of(blocks);
        // The blocks are only created when the registers are registered, so they are looked up lazily
        var type = this.tiles.registerTile(name, () -> new BlockEntityType<>(factory, blockHolders.stream().map(DeferredBlock::get).collect(Collectors.<Block>toSet())));
        var token = new TileToken(type, tileClass);
        this.tileTypes.add(token);
        this.tileBind.add(Pair.of(token, Set.of()));
        return type;
    }

    public <T> DeferredDataComponentType<T> comp(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return this.components.registerComponent(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    public <T> DeferredDataComponentType<T> comp(String name, Codec<T> codec) {
        return this.comp(name, builder -> builder.persistent(codec));
    }

    public <T> DeferredDataComponentType<T> comp(String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> netCodec) {
        return this.comp(name, builder -> builder.persistent(codec).networkSynchronized(netCodec));
    }

    /**
     * Exposes an API (NeoForge: capability) on all block entities of this mod that implement the given interface.
     */
    public <T, A, C> void cap(Class<T> capInterface, BlockApiLookup<A, C> cap, BiFunction<T, C, A> map) {
        this.tileCaps.add(new TileCapabilityMap<>(capInterface, cap, map));
    }

    /**
     * Exposes an API (NeoForge: capability) on all items of this mod whose class implements the given interface.
     */
    public <T, A, C> void cap(Class<T> capInterface, ItemApiLookup<A, C> cap, BiFunction<ItemStack, C, A> map) {
        this.itemCaps.add(new ItemCapabilityMap<>(capInterface, cap, map));
    }

    public record TileCapabilityMap<T, A, C>(Class<T> capInterface, BlockApiLookup<A, C> cap, BiFunction<T, C, A> map) {
    }

    public record ItemCapabilityMap<T, A, C>(Class<T> capInterface, ItemApiLookup<A, C> cap, BiFunction<ItemStack, C, A> map) {

        public void register(Item item) {
            if (capInterface.isAssignableFrom(item.getClass())) {
                this.cap.registerForItems((stack, context) -> this.map.apply(stack, context), item);
            }
        }

    }

}
