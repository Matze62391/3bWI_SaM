package appeng.api.parts;

import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class RegisterPartCapabilitiesEventInternal {
    private RegisterPartCapabilitiesEventInternal() {
    }

    /**
     * Receives the block entity API providers that forward lookups to the parts of part hosts.
     */
    public interface ProviderSink {
        <T, C> void register(BlockApiLookup<T, C> lookup, BlockEntityType<?> hostType,
                BiFunction<BlockEntity, C, @Nullable T> provider);
    }

    public static void register(RegisterPartCapabilitiesEvent partEvent, ProviderSink sink) {
        for (var registration : partEvent.capabilityRegistrations.values()) {
            register(partEvent, sink, registration);
        }
    }

    private static <T, C> void register(RegisterPartCapabilitiesEvent partEvent, ProviderSink sink,
            RegisterPartCapabilitiesEvent.BlockCapabilityRegistration<T, C> registration) {
        for (var hostType : partEvent.hostTypes) {
            sink.register(registration.capability(), hostType,
                    (blockEntity, context) -> registration.find((IPartHost) blockEntity, context));
        }
    }
}
