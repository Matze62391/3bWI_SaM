package com.glodblock.github.extendedae.client;

import appeng.api.client.StorageCellModels;
import appeng.client.InitScreens;
import com.glodblock.github.extendedae.ExtendedAE;
import com.glodblock.github.extendedae.client.gui.GuiActiveFormationPlane;
import com.glodblock.github.extendedae.client.gui.GuiAssemblerMatrix;
import com.glodblock.github.extendedae.client.gui.GuiCaner;
import com.glodblock.github.extendedae.client.gui.GuiCircuitCutter;
import com.glodblock.github.extendedae.client.gui.GuiConfigModifier;
import com.glodblock.github.extendedae.client.gui.GuiCrystalAssembler;
import com.glodblock.github.extendedae.client.gui.GuiExDrive;
import com.glodblock.github.extendedae.client.gui.GuiExIOBus;
import com.glodblock.github.extendedae.client.gui.GuiExIOPort;
import com.glodblock.github.extendedae.client.gui.GuiExInterface;
import com.glodblock.github.extendedae.client.gui.GuiExMolecularAssembler;
import com.glodblock.github.extendedae.client.gui.GuiExPatternProvider;
import com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal;
import com.glodblock.github.extendedae.client.gui.GuiIngredientBuffer;
import com.glodblock.github.extendedae.client.gui.GuiModExportBus;
import com.glodblock.github.extendedae.client.gui.GuiModStorageBus;
import com.glodblock.github.extendedae.client.gui.GuiPatternModifier;
import com.glodblock.github.extendedae.client.gui.GuiPreciseExportBus;
import com.glodblock.github.extendedae.client.gui.GuiPreciseStorageBus;
import com.glodblock.github.extendedae.client.gui.GuiRenamer;
import com.glodblock.github.extendedae.client.gui.GuiSmartAnnihilationPlane;
import com.glodblock.github.extendedae.client.gui.GuiTagExportBus;
import com.glodblock.github.extendedae.client.gui.GuiTagStorageBus;
import com.glodblock.github.extendedae.client.gui.GuiThresholdExportBus;
import com.glodblock.github.extendedae.client.gui.GuiThresholdLevelEmitter;
import com.glodblock.github.extendedae.client.gui.GuiVoidCell;
import com.glodblock.github.extendedae.client.gui.GuiWirelessConnector;
import com.glodblock.github.extendedae.client.gui.GuiWirelessHub;
import com.glodblock.github.extendedae.client.gui.pattern.GuiCraftingPattern;
import com.glodblock.github.extendedae.client.gui.pattern.GuiProcessingPattern;
import com.glodblock.github.extendedae.client.gui.pattern.GuiSmithingTablePattern;
import com.glodblock.github.extendedae.client.gui.pattern.GuiStonecuttingPattern;
import com.glodblock.github.extendedae.client.hotkey.EAEHotKey;
import com.glodblock.github.extendedae.client.model.AssemblerGlassModel;
import com.glodblock.github.extendedae.client.model.ExDriveModel;
import com.glodblock.github.extendedae.client.render.tesr.CanerTESR;
import com.glodblock.github.extendedae.client.render.tesr.CircuitCutterTESR;
import com.glodblock.github.extendedae.client.render.tesr.CrystalFixerTESR;
import com.glodblock.github.extendedae.client.render.tesr.ExChargerTESR;
import com.glodblock.github.extendedae.client.render.tesr.ExDriveTESR;
import com.glodblock.github.extendedae.client.render.tesr.ExMolecularAssemblerTESR;
import com.glodblock.github.extendedae.client.render.tesr.IngredientBufferTESR;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.extendedae.container.ContainerActiveFormationPlane;
import com.glodblock.github.extendedae.container.ContainerAssemblerMatrix;
import com.glodblock.github.extendedae.container.ContainerCaner;
import com.glodblock.github.extendedae.container.ContainerCircuitCutter;
import com.glodblock.github.extendedae.container.ContainerConfigModifier;
import com.glodblock.github.extendedae.container.ContainerCrystalAssembler;
import com.glodblock.github.extendedae.container.ContainerExDrive;
import com.glodblock.github.extendedae.container.ContainerExIOBus;
import com.glodblock.github.extendedae.container.ContainerExIOPort;
import com.glodblock.github.extendedae.container.ContainerExInterface;
import com.glodblock.github.extendedae.container.ContainerExMolecularAssembler;
import com.glodblock.github.extendedae.container.ContainerExPatternProvider;
import com.glodblock.github.extendedae.container.ContainerExPatternTerminal;
import com.glodblock.github.extendedae.container.ContainerIngredientBuffer;
import com.glodblock.github.extendedae.container.ContainerModExportBus;
import com.glodblock.github.extendedae.container.ContainerModStorageBus;
import com.glodblock.github.extendedae.container.ContainerPatternModifier;
import com.glodblock.github.extendedae.container.ContainerPreciseExportBus;
import com.glodblock.github.extendedae.container.ContainerPreciseStorageBus;
import com.glodblock.github.extendedae.container.ContainerRenamer;
import com.glodblock.github.extendedae.container.ContainerSmartAnnihilationPlane;
import com.glodblock.github.extendedae.container.ContainerTagExportBus;
import com.glodblock.github.extendedae.container.ContainerTagStorageBus;
import com.glodblock.github.extendedae.container.ContainerThresholdExportBus;
import com.glodblock.github.extendedae.container.ContainerThresholdLevelEmitter;
import com.glodblock.github.extendedae.container.ContainerVoidCell;
import com.glodblock.github.extendedae.container.ContainerWirelessConnector;
import com.glodblock.github.extendedae.container.ContainerWirelessHub;
import com.glodblock.github.extendedae.container.pattern.ContainerCraftingPattern;
import com.glodblock.github.extendedae.container.pattern.ContainerProcessingPattern;
import com.glodblock.github.extendedae.container.pattern.ContainerSmithingTablePattern;
import com.glodblock.github.extendedae.container.pattern.ContainerStonecuttingPattern;
import com.glodblock.github.extendedae.xmod.wt.ContainerWirelessExPAT;
import com.glodblock.github.extendedae.xmod.wt.GuiWirelessExPAT;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClientRegistryHandler {

    public static final ClientRegistryHandler INSTANCE = new ClientRegistryHandler();

    public void init() {
        this.registerGui();
        this.registerModels();
        this.registerTESR();
        this.registerHotKey();
    }

    private void registerGui() {
        InitScreens.register(ContainerExPatternProvider.TYPE, GuiExPatternProvider::new, "/screens/ex_pattern_provider.json");
        InitScreens.register(ContainerExInterface.TYPE, GuiExInterface::new, "/screens/ex_interface.json");
        InitScreens.register(ContainerExIOBus.EXPORT_TYPE, GuiExIOBus::new, "/screens/ex_export_bus.json");
        InitScreens.register(ContainerExIOBus.IMPORT_TYPE, GuiExIOBus::new, "/screens/ex_import_bus.json");
        InitScreens.<ContainerExPatternTerminal, GuiExPatternTerminal<ContainerExPatternTerminal>>register(ContainerExPatternTerminal.TYPE, GuiExPatternTerminal::new, "/screens/ex_pattern_access_terminal.json");
        InitScreens.register(ContainerWirelessConnector.TYPE, GuiWirelessConnector::new, "/screens/wireless_connector.json");
        InitScreens.register(ContainerIngredientBuffer.TYPE, GuiIngredientBuffer::new, "/screens/ingredient_buffer.json");
        InitScreens.register(ContainerExDrive.TYPE, GuiExDrive::new, "/screens/ex_drive.json");
        InitScreens.register(ContainerPatternModifier.TYPE, GuiPatternModifier::new, "/screens/pattern_modifier.json");
        InitScreens.register(ContainerExMolecularAssembler.TYPE, GuiExMolecularAssembler::new, "/screens/ex_molecular_assembler.json");
        InitScreens.register(ContainerTagStorageBus.TYPE, GuiTagStorageBus::new, "/screens/tag_storage_bus.json");
        InitScreens.register(ContainerTagExportBus.TYPE, GuiTagExportBus::new, "/screens/tag_export_bus.json");
        InitScreens.register(ContainerThresholdLevelEmitter.TYPE, GuiThresholdLevelEmitter::new, "/screens/threshold_level_emitter.json");
        InitScreens.register(ContainerRenamer.TYPE, GuiRenamer::new, "/screens/renamer.json");
        InitScreens.register(ContainerModStorageBus.TYPE, GuiModStorageBus::new, "/screens/mod_storage_bus.json");
        InitScreens.register(ContainerModExportBus.TYPE, GuiModExportBus::new, "/screens/mod_export_bus.json");
        InitScreens.register(ContainerActiveFormationPlane.TYPE, GuiActiveFormationPlane::new, "/screens/active_formation_plane.json");
        InitScreens.register(ContainerCaner.TYPE, GuiCaner::new, "/screens/caner.json");
        InitScreens.register(ContainerPreciseExportBus.TYPE, GuiPreciseExportBus::new, "/screens/precise_export_bus.json");
        InitScreens.register(ContainerWirelessExPAT.TYPE, GuiWirelessExPAT::new, "/screens/wireless_ex_pat.json");
        InitScreens.register(ContainerExIOPort.TYPE, GuiExIOPort::new, "/screens/ex_io_port.json");
        InitScreens.register(ContainerPreciseStorageBus.TYPE, GuiPreciseStorageBus::new, "/screens/precise_storage_bus.json");
        InitScreens.register(ContainerThresholdExportBus.TYPE, GuiThresholdExportBus::new, "/screens/threshold_export_bus.json");
        InitScreens.register(ContainerCrystalAssembler.TYPE, GuiCrystalAssembler::new, "/screens/crystal_assembler.json");
        InitScreens.register(ContainerCircuitCutter.TYPE, GuiCircuitCutter::new, "/screens/circuit_cutter.json");
        InitScreens.register(ContainerExInterface.TYPE_OVERSIZE, GuiExInterface::new, "/screens/oversize_interface.json");
        InitScreens.register(ContainerAssemblerMatrix.TYPE, GuiAssemblerMatrix::new, "/screens/assembler_matrix.json");
        InitScreens.register(ContainerVoidCell.TYPE, GuiVoidCell::new, "/screens/void_cell.json");
        InitScreens.register(ContainerConfigModifier.TYPE, GuiConfigModifier::new, "/screens/config_modifier.json");
        InitScreens.register(ContainerWirelessHub.TYPE, GuiWirelessHub::new, "/screens/wireless_hub.json");
        InitScreens.register(ContainerSmartAnnihilationPlane.TYPE, GuiSmartAnnihilationPlane::new, "/screens/smart_annihilation_plane.json");
        MenuScreens.register(ContainerProcessingPattern.TYPE, GuiProcessingPattern::new);
        MenuScreens.register(ContainerCraftingPattern.TYPE, GuiCraftingPattern::new);
        MenuScreens.register(ContainerStonecuttingPattern.TYPE, GuiStonecuttingPattern::new);
        MenuScreens.register(ContainerSmithingTablePattern.TYPE, GuiSmithingTablePattern::new);
    }

    private void registerModels() {
        CustomUnbakedBlockStateModel.register(ExtendedAE.id("ex_drive"), ExDriveModel.CODEC);
        CustomUnbakedBlockStateModel.register(ExtendedAE.id("assembler_matrix_glass"), AssemblerGlassModel.MAP_CODEC);
        StorageCellModels.registerModel(EAESingletons.INFINITY_WATER_CELL, ExtendedAE.id("block/drive/infinity_water_cell"));
        StorageCellModels.registerModel(EAESingletons.INFINITY_COBBLESTONE_CELL, ExtendedAE.id("block/drive/infinity_cobblestone_cell"));
        StorageCellModels.registerModel(EAESingletons.VOID_CELL, ExtendedAE.id("block/drive/void_cell"));
    }

    private void registerTESR() {
        BlockEntityRendererRegistry.register(EAESingletons.INGREDIENT_BUFFER.get().getBlockEntityType(), IngredientBufferTESR::new);
        BlockEntityRendererRegistry.register(EAESingletons.EX_DRIVE.get().getBlockEntityType(), ExDriveTESR::new);
        BlockEntityRendererRegistry.register(EAESingletons.EX_ASSEMBLER.get().getBlockEntityType(), ExMolecularAssemblerTESR::new);
        BlockEntityRendererRegistry.register(EAESingletons.EX_CHARGER.get().getBlockEntityType(), ExChargerTESR::new);
        BlockEntityRendererRegistry.register(EAESingletons.CANER.get().getBlockEntityType(), CanerTESR::new);
        BlockEntityRendererRegistry.register(EAESingletons.CRYSTAL_FIXER.get().getBlockEntityType(), CrystalFixerTESR::new);
        BlockEntityRendererRegistry.register(EAESingletons.CIRCUIT_CUTTER.get().getBlockEntityType(), CircuitCutterTESR::new);
    }

    private void registerHotKey() {
        KeyMappingHelper.registerKeyMapping(EAEHotKey.VIEW_PATTERN);
        KeyMappingHelper.registerKeyMapping(EAEHotKey.SET_AMOUNT);
    }

}
