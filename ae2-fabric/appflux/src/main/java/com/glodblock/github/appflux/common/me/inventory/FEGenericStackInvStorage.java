package com.glodblock.github.appflux.common.me.inventory;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.GenericStack;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * Exposes the energy in a generic inventory (interface, pattern provider with an induction card) as an energy storage.
 * NeoForge: {@code GenericStackInvHandler} with an energy resource; Fabric's energy API has no resources, so the slots
 * are handled here directly.
 */
public class FEGenericStackInvStorage implements EnergyStorage {

    private final GenericInternalInventory inv;

    public FEGenericStackInvStorage(GenericInternalInventory inv) {
        this.inv = inv;
    }

    private static FluxKey key() {
        return FluxKey.of(EnergyType.FE);
    }

    private boolean isUsableSlot(int slot) {
        var current = this.inv.getKey(slot);
        return current == null ? this.inv.isAllowedIn(slot, key()) : key().equals(current);
    }

    @Override
    public long getAmount() {
        long cnt = 0;
        for (int slot = 0; slot < this.inv.size(); slot ++) {
            var stack = this.inv.getStack(slot);
            if (stack != null && key().equals(stack.what())) {
                cnt += stack.amount();
            }
        }
        return cnt;
    }

    @Override
    public long getCapacity() {
        int cnt = 0;
        for (int slot = 0; slot < this.inv.size(); slot ++) {
            var stack = this.inv.getStack(slot);
            if (stack == null || key().equals(stack.what())) {
                cnt ++;
            }
        }
        return cnt * this.inv.getMaxAmount(key());
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        if (!this.inv.canInsert()) {
            return 0;
        }
        long inserted = 0;
        // First the slots that already hold energy, then empty slots
        for (int pass = 0; pass < 2 && inserted < maxAmount; pass ++) {
            for (int slot = 0; slot < this.inv.size() && inserted < maxAmount; slot ++) {
                var current = this.inv.getKey(slot);
                if ((pass == 0) != (current != null) || !this.isUsableSlot(slot)) {
                    continue;
                }
                var amount = this.inv.getAmount(slot);
                var toAdd = Math.min(maxAmount - inserted, this.inv.getMaxAmount(key()) - amount);
                if (toAdd > 0) {
                    this.inv.updateSnapshots(transaction);
                    this.inv.beginBatch();
                    this.inv.setStack(slot, new GenericStack(key(), amount + toAdd));
                    this.inv.endBatchSuppressed();
                    inserted += toAdd;
                }
            }
        }
        return inserted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        if (!this.inv.canExtract()) {
            return 0;
        }
        long extracted = 0;
        for (int slot = 0; slot < this.inv.size() && extracted < maxAmount; slot ++) {
            if (!key().equals(this.inv.getKey(slot))) {
                continue;
            }
            var amount = this.inv.getAmount(slot);
            var toTake = Math.min(maxAmount - extracted, amount);
            if (toTake > 0) {
                this.inv.updateSnapshots(transaction);
                this.inv.beginBatch();
                this.inv.setStack(slot, amount - toTake <= 0 ? null : new GenericStack(key(), amount - toTake));
                this.inv.endBatchSuppressed();
                extracted += toTake;
            }
        }
        return extracted;
    }
}
