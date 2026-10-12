package rearth.oritech.port.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import rearth.oritech.port.neoforge.fluids.FluidType;

public final class NeoForgeRegistries {
    private NeoForgeRegistries() {}

    public static final class Keys {
        /** Not a real registry on Fabric, {@link DeferredRegister} keeps these entries to itself. */
        public static final ResourceKey<Registry<FluidType>> FLUID_TYPES = ResourceKey
                .createRegistryKey(Identifier.fromNamespaceAndPath("neoforge", "fluid_type"));

        private Keys() {}
    }
}
