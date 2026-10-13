package guideme.internal;

import guideme.Guide;
import guideme.PageAnchor;
import guideme.color.LightDarkMode;
import guideme.internal.command.GuideClientCommand;
import guideme.internal.command.StructureCommands;
import guideme.internal.hotkey.OpenGuideHotkey;
import guideme.internal.item.GuideItemDispatchUnbaked;
import guideme.internal.network.OpenGuideRequest;
import guideme.internal.scene.ScenePictureInPictureRenderer;
import guideme.internal.screen.GlobalInMemoryHistory;
import guideme.internal.screen.GuideNavigation;
import guideme.internal.search.GuideSearch;
import guideme.internal.siteexport.SiteExportOnStartup;
import guideme.internal.siteexport.TextureDownloader;
import guideme.internal.util.Blitter;
import guideme.navigation.NavigationNode;
import guideme.render.GuiAssets;
import guideme.scene.FluidModelFeatureRenderer;
import guideme.scene.GuidebookLevelRenderer;
import guideme.scene.annotation.InWorldAnnotationRenderer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuideMEClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(GuideMEClient.class);

    public static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.register(GuideME.makeId("category"));

    private static GuideMEClient INSTANCE;

    public static final Identifier GUIDE_CLICK_ID = GuideME.makeId("guide.click");
    public static SoundEvent GUIDE_CLICK_EVENT = SoundEvent.createVariableRangeEvent(GUIDE_CLICK_ID);

    private final GuideSearch search = new GuideSearch();

    private final ClientConfig clientConfig = new ClientConfig();

    private RecipeMap recipeMap = RecipeMap.EMPTY;
    private Set<RecipeType<?>> availableRecipeTypes = Set.of();

    public GuideMEClient() {
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        GuideME.PROXY = new GuideMEClientProxy();

        clientConfig.load(FabricLoader.getInstance().getConfigDir().resolve("guideme.json"));

        Registry.register(BuiltInRegistries.SOUND_EVENT, GUIDE_CLICK_ID, GUIDE_CLICK_EVENT);
        KeyMappingHelper.registerKeyMapping(OpenGuideHotkey.getHotkey());
        ItemModels.ID_MAPPER.put(GuideItemDispatchUnbaked.ID, GuideItemDispatchUnbaked.CODEC);
        PictureInPictureRendererRegistry.register(context -> new ScenePictureInPictureRenderer());
        FeatureRendererRegistry.register(FluidModelFeatureRenderer.TYPE, FluidModelFeatureRenderer::new);

        ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> GuideClientCommand.register(dispatcher));
        // These are meant for command blocks only usable in single player
        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> StructureCommands.register(dispatcher));

        OpenGuideHotkey.init();

        var resourceLoader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        resourceLoader.registerReloadListener(GuideReloadListener.ID, new GuideReloadListener());
        // GUI sprites have to be looked up again whenever the GUI atlas was rebuilt
        var resetSpritesId = GuideME.makeId("reset_gui_sprites");
        resourceLoader.registerReloadListener(resetSpritesId,
                (ResourceManagerReloadListener) resourceManager -> GuiAssets.resetSprites());
        resourceLoader.addListenerOrdering(ResourceReloaderKeys.Client.ATLAS, resetSpritesId);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            search.processWork();
            processDevWatchers();
        });

        GuideOnStartup.init();
        SiteExportOnStartup.init();

        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> onReceiveRecipes(recipes));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> invalidateNavigationIcons());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onPlayerDisconnect());

        ClientPlayNetworking.registerGlobalReceiver(OpenGuideRequest.TYPE, (payload, context) -> {
            var anchor = payload.pageAnchor().orElse(null);
            if (anchor != null) {
                GuideMEProxy.instance().openGuide(context.player(), payload.guideId(), anchor);
            } else {
                GuideMEProxy.instance().openGuide(context.player(), payload.guideId());
            }
        });
    }

    private void onReceiveRecipes(SynchronizedRecipes recipes) {
        var byType = ImmutableMultimap.<RecipeType<?>, RecipeHolder<?>>builder();
        var byKey = ImmutableMap.<ResourceKey<Recipe<?>>, RecipeHolder<?>>builder();
        var types = new HashSet<RecipeType<?>>();
        for (var recipe : recipes.recipes()) {
            byType.put(recipe.value().getType(), recipe);
            byKey.put(recipe.id(), recipe);
            types.add(recipe.value().getType());
        }
        recipeMap = new RecipeMap(byType.build(), byKey.buildKeepingLast());
        availableRecipeTypes = Set.copyOf(types);
    }

    private void onPlayerDisconnect() {
        recipeMap = RecipeMap.EMPTY;
        availableRecipeTypes = Set.of();
        invalidateNavigationIcons();
        GuidebookLevelRenderer.getInstance().clearCache();
    }

    /**
     * Navigation icons are item stacks, which can only be created while the dynamic registries are available (i.e.
     * while connected to a world). Drop the cached icons whenever that changes.
     */
    private static void invalidateNavigationIcons() {
        for (var guide : GuideRegistry.getAll()) {
            invalidateNavigationIcons(guide.getNavigationTree().getRootNodes());
        }
    }

    private static void invalidateNavigationIcons(List<NavigationNode> nodes) {
        for (var node : nodes) {
            var iconFactory = node.iconFactory();
            if (iconFactory != null) {
                iconFactory.invalidate();
            }
            invalidateNavigationIcons(node.children());
        }
    }

    private void processDevWatchers() {
        for (var guide : GuideRegistry.getAll()) {
            guide.tick();
        }
    }

    public static LightDarkMode currentLightDarkMode() {
        return LightDarkMode.LIGHT_MODE;
    }

    public static GuideMEClient instance() {
        return Objects.requireNonNull(INSTANCE, "Mod is not initialized");
    }

    public boolean isShowDebugGuiOverlays() {
        return clientConfig.showDebugGuiOverlays.getAsBoolean();
    }

    public boolean isAdaptiveScalingEnabled() {
        return clientConfig.adaptiveScaling.getAsBoolean();
    }

    public boolean isIgnoreTranslatedGuides() {
        return clientConfig.ignoreTranslatedGuides.getAsBoolean();
    }

    public boolean isHideMissingRecipeErrors() {
        return clientConfig.hideMissingRecipeErrors.getAsBoolean();
    }

    public boolean isFullWidthLayout() {
        return clientConfig.fullWidthLayout.getAsBoolean();
    }

    public void setFullWidthLayout(boolean fullWidth) {
        if (fullWidth != isFullWidthLayout()) {
            clientConfig.fullWidthLayout.set(fullWidth);
            clientConfig.save();
            var minecraft = Minecraft.getInstance();
            var screen = minecraft.gui.screen();
            if (screen != null) {
                var window = minecraft.getWindow();
                screen.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }
    }

    public static boolean openGuideAtPreviousPage(Guide guide, Identifier initialPage) {
        try {
            var history = GlobalInMemoryHistory.get(guide);
            var historyPage = history.current();
            if (historyPage.isPresent()) {
                GuideNavigation.navigateTo(guide, historyPage.get());
            } else {
                GuideNavigation.navigateTo(guide, PageAnchor.page(initialPage));
            }
            return true;
        } catch (Exception e) {
            LOG.error("Failed to open guide.", e);
            return false;
        }
    }

    public static boolean openGuideAtAnchor(Guide guide, PageAnchor anchor) {
        try {
            GuideNavigation.navigateTo(guide, anchor);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to open guide at {}.", anchor, e);
            return false;
        }
    }

    public GuideSearch getSearch() {
        return search;
    }

    public RecipeMap getRecipeMap() {
        return recipeMap;
    }

    public boolean isRecipeTypeAvailable(RecipeType<?> recipeType) {
        return availableRecipeTypes.contains(recipeType);
    }

    /**
     * The client config of GuideME, stored as JSON. NeoForge's version uses NeoForge's config system.
     */
    private static class ClientConfig {
        private final Map<String, BooleanOption> options = new LinkedHashMap<>();
        // Never load translated guide pages for your current language.
        final BooleanOption ignoreTranslatedGuides = define("ignoreTranslatedGuides", false);
        // Never show errors in guides when recipes can't be found (i.e. because they were hidden by a datapack).
        final BooleanOption hideMissingRecipeErrors = define("hideMissingRecipeErrors", false);
        // Adapt GUI scaling for the Guide screen to fix Minecraft font issues at GUI scale 1 and 3.
        final BooleanOption adaptiveScaling = define("adaptiveScaling", true);
        // Use the full width of the screen for the guide when it is opened.
        final BooleanOption fullWidthLayout = define("fullWidthLayout", true);
        // Show debugging overlays in GUI on mouse-over.
        final BooleanOption showDebugGuiOverlays = define("showDebugGuiOverlays", false);

        @Nullable
        private Path file;

        private BooleanOption define(String name, boolean defaultValue) {
            var option = new BooleanOption(defaultValue);
            options.put(name, option);
            return option;
        }

        void load(Path file) {
            this.file = file;
            if (Files.exists(file)) {
                try {
                    var json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                    if (json.isJsonObject()) {
                        for (var entry : options.entrySet()) {
                            var value = json.getAsJsonObject().get(entry.getKey());
                            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                                entry.getValue().set(value.getAsBoolean());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to read GuideME config {}, using defaults", file, e);
                }
            }
            save();
        }

        void save() {
            if (file == null) {
                return;
            }
            var json = new JsonObject();
            for (var entry : options.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue().getAsBoolean());
            }
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error("Failed to write GuideME config {}", file, e);
            }
        }
    }

    private static final class BooleanOption implements BooleanSupplier {
        private boolean value;

        BooleanOption(boolean defaultValue) {
            this.value = defaultValue;
        }

        @Override
        public boolean getAsBoolean() {
            return value;
        }

        void set(boolean value) {
            this.value = value;
        }
    }
}
