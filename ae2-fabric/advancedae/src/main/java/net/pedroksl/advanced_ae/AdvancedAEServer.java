package net.pedroksl.advanced_ae;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;

public class AdvancedAEServer extends AdvancedAE {

    public AdvancedAEServer() {
        super();
    }

    @Override
    @Nullable
    public Level getClientLevel() {
        return null;
    }

    @Override
    public void registerHotkey(String id) {}
}
