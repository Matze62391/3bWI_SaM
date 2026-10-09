package appeng.init;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.RegisterPartCapabilitiesEvent;
import appeng.api.parts.RegisterPartCapabilitiesEventInternal;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.blockentity.misc.GrowthAcceleratorBlockEntity;
import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.blockentity.powersink.AEBasePoweredBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.helpers.externalstorage.GenericStackFluidHandler;
import appeng.helpers.externalstorage.GenericStackItemHandler;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;
import appeng.parts.crafting.PatternProviderPart;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.parts.misc.InterfacePart;
import appeng.parts.networking.EnergyAcceptorPart;
import appeng.parts.p2p.FEP2PTunnelPart;
import appeng.parts.p2p.FluidP2PTunnelPart;
import appeng.parts.p2p.ItemP2PTunnelPart;

public final class InitCapabilityProviders {

    private InitCapabilityProviders() {
    }

    /**
     * Registers all block and item API providers. Must be called after all blocks and block entity types have been
     * registered.
     */
    public static void register() {
        var event = new BlockApiRegistrar();

        var partEvent = new RegisterPartCapabilitiesEvent();
        partEvent.addHostType(AEBlockEntities.CABLE_BUS.get());
        registerPartCapabilities(partEvent);
        RegisterPartCapabilitiesEvent.EVENT.invoker().register(partEvent);
        RegisterPartCapabilitiesEventInternal.register(partEvent, new RegisterPartCapabilitiesEventInternal.ProviderSink() {
            @Override
            public <T, C> void register(BlockApiLookup<T, C> lookup, BlockEntityType<?> hostType,
                    BiFunction<BlockEntity, C, T> provider) {
                event.registerBlockEntityUnchecked(lookup, hostType, provider);
            }
        });

        initInterface(event);
        initPatternProvider(event);
        initCondenser(event);
        initMEChest(event);
        initMisc(event);
        initPoweredItem(event);
        initCrankable(event);

        for (var type : AEBlockEntities.getSubclassesOf(AEBaseInvBlockEntity.class)) {
            event.registerBlockEntity(ItemStorage.SIDED, type,
                    AEBaseInvBlockEntity::getExposedItemHandler);
        }
        for (var type : AEBlockEntities.getSubclassesOf(AEBasePoweredBlockEntity.class)) {
            event.registerBlockEntity(EnergyStorage.SIDED, type,
                    AEBasePoweredBlockEntity::getEnergyStorage);
        }
        for (var type : AEBlockEntities.getImplementorsOf(IInWorldGridNodeHost.class)) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type,
                    (object, context) -> (IInWorldGridNodeHost) object);
        }

        // Adapters need to be registered last, since they check which blocks expose generic inventories
        registerGenericAdapters(event);

        event.registerAll();
    }

    /**
     * Registers adapters from AE2's generic inventories to Fabric item and fluid storages.
     */
    private static void registerGenericAdapters(BlockApiRegistrar event) {

        for (var block : BuiltInRegistries.BLOCK) {
            if (event.isBlockRegistered(AECapabilities.GENERIC_INTERNAL_INV, block)) {
                registerGenericInvAdapter(event, block, ItemStorage.SIDED, GenericStackItemHandler::new);
                registerGenericInvAdapter(event, block, FluidStorage.SIDED, GenericStackFluidHandler::new);
            }
        }

    }

    private static <T> void registerGenericInvAdapter(BlockApiRegistrar event,
            Block block,
            BlockApiLookup<T, Direction> capability,
            Function<GenericInternalInventory, T> adapter) {
        event.registerBlock(
                capability,
                (level, pos, state, blockEntity, context) -> {
                    var genericInv = AECapabilities.GENERIC_INTERNAL_INV.find(level, pos, state,
                            blockEntity, context);
                    if (genericInv != null) {
                        return adapter.apply(genericInv);
                    }
                    return null;
                },
                block);
    }

    private static void initInterface(BlockApiRegistrar event) {
        event.registerBlockEntity(
                AECapabilities.GENERIC_INTERNAL_INV,
                AEBlockEntities.INTERFACE.get(),
                (be, context) -> be.getInterfaceLogic().getStorage());

        event.registerBlockEntity(
                AECapabilities.ME_STORAGE,
                AEBlockEntities.INTERFACE.get(),
                (blockEntity, context) -> {
                    return blockEntity.getInterfaceLogic().getInventory();
                });
    }

    private static void initPatternProvider(BlockApiRegistrar event) {
        event.registerBlockEntity(
                AECapabilities.GENERIC_INTERNAL_INV,
                AEBlockEntities.PATTERN_PROVIDER.get(),
                (blockEntity, context) -> blockEntity.getLogic().getReturnInv());
    }

    private static void initCondenser(BlockApiRegistrar event) {
        // Condenser will always return its external inventory, even when context is null
        // (unlike the base class it derives from)
        event.registerBlockEntity(ItemStorage.SIDED, AEBlockEntities.CONDENSER.get(),
                (blockEntity, context) -> {
                    return blockEntity.getExternalInv().toStorage();
                });
        event.registerBlockEntity(FluidStorage.SIDED, AEBlockEntities.CONDENSER.get(),
                ((blockEntity, context) -> {
                    return blockEntity.getFluidHandler();
                }));
        event.registerBlockEntity(AECapabilities.ME_STORAGE, AEBlockEntities.CONDENSER.get(),
                (blockEntity, context) -> {
                    return blockEntity.getMEStorage();
                });
    }

    private static void initMEChest(BlockApiRegistrar event) {
        event.registerBlockEntity(FluidStorage.SIDED, AEBlockEntities.ME_CHEST.get(),
                MEChestBlockEntity::getFluidHandler);
        event.registerBlockEntity(AECapabilities.ME_STORAGE, AEBlockEntities.ME_CHEST.get(),
                MEChestBlockEntity::getMEStorage);
    }

    private static void initMisc(BlockApiRegistrar event) {
        event.registerBlockEntity(
                AECapabilities.CRAFTING_MACHINE,
                AEBlockEntities.MOLECULAR_ASSEMBLER.get(),
                (object, context) -> object);
        event.registerBlockEntity(
                ItemStorage.SIDED,
                AEBlockEntities.DEBUG_ITEM_GEN.get(),
                (object, context) -> object.getItemHandler());
        event.registerBlockEntity(
                EnergyStorage.SIDED,
                AEBlockEntities.DEBUG_ENERGY_GEN.get(),
                (object, context) -> object);
        event.registerBlockEntity(
                FluidStorage.SIDED,
                AEBlockEntities.SKY_STONE_TANK.get(),
                (object, context) -> object.getFluidHandler());
    }

    private static void initPoweredItem(BlockApiRegistrar event) {
        registerPowerStorageItem(event, AEItems.ENTROPY_MANIPULATOR);
        registerPowerStorageItem(event, AEItems.CHARGED_STAFF);
        registerPowerStorageItem(event, AEItems.COLOR_APPLICATOR);
        registerPowerStorageItem(event, AEItems.PORTABLE_ITEM_CELL1K);
        registerPowerStorageItem(event, AEItems.PORTABLE_ITEM_CELL4K);
        registerPowerStorageItem(event, AEItems.PORTABLE_ITEM_CELL16K);
        registerPowerStorageItem(event, AEItems.PORTABLE_ITEM_CELL64K);
        registerPowerStorageItem(event, AEItems.PORTABLE_ITEM_CELL256K);
        registerPowerStorageItem(event, AEItems.PORTABLE_FLUID_CELL1K);
        registerPowerStorageItem(event, AEItems.PORTABLE_FLUID_CELL4K);
        registerPowerStorageItem(event, AEItems.PORTABLE_FLUID_CELL16K);
        registerPowerStorageItem(event, AEItems.PORTABLE_FLUID_CELL64K);
        registerPowerStorageItem(event, AEItems.PORTABLE_FLUID_CELL256K);
        registerPowerStorageItem(event, AEItems.MATTER_CANNON);
        registerPowerStorageItem(event, AEItems.WIRELESS_TERMINAL);
        registerPowerStorageItem(event, AEItems.WIRELESS_CRAFTING_TERMINAL);
    }

    private static <T extends Item & IAEItemPowerStorage> void registerPowerStorageItem(BlockApiRegistrar event,
            ItemDefinition<T> definition) {
        IAEItemPowerStorage powerStorage = definition.get();

        EnergyStorage.ITEM.registerForItems(
                (object, context) -> new PoweredItemCapabilities(context, definition.asItem(), powerStorage),
                definition);
    }

    private static void initCrankable(BlockApiRegistrar event) {
        event.registerBlockEntity(AECapabilities.CRANKABLE, AEBlockEntities.CHARGER.get(),
                ChargerBlockEntity::getCrankable);
        event.registerBlockEntity(AECapabilities.CRANKABLE, AEBlockEntities.INSCRIBER.get(),
                InscriberBlockEntity::getCrankable);
        event.registerBlockEntity(AECapabilities.CRANKABLE, AEBlockEntities.GROWTH_ACCELERATOR.get(),
                GrowthAcceleratorBlockEntity::getCrankable);
    }

    private static void registerPartCapabilities(RegisterPartCapabilitiesEvent event) {
        event.register(ItemStorage.SIDED,
                (part, direction) -> part.getLogic().getBlankPatternInv().toStorage(),
                PatternEncodingTerminalPart.class);
        event.register(AECapabilities.GENERIC_INTERNAL_INV, (part, context) -> part.getLogic().getReturnInv(),
                PatternProviderPart.class);
        event.register(AECapabilities.GENERIC_INTERNAL_INV,
                (part, context) -> part.getInterfaceLogic().getStorage(),
                InterfacePart.class);
        event.register(AECapabilities.ME_STORAGE,
                (part, context) -> part.getInterfaceLogic().getInventory(), InterfacePart.class);

        event.register(ItemStorage.SIDED, (part, context) -> part.getExposedApi(),
                ItemP2PTunnelPart.class);
        event.register(EnergyStorage.SIDED, (part, context) -> part.getExposedApi(),
                FEP2PTunnelPart.class);
        event.register(FluidStorage.SIDED, (part, context) -> part.getExposedApi(),
                FluidP2PTunnelPart.class);

        event.register(EnergyStorage.SIDED, (part, context) -> part.getEnergyStorage(),
                EnergyAcceptorPart.class);
    }

}
