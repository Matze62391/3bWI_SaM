package net.pedroksl.ae2addonlib.fluid;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;

/**
 * The shared properties of a fluid's source and flowing variants (replaces NeoForge's FluidType, which has no
 * equivalent on Fabric). Only the properties used by AE2 addons are kept.
 */
public class FluidType {
    private final int density;
    private final int viscosity;
    private final Map<SoundAction, SoundEvent> sounds;

    public FluidType(Properties properties) {
        this.density = properties.density;
        this.viscosity = properties.viscosity;
        this.sounds = properties.sounds;
    }

    public int getDensity() {
        return density;
    }

    public int getViscosity() {
        return viscosity;
    }

    @Nullable
    public SoundEvent getSound(SoundAction action) {
        return sounds.get(action);
    }

    public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
        return true;
    }

    public enum SoundAction {
        BUCKET_FILL,
        BUCKET_EMPTY
    }

    public static final class Properties {
        private int density = 1000;
        private int viscosity = 1000;
        private final Map<SoundAction, SoundEvent> sounds = new EnumMap<>(SoundAction.class);

        private Properties() {
        }

        public static Properties create() {
            return new Properties();
        }

        public Properties density(int density) {
            this.density = density;
            return this;
        }

        public Properties viscosity(int viscosity) {
            this.viscosity = viscosity;
            return this;
        }

        public Properties sound(SoundAction action, SoundEvent sound) {
            sounds.put(action, sound);
            return this;
        }
    }
}
