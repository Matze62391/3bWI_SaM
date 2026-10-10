package com.glodblock.github.glodium.util;

import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public abstract class GlodUtil {

    private GlodUtil() {
        // NO-OP
    }

    public static <T> DataComponentType<@NotNull T> getComponentType(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, @NotNull T> stream) {
        return DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build();
    }

    public static boolean checkInvalidRL(String rl, Registry<?> registry) {
        return checkInvalidRL(Identifier.parse(rl), registry);
    }

    public static boolean checkInvalidRL(Identifier rl, Registry<?> registry) {
        return registry.containsKey(rl);
    }

    public static double clamp(double num, double floor, double ceil) {
        return Math.min(ceil, Math.max(floor, num));
    }

    public static long clamp(long num, long floor, long ceil) {
        return Math.min(ceil, Math.max(floor, num));
    }

    public static boolean checkMod(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

}
