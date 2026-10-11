package gripe._90.megacells;

import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.AECapabilities;
import appeng.api.features.HotkeyAction;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.RegisterPartCapabilitiesEvent;
import appeng.api.storage.MEStorage;
import appeng.helpers.externalstorage.GenericStackFluidHandler;
import appeng.helpers.externalstorage.GenericStackItemHandler;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.hotkeys.HotkeyActions;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;

import gripe._90.megacells.definition.MEGABlockEntities;
import gripe._90.megacells.definition.MEGABlocks;
import gripe._90.megacells.definition.MEGAComponents;
import gripe._90.megacells.definition.MEGACreativeTab;
import gripe._90.megacells.definition.MEGAItems;
import gripe._90.megacells.definition.MEGAMenus;
import gripe._90.megacells.integration.Addons;
import gripe._90.megacells.item.cell.BulkCellItem;
import gripe._90.megacells.item.part.CellDockPart;
import gripe._90.megacells.item.part.MEGAInterfacePart;
import gripe._90.megacells.item.part.MEGAPatternProviderPart;
import gripe._90.megacells.misc.CompressionService;
import gripe._90.megacells.misc.SyncCompressionChainsPacket;

/**
 * Fabric entrypoint ({@code ae2:addon}, runs after AE2 has registered its content).
 */
public class MEGACells implements ModInitializer {
    public static final String MODID = "megacells";

    @Override
    public void onInitialize() {
        MEGAComponents.DR.register();
        MEGABlocks.DR.register();
        MEGAItems.DR.register();
        MEGABlockEntities.DR.register();
        MEGAMenus.DR.register();
        MEGACreativeTab.register();

        CompressionService.init();
        PayloadTypeRegistry.clientboundPlay()
                .register(SyncCompressionChainsPacket.TYPE, SyncCompressionChainsPacket.STREAM_CODEC);

        initUpgrades();
        initStorageCells();
        initCapabilities();
    }

