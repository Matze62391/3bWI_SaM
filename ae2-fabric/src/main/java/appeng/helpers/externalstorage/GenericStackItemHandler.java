package appeng.helpers.externalstorage;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEKeyType;
import appeng.helpers.ResourceConversion;

/**
 * Exposes a {@link GenericInternalInventory} as the platforms external item storage interface.
 */
public class GenericStackItemHandler extends GenericStackInvHandler<ItemVariant> {
    public GenericStackItemHandler(GenericInternalInventory inv) {
        super(ResourceConversion.ITEM, AEKeyType.items(), inv);
    }
}
