package appeng.core.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.VarInt;
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
                return values[VarInt.read(buf)];
            }

            @Override
            public void encode(B buf, T value) {
                VarInt.write(buf, value.ordinal());
            }
        };
    }
}
