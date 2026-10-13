package com.glodblock.github.appflux;

import appeng.api.AECapabilities;
import com.glodblock.github.appflux.common.AFRegistryHandler;
import com.glodblock.github.appflux.common.AFSingletons;
import com.glodblock.github.appflux.common.me.inventory.FEGenericStackInvStorage;
import com.glodblock.github.appflux.config.AFConfig;
import com.glodblock.github.appflux.network.AFNetworkHandler;
import com.glodblock.github.appflux.util.AFUtil;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import team.reborn.energy.api.EnergyStorage;

/**
 * Fabric entrypoint ({@code ae2:addon}, runs after AE2 has registered its content).
 */
public class AppFlux implements ModInitializer {

    public static final String MODID = "appflux";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static AppFlux INSTANCE;

    @Override
    public void onInitialize() {
        assert INSTANCE == null;
        INSTANCE = this;
        AFConfig.load();
        AFRegistryHandler.INSTANCE = new AFRegistryHandler();
        AFSingletons.init(AFRegistryHandler.INSTANCE);
        AFRegistryHandler.INSTANCE.register();
        AFRegistryHandler.INSTANCE.registerTab();
        AFNetworkHandler.INSTANCE.register();
        // Blocks with a generic inventory (interfaces, pattern providers) accept and offer the energy in it when they
        // have an induction card. NeoForge registers this for every block with that capability; on Fabric it is a
        // fallback provider, which is only asked when the block has no energy storage of its own.
        EnergyStorage.SIDED.registerFallback((level, pos, state, tile, side) -> {
            if (tile == null || side == null || !AFUtil.shouldTryCast(tile, side)) {
                return null;
            }
            var genericInv = AECapabilities.GENERIC_INTERNAL_INV.find(level, pos, state, tile, side);
            return genericInv != null ? new FEGenericStackInvStorage(genericInv) : null;
        });
        AFRegistryHandler.INSTANCE.init();
    }

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }

    public static String stringId(String id) {
        return id(id).toString();
    }

}
