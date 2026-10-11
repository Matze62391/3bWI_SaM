package gripe._90.megacells.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;

import appeng.api.client.StorageCellModels;
import appeng.client.InitScreens;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;
import appeng.client.gui.implementations.InterfaceScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.renderer.blockentity.CraftingMonitorRenderer;
import appeng.menu.implementations.InterfaceMenu;
import appeng.menu.implementations.PatternProviderMenu;

import gripe._90.megacells.MEGACells;
import gripe._90.megacells.client.render.CellDockPartRenderer;
import gripe._90.megacells.client.render.MEGACraftingUnitModelProvider;
import gripe._90.megacells.client.render.PortableCellWorkbenchClientTooltipComponent;
import gripe._90.megacells.client.screen.CellDockScreen;
import gripe._90.megacells.client.screen.PortableCellWorkbenchScreen;
import gripe._90.megacells.definition.MEGABlockEntities;
import gripe._90.megacells.definition.MEGAItems;
import gripe._90.megacells.definition.MEGAMenus;
import gripe._90.megacells.definition.MEGATranslations;
import gripe._90.megacells.item.cell.PortableCellWorkbenchTooltipComponent;
import gripe._90.megacells.item.part.CellDockPart;
import gripe._90.megacells.misc.CompressionService;
import gripe._90.megacells.misc.SyncCompressionChainsPacket;

public class MEGACellsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        initScreens();
        registerBlockStateModels();
        initBlockEntityRenderers();
        RegisterPartRendererEvent.EVENT.register(MEGACellsClient::initPartRenderers);
        initStorageCellModels();
        initTooltipComponents();
        initResourcePack();

        ClientPlayNetworking.registerGlobalReceiver(
                SyncCompressionChainsPacket.TYPE,
                (packet, context) -> context.client().execute(() -> CompressionService.setClientChains(packet)));
    }

    private static void initPartRenderers(RegisterPartRendererEvent event) {
        event.register(CellDockPart.class, new CellDockPartRenderer());
    }

    private static void registerBlockStateModels() {
        CustomUnbakedBlockStateModel.register(
                MEGACraftingUnitModelProvider.Unbaked.ID, MEGACraftingUnitModelProvider.Unbaked.MAP_CODEC);
    }

    private static void initScreens() {
        InitScreens.register(
                MEGAMenus.MEGA_INTERFACE.get(),
                InterfaceScreen<InterfaceMenu>::new,
                "/screens/megacells/mega_interface.json");
        InitScreens.register(
                MEGAMenus.MEGA_PATTERN_PROVIDER.get(),
                PatternProviderScreen<PatternProviderMenu>::new,
                "/screens/megacells/mega_pattern_provider.json");
        InitScreens.register(MEGAMenus.CELL_DOCK.get(), CellDockScreen::new, "/screens/megacells/cell_dock.json");
        InitScreens.register(
                MEGAMenus.PORTABLE_CELL_WORKBENCH.get(),
                PortableCellWorkbenchScreen::new,
                "/screens/megacells/portable_cell_workbench.json");
    }

    private static void initBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(MEGABlockEntities.MEGA_CRAFTING_MONITOR.get(), CraftingMonitorRenderer::new);
    }

    private static void initStorageCellModels() {
        var modelPrefix = "block/drive/cells/";

        for (var cell : MEGAItems.getTieredCells()) {
            StorageCellModels.registerModel(
                    cell.item(),
                    MEGACells.makeId(modelPrefix + cell.tier().namePrefix() + "_" + cell.keyType() + "_cell"));
        }

        StorageCellModels.registerModel(
                MEGAItems.BULK_ITEM_CELL, MEGACells.makeId(modelPrefix + MEGAItems.BULK_ITEM_CELL.id().getPath()));

        StorageCellModels.registerModel(
                MEGAItems.RADIOACTIVE_CHEMICAL_CELL,
                MEGACells.makeId(modelPrefix + MEGAItems.RADIOACTIVE_CHEMICAL_CELL.id().getPath()));
    }

    private static void initTooltipComponents() {
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof PortableCellWorkbenchTooltipComponent component
                ? new PortableCellWorkbenchClientTooltipComponent(component)
                : null);
    }

    private static void initResourcePack() {
        // Optional resource pack with the classic cell colours (src/main/resources/resourcepacks/optional_cell_colours)
        FabricLoader.getInstance().getModContainer(MEGACells.MODID).ifPresent(container -> ResourceLoader.registerBuiltinPack(
                MEGACells.makeId("optional_cell_colours"),
                container,
                MEGATranslations.ClassicCellColours.text(),
                PackActivationType.NORMAL));
    }
}
