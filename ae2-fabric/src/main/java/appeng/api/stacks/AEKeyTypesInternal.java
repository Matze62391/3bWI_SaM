package appeng.api.stacks;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;

/**
 * Manages the registry used to synchronize key spaces to the client.
 */
@ApiStatus.Internal
public final class AEKeyTypesInternal {
    @Nullable
    private static Registry<AEKeyType> registry;

    @Nullable
    private static Set<AEKeyType> allTypes;

    private AEKeyTypesInternal() {
    }

    public static Registry<AEKeyType> getRegistry() {
        Preconditions.checkState(registry != null, "AE2 isn't initialized yet.");
        return registry;
    }

    public static void setRegistry(Registry<AEKeyType> registry) {
        Preconditions.checkState(AEKeyTypesInternal.registry == null);
        AEKeyTypesInternal.registry = registry;
        // Recompute the set of all types whenever a type is added
        RegistryEntryAddedCallback.event(registry).register((rawId, id, object) -> allTypes = null);
    }

    public static Set<AEKeyType> getAllTypes() {
        Preconditions.checkState(registry != null, "AE2 isn't initialized yet.");
        var result = allTypes;
        if (result == null) {
            var types = new HashSet<AEKeyType>();
            for (var aeKeyType : registry) {
                types.add(aeKeyType);
            }
            allTypes = result = Set.copyOf(types);
        }
        return result;
    }

    public static void register(AEKeyType keyType) {
        Registry.register(getRegistry(), keyType.getId(), keyType);
    }
}