    public static Identifier makeId(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static void initUpgrades() {
        {
            var storageCellGroup = GuiText.StorageCells.getTranslationKey();
            var portableCellGroup = GuiText.PortableCells.getTranslationKey();
            var interfaceGroup = GuiText.Interface.getTranslationKey();
            var wirelessTerminalGroup = GuiText.WirelessTerminals.getTranslationKey();

            for (var cell : MEGAItems.getTieredCells()) {
                if (!(cell.keyType().equals("item") || cell.keyType().equals("fluid"))) {
                    continue;
                }

                Upgrades.add(AEItems.INVERTER_CARD, cell.item(), 1, storageCellGroup);
                Upgrades.add(AEItems.EQUAL_DISTRIBUTION_CARD, cell.item(), 1, storageCellGroup);
                Upgrades.add(AEItems.VOID_CARD, cell.item(), 1, storageCellGroup);

                if (cell.keyType().equals("item")) {
                    Upgrades.add(AEItems.FUZZY_CARD, cell.item(), 1, storageCellGroup);
                }

                if (cell.portable()) {
                    Upgrades.add(MEGAItems.GREATER_ENERGY_CARD, cell.item(), 2, portableCellGroup);
                }
            }

            Upgrades.add(AEItems.CRAFTING_CARD, MEGABlocks.MEGA_INTERFACE, 1, interfaceGroup);
            Upgrades.add(AEItems.CRAFTING_CARD, MEGAItems.MEGA_INTERFACE, 1, interfaceGroup);
            Upgrades.add(AEItems.FUZZY_CARD, MEGABlocks.MEGA_INTERFACE, 1, interfaceGroup);
            Upgrades.add(AEItems.FUZZY_CARD, MEGAItems.MEGA_INTERFACE, 1, interfaceGroup);

            Upgrades.add(MEGAItems.GREATER_ENERGY_CARD, AEItems.WIRELESS_TERMINAL, 2, wirelessTerminalGroup);
            Upgrades.add(MEGAItems.GREATER_ENERGY_CARD, AEItems.WIRELESS_CRAFTING_TERMINAL, 2, wirelessTerminalGroup);

            Upgrades.add(MEGAItems.GREATER_ENERGY_CARD, AEItems.COLOR_APPLICATOR, 2);
            Upgrades.add(MEGAItems.GREATER_ENERGY_CARD, AEItems.MATTER_CANNON, 2);

            Upgrades.add(MEGAItems.COMPRESSION_CARD, MEGAItems.BULK_ITEM_CELL, 1);

            for (var portableCell : List.of(
                    AEItems.PORTABLE_ITEM_CELL1K,
                    AEItems.PORTABLE_ITEM_CELL4K,
                    AEItems.PORTABLE_ITEM_CELL16K,
                    AEItems.PORTABLE_ITEM_CELL64K,
                    AEItems.PORTABLE_ITEM_CELL256K,
                    AEItems.PORTABLE_FLUID_CELL1K,
                    AEItems.PORTABLE_FLUID_CELL4K,
                    AEItems.PORTABLE_FLUID_CELL16K,
                    AEItems.PORTABLE_FLUID_CELL64K,
                    AEItems.PORTABLE_FLUID_CELL256K)) {
                Upgrades.add(MEGAItems.GREATER_ENERGY_CARD, portableCell, 2, portableCellGroup);
            }

            for (var addon : Addons.values()) {
                if (addon.isLoaded()) {
                    addon.getHelper().initUpgrades();
                }
            }
        }
    }

    private static void initStorageCells() {
        {
            BulkCellItem.registerHandler();

            for (var cell : MEGAItems.getTieredCells()) {
                if (cell.item().asItem() instanceof AbstractPortableCell portable) {
                    HotkeyActions.register(
                            portable,
                            portable::openFromInventory,
                            cell.keyType().equals("item")
                                    ? HotkeyAction.PORTABLE_ITEM_CELL
                                    : HotkeyAction.PORTABLE_FLUID_CELL);
                }
            }
        }
    }

    private static void initCapabilities() {
        for (var type : MEGABlockEntities.DR.getEntries()) {
            AECapabilities.IN_WORLD_GRID_NODE_HOST.registerForBlockEntity(
                    (be, _) -> (IInWorldGridNodeHost) be, (BlockEntityType<?>) type.get());
        }

        registerGenericInv(MEGABlockEntities.MEGA_INTERFACE.get(), be -> be.getInterfaceLogic().getStorage());
        AECapabilities.ME_STORAGE.registerForBlockEntity(
                (be, _) -> be.getInterfaceLogic().getInventory(), MEGABlockEntities.MEGA_INTERFACE.get());

        registerGenericInv(MEGABlockEntities.MEGA_PATTERN_PROVIDER.get(), be -> be.getLogic().getReturnInv());

        for (var cell : MEGAItems.getTieredCells()) {
            if (cell.portable() && cell.item().asItem() instanceof IAEItemPowerStorage powered) {
                EnergyStorage.ITEM.registerForItems(
                        (_, context) -> new PoweredItemCapabilities(context, cell.item().asItem(), powered),
                        cell.item());
            }
        }
    }

    /**
     * Exposes AE2's generic inventory of a block entity, and adapts it to Fabric's item and fluid storage like AE2
     * does for its own interfaces and pattern providers.
     */
    private static <T extends BlockEntity> void registerGenericInv(
            BlockEntityType<T> type, java.util.function.Function<T, GenericInternalInventory> getter) {
        AECapabilities.GENERIC_INTERNAL_INV.registerForBlockEntity((be, _) -> getter.apply(be), type);
        ItemStorage.SIDED.registerForBlockEntity((be, _) -> new GenericStackItemHandler(getter.apply(be)), type);
        FluidStorage.SIDED.registerForBlockEntity((be, _) -> new GenericStackFluidHandler(getter.apply(be)), type);
    }

    /**
     * {@code ae2:part_capabilities} entrypoint.
     */
    public static void registerPartCapabilities(RegisterPartCapabilitiesEvent event) {
        event.register(
                AECapabilities.GENERIC_INTERNAL_INV,
                (part, _) -> part.getInterfaceLogic().getStorage(),
                MEGAInterfacePart.class);
        event.register(
                AECapabilities.ME_STORAGE,
                (part, _) -> part.getInterfaceLogic().getInventory(),
                MEGAInterfacePart.class);

        event.register(
                AECapabilities.GENERIC_INTERNAL_INV,
                (part, _) -> part.getLogic().getReturnInv(),
                MEGAPatternProviderPart.class);

        event.register(ItemStorage.SIDED, (part, _) -> part.getCellInventory().toStorage(), CellDockPart.class);
    }
}
