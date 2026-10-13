package com.glodblock.github.extendedae.xmod.aae;

import com.glodblock.github.extendedae.client.gui.pattern.GuiProcessingPattern;
import com.glodblock.github.extendedae.container.pattern.PatternGuiHandler;
import com.glodblock.github.extendedae.xmod.ModConstants;
import com.glodblock.github.glodium.registry.RegistryHandler;
import com.glodblock.github.glodium.xmod.ThirdParty;
import com.glodblock.github.glodium.xmod.XModLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.MenuScreens;
import net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern;

@ThirdParty(ModConstants.ADV_AE)
public class AAELoader implements XModLoader {

    @Override
    public String modid() {
        return ModConstants.ADV_AE;
    }

    @Override
    public void loadCommon() {
        PatternGuiHandler.addPatternHandler(AdvProcessingPattern.class, ContainerAdvProcessingPattern.ID);
    }

    @Override
    public void loadClient() {
        MenuScreens.register(ContainerAdvProcessingPattern.TYPE, GuiProcessingPattern::new);
    }

    @Override
    public void onRegister(RegistryHandler handler) {
        Registry.register(BuiltInRegistries.MENU, ContainerAdvProcessingPattern.ID, ContainerAdvProcessingPattern.TYPE);
    }

}
