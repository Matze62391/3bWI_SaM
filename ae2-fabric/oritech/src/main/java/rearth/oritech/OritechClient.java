package rearth.oritech;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import rearth.oritech.port.fml.ModContainer;
import rearth.oritech.port.fml.config.ModConfig;
import rearth.oritech.port.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.port.neoforge.client.event.RegisterMenuScreensEvent;
import rearth.oritech.port.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import rearth.oritech.port.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.port.neoforge.client.network.ClientPayloadHandlers;
import rearth.oritech.port.neoforge.registries.datamaps.DataMaps;
import rearth.oritech.init.compat.jei.OritechJeiRecipeSync;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.cablesurfer.ActiveCableRenderer;
import rearth.oritech.client.cablesurfer.ClientZiplineHandler;
import rearth.oritech.client.cablesurfer.ZiplineFxHandler;
import rearth.oritech.client.init.*;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.OreFinderRenderer;
import rearth.oritech.client.renderers.PortableTankItemRenderer;
import rearth.oritech.client.ui.AugmentSelectionScreen;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.EndericRailgunItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import rearth.oritech.item.tools.util.ClientTickableItem;


public final class OritechClient implements ClientModInitializer {

    public static final KeyMapping.Category ORITECH_KEYS_CATEGORY = KeyMapping.Category.register(Oritech.id("keys"));
    public static final KeyMapping AUGMENT_SELECTOR = new KeyMapping("key.oritech.augment_screen", InputConstants.KEY_G, ORITECH_KEYS_CATEGORY);

    public static AugmentSelectionScreen activeScreen = null;

    public static boolean laserActive = false;

    @Override
    public void onInitializeClient() {
        Oritech.LOGGER.info("Oritech client initialization");

        new ModContainer(Oritech.MOD_ID).registerConfig(ModConfig.Type.CLIENT, OritechClientConfig.CLIENT_SPEC);

        ClientPayloadHandlers.register();
        ClientPlayNetworking.registerGlobalReceiver(DataMaps.SyncPayload.TYPE, (payload, context) -> DataMaps.receive(payload));
        OritechJeiRecipeSync.register();

        ClientTickEvents.START_CLIENT_TICK.register(this::onPreClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> onPostClientTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onPlayerLoggingIn(client));

        // block outlines, zipline cables and the ore-scanner highlight use the split extraction/submission flow
        LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register(BlockOutlineRenderer::onOutlineExtract);
        LevelRenderEvents.COLLECT_SUBMITS.register(BlockOutlineRenderer::onSubmit);
        LevelExtractionEvents.END_EXTRACTION.register(ActiveCableRenderer::onExtractRenderState);
        LevelRenderEvents.COLLECT_SUBMITS.register(ActiveCableRenderer::onSubmitGeometry);
        LevelExtractionEvents.END_EXTRACTION.register(OreFinderRenderer::onExtractRenderState);
        LevelRenderEvents.COLLECT_SUBMITS.register(OreFinderRenderer::onSubmitGeometry);

        KeyMappingHelper.registerKeyMapping(AUGMENT_SELECTOR);

        // wires the portable tank item renderer into the special-model item pipeline
        SpecialModelRenderers.ID_MAPPER.put(PortableTankItemRenderer.ID, PortableTankItemRenderer.Unbaked.MAP_CODEC);

        ModScreens.registerScreens(new RegisterMenuScreensEvent());
        ModRenderers.registerRenderers(new EntityRenderersEvent.RegisterRenderers());
        ClientGuiRenderers.registerPipRenderers(new RegisterPictureInPictureRenderersEvent());
        FluidModelContent.registerFluidModels();
    }

    private void onPlayerLoggingIn(Minecraft client) {
        if (!OritechClientConfig.showOracleIndexWarning.get() || FabricLoader.getInstance().isModLoaded("oracle_index")) return;
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.translatable("message.oritech.oracle_index_missing"));
        OritechClientConfig.showOracleIndexWarning.set(false);
        OritechClientConfig.CLIENT_SPEC.save();
    }

    private void onPreClientTick(Minecraft client) {
        var player = client.player;
        if (client.player == null || client.level == null) return;

        tickClientItems(client);

        // ensure prometheum pick is animated correctly
        var stack = client.player.getMainHandItem();
        if (stack.getItem() instanceof PromethiumPickaxeItem pickaxeItem) {
            pickaxeItem.onHeldTick(stack, client.player, client.level);
        }

        // laser player use event
        if (player.getMainHandItem().getItem() instanceof EndericRailgunItem && laserActive) {
            ClientPacketDistributor.sendToServer(new EndericRailgunItem.LaserPlayerUsePacket());
        } else {
            laserActive = false;
        }

        // used for augment UI
        if (AUGMENT_SELECTOR.consumeClick() && activeScreen == null) {
            activeScreen = new AugmentSelectionScreen();
            client.gui.setScreen(activeScreen);
        } else if (activeScreen != null && !InputConstants.isKeyDown(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(AUGMENT_SELECTOR).getValue())) {
            activeScreen.onClose();
        }

        for (var augment : PlayerAugments.getAllAugments(player.registryAccess()).values()) {
            if (augment.isEnabled((Player) player))
                augment.refreshClient(player);
        }
    }

    private static void tickClientItems(Minecraft client) {
        var player = client.player;
        var level = client.level;
        var inventory = player.getInventory();
        var selectedSlot = inventory.getSelectedSlot();
        var mainItems = inventory.getNonEquipmentItems();

        for (int i = 0; i < mainItems.size(); i++) {
            var slot = i == selectedSlot ? EquipmentSlot.MAINHAND : null;
            tickClientItem(mainItems.get(i), level, player, slot);
        }

        for (var slot : EquipmentSlot.VALUES) {
            if (slot == EquipmentSlot.MAINHAND) continue;
            tickClientItem(player.getItemBySlot(slot), level, player, slot);
        }
    }

    private static void tickClientItem(ItemStack stack, Level level, Player player, @Nullable EquipmentSlot slot) {
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ClientTickableItem item) {
            item.clientInventoryTick(stack, level, player, slot);
        }
    }

    private void onPostClientTick() {
        ClientZiplineHandler.onClientTick();
        ZiplineFxHandler.tick();
    }

    /** Called from the mouse handler mixin, returns true to swallow the click. */
    public static boolean onMouseButton(Minecraft client, int button, int action) {
        if (client.player != null && client.player.getMainHandItem().getItem() instanceof EndericRailgunItem && button == 0 && client.gui.screen() == null) {
            var wasDown = action == 1;
            laserActive = wasDown; // activate laser on mouse down
            return wasDown;
        }
        return false;
    }
}
