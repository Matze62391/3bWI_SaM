package net.pedroksl.advanced_ae.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.pedroksl.ae2addonlib.client.RegisterClientPayloadHandlersEvent;
import net.pedroksl.advanced_ae.AdvancedAE;
import net.pedroksl.advanced_ae.client.gui.*;
import net.pedroksl.advanced_ae.client.item.QuantumArmorItemModel;
import net.pedroksl.advanced_ae.client.renderer.QuantumComputerModel;
import net.pedroksl.advanced_ae.client.renderer.ReactionChamberRenderer;
import net.pedroksl.advanced_ae.client.renderer.ThroughputMonitorRenderer;
import net.pedroksl.advanced_ae.common.definitions.AAEBlockEntities;
import net.pedroksl.advanced_ae.common.definitions.AAEFluids;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.parts.ThroughputMonitorPart;
import net.pedroksl.advanced_ae.gui.AdvancedIOBusMenu;
import net.pedroksl.advanced_ae.gui.QuantumCrafterTermMenu;
import net.pedroksl.advanced_ae.gui.StockExportBusMenu;
import net.pedroksl.ae2addonlib.client.render.WaterBasedFluidModel;
import net.pedroksl.ae2addonlib.util.WaterBasedFluidType;

import appeng.client.InitScreens;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;
import appeng.core.AELog;

/**
 * Client side of Advanced AE. Created by {@link net.pedroksl.advanced_ae.AdvancedAEFabricEntrypoint} on the client.
 */
public class AAEClient extends AdvancedAE {

    private static AAEClient INSTANCE;

    // Recipes synchronized from the server
    private RecipeMap recipeMap = RecipeMap.EMPTY;
    private final Set<RecipeType<?>> knownRecipeTypes = Collections.newSetFromMap(new IdentityHashMap<>());

