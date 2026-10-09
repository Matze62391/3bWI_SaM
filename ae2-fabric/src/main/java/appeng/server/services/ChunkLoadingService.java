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

package appeng.server.services;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.blockentity.spatial.SpatialAnchorBlockEntity;

/**
 * Forces chunks to stay loaded for spatial anchors. NeoForge's ticket controllers do not exist on Fabric, so the
 * forced chunks are tracked in {@link ChunkLoadState} and forced using the vanilla mechanism.
 */
public class ChunkLoadingService {

    private static final ChunkLoadingService INSTANCE = new ChunkLoadingService();

    // Flag to ignore a server after it is stopping as grid nodes might reevaluate their grids during a shutdown.
    private boolean running = true;

    public void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.running = true);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.running = false);
        ServerLevelEvents.LOAD.register((server, level) -> validateTickets(level));
    }

    public static ChunkLoadingService getInstance() {
        return INSTANCE;
    }

    public void validateTickets(ServerLevel level) {
        var state = ChunkLoadState.get(level);

        for (var entry : state.getAllBlocks().entrySet()) {
            var blockPos = entry.getKey();
            var chunks = entry.getValue();
            BlockEntity blockEntity = level.getBlockEntity(blockPos);

            // Add all persisted chunks to the list of handled ones by each anchor.
            // Or remove all in case the anchor no longer exists.
            if (blockEntity instanceof SpatialAnchorBlockEntity anchor) {
                for (long chunk : chunks) {
                    anchor.registerChunk(ChunkPos.unpack(chunk));
                }
            } else {
                state.releaseAll(blockPos);
            }
        }
    }

    public boolean forceChunk(ServerLevel level, BlockPos owner, ChunkPos position) {
        if (running) {
            ChunkLoadState.get(level).forceChunk(position, owner);
            return true;
        }

        return false;
    }

    public boolean releaseChunk(ServerLevel level, BlockPos owner, ChunkPos position) {
        if (running) {
            ChunkLoadState.get(level).releaseChunk(position, owner);
            return true;
        }

        return false;
    }

    public boolean isChunkForced(ServerLevel level, int chunkX, int chunkZ) {
        return ChunkLoadState.get(level).isForceLoaded(chunkX, chunkZ);
    }

}
