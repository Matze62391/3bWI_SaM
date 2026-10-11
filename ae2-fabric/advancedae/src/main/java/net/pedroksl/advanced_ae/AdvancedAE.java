package net.pedroksl.advanced_ae;

import com.mojang.logging.LogUtils;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.pedroksl.advanced_ae.common.definitions.*;
import net.pedroksl.advanced_ae.common.parts.AdvPatternProviderPart;
import net.pedroksl.advanced_ae.common.parts.SmallAdvPatternProviderPart;
import net.pedroksl.advanced_ae.common.helpers.AAEPersistentData;
import net.pedroksl.advanced_ae.events.AAEPlayerEvents;
import net.pedroksl.advanced_ae.network.AAENetworkHandler;
import net.pedroksl.advanced_ae.recipes.AAERecipeSerializers;
import net.pedroksl.advanced_ae.recipes.AAERecipeTypes;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe;
import net.pedroksl.ae2addonlib.api.IGridLinkedItem;

import appeng.api.AECapabilities;
import appeng.api.features.GridLinkables;
import net.pedroksl.advanced_ae.xmod.wtlib.AE2wtlibPlugin;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.RegisterPartCapabilitiesEvent;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.powersink.AEBasePoweredBlockEntity;
import appeng.core.AELog;
import appeng.core.definitions.AEItems;
import appeng.helpers.externalstorage.GenericStackFluidHandler;
import appeng.helpers.externalstorage.GenericStackItemHandler;
import appeng.items.tools.powered.powersink.PoweredItemCapabilities;

import team.reborn.energy.api.EnergyStorage;

public abstract class AdvancedAE {
    public static final String MOD_ID = "advanced_ae";

    static AdvancedAE INSTANCE;

    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedAE() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;

        AAEConfig.register();

        // On NeoForge, these registrations are driven by the RegisterEvent of each registry. Fabric registers
        // directly, so registries that are referenced by later registrations have to come first.
        var items = AAEItems.INSTANCE;
        var blocks = AAEBlocks.INSTANCE;
        AAEComponents.INSTANCE.register();
        blocks.register();
        items.register();
        AAEFluids.INSTANCE.register();
        AAEBlockEntities.INSTANCE.register();
        AAEMenus.INSTANCE.register();
        AAECreativeTab.INSTANCE.register();
        AAERecipeTypes.DR.register();
        AAERecipeSerializers.DR.register();

        AAENetworkHandler.INSTANCE.register();
        // Vanilla no longer sends recipes to the client, but the reaction chamber's screen and JEI need them
        RecipeSynchronization.synchronizeRecipeSerializer(ReactionChamberRecipe.SERIALIZER);

        initUpgrades();
        initCapabilities();

        AAEPersistentData.register();
        AAEPlayerEvents.register();

        AAEHotkeysRegistry.INSTANCE.init();