    public AAEClient() {
        super();

        INSTANCE = this;

        AAEClientPlayerEvents.register();

        initScreens();
        initRenderers();
        initFluidModels();
        registerItemModels();
        registerBlockStateModels();
        AAEHotkeys.INSTANCE.finalizeRegistration();

        new AAEClientNetworkHandler().registerPackets(new RegisterClientPayloadHandlersEvent());

        RegisterPartRendererEvent.EVENT.register(this::registerPartRenderers);

        ClientTickEvents.END_CLIENT_TICK.register(client -> AAEHotkeys.INSTANCE.checkHotkeys());

        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> receiveRecipes(recipes));
    }

    public RecipeMap getRecipeMapForType(Level level, RecipeType<?> recipeType) {
        if (level instanceof ClientLevel) {
            if (!knownRecipeTypes.contains(recipeType)) {
                AELog.warn("Haven't received recipes of type {} from server yet.", recipeType);
                return RecipeMap.EMPTY;
            }

            return recipeMap;
        }

        return super.getRecipeMapForType(level, recipeType);
    }

    private static void initScreens() {
        InitScreens.register(
                AAEMenus.QUANTUM_COMPUTER.get(), QuantumComputerScreen::new, "/screens/quantum_computer.json");

        InitScreens.register(
                AAEMenus.ADV_PATTERN_PROVIDER.get(),
                AdvPatternProviderScreen::new,
                "/screens/adv_pattern_provider.json");
        InitScreens.register(
                AAEMenus.SMALL_ADV_PATTERN_PROVIDER.get(),
                SmallAdvPatternProviderScreen::new,
                "/screens/small_adv_pattern_provider.json");
        InitScreens.register(
                AAEMenus.ADV_PATTERN_ENCODER.get(),
                AdvPatternEncoderScreen::new,
                "/screens/adv_pattern_encoder.json");
        InitScreens.register(
                AAEMenus.REACTION_CHAMBER.get(), ReactionChamberScreen::new, "/screens/reaction_chamber.json");
        InitScreens.register(
                AAEMenus.QUANTUM_CRAFTER.get(), QuantumCrafterScreen::new, "/screens/quantum_crafter.json");
        InitScreens.<QuantumCrafterTermMenu, QuantumCrafterTermScreen<QuantumCrafterTermMenu>>register(
                AAEMenus.QUANTUM_CRAFTER_TERMINAL.get(),
                QuantumCrafterTermScreen::new,
                "/screens/quantum_crafter_terminal.json");

        InitScreens.<StockExportBusMenu, StockExportBusScreen<StockExportBusMenu>>register(
                AAEMenus.STOCK_EXPORT_BUS.get(), StockExportBusScreen::new, "/screens/stock_export_bus.json");
        InitScreens.register(
                AAEMenus.IMPORT_EXPORT_BUS.get(), ImportExportBusScreen::new, "/screens/import_export_bus.json");
        InitScreens.<AdvancedIOBusMenu, StockExportBusScreen<AdvancedIOBusMenu>>register(
                AAEMenus.ADVANCED_IO_BUS.get(), AdvancedIOBusScreen::new, "/screens/advanced_io_bus.json");

        InitScreens.register(
                AAEMenus.CRAFTER_PATTERN_CONFIG.get(),
                QuantumCrafterConfigPatternScreen::new,
                "/screens/quantum_crafter_pattern_config.json");

        InitScreens.register(
                AAEMenus.QUANTUM_ARMOR_CONFIG.get(),
                QuantumArmorConfigScreen::new,
                "/screens/quantum_armor_config.json");
        InitScreens.register(
                AAEMenus.QUANTUM_ARMOR_NUM_INPUT.get(),
                QuantumArmorNumInputConfigScreen::new,
                "/screens/quantum_armor_num_input_config.json");
        InitScreens.register(
                AAEMenus.QUANTUM_ARMOR_FILTER_CONFIG.get(),
                QuantumArmorFilterConfigScreen::new,
                "/screens/quantum_armor_filter_config.json");
        InitScreens.register(
                AAEMenus.QUANTUM_ARMOR_MAGNET.get(),
                QuantumArmorMagnetScreen::new,
                "/screens/quantum_armor_magnet.json");
        InitScreens.register(
                AAEMenus.QUANTUM_ARMOR_STYLE_CONFIG.get(),
                QuantumArmorStyleConfigScreen::new,
                "/screens/quantum_armor_style.json");
        InitScreens.register(
                AAEMenus.PORTABLE_WORKBENCH.get(),
                PortableWorkbenchScreen::new,
                "/screens/portable_workbench.json");
    }

    private void registerPartRenderers(RegisterPartRendererEvent event) {
        event.register(ThroughputMonitorPart.class, new ThroughputMonitorRenderer());
    }

    private static void initRenderers() {
        BlockEntityRendererRegistry.register(AAEBlockEntities.REACTION_CHAMBER.get(), ReactionChamberRenderer::new);
    }

    private static void registerBlockStateModels() {
        CustomUnbakedBlockStateModel.register(QuantumComputerModel.Unbaked.ID, QuantumComputerModel.Unbaked.MAP_CODEC);
    }

    private static void registerItemModels() {
        ItemModels.ID_MAPPER.put(QuantumArmorItemModel.Unbaked.ID, QuantumArmorItemModel.Unbaked.MAP_CODEC);
    }

    private static void initFluidModels() {
        for (var fluid : AAEFluids.INSTANCE.getFluids()) {
            if (fluid.fluidType() instanceof WaterBasedFluidType type) {
                FluidRenderingRegistry.register(fluid.source(), fluid.flowing(), new WaterBasedFluidModel<>(type).get());
            }
        }
    }

    public static AAEClient instance() {
        return INSTANCE;
    }

    @Override
    @Nullable
    public Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public void registerHotkey(String id) {
        AAEHotkeys.INSTANCE.registerHotkey(id);
    }

    private void receiveRecipes(SynchronizedRecipes recipes) {
        var byType = ImmutableMultimap.<RecipeType<?>, RecipeHolder<?>>builder();
        var byKey = ImmutableMap.<ResourceKey<Recipe<?>>, RecipeHolder<?>>builder();
        knownRecipeTypes.clear();
        for (var recipe : recipes.recipes()) {
            byType.put(recipe.value().getType(), recipe);
            byKey.put(recipe.id(), recipe);
            knownRecipeTypes.add(recipe.value().getType());
        }
        recipeMap = new RecipeMap(byType.build(), byKey.buildKeepingLast());
    }
}
