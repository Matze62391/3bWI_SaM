package appeng.parts.p2p;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKeyType;

public class ItemP2PTunnelPart extends ResourceHandlerP2PTunnelPart<ItemP2PTunnelPart, ItemVariant> {
    public ItemP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ItemStorage.SIDED, AEKeyType.items());
    }
}
