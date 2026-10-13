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

package appeng.util.fluid;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import appeng.api.stacks.AEFluidKey;

/**
 * Helps with playing fill/empty sounds for fluids to players.
 */
public final class FluidSoundHelper {

    private FluidSoundHelper() {
    }

    public static void playFillSound(Player player, @Nullable AEFluidKey fluid) {
        if (fluid == null) {
            return;
        }

        playSound(player, FluidVariantAttributes.getFillSound(fluid.toVariant()));
    }

    public static void playEmptySound(Player player, @Nullable AEFluidKey fluid) {
        if (fluid == null) {
            return;
        }

        playSound(player, FluidVariantAttributes.getEmptySound(fluid.toVariant()));
    }

    /**
     * Plays the given sound at the player's position.
     */
    private static void playSound(Player player, SoundEvent fillSound) {
        // TODO 1.21.11: This now plays it for everyone.
        // This should just play the sound for the player themselves
        player.playSound(fillSound);
    }

}
