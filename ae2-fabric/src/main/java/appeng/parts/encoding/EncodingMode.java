package appeng.parts.encoding;

import appeng.core.network.AEStreamCodecs;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum EncodingMode {
    CRAFTING,
    PROCESSING,
    SMITHING_TABLE,
    STONECUTTING,
    ;

    public static final StreamCodec<FriendlyByteBuf, EncodingMode> STREAM_CODEC = AEStreamCodecs
            .enumCodec(EncodingMode.class);
}
