package rearth.oritech.api.transfer.energy;

import net.minecraft.core.Direction;
import rearth.oritech.port.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;

public interface EnergyProvider {

    EnergyHandler getEnergyLookup(@Nullable Direction direction);

    interface Item {
        int getEnergyCapacity();
        int getMaxRFInputRate();
        int getMaxRFOutputRate();
    }

}
