package rearth.oritech.port.neoforge.transfer.access;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import rearth.oritech.port.neoforge.transfer.ResourceHandler;
import rearth.oritech.port.neoforge.transfer.item.ItemResource;
import rearth.oritech.port.neoforge.transfer.transaction.SnapshotJournal;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

final class ItemAccessImpl {
    private ItemAccessImpl() {}

    /**
     * An item access on a single stack location; the stack is replaced on changes, which are journaled.
     */
    abstract static class StackLocation extends SnapshotJournal<ItemStack> implements ItemAccess {
        protected abstract ItemStack get();

        protected abstract void put(ItemStack stack);

        @Override
        public ItemResource getResource() {
            return ItemResource.of(get());
        }

        @Override
        public int getAmount() {
            return get().getCount();
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            var stack = get();
            if (amount <= 0 || resource.isEmpty() || !stack.isEmpty() && !resource.matches(stack)) {
                return 0;
            }
            var inserted = Math.min(amount, resource.getMaxStackSize() - stack.getCount());
            if (inserted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            put(resource.toStack(stack.getCount() + inserted));
            return inserted;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            var stack = get();
            if (amount <= 0 || stack.isEmpty() || !resource.matches(stack)) {
                return 0;
            }
            var extracted = Math.min(amount, stack.getCount());
            updateSnapshots(transaction);
            var remaining = stack.getCount() - extracted;
            put(remaining > 0 ? resource.toStack(remaining) : ItemStack.EMPTY);
            return extracted;
        }

        @Override
        protected ItemStack createSnapshot() {
            return get().copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            put(snapshot);
        }
    }

    /**
     * A stack that is changed in place, like NeoForge's stack item access.
     */
    static final class Stack extends StackLocation {
        private final ItemStack stack;

        Stack(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected ItemStack get() {
            return stack;
        }

        @Override
        protected void put(ItemStack newStack) {
            if (newStack.isEmpty()) {
                stack.setCount(0);
                return;
            }
            if (stack.getItem() == newStack.getItem()) {
                stack.setCount(newStack.getCount());
                stack.applyComponents(newStack.getComponentsPatch());
                for (var type : stack.getComponents().keySet()) {
                    if (!newStack.has(type)) {
                        stack.remove(type);
                    }
                }
            }
            // a stack can't change its item in place; capabilities that do so need another access
        }
    }

    static final class PlayerHand extends StackLocation {
        private final Player player;
        private final InteractionHand hand;

        PlayerHand(Player player, InteractionHand hand) {
            this.player = player;
            this.hand = hand;
        }

        @Override
        protected ItemStack get() {
            return player.getItemInHand(hand);
        }

        @Override
        protected void put(ItemStack stack) {
            player.setItemInHand(hand, stack);
        }

        @Override
        protected void onRootCommit(ItemStack originalState) {
            player.getInventory().setChanged();
        }
    }

    static final class PlayerSlot extends StackLocation {
        private final Player player;
        private final int slot;

        PlayerSlot(Player player, int slot) {
            this.player = player;
            this.slot = slot;
        }

        @Override
        protected ItemStack get() {
            return player.getInventory().getItem(slot);
        }

        @Override
        protected void put(ItemStack stack) {
            player.getInventory().setItem(slot, stack);
        }
    }

    static final class Cursor extends StackLocation {
        private final AbstractContainerMenu menu;

        Cursor(AbstractContainerMenu menu) {
            this.menu = menu;
        }

        @Override
        protected ItemStack get() {
            return menu.getCarried();
        }

        @Override
        protected void put(ItemStack stack) {
            menu.setCarried(stack);
        }
    }

    static final class Infinite implements ItemAccess {
        private final ItemStack contents;

        Infinite(ItemStack contents) {
            this.contents = contents;
        }

        @Override
        public ItemResource getResource() {
            return ItemResource.of(contents);
        }

        @Override
        public int getAmount() {
            return contents.getCount();
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return amount;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return resource.matches(contents) ? amount : 0;
        }
    }

    static final class HandlerIndex implements ItemAccess {
        private final ResourceHandler<ItemResource> handler;
        private final int index;

        HandlerIndex(ResourceHandler<ItemResource> handler, int index) {
            this.handler = handler;
            this.index = index;
        }

        @Override
        public ItemResource getResource() {
            return handler.getResource(index);
        }

        @Override
        public int getAmount() {
            return handler.getAmountAsInt(index);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return resource.isEmpty() || amount <= 0 ? 0 : handler.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return resource.isEmpty() || amount <= 0 ? 0 : handler.extract(index, resource, amount, transaction);
        }
    }
}
