/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.core;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import appeng.core.network.NetworkHelper;

import appeng.api.ids.AEComponents;
import appeng.api.parts.CableRenderMode;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypesInternal;
import appeng.core.definitions.AEAttachmentTypes;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEEntities;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.InitNetwork;
import appeng.core.particles.InitParticleTypes;
import appeng.block.networking.CableBusBlock;
import appeng.hooks.FabricItemHooks;
import appeng.hooks.WrenchHook;
import appeng.hooks.ticking.TickHandler;
import appeng.hotkeys.HotkeyActions;
import appeng.init.InitAdvancementTriggers;
import appeng.init.InitCapabilityProviders;
import appeng.init.InitCauldronInteraction;
import appeng.init.InitDispenserBehavior;
import appeng.init.InitMenuTypes;
import appeng.init.InitStats;
import appeng.init.InitVillager;
import appeng.init.internal.InitBlockEntityMoveStrategies;
import appeng.init.internal.InitGridLinkables;
import appeng.init.internal.InitGridServices;
import appeng.init.internal.InitP2PAttunements;
import appeng.init.internal.InitStorageCells;
import appeng.init.internal.InitUpgrades;
import appeng.init.worldgen.InitStructures;
import appeng.integration.Integrations;
import appeng.recipes.AERecipeSerializers;
import appeng.recipes.AERecipeTypes;
import appeng.recipes.conditions.TagNotEmptyCondition;
import appeng.recipes.transform.TransformLogic;
import appeng.server.AECommand;
import appeng.server.services.ChunkLoadingService;
import appeng.server.testworld.GameTestPlotAdapter;
import appeng.sounds.AppEngSounds;
import appeng.spatial.SpatialStorageChunkGenerator;
import appeng.spatial.SpatialStorageDimensionIds;

/**
 * Mod functionality that is common to both dedicated server and client.
 * <p>
 * Note that a client will still have zero or more embedded servers (although only one at a time).
 */
public abstract class AppEngBase implements AppEng {

    private static final Logger LOG = LoggerFactory.getLogger(AppEngBase.class);

    /**
     * While we process a player-specific part placement/cable interaction packet, we need to use that player's
     * transparent-facade mode to understand whether the player can see through facades or not.
     * <p>
     * We need to use this method since the collision shape methods do not know about the player that the shape is being
     * requested for, so they will call {@link #getCableRenderMode()} below, which then will use this field to figure
     * out which player it's for.
     */
    private final ThreadLocal<Player> partInteractionPlayer = new ThreadLocal<>();

    static AppEngBase INSTANCE;

    @Nullable
    private MinecraftServer currentServer;

    public AppEngBase() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;

        AEConfig.register();

        InitGridServices.init();
        InitBlockEntityMoveStrategies.init();

