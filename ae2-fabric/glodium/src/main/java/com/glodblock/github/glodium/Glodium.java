package com.glodblock.github.glodium;

import com.glodblock.github.glodium.xmod.XModManager;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class Glodium implements ModInitializer {

    public static final String MODID = "glodium";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static Glodium INSTANCE;

    @Nullable
    private MinecraftServer server;

    public Glodium() {
        assert INSTANCE == null;
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
        XModManager.load();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> this.server = null);
    }

    @Nullable
    public MinecraftServer getServer() {
        return this.server;
    }

    public static Identifier id(String modid, String name) {
        return Identifier.fromNamespaceAndPath(modid, name);
    }

}
