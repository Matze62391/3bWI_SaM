package appeng.core.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Stream codecs that NeoForge provides in NeoForgeStreamCodecs.
 */
public final class AEStreamCodecs {
    private AEStreamCodecs() {
    }

    public static <B extends ByteBuf, T extends Enum<T>> StreamCodec<B, T> enumCodec(Class<T> enumClass) {
        var values = enumClass.getEnumConstants();
        return new StreamCodec<>() {
            @Override
            public T decode(B buf) {
                return values[FriendlyByteBuf.readVarInt(buf)];
            }

            @Override
            public void encode(B buf, T value) {
                FriendlyByteBuf.writeVarInt(buf, value.ordinal());
            }
        };
    }
}
