/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.ccpregen;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * @author DaPorkchop_
 */
public class PregenerationWorker implements WorldWorkerManager.IWorker {
    private final ICommandSender sender;
    private final int dim;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private int y;
    private int x;
    private int z;
    private long remaining;
    private final long total;
    private long lastMsg = System.currentTimeMillis();
    private Boolean keepingLoaded;

    public PregenerationWorker(ICommandSender sender, BlockPos min, BlockPos max, int dim) {
        this.sender = sender;
        this.dim = dim;
        this.minX = (min.getX() >> 4) - 1;
        this.minY = (min.getY() >> 4) - 1;
        this.minZ = (min.getZ() >> 4) - 1;
        this.maxX = (max.getX() >> 4) + 1;
        this.maxY = (max.getY() >> 4) + 1;
        this.maxZ = (max.getZ() >> 4) + 1;
        this.y = this.maxY;
        this.x = this.minX;
        this.z = this.maxZ;
        this.remaining = this.total = (long) (this.maxX - this.minX) * (long) (this.maxY - this.minY) * (long) (this.maxZ - this.minZ);
    }

    @Override
    public boolean hasWork() {
        return this.remaining > 0L;
    }

    @Override
    public boolean doWork() {
        WorldServer world = DimensionManager.getWorld(this.dim);
        if (world == null) {
            DimensionManager.initDimension(this.dim);
            world = DimensionManager.getWorld(this.dim);
            if (world == null) {
                this.sender.sendMessage(new TextComponentString("Unable to load dimension " + this.dim));
                this.remaining = 0L;
                return false;
            }
        }

        ICubeProviderServer provider = ((ICubicWorldServer) world).getCubeCache();
        int saveQueueSize = ((ICubeProviderInternal.Server) provider).getCubeIO().getPendingCubeCount();

        if (this.lastMsg + PregenConfig.notificationInterval < System.currentTimeMillis()) {
            this.lastMsg = System.currentTimeMillis();
            this.sender.sendMessage(new TextComponentString(String.format(
                    "Generated %d/%d cubes, current block Y: %d, save queue size: %d",
                    this.total - this.remaining, this.total, this.y << 4, saveQueueSize
            )));
        } else if (saveQueueSize > PregenConfig.maxSaveQueueSize) {
            return false;
        }

        if (this.keepingLoaded == null) {
            this.keepingLoaded = DimensionManager.keepDimensionLoaded(this.dim, true);
        }

        if (this.hasWork()) {
            //generate the chunk at the current position
            ICube cube = provider.getCube(this.x, this.y, this.z, ICubeProviderServer.Requirement.POPULATE);
            if (!cube.isFullyPopulated()) {
                throw new IllegalStateException("Cube isn't fully populated!");
            }

            ((ICubeProviderInternal.Server) provider).getCubeIO().saveCube((Cube) cube);
            if ((this.remaining & 8191L) == 0L) {
                ((ICubicWorldServer) world).unloadOldCubes(); //avoid OOM
            }

            if (++this.z > this.maxZ) {
                if (++this.x > this.maxX) {
                    if (--this.y < this.minY) {
                        if (this.remaining > 1L) {
                            throw new IllegalStateException(this.remaining + " chunks remaining when we were finished!");
                        }
                    }
                    this.x = this.minX;
                }
                this.z = this.minZ;
            }
        }

        boolean hasWork = --this.remaining > 0L;
        if (!hasWork) {
            this.sender.sendMessage(new TextComponentString("Generation complete."));
            if (this.keepingLoaded != null && this.keepingLoaded) {
                DimensionManager.keepDimensionLoaded(this.dim, false);
            }
            ((ICubicWorldServer) world).unloadOldCubes();
        }
        return hasWork;
    }
}
