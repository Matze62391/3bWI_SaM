package appeng.core.registries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Minimal Fabric replacement for NeoForge's DeferredRegister. Entries are collected when they are declared and
 * registered in declaration order when {@link #register()} is called during mod initialization.
 */
public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final List<Entry<T, ?>> entries = new ArrayList<>();
    private final List<DeferredHolder<T, ?>> holders = new ArrayList<>();
    private boolean registered;

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    public static DataComponents createDataComponents(ResourceKey<? extends Registry<net.minecraft.core.component.DataComponentType<?>>> registryKey,
            String namespace) {
        return new DataComponents(namespace);
    }

    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return registryKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> factory) {
        return register(name, id -> factory.get());
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Function<Identifier, ? extends I> factory) {
        var key = ResourceKey.create(registryKey, Identifier.fromNamespaceAndPath(namespace, name));
        return addEntry(key, factory, new DeferredHolder<>(key));
    }

    protected <I extends T, H extends DeferredHolder<T, I>> H addEntry(ResourceKey<T> key,
            Function<Identifier, ? extends I> factory, H holder) {
        if (registered) {
            throw new IllegalStateException("Cannot add entries to " + registryKey + " after registration");
        }
        entries.add(new Entry<>(key, factory, holder));
        holders.add(holder);
        return holder;
    }

    public Collection<DeferredHolder<T, ?>> getEntries() {
        return Collections.unmodifiableList(holders);
    }

    /**
     * Registers all collected entries with the vanilla registry.
     */
    public void register() {
        if (registered) {
            throw new IllegalStateException("Already registered: " + registryKey);
        }
        registered = true;
        Registry<T> registry = DeferredHolder.lookupRegistry(registryKey);
        for (var entry : entries) {
            entry.register(registry);
        }
    }

    private record Entry<T, I extends T>(ResourceKey<T> key, Function<Identifier, ? extends I> factory,
            DeferredHolder<T, ?> holder) {
        void register(Registry<T> registry) {
            var reference = Registry.registerForHolder(registry, key, factory.apply(key.identifier()));
            holder.bind(reference);
        }
    }

    public static class Items extends DeferredRegister<Item> {
        protected Items(String namespace) {
            super(Registries.ITEM, namespace);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> factory) {
            return register(name, id -> factory.get());
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Function<Identifier, ? extends I> factory) {
            var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(getNamespace(), name));
            return addEntry(key, factory, new DeferredItem<>(key));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name,
                Function<Item.Properties, ? extends I> factory) {
            return registerItem(name, factory, Item.Properties::new);
        }

        public <I extends Item> DeferredItem<I> registerItem(String name,
                Function<Item.Properties, ? extends I> factory, Supplier<Item.Properties> properties) {
            return register(name, id -> factory.apply(properties.get().setId(ResourceKey.create(Registries.ITEM, id))));
        }
    }

    public static class Blocks extends DeferredRegister<Block> {
        protected Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> factory) {
            return register(name, id -> factory.get());
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Function<Identifier, ? extends B> factory) {
            var key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(getNamespace(), name));
            return addEntry(key, factory, new DeferredBlock<>(key));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name,
                Function<BlockBehaviour.Properties, ? extends B> factory) {
            return registerBlock(name, factory, BlockBehaviour.Properties::of);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name,
                Function<BlockBehaviour.Properties, ? extends B> factory,
                Supplier<BlockBehaviour.Properties> properties) {
            return register(name,
                    id -> factory.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, id))));
        }
    }

    public static class DataComponents
            extends DeferredRegister<net.minecraft.core.component.DataComponentType<?>> {
        protected DataComponents(String namespace) {
            super(Registries.DATA_COMPONENT_TYPE, namespace);
        }
    }
}
