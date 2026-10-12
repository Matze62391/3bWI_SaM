package rearth.oritech.init.datamap;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import rearth.oritech.port.neoforge.registries.datamaps.DataMapType;
import rearth.oritech.Oritech;

public class DataMapContent {

    // attaches a quality multiplier to blocks that the unstable container can capture
    // data files are located at <namespace>/data_maps/block/schrodingers_safe_source.json
    public static final DataMapType<Block, SchrodingersSafeSource> SCHRODINGERS_SAFE_SOURCE = DataMapType.builder(
            Oritech.id("schrodingers_safe_source"),
            Registries.BLOCK,
            SchrodingersSafeSource.CODEC
    ).synced(SchrodingersSafeSource.CODEC, false).build();

    public static void init() {
    }
}


