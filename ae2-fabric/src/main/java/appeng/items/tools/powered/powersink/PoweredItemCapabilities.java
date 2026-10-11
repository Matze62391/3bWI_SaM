/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.items.tools.powered.powersink;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import team.reborn.energy.api.EnergyStorage;

import net.minecraft.world.item.Item;
import appeng.util.transfer.TransferPreconditions;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.implementations.items.IAEItemPowerStorage;

/**
 * The capability provider to expose chargable items to other mods.
 */
public class PoweredItemCapabilities implements EnergyStorage {
    private final ContainerItemContext itemAccess;
    private final Item validItem;
    private final IAEItemPowerStorage item;

    public PoweredItemCapabilities(ContainerItemContext itemAccess, Item validItem, IAEItemPowerStorage item) {
        this.itemAccess = itemAccess;
        this.validItem = validItem;
        this.item = item;
    }

    private long getAmountFrom(ItemVariant currentItem) {
        if (!currentItem.isOf(validItem)) {
            return 0;
        }
        return (long) PowerUnit.AE.convertTo(PowerUnit.TR, item.getAECurrentPower(currentItem.toStack()));
    }

    @Override
    public long getAmount() {
        var currentItem = itemAccess.getItemVariant();
        return getAmountFrom(currentItem);
    }

    @Override
    public long getCapacity() {
        var currentItem = itemAccess.getItemVariant();
        if (!currentItem.isOf(validItem)) {
            return 0;
        }
        return (long) PowerUnit.AE.convertTo(PowerUnit.TR, item.getAEMaxPower(currentItem.toStack()));
    }

    @Override
    public long insert(long amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);

        long accessAmount = itemAccess.getAmount();
        if (accessAmount == 0) {
            return 0;
        }
        long amountPerItem = amount / accessAmount;
        if (amountPerItem == 0) {
            return 0;
        }

        ItemVariant accessResource = itemAccess.getItemVariant();
        if (!accessResource.isOf(validItem)) {
            return 0;
        }

        // We'll essentially perform the insertion into a copy of the stack, then convert back to the resource
        var amountAE = PowerUnit.TR.convertTo(PowerUnit.AE, amount);
        var mutableStack = accessResource.toStack();
        double overflowAE = item.injectAEPower(mutableStack, amountAE, Actionable.MODULATE);
        var insertedPerItem = (long) PowerUnit.AE.convertTo(PowerUnit.TR, amountAE - overflowAE);

        insertedPerItem = Math.min(amountPerItem, insertedPerItem);
        if (insertedPerItem > 0) {
            var filledResource = ItemVariant.of(mutableStack);

            if (!filledResource.isBlank()) {
                return insertedPerItem * itemAccess.exchange(filledResource, accessAmount, transaction);
            }
        }

        return 0;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long extract(long amount, TransactionContext transaction) {
        return 0;
    }
}
