package rearth.oritech.client.ui;

import net.minecraft.world.item.ItemStack;
import rearth.oritech.port.neoforge.transfer.StacksResourceHandler;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.item.ResourceHandlerSlot;

public class BasicMachineOutputSlot extends ResourceHandlerSlot {

    // An output-only slot. This could be expanded to give XP to the player when items are removed, similar to the FurnaceOutputSlot.

    public BasicMachineOutputSlot(StacksResourceHandler<ItemStack, ItemResource> inventory, int index, int x, int y) {
        super(inventory, inventory::set, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
