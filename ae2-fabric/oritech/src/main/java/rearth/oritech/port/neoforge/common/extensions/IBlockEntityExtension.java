package rearth.oritech.port.neoforge.common.extensions;

/**
 * Block entity hooks. {@link #onLoad()} is called when the block entity is added to a server level.
 */
public interface IBlockEntityExtension {
    default void onLoad() {
    }
}
