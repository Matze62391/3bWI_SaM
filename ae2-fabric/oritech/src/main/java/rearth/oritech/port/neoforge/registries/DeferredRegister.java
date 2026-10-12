package rearth.oritech.port.neoforge.registries;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import rearth.oritech.port.bus.IEventBus;

/**
 * Collects registry entries and registers them all when {@link #register(IEventBus)} is called. On Fabric this
 * happens directly during mod initialization, so the caller decides the order between registries (blocks before
 * items, etc).
 */
public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private boolean registered;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new DeferredRegister<>(registry.key(), namespace);
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace) {
        return new DeferredRegister<>(key, namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    public static DataComponents createDataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey,
            String namespace) {
        return new DataComponents(registryKey, namespace);
    }

    public static Entities createEntities(String namespace) {
        return new Entities(namespace);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
        return register(name, id -> sup.get());
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<Identifier, ? extends I> func) {
        return registerEntry(name, func, this::createHolder);
    }

    protected <I extends T, H extends DeferredHolder<T, I>> H registerEntry(String name,
            Function<Identifier, ? extends I> func, Function<ResourceKey<T>, H> holderFactory) {
        if (registered) {
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after register()");
        }
        var id = Identifier.fromNamespaceAndPath(namespace, name);
        var key = ResourceKey.create(registryKey, id);
        var holder = holderFactory.apply(key);
        if (entries.putIfAbsent(holder, () -> func.apply(id)) != null) {
            throw new IllegalArgumentException("Duplicate registration " + name);
        }
        return holder;
    }

    @SuppressWarnings("unchecked")
    protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<T> key) {
        return (DeferredHolder<T, I>) DeferredHolder.create(key);
    }

    public Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public TagKey<T> createTagKey(String path) {
        return TagKey.create(registryKey, id(path));
    }

    public TagKey<T> createTagKey(Identifier location) {
        return TagKey.create(registryKey, location);
    }

    /**
     * Registers all entries. Entries of registries that only exist on NeoForge are kept in their holders.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void register(IEventBus bus) {
        if (registered) {
            throw new IllegalStateException("Cannot register DeferredRegister to more than one event bus.");
        }
        registered = true;
        var registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue((ResourceKey) registryKey);
        for (var entry : entries.entrySet()) {
            var holder = (DeferredHolder<T, T>) entry.getKey();
            T value = entry.getValue().get();
            if (registry != null) {
                Registry.register(registry, holder.getKey(), value);
            } else {
                holder.direct = value;
            }
        }
    }

    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return registryKey;
    }

    public Identifier getRegistryName() {
        return registryKey.identifier();
    }

    public String getNamespace() {
        return namespace;
    }

    public static class Blocks extends DeferredRegister<Block> {
        protected Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Function<Identifier, ? extends B> func) {
            return (DeferredBlock<B>) registerEntry(name, func, DeferredBlock::new);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> sup) {
            return register(name, id -> sup.get());
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name,
                Function<BlockBehaviour.Properties, ? extends B> func, Supplier<BlockBehaviour.Properties> properties) {
            return register(name, id -> func.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, id))));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name,
                Function<BlockBehaviour.Properties, ? extends B> func, UnaryOperator<BlockBehaviour.Properties> properties) {
            return registerBlock(name, func, () -> properties.apply(BlockBehaviour.Properties.of()));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name,
                Function<BlockBehaviour.Properties, ? extends B> func) {
            return registerBlock(name, func, BlockBehaviour.Properties::of);
        }

        public DeferredBlock<Block> registerSimpleBlock(String name, Supplier<BlockBehaviour.Properties> properties) {
            return registerBlock(name, Block::new, properties);
        }

        public DeferredBlock<Block> registerSimpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
            return registerBlock(name, Block::new, properties);
        }

        public DeferredBlock<Block> registerSimpleBlock(String name) {
            return registerBlock(name, Block::new);
        }
    }

    public static class Items extends DeferredRegister<Item> {
        protected Items(String namespace) {
            super(Registries.ITEM, namespace);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <I extends Item> DeferredItem<I> register(String name, Function<Identifier, ? extends I> func) {
            return (DeferredItem<I>) registerEntry(name, func, DeferredItem::new);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> sup) {
            return register(name, id -> sup.get());
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func,
                Supplier<Item.Properties> properties) {
            return register(name, id -> func.apply(properties.get().setId(ResourceKey.create(Registries.ITEM, id))));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func,
                UnaryOperator<Item.Properties> properties) {
            return registerItem(name, func, () -> properties.apply(new Item.Properties()));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func) {
            return registerItem(name, func, Item.Properties::new);
        }

        public DeferredItem<Item> registerSimpleItem(String name, Supplier<Item.Properties> properties) {
            return registerItem(name, Item::new, properties);
        }

        public DeferredItem<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> properties) {
            return registerItem(name, Item::new, properties);
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return registerItem(name, Item::new);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block,
                UnaryOperator<Item.Properties> properties) {
            return registerItem(name, props -> new BlockItem(block.get(), props.useBlockDescriptionPrefix()),
                    properties);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) {
            return registerSimpleBlockItem(name, block, UnaryOperator.identity());
        }
    }

    public static class DataComponents extends DeferredRegister<DataComponentType<?>> {
        protected DataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String namespace) {
            super(registryKey, namespace);
        }

        public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(String name,
                UnaryOperator<DataComponentType.Builder<D>> builder) {
            return register(name, () -> builder.apply(DataComponentType.builder()).build());
        }
    }

    public static class Entities extends DeferredRegister<EntityType<?>> {
        protected Entities(String namespace) {
            super(Registries.ENTITY_TYPE, namespace);
        }
    }
}
