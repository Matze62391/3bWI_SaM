package net.pedroksl.ae2addonlib.client.render;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.pedroksl.ae2addonlib.core.AE2AddonLib;
import net.pedroksl.ae2addonlib.util.WaterBasedFluidType;

/**
 * The model of a fluid that looks like water with a tint. Register it with Fabric's FluidRenderingRegistry.
 */
public class WaterBasedFluidModel<T extends WaterBasedFluidType> {

    private final T type;

    private final Material WATER_STILL = new Material(AE2AddonLib.makeId("block/water_still"));
    private final Material WATER_FLOW = new Material(AE2AddonLib.makeId("block/water_flowing"));
    private final Material WATER_OVERLAY = new Material(AE2AddonLib.makeId("block/water_overlay"));

    public WaterBasedFluidModel(T type) {
        this.type = type;
    }

    public FluidModel.Unbaked get() {
        return new FluidModel.Unbaked(WATER_STILL, WATER_FLOW, WATER_OVERLAY, (_) -> this.type.getTintColor());
    }
}
