package rearth.oritech.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import rearth.oritech.port.neoforge.common.util.FakePlayer;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.mixin.LaserDropsMixin;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;

public final class LaserMachinePlayer extends FakePlayer {

    public static final ThreadLocal<LaserDropsMixin.Capture> CAPTURED_DROPS = new ThreadLocal<>();

    private final SimpleInventoryStorage targetInventory;

    public LaserMachinePlayer(ServerLevel level, GameProfile profile, SimpleInventoryStorage targetInventory) {
        super(level, profile);
        this.targetInventory = targetInventory;
    }

    public void collectDrops(java.util.List<ItemStack> drops) {
        var player = this;
        try (var transaction = Transaction.openRoot()) {
            long inserted = 0;

            for (var stack : drops) {
                inserted += player.targetInventory.insert(ItemResource.of(stack), stack.getCount(), transaction);
            }

            if (inserted > 0) transaction.commit();
        }

        // Match block-harvesting behavior: anything that does not fit is discarded.
    }
}