        // On NeoForge, these registrations are driven by the RegisterEvent of each registry.
        // Fabric registers directly, so the order matters: registries that are referenced by
        // later registrations (i.e. data components by items) have to come first.
        registerRegistries();
        registerKeyTypes(AEKeyTypesInternal.getRegistry());
        registerSounds(BuiltInRegistries.SOUND_EVENT);
        AEAttachmentTypes.register();
        AEComponents.DR.register();
        AEParts.init();
        AEBlocks.DR.register();
        AEItems.DR.register();
        AEBlockEntities.DR.register();
        AEEntities.DR.register();
        AERecipeTypes.DR.register();
        AERecipeSerializers.DR.register();
        InitStructures.register();
        InitStats.init(BuiltInRegistries.CUSTOM_STAT);
        InitAdvancementTriggers.init(BuiltInRegistries.TRIGGER_TYPES);
        InitParticleTypes.init(BuiltInRegistries.PARTICLE_TYPE);
        InitMenuTypes.init(BuiltInRegistries.MENU);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SpatialStorageDimensionIds.CHUNK_GENERATOR_ID,
                SpatialStorageChunkGenerator.CODEC);
        InitVillager.initProfession(BuiltInRegistries.VILLAGER_PROFESSION);
        InitVillager.initPointOfInterestType(BuiltInRegistries.POINT_OF_INTEREST_TYPE);
        Registry.register(BuiltInRegistries.TEST_INSTANCE_TYPE, AppEng.makeId("plot_adapter"),
                GameTestPlotAdapter.CODEC);
        registerCreativeTabs(BuiltInRegistries.CREATIVE_MODE_TAB);
        MainCreativeTab.initExternal();

        TagNotEmptyCondition.register();
        TransformLogic.init();
        InitNetwork.init();
        ChunkLoadingService.getInstance().register();
        InitCapabilityProviders.register();
        registerSynchronizedRecipes();

        TickHandler.instance().init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::serverStopped);
        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> new AECommand().register(dispatcher));

        UseBlockCallback.EVENT.register(WrenchHook::onPlayerUseBlock);
        FabricItemHooks.register();
        PlayerPickItemEvents.BLOCK.register((player, pos, state, includeData) -> {
            if (state.getBlock() instanceof CableBusBlock cableBus) {
                var picked = cableBus.getCloneItemStack(player.level(), pos, state, includeData, player);
                return picked.isEmpty() ? null : picked;
            }
            return null;
        });

        HotkeyActions.init();

        // NeoForge runs this in the common setup event, after all mods have registered their content.
        // On Fabric, all registrations of AE2 are done at this point.
        postRegistrationInitialization();
    }

    /**
     * Vanilla no longer sends recipes to the client. AE2 needs them for its UI (and GuideME on NeoForge).
     */
    private void registerSynchronizedRecipes() {
        for (var serializer : BuiltInRegistries.RECIPE_SERIALIZER) {
            var id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
            if (id != null && (id.getNamespace().equals("minecraft") || id.getNamespace().equals(AppEng.MOD_ID))) {
                RecipeSynchronization.synchronizeRecipeSerializer(serializer);
            }
        }
    }

    /**
     * Runs after all mods have had time to run their registrations into registries.
     */
    public void postRegistrationInitialization() {
        // Now that item instances are available, we can initialize registries that need item instances
        InitGridLinkables.init();
        InitStorageCells.init();

        InitP2PAttunements.init();

        InitCauldronInteraction.init();
        InitDispenserBehavior.init();

        InitUpgrades.init();
    }

    public void registerKeyTypes(Registry<AEKeyType> registry) {
        Registry.register(registry, AEKeyType.items().getId(), AEKeyType.items());
        Registry.register(registry, AEKeyType.fluids().getId(), AEKeyType.fluids());
    }

    public void registerSounds(Registry<SoundEvent> registry) {
        AppEngSounds.register(registry);
    }

    public void registerRegistries() {
        var registry = FabricRegistryBuilder.create(AEKeyType.REGISTRY_KEY)
                .attribute(RegistryAttribute.SYNCED)
                .buildAndRegister();
        AEKeyTypesInternal.setRegistry(registry);
    }

    private void serverStopped(MinecraftServer server) {
        TickHandler.instance().shutdown();
        if (this.currentServer == server) {
            this.currentServer = null;
        }
    }

    public void registerCreativeTabs(Registry<CreativeModeTab> registry) {
        MainCreativeTab.init(registry);
        FacadeCreativeTab.init(registry);
    }

    @Override
    public Collection<ServerPlayer> getPlayers() {
        var server = getCurrentServer();

        if (server != null) {
            return server.getPlayerList().getPlayers();
        }

        return Collections.emptyList();
    }

    @Override
    public void sendToAllNearExcept(Player p, double x, double y, double z,
            double dist, Level level, ClientboundPacket packet) {
        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer except = null;
            if (p instanceof ServerPlayer) {
                except = (ServerPlayer) p;
            }
            NetworkHelper.sendToPlayersNear(serverLevel, except, x, y, z, dist, packet);
        }
    }

    @Override
    public void setPartInteractionPlayer(Player player) {
        this.partInteractionPlayer.set(player);
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        return this.getCableRenderModeForPlayer(partInteractionPlayer.get());
    }

    @Nullable
    @Override
    public MinecraftServer getCurrentServer() {
        return currentServer;
    }

    @Override
    public void sendSystemMessage(Player player, Component text) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(text);
        }
    }

    protected final CableRenderMode getCableRenderModeForPlayer(@Nullable Player player) {
        if (player != null) {
            if (AEItems.NETWORK_TOOL.is(player.getItemInHand(InteractionHand.MAIN_HAND))
                    || AEItems.NETWORK_TOOL.is(player.getItemInHand(InteractionHand.OFF_HAND))) {
                return CableRenderMode.CABLE_VIEW;
            }
        }

        return CableRenderMode.STANDARD;
    }

    @Override
    public RecipeMap getRecipeMapForType(Level level, RecipeType<?> recipeType) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.recipeAccess().recipes;
        } else {
            LOG.warn("Don't know how to retrieve recipe information for level type {}", level);
            return RecipeMap.EMPTY;
        }
    }
}
