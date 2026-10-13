package rearth.oritech.util;

import rearth.oritech.port.neoforge.fluids.FluidStack;

public final class ColorHelper {

    public static int argb(float r, float g, float b) {
        return argb(r, g, b, 1f);
    }

    public static int argb(float r, float g, float b, float a) {
        return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int makeOpaque(int argbColor) {
        return argbColor | 0xFF000000;
    }

    /**
     * Resolves the tint color (ARGB) of the given fluid using the data-driven fluid models introduced in NeoForge 26.1.
     * This replaces the removed {@code IClientFluidTypeExtensions#getTintColor}. Client-side only.
     *
     * @return the fluid's ARGB tint color, or {@link #WHITE} if the fluid has no tint source.
     */
    public static int getFluidTint(FluidStack stack) {
        if (stack.isEmpty()) return WHITE;
        return makeOpaque(net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering.getColor(stack.toVariant()));
    }

    public static final int WHITE = 0xFFFFFFFF;

    private ColorHelper() {
    }
}