        postRegistrationInitialization();
        AE2wtlibPlugin.registerTerminal();
    }

    public static AdvancedAE instance() {
        return INSTANCE;
    }

    public RecipeMap getRecipeMapForType(Level level, RecipeType<?> recipeType) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.recipeAccess().recipes;
        } else {
            AELog.warn("Don't know how to retrieve recipe information for level type {}", level);
            return RecipeMap.EMPTY;
        }
    }

    public void postRegistrationInitialization() {
        GridLinkables.register(AAEItems.QUANTUM_HELMET, IGridLinkedItem.LINKABLE_HANDLER);
        GridLinkables.register(AAEItems.QUANTUM_CHESTPLATE, IGridLinkedItem.LINKABLE_HANDLER);
        GridLinkables.register(AAEItems.QUANTUM_LEGGINGS, IGridLinkedItem.LINKABLE_HANDLER);
        GridLinkables.register(AAEItems.QUANTUM_BOOTS, IGridLinkedItem.LINKABLE_HANDLER);
        GridLinkables.register(AAEItems.QUANTUM_CRAFTER_WIRELESS_TERMINAL, WirelessTerminalItem.LINKABLE_HANDLER);
    }

    private static void initUpgrades() {
        Upgrades.add(AEItems.SPEED_CARD, AAEBlocks.REACTION_CHAMBER, 4);
        Upgrades.add(AEItems.SPEED_CARD, AAEBlocks.QUANTUM_CRAFTER, 4);
        Upgrades.add(AEItems.REDSTONE_CARD, AAEBlocks.QUANTUM_CRAFTER, 1);
        Upgrades.add(AEItems.SPEED_CARD, AAEItems.STOCK_EXPORT_BUS, 4);
        Upgrades.add(AEItems.CAPACITY_CARD, AAEItems.STOCK_EXPORT_BUS, 5);
        Upgrades.add(AEItems.REDSTONE_CARD, AAEItems.STOCK_EXPORT_BUS, 1);
        Upgrades.add(AEItems.CRAFTING_CARD, AAEItems.STOCK_EXPORT_BUS, 1);
        Upgrades.add(AEItems.FUZZY_CARD, AAEItems.STOCK_EXPORT_BUS, 1);
        Upgrades.add(AEItems.SPEED_CARD, AAEItems.IMPORT_EXPORT_BUS, 4);
        Upgrades.add(AEItems.CAPACITY_CARD, AAEItems.IMPORT_EXPORT_BUS, 5);
        Upgrades.add(AEItems.REDSTONE_CARD, AAEItems.IMPORT_EXPORT_BUS, 1);
        Upgrades.add(AEItems.CRAFTING_CARD, AAEItems.IMPORT_EXPORT_BUS, 1);
        Upgrades.add(AEItems.FUZZY_CARD, AAEItems.IMPORT_EXPORT_BUS, 1);
        Upgrades.add(AEItems.SPEED_CARD, AAEItems.ADVANCED_IO_BUS, 4);
        Upgrades.add(AEItems.CAPACITY_CARD, AAEItems.ADVANCED_IO_BUS, 5);
        Upgrades.add(AEItems.REDSTONE_CARD, AAEItems.ADVANCED_IO_BUS, 1);
        Upgrades.add(AEItems.CRAFTING_CARD, AAEItems.ADVANCED_IO_BUS, 1);
        Upgrades.add(AEItems.FUZZY_CARD, AAEItems.ADVANCED_IO_BUS, 1);
    }

    private static void initCapabilities() {
        for (var type : AAEBlockEntities.INSTANCE.getImplementorsOf(IInWorldGridNodeHost.class)) {
            AECapabilities.IN_WORLD_GRID_NODE_HOST.registerForBlockEntity(
                    (be, context) -> (IInWorldGridNodeHost) be, type);
        }
        for (var type : AAEItems.INSTANCE.getItems()) {
            if (type.get() instanceof IAEItemPowerStorage powerStorage) {
                EnergyStorage.ITEM.registerForItems(
                        (object, context) -> new PoweredItemCapabilities(context, type.asItem(), powerStorage),
                        type);
            }
        }

        // Pattern providers expose their return inventory, like AE2's
        var advPatternProvider = AAEBlockEntities.ADV_PATTERN_PROVIDER.get();
        AECapabilities.GENERIC_INTERNAL_INV.registerForBlockEntity(
                (be, context) -> be.getLogic().getReturnInv(), advPatternProvider);
        ItemStorage.SIDED.registerForBlockEntity(
                (be, context) -> new GenericStackItemHandler(be.getLogic().getReturnInv()), advPatternProvider);
        FluidStorage.SIDED.registerForBlockEntity(
                (be, context) -> new GenericStackFluidHandler(be.getLogic().getReturnInv()), advPatternProvider);

        var smallAdvPatternProvider = AAEBlockEntities.SMALL_ADV_PATTERN_PROVIDER.get();
        AECapabilities.GENERIC_INTERNAL_INV.registerForBlockEntity(
                (be, context) -> be.getLogic().getReturnInv(), smallAdvPatternProvider);
        ItemStorage.SIDED.registerForBlockEntity(
                (be, context) -> new GenericStackItemHandler(be.getLogic().getReturnInv()), smallAdvPatternProvider);
        FluidStorage.SIDED.registerForBlockEntity(
                (be, context) -> new GenericStackFluidHandler(be.getLogic().getReturnInv()),
                smallAdvPatternProvider);

        // The reaction chamber takes items through its item handler and fluids through its tank
        var reactionChamber = AAEBlockEntities.REACTION_CHAMBER.get();
        ItemStorage.SIDED.registerForBlockEntity(AEBaseInvBlockEntity::getExposedItemHandler, reactionChamber);
        AECapabilities.GENERIC_INTERNAL_INV.registerForBlockEntity((be, context) -> be.getTank(), reactionChamber);
        FluidStorage.SIDED.registerForBlockEntity(
                (be, context) -> new GenericStackFluidHandler(be.getTank()), reactionChamber);
        EnergyStorage.SIDED.registerForBlockEntity(AEBasePoweredBlockEntity::getEnergyStorage, reactionChamber);
    }

    /**
     * Registers the capabilities of Advanced AE's parts on AE2's cable bus. Called by AE2 through the
     * {@code ae2:part_capabilities} entrypoint.
     */
    public static void registerPartCapabilities(RegisterPartCapabilitiesEvent event) {
        event.register(
                AECapabilities.GENERIC_INTERNAL_INV,
                (part, context) -> part.getLogic().getReturnInv(),
                AdvPatternProviderPart.class);
        event.register(
                AECapabilities.GENERIC_INTERNAL_INV,
                (part, context) -> part.getLogic().getReturnInv(),
                SmallAdvPatternProviderPart.class);
    }

    public static Identifier makeId(String id) {
        return Identifier.fromNamespaceAndPath(MOD_ID, id);
    }

    @Nullable
    public abstract Level getClientLevel();

    public abstract void registerHotkey(String id);
}
