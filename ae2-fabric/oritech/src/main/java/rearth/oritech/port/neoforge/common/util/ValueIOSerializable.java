package rearth.oritech.port.neoforge.common.util;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface ValueIOSerializable {
    void serialize(ValueOutput output);

    void deserialize(ValueInput input);
}
