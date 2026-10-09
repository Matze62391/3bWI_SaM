package appeng.api.parts;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Allows registering which APIs (block API lookups) parts expose through their host. Addons can listen to
 * {@link #EVENT} to register APIs for their own parts.
 */
public class RegisterPartCapabilitiesEvent {

    /**
     * Fired once during AE2's initialization to collect API registrations for parts.
     */
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class,
            listeners -> event -> {
                for (var listener : listeners) {
                    listener.register(event);
                }
            });

    @FunctionalInterface
    public interface Listener {
        void register(RegisterPartCapabilitiesEvent event);
    }

    /**
     * Provides an API instance for a part.
     */
    @FunctionalInterface
    public interface PartApiProvider<P, C, T> {
        @Nullable
        T getCapability(P part, C context);
    }

    final Set<BlockEntityType<? extends IPartHost>> hostTypes = new HashSet<>();

    final Map<BlockApiLookup<?, ?>, Function<?, Direction>> contextMappers = new HashMap<>();

    final Map<BlockApiLookup<?, ?>, BlockCapabilityRegistration<?, ?>> capabilityRegistrations = new HashMap<>();

    record BlockCapabilityRegistration<T, C>(
            BlockApiLookup<T, C> capability,
            Function<C, Direction> contextToSide,
            Map<Class<? extends IPart>, PartApiProvider<?, C, T>> parts) {
        public BlockCapabilityRegistration(BlockApiLookup<T, C> capability, Function<C, Direction> contextToSide) {
            this(capability, contextToSide, new HashMap<>());
        }

        <P extends IPart> void add(Class<P> partClass, PartApiProvider<P, C, T> provider) {
            if (parts.putIfAbsent(partClass, provider) != null) {
                throw new IllegalStateException("Cannot register an additional capability provider for part "
                        + partClass + " since there already is one for capability " + capability);
            }
        }

        @Nullable
        public T find(IPartHost partHost, C context) {
            // Get side from context
            var side = contextToSide.apply(context);
            if (side == null) {
                return null;
            }
            var part = partHost.getPart(side);
            if (part != null) {
                return handlePart(part, context);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        private <P extends IPart> T handlePart(P part, C context) {
            var partProvider = (PartApiProvider<P, C, T>) parts.get(part.getClass());
            if (partProvider != null) {
                return partProvider.getCapability(part, context);
            }
            return null;
        }
    }

    /**
     * When using APIs with a context other than {@link Direction}, you need to register a mapping function for AE2 to
     * get the side from the context. It cannot determine which part on a part host should handle the API otherwise.
     */
    public <T, C> void registerContext(BlockApiLookup<T, C> capability, Function<C, Direction> directionGetter) {
        contextMappers.put(capability, directionGetter);
    }

    /**
     * Expose an API for a part class.
     * <p>
     * If the context of the lookup is not {@link Direction}, you need to register a mapping function for your custom
     * context! That must be done before this function is called.
     */
    @SuppressWarnings("unchecked")
    public <T, C, P extends IPart> void register(BlockApiLookup<T, C> capability,
            PartApiProvider<P, C, T> provider,
            Class<P> partClass) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(partClass, "partClass");
        Objects.requireNonNull(provider, "provider");

        if (partClass.isInterface() || Modifier.isAbstract(partClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "Capabilities can only be registered for concrete part classes: " + partClass.getCanonicalName());
        }

        var mapper = (Function<C, Direction>) contextMappers.getOrDefault(capability, c -> (Direction) c);

        var registrations = (BlockCapabilityRegistration<T, C>) capabilityRegistrations
                .computeIfAbsent(capability, ignored -> new BlockCapabilityRegistration<>(capability, mapper));
        registrations.add(partClass, provider);
    }

    /**
     * Adds a new type of block entity that will participate in forwarding API lookups to its attached parts.
     */
    public <T extends BlockEntity & IPartHost> void addHostType(BlockEntityType<T> hostType) {
        hostTypes.add(hostType);
    }
}
