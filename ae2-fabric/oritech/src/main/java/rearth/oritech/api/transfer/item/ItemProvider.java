package rearth.oritech.api.transfer.item;

import net.minecraft.core.Direction;
import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public interface ItemProvider {

    ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction);

}
