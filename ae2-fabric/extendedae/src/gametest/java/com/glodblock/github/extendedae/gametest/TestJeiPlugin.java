package com.glodblock.github.extendedae.gametest;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Gives the client game test access to JEI's runtime.
 */
public class TestJeiPlugin implements IModPlugin {
    @Nullable
    static IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("extendedae-gametest", "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }
}
