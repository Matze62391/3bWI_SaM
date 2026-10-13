package com.glodblock.github.extendedae;

import com.glodblock.github.extendedae.common.EAERegistryHandler;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.extendedae.common.hooks.CutterHook;
import com.glodblock.github.extendedae.config.EAEConfig;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.recipe.CircuitCutterRecipeSerializer;
import com.glodblock.github.extendedae.recipe.CrystalAssemblerRecipeSerializer;
import com.glodblock.github.extendedae.recipe.CrystalFixerRecipeSerializer;
import com.glodblock.github.extendedae.xmod.wt.ContainerWirelessExPAT;
import com.glodblock.github.extendedae.xmod.wt.HostWirelessExPAT;
import com.glodblock.github.glodium.Glodium;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * Fabric entrypoint ({@code ae2:addon}, runs after AE2 has registered its content).
 */
public class ExtendedAE implements ModInitializer {

    public static final String MODID = "extendedae";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ExtendedAE INSTANCE;

    @Override
    public void onInitialize() {
        assert INSTANCE == null;
        INSTANCE = this;
        EAEConfig.load();
        EAERegistryHandler.INSTANCE = new EAERegistryHandler();
        EAESingletons.init(EAERegistryHandler.INSTANCE);
        EAERegistryHandler.INSTANCE.register();
        EAERegistryHandler.INSTANCE.registerTab();
        Icon.Texture TX = new Icon.Texture(id("textures/guis/nicons.png"), 64, 64);
        AddTerminalEvent.register(event -> event.builder(
                "ex_pattern_access",
                HostWirelessExPAT::new,
                ContainerWirelessExPAT.TYPE,
                EAESingletons.WIRELESS_EX_PAT.get(),
                new Icon(32, 32, 16, 16, TX)
        ).hotkeyName("wireless_pattern_access_terminal").addTerminal());
        EAENetworkHandler.INSTANCE.register();
        CutterHook.register();
        RecipeSynchronization.synchronizeRecipeSerializer(CircuitCutterRecipeSerializer.INSTANCE);
        RecipeSynchronization.synchronizeRecipeSerializer(CrystalAssemblerRecipeSerializer.INSTANCE);
        RecipeSynchronization.synchronizeRecipeSerializer(CrystalFixerRecipeSerializer.INSTANCE);
        EAERegistryHandler.INSTANCE.onInit();
    }

    public static Identifier id(String id) {
        return Glodium.id(MODID, id);
    }

    public static String stringId(String id) {
        return id(id).toString();
    }

}
