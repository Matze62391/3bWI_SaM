package rearth.oritech.port.neoforge.capabilities;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;

import team.reborn.energy.api.EnergyStorage;

import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.access.ItemAccess;
import rearth.oritech.port.neoforge.transfer.energy.EnergyHandler;
import rearth.oritech.port.neoforge.transfer.fluid.FluidResource;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;

/**
 * The item, fluid and energy capabilities, mapped to Fabric's {@link ItemStorage}, {@link FluidStorage} and Team
 * Reborn Energy's {@link EnergyStorage}.
 */
public final class Capabilities {
    private Capabilities() {}

    public static final class Energy {
        public static final BlockCapability<EnergyHandler, Direction> BLOCK = new BlockCapability<>(
                EnergyStorage.SIDED, FabricAdapters::fromFabric, FabricAdapters::toFabric);
        public static final ItemCapability<EnergyHandler, ItemAccess> ITEM = new ItemCapability<>(
                EnergyStorage.ITEM, FabricAdapters::fromFabric, FabricAdapters::toFabric);

        private Energy() {}
    }

    public static final class Item {
        public static final BlockCapability<ResourceHandler<ItemResource>, Direction> BLOCK = new BlockCapability<>(
                ItemStorage.SIDED, FabricAdapters::fromFabricItems, FabricAdapters::toFabricItems);

        private Item() {}
    }

    public static final class Fluid {
        public static final BlockCapability<ResourceHandler<FluidResource>, Direction> BLOCK = new BlockCapability<>(
                FluidStorage.SIDED, FabricAdapters::fromFabricFluids, FabricAdapters::toFabricFluids);
        public static final ItemCapability<ResourceHandler<FluidResource>, ItemAccess> ITEM = new ItemCapability<>(
                FluidStorage.ITEM, FabricAdapters::fromFabricFluids, FabricAdapters::toFabricFluids);

        private Fluid() {}
    }
}
