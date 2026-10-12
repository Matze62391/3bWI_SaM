package rearth.oritech.port.neoforge.transfer.energy;

import java.util.function.Supplier;

import rearth.oritech.port.neoforge.transfer.transaction.TransactionContext;

public class DelegatingEnergyHandler implements EnergyHandler {
    protected final Supplier<EnergyHandler> delegate;

    public DelegatingEnergyHandler(EnergyHandler delegate) {
        this(() -> delegate);
    }

    public DelegatingEnergyHandler(Supplier<EnergyHandler> delegate) {
        this.delegate = delegate;
    }

    public EnergyHandler getDelegate() {
        return delegate.get();
    }

    @Override
    public long getAmountAsLong() {
        return getDelegate().getAmountAsLong();
    }

    @Override
    public long getCapacityAsLong() {
        return getDelegate().getCapacityAsLong();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return getDelegate().insert(amount, transaction);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return getDelegate().extract(amount, transaction);
    }
}
