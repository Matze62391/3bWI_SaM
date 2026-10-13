package com.glodblock.github.appflux.util;

import appeng.api.config.Actionable;
import appeng.api.parts.IPartHost;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.parts.AEBasePart;
import com.glodblock.github.appflux.common.AFSingletons;
import com.glodblock.github.appflux.util.helpers.Constants;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.minecraft.world.entity.player.Player;
import team.reborn.energy.api.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class AFUtil {

    public static int clampLong(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    /**
     * The energy storage of a single item, without a container (NeoForge: {@code ItemAccess.forStack(stack).oneByOne()}).
     * Changes are written back to the given stack.
     */
    @Nullable
    public static EnergyStorage findEnergy(ItemStack stack) {
        if (!stack.isEmpty()) {
            return EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        }
        return null;
    }

    /**
     * Like {@link #findEnergy}, but changes made through the storage update the stack.
     */
    @Nullable
    public static EnergyStorage findEnergy(Player player, int slot) {
        var stack = player.getInventory().getItem(slot);
        if (!stack.isEmpty()) {
            var slots = PlayerInventoryStorage.of(player).getSlots();
            return EnergyStorage.ITEM.find(stack, ContainerItemContext.ofPlayerSlot(player, slots.get(slot)));
        }
        return null;
    }

    public static boolean shouldTryCast(BlockEntity tile, Direction side) {
        if (tile instanceof IUpgradeableObject upgradeable) {
            return upgradeable.isUpgradedWith(AFSingletons.INDUCTION_CARD);
        }
        if (tile instanceof IPartHost host) {
            if (host.getPart(side) instanceof IUpgradeableObject upgradeable) {
                return upgradeable.isUpgradedWith(AFSingletons.INDUCTION_CARD);
            }
        }
        return true;
    }

    public static Set<Direction> getSides(Object host) {
        if (host instanceof BlockEntity) {
            return Constants.ALL_DIRECTIONS;
        } else if (host instanceof AEBasePart part) {
            if (part.getSide() == null) {
                return Constants.NO_DIRECTIONS;
            }
            return Set.of(part.getSide());
        } else {
            return Constants.NO_DIRECTIONS;
        }
    }

    public static Actionable ofSim(boolean sim) {
        return sim ? Actionable.SIMULATE : Actionable.MODULATE;
    }

}
