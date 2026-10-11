package com.glodblock.github.appflux.common.me.strategy;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.util.AFUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class FEContainerItemStrategy implements ContainerItemStrategy<FluxKey, EnergyStorage> {

    @Override
    public @Nullable GenericStack getContainedStack(ItemStack stack) {
        var energy = AFUtil.findEnergy(stack);
        if (energy != null && energy.getAmount() > 0) {
            return new GenericStack(FluxKey.of(EnergyType.FE), energy.getAmount());
        }
        return null;
    }

    @Override
    public @Nullable EnergyStorage findCarriedContext(Player player, AbstractContainerMenu menu) {
        return EnergyStorage.ITEM.find(menu.getCarried(), ContainerItemContext.ofPlayerCursor(player, menu));
    }

    @Override
    public @Nullable EnergyStorage findPlayerSlotContext(Player player, int slot) {
        var playerInv = PlayerInventoryStorage.of(player);
        return EnergyStorage.ITEM.find(player.getInventory().getItem(slot), ContainerItemContext.ofPlayerSlot(player, playerInv.getSlots().get(slot)));
    }

    @Override
    public long extract(EnergyStorage context, FluxKey what, long amount, Actionable mode) {
        try (var tx = Transaction.openOuter()) {
            var extracted = context.extract(amount, tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return extracted;
        }
    }

    @Override
    public long insert(EnergyStorage context, FluxKey what, long amount, Actionable mode) {
        try (var tx = Transaction.openOuter()) {
            var inserted = context.insert(amount, tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return inserted;
        }
    }

    @Override
    public void playFillSound(Player player, FluxKey what) {
        // NO-OP
    }

    @Override
    public void playEmptySound(Player player, FluxKey what) {
        // NO-OP
    }

    @Override
    public @Nullable GenericStack getExtractableContent(EnergyStorage context) {
        var stored = context.getAmount();
        if (stored > 0) {
            return new GenericStack(FluxKey.of(EnergyType.FE), stored);
        }
        return null;
    }

}
