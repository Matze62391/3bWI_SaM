package net.pedroksl.ae2addonlib.api;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.ae2addonlib.core.network.clientPacket.FluidTankClientAudioPacket;
import net.pedroksl.ae2addonlib.fluid.FluidContainers;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;

/**
 * Interface used in menus that contain FluidTankSlots.
 * Attaches the handler directly to the menu.
 * @see net.pedroksl.ae2addonlib.client.widgets.FluidTankSlot
 */
public interface IFluidTankHandler {

    /**
     * Getter for the server player.
     * @return The server player.
     */
    ServerPlayer getServerPlayer();

    /**
     * Getter for the carried item.
     * @return The carried item.
     */
    ItemStack getCarriedItem();

    /**
     * Getter for the tank.
     * @return The tank.
     */
    GenericStackInv getTank();

    /**
     * Checks if the current tank can be extracted from.
     * @param index The tank index.
     * @return If it can be extracted from.
     */
    boolean canExtractFromTank(int index);

    /**
     * Checks if the current tank can be inserted into.
     * @param index The tank index.
     * @return If it can be inserted into.
     */
    boolean canInsertInto(int index);

    /**
     * Handles item usage relating to the tank. Will try to fill/empty containers, depending on the button used to click.
     * @param index The tank index.
     * @param button The button used to interact with the tank.
     */
    default void onItemUse(int index, int button) {
        var stack = getCarriedItem();
        if (stack.isEmpty()) {
            return;
        }
        var player = getServerPlayer();
        var handler = ContainerItemContext.ofPlayerCursor(player, player.containerMenu).find(FluidStorage.ITEM);
        if (handler == null) {
            return;
        }

        var tank = getTank();
        if (tank == null) return;

        boolean isBucket = stack.getItem() instanceof BucketItem;
        // button: 0 = left, 1 = right (see FluidTankSlot)
        if ((!isBucket && button == 0)
                || (isBucket && FluidContainers.getFirstStackContained(stack).isEmpty())) {
            // Fill the container from the tank
            if (!canExtractFromTank(index)) return;

            var genStack = tank.getStack(index);
            if (genStack != null && genStack.what() instanceof AEFluidKey fluidKey) {
                var extracted = Math.min(genStack.amount(), FluidConstants.BUCKET);

                try (var tx = Transaction.openOuter()) {
                    long inserted = handler.insert(fluidKey.toVariant(), extracted, tx);
                    if (inserted == 0) {
                        return;
                    }

                    var endAmount = genStack.amount() - inserted;
                    if (endAmount > 0) {
                        tank.setStack(index, new GenericStack(genStack.what(), endAmount));
                    } else {
                        tank.setStack(index, null);
                    }
                    tx.commit();

                    ServerPlayNetworking.send(player, new FluidTankClientAudioPacket(true));
                }
            }
        } else {
            // Empty the container into the tank
            if (!canInsertInto(index)) return;
            var fluid = FluidContainers.getFirstStackContained(stack);
            if (fluid.isEmpty()) return;

            var what = fluid.toKey();
            try (var tx = Transaction.openOuter()) {
                long extracted = handler.extract(fluid.getVariant(), FluidConstants.BUCKET, tx);
                if (extracted == 0) {
                    return;
                }

                var inserted = tank.insert(index, what, extracted, Actionable.MODULATE);
                if (inserted < extracted) {
                    // The tank couldn't take everything: don't empty the container
                    if (inserted > 0) {
                        tank.extract(index, what, inserted, Actionable.MODULATE);
                    }
                    return;
                }

                tx.commit();

                ServerPlayNetworking.send(player, new FluidTankClientAudioPacket(true));
            }
        }
    }
}
