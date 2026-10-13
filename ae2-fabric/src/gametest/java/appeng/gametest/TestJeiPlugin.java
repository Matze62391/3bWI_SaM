package appeng.gametest;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * Gives the client game test access to JEI's runtime.
 */
public class TestJeiPlugin implements IModPlugin {
    @Nullable
    static IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("ae2-gametest", "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }
}
