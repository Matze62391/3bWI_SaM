package rearth.oritech;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import rearth.oritech.init.compat.jei.OritechJeiRecipeServerSync;
import rearth.oritech.port.fml.ModContainer;
import rearth.oritech.port.fml.config.ModConfig;
import rearth.oritech.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import rearth.oritech.port.neoforge.common.extensions.IBlockEntityExtension;
import rearth.oritech.port.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.port.neoforge.registries.datamaps.DataMaps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import org.slf4j.Logger;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.behavior.EndericLaserBlockBehavior;
import rearth.oritech.block.behavior.EndericLaserEntityBehavior;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.item.ItemPipeBlock;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.interaction.EnergyTransmissionPoleEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.*;
import rearth.oritech.init.datamap.DataMapContent;
import rearth.oritech.init.datapack.AugmentContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.init.world.FeatureContent;
import rearth.oritech.item.tools.ElectricMaceItem;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import rearth.oritech.item.tools.harvesting.PromethiumAxeItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import rearth.oritech.util.ServerZiplineHandler;

public final class Oritech implements ModInitializer {

    public static final String MOD_ID = "oritech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {

        LOGGER.info("Hello from Oritech!");

        var modContainer = new ModContainer(MOD_ID);
        modContainer.registerConfig(ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.STARTUP, OritechStartupConfig.STARTUP_SPEC);

        // codecs for reflective builders
        NetworkManager.loadDefaultCodecs();

        // registrations, in the order NeoForge fires its registry events
        SoundContent.SOUND_EVENTS.register(null);
        ComponentContent.COMPONENTS.register(null);
        FluidContent.FLUID_TYPES.register(null);
        FluidContent.FLUIDS.register(null);
        BlockContent.BLOCKS.register(null);
        FluidContent.FLUID_BLOCKS.register(null);
        ItemContent.ITEMS.register(null);
        BlockContent.AddBlockItems();
        BlockContent.BLOCK_ITEMS.register(null);
        ToolsContent.EQUIPMENT.register(null);
        FluidContent.BUCKET_ITEMS.register(null);
        BlockEntitiesContent.BLOCK_ENTITY_TYPES.register(null);
        EntitiesContent.ENTITY_TYPES.register(null);
        ModScreens.MENUS.register(null);
        FeatureContent.FEATURES.register(null);
        LootContent.LOOT_FUNCTIONS.register(null);
        RecipeContent.RECIPE_TYPES.register(null);
        RecipeContent.RECIPE_SERIALIZERS.register(null);
        ItemGroups.TABS.register(null);
        AttachmentContent.init();

        AugmentContent.registerDataPackRegistries();
        DataMapContent.init();
        DataMaps.init(MOD_ID);

        var capabilities = new RegisterCapabilitiesEvent();
        BlockEntitiesContent.registerBlockEntityCapabilities(capabilities);
        ToolsContent.registerItemCapabilities(capabilities);
        ToolsContent.modifyDefaultComponents();

        // networking
        var registrar = new PayloadRegistrar("1");
        NetworkManager.initClientBound(registrar);
        NetworkManager.initServerBound(registrar);
        OritechJeiRecipeServerSync.register();

