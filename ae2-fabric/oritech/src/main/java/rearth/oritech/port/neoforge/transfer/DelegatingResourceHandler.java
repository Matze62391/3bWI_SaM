package rearth.oritech.port.neoforge.transfer;

import java.util.function.Supplier;

import rearth.oritech.port.neoforge.transfer.resource.Resource;
import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

public class DelegatingResourceHandler<T extends Resource> implements ResourceHandler<T> {
    protected final Supplier<ResourceHandler<T>> delegate;

    public DelegatingResourceHandler(ResourceHandler<T> delegate) {
        this(() -> delegate);
    }

    public DelegatingResourceHandler(Supplier<ResourceHandler<T>> delegate) {
        this.delegate = delegate;
    }

    public ResourceHandler<T> getDelegate() {
        return delegate.get();
    }

    @Override
    public int size() {
        return getDelegate().size();
    }

    protected int convertIndex(int index) {
        return index;
    }

    @Override
    public T getResource(int index) {
        return getDelegate().getResource(convertIndex(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return getDelegate().getAmountAsLong(convertIndex(index));
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return getDelegate().getCapacityAsLong(convertIndex(index), resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return getDelegate().isValid(convertIndex(index), resource);
    }

    @Override
    public int insert(int index, T resource, int amount, TransactionContext transaction) {
        return getDelegate().insert(convertIndex(index), resource, amount, transaction);
    }

    @Override
    public int insert(T resource, int amount, TransactionContext transaction) {
        return getDelegate().insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, T resource, int amount, TransactionContext transaction) {
        return getDelegate().extract(convertIndex(index), resource, amount, transaction);
    }

    @Override
    public int extract(T resource, int amount, TransactionContext transaction) {
        return getDelegate().extract(resource, amount, transaction);
    }
}
