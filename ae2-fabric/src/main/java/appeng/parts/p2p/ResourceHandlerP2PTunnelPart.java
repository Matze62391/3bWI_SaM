/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.p2p;

import java.util.Collections;
import java.util.Iterator;

import com.google.common.collect.Iterators;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;

import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKeyType;
import appeng.util.transfer.TransferPreconditions;

/**
 * Base class for P2P tunnels that work with Fabric's {@code Storage<T>}.
 */
public abstract class ResourceHandlerP2PTunnelPart<P extends ResourceHandlerP2PTunnelPart<P, T>, T extends TransferVariant<?>>
        extends CapabilityP2PTunnelPart<P, Storage<T>> {

    private final AEKeyType keyType;

    public ResourceHandlerP2PTunnelPart(IPartItem<?> partItem,
            BlockApiLookup<Storage<T>, Direction> capability,
            AEKeyType keyType) {
        super(partItem, capability);
        this.inputHandler = new InputStorage();
        this.outputHandler = new OutputStorage();
        this.emptyHandler = Storage.empty();
        this.keyType = keyType;
    }

    private class InputStorage implements InsertionOnlyStorage<T> {
        @Override
        public long insert(T resource, long maxAmount, TransactionContext tx) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, maxAmount);
            long total = 0;

            var outputs = getOutputs();
            final int outputTunnels = outputs.size();
            final long amount = maxAmount;

            if (outputTunnels == 0 || amount == 0) {
                return 0;
            }

            final long amountPerOutput = amount / outputTunnels;
            long overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            for (var target : outputs) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final Storage<T> output = capabilityGuard.get();
                    final long toSend = amountPerOutput + overflow;

                    final long received = output.insert(resource, toSend, tx);

                    overflow = toSend - received;
                    total += received;
                }
            }

            deductTransportCost(total, keyType, tx);
            return total;
        }

        @Override
        public Iterator<StorageView<T>> iterator() {
            return Collections.emptyIterator();
        }
    }

    private class OutputStorage implements ExtractionOnlyStorage<T> {
        @Override
        public long extract(T resource, long maxAmount, TransactionContext tx) {
            try (CapabilityGuard input = getInputCapability()) {
                long extracted = input.get().extract(resource, maxAmount, tx);
                deductTransportCost(extracted, keyType, tx);
                return extracted;
            }
        }

        @Override
        public Iterator<StorageView<T>> iterator() {
            try (CapabilityGuard input = getInputCapability()) {
                return Iterators.transform(input.get().iterator(), TransportCostView::new);
            }
        }
    }

    /**
     * Deducts the transport cost when resources are extracted through a view of the input's storage.
     */
    private class TransportCostView implements StorageView<T> {
        private final StorageView<T> delegate;

        TransportCostView(StorageView<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public long extract(T resource, long maxAmount, TransactionContext tx) {
            long extracted = delegate.extract(resource, maxAmount, tx);
            deductTransportCost(extracted, keyType, tx);
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return delegate.isResourceBlank();
        }

        @Override
        public T getResource() {
            return delegate.getResource();
        }

        @Override
        public long getAmount() {
            return delegate.getAmount();
        }

        @Override
        public long getCapacity() {
            return delegate.getCapacity();
        }
    }
}
