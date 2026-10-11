package de.mari_023.ae2wtlib;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import appeng.core.network.AEStreamCodecs;

import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

public class AE2wtlibAdditionalComponents {
    public static final DataComponentType<MagnetMode> MAGNET_SETTINGS = AE2wtlibComponents.register("magnet_settings",
            builder -> builder.persistent(Codec.BYTE.comapFlatMap(b -> DataResult.success(MagnetMode.fromByte(b)),
                    MagnetMode::getId))
                    .networkSynchronized(AEStreamCodecs.enumCodec(MagnetMode.class)));


    @SuppressWarnings("EmptyMethod")
    public static void init() {}
}
