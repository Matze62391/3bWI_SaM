package rearth.oritech.port.neoforge.transfer;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import rearth.oritech.port.neoforge.transfer.resource.Resource;
import rearth.oritech.port.neoforge.transfer.transaction.SnapshotJournal;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

/**
 * A resource handler backed by a list of mutable stacks. Changes in a transaction are journaled per index and undone
 * if the transaction is aborted; {@link #onContentsChanged} is called once the outermost transaction commits.
 */
public abstract class StacksResourceHandler<S, T extends Resource>
        implements ResourceHandler<T>, rearth.oritech.port.neoforge.common.util.ValueIOSerializable {
    protected final S emptyStack;
    protected final Codec<S> stackCodec;
    protected NonNullList<S> stacks;
    private List<IndexJournal> journals;

    protected StacksResourceHandler(int size, S emptyStack, Codec<S> stackCodec) {
        this(NonNullList.withSize(size, emptyStack), emptyStack, stackCodec);
    }

    protected StacksResourceHandler(NonNullList<S> stacks, S emptyStack, Codec<S> stackCodec) {
        this.emptyStack = emptyStack;
        this.stackCodec = stackCodec;
        setStacks(stacks);
    }

    protected void setStacks(NonNullList<S> stacks) {
        this.stacks = stacks;
        this.journals = new ArrayList<>(stacks.size());
        for (var i = 0; i < stacks.size(); i++) {
            journals.add(new IndexJournal(i));
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("stacks", stackCodec.listOf(), stacks);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read("stacks", stackCodec.listOf()).ifPresent(list -> {
            for (var i = 0; i < stacks.size(); i++) {
                stacks.set(i, i < list.size() ? list.get(i) : emptyStack);
            }
        });
    }

    public void set(int index, T resource, int amount) {
        stacks.set(index, resource.isEmpty() || amount <= 0 ? emptyStack : getStackFrom(resource, amount));
    }

    protected abstract T getResourceFrom(S stack);

    protected abstract int getAmountFrom(S stack);

    protected abstract S getStackFrom(T resource, int amount);

    protected abstract S copyOf(S stack);

    protected boolean matches(S stack, T resource) {
        return getResourceFrom(stack).equals(resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return true;
    }

    protected abstract int getCapacity(int index, T resource);

    protected void onContentsChanged(int index, S previousContents) {
    }

    public NonNullList<S> copyToList() {
        var copy = NonNullList.withSize(stacks.size(), emptyStack);
        for (var i = 0; i < stacks.size(); i++) {
            copy.set(i, copyOf(stacks.get(i)));
        }
        return copy;
    }

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public T getResource(int index) {
        return getResourceFrom(stacks.get(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return getAmountFrom(stacks.get(index));
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return getCapacity(index, resource.isEmpty() ? getResource(index) : resource);
    }

    @Override
    public int insert(int index, T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !isValid(index, resource)) {
            return 0;
        }
        var current = stacks.get(index);
        var currentAmount = getAmountFrom(current);
        if (currentAmount > 0 && !matches(current, resource)) {
            return 0;
        }
        var inserted = Math.min(amount, getCapacity(index, resource) - currentAmount);
        if (inserted <= 0) {
            return 0;
        }
        journals.get(index).updateSnapshots(transaction);
        stacks.set(index, getStackFrom(resource, currentAmount + inserted));
        return inserted;
    }

    @Override
    public int extract(int index, T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        var current = stacks.get(index);
        var currentAmount = getAmountFrom(current);
        if (amount == 0 || currentAmount == 0 || !matches(current, resource)) {
            return 0;
        }
        var extracted = Math.min(amount, currentAmount);
        journals.get(index).updateSnapshots(transaction);
        var remaining = currentAmount - extracted;
        stacks.set(index, remaining > 0 ? getStackFrom(resource, remaining) : emptyStack);
        return extracted;
    }

    private final class IndexJournal extends SnapshotJournal<S> {
        private final int index;

        private IndexJournal(int index) {
            this.index = index;
        }

        @Override
        protected S createSnapshot() {
            return copyOf(stacks.get(index));
        }

        @Override
        protected void revertToSnapshot(S snapshot) {
            stacks.set(index, snapshot);
        }

        @Override
        protected void onRootCommit(S originalState) {
            onContentsChanged(index, originalState);
        }
    }
}
