package rearth.oritech.port.neoforge.transfer;

import rearth.oritech.port.neoforge.transfer.resource.Resource;

@FunctionalInterface
public interface IndexModifier<T extends Resource> {
    void set(int index, T resource, int amount);
}
