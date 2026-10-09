package appeng.util;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Collects the item stacks a player carries, e.g. to find items for autocrafting notifications. Addons that add extra
 * inventories (such as accessory slots) can contribute their stacks by listening to {@link #EVENT}.
 */
public final class SearchInventoryEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class,
            listeners -> (player, stacks) -> {
                for (var listener : listeners) {
                    listener.collect(player, stacks);
                }
            });

    @FunctionalInterface
    public interface Listener {
        void collect(Player player, List<ItemStack> stacks);
    }

    private SearchInventoryEvent() {
    }

    public static List<ItemStack> getItems(Player player) {
        List<ItemStack> items = new ArrayList<>(player.getInventory().getNonEquipmentItems());
        EVENT.invoker().collect(player, items);
        return items;
    }
}