        // runtime events
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            AcceleratorParticleLogic.onTickEnd();
            for (var player : server.getPlayerList().getPlayers()) {
                onPlayerTickPost(player);
            }
        });
        ServerTickEvents.END_LEVEL_TICK.register(this::onLevelTickPost);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                PromethiumPickaxeItem.preMine(serverLevel, pos, state, serverPlayer);
            }
            return true;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !ExoArmorItem.CancelFallDamage(source, entity));
        ServerEntityEvents.EQUIPMENT_CHANGE.register(ArmorEventHandler::processEvent);
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
            if (!(blockEntity instanceof IBlockEntityExtension extension)) return;
            // NeoForge calls onLoad once the chunk is ready. Fabric fires this event while the chunk is still loading,
            // and reading that chunk from here (multiblock rescans, setChanged) waits for it forever. Queue onLoad as a
            // server task instead; schedule() never runs it inline, unlike execute() on the server thread.
            var server = level.getServer();
            server.schedule(server.wrapRunnable(() -> {
                if (!blockEntity.isRemoved() && blockEntity.getLevel() == level) extension.onLoad();
            }));
        });

        // world generation
        if (OritechStartupConfig.generateOresFabricOnly.get()) {
            addFeature(BiomeTags.IS_OVERWORLD, "oil_spring", GenerationStep.Decoration.LAKES);
            addFeature(TagKey.create(Registries.BIOME, Identifier.withDefaultNamespace("has_structure/desert_pyramid")), "oil_spring_desert", GenerationStep.Decoration.LAKES);
            addFeature(BiomeTags.IS_OVERWORLD, "ore_nickel", GenerationStep.Decoration.UNDERGROUND_ORES);
            addFeature(BiomeTags.IS_OVERWORLD, "ore_platinum", GenerationStep.Decoration.UNDERGROUND_ORES);
            // Only the vanilla End biomes: Fabric appends the ore to each biome's list, and mods with their own End
            // biomes (Better End) list their ores before it there, but add them after it in the vanilla biomes. The
            // two orders contradict each other, and Minecraft then crashes generating End chunks ("Feature order
            // cycle found").
            addFeature(BiomeSelectors.tag(BiomeTags.IS_END).and(context ->
                            context.getBiomeKey().identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)),
                    "ore_platinum_end", GenerationStep.Decoration.UNDERGROUND_ORES);
            addFeature(BiomeTags.IS_OVERWORLD, "uranium_patch", GenerationStep.Decoration.UNDERGROUND_ORES);
            addFeature(BiomeTags.IS_OVERWORLD, "resource_node_common", GenerationStep.Decoration.TOP_LAYER_MODIFICATION);
            addFeature(BiomeTags.IS_OVERWORLD, "resource_node_other", GenerationStep.Decoration.TOP_LAYER_MODIFICATION);
            addFeature(BiomeTags.IS_OVERWORLD, "resource_node_rare", GenerationStep.Decoration.TOP_LAYER_MODIFICATION);
        }

        // compostables (a NeoForge data map upstream)
        var compostables = java.util.Map.of(
                "banana", net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_MEDIUM,
                "biomass", net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_LOW,
                "biomass_block", net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE,
                "packed_wheat", net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE);
        net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents.MODIFY.register(context -> compostables.forEach((name, chance) ->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id(name)).ifPresent(item -> context.modify(item, components ->
                        components.set(net.minecraft.core.component.DataComponents.COMPOSTABLE, new net.minecraft.world.item.component.Compostable(chance))))));

        // former common setup
        EndericLaserBlockBehavior.registerDefaults();
        EndericLaserEntityBehavior.registerDefaults();
        BlockContent.registerDispenserBehaviors();

        LOGGER.info("All events registered");
    }

    private static void addFeature(TagKey<Biome> biomes, String feature, GenerationStep.Decoration step) {
        addFeature(BiomeSelectors.tag(biomes), feature, step);
    }

    private static void addFeature(java.util.function.Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> biomes,
            String feature, GenerationStep.Decoration step) {
        BiomeModifications.addFeature(biomes, step, ResourceKey.create(Registries.PLACED_FEATURE, id(feature)));
    }

    private void onLevelTickPost(ServerLevel serverLevel) {
        ElectricMaceItem.processLightningEvents(serverLevel);
        PromethiumAxeItem.onTick(serverLevel);
    }

    private void onPlayerTickPost(ServerPlayer serverPlayer) {
        ServerZiplineHandler.onPlayerTick(serverPlayer);
        PlayerAugments.serverTickAugments(serverPlayer);
    }

    private void onServerStarted(net.minecraft.server.MinecraftServer server) {
        // load pipe data to memory
        server.getAllLevels().forEach(this::loadLevelPipeData);
    }

    private void loadLevelPipeData(ServerLevel level) {
        var dimId = level.dimension().identifier();
        var result = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.ENERGY_TYPE);
        EnergyPipeBlock.ENERGY_PIPE_DATA.put(dimId, result);

        var fluidResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.FLUID_TYPE);
        FluidPipeBlock.FLUID_PIPE_DATA.put(dimId, fluidResult);

        var itemResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.ITEM_TYPE);
        ItemPipeBlock.ITEM_PIPE_DATA.put(dimId, itemResult);

        var superConductorResult = level.getDataStorage().computeIfAbsent(GenericPipeInterfaceEntity.PipeNetworkData.SUPERCONDUCTOR_TYPE);
        SuperConductorBlock.SUPERCONDUCTOR_DATA.put(dimId, superConductorResult);

        var energyTransmissionPoleResult = level.getDataStorage().computeIfAbsent(EnergyTransmissionPoleEntity.PoleNetworkData.TYPE);
        EnergyTransmissionPoleEntity.POLE_NETWORK_DATA.put(dimId, energyTransmissionPoleResult);
    }
}
