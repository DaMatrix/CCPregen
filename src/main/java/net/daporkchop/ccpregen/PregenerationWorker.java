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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.WorldWorkerManager;

import static java.lang.Long.*;
import static net.daporkchop.ccpregen.PregenState.*;

/**
 * @author DaPorkchop_
 */
public class PregenerationWorker implements WorldWorkerManager.IWorker {
    private final ICommandSender sender;
    private long lastMsg = System.currentTimeMillis();
    private WorldServer world;
    private boolean keepingLoaded;

    public PregenerationWorker(ICommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasWork() {
        return active && parseUnsignedLong(generated) < parseUnsignedLong(total);
    }

    @Override
    public boolean doWork() {
        if (active) {
            if (this.world == null) {
                WorldServer world = DimensionManager.getWorld(dim);
                if (world == null) {
                    DimensionManager.initDimension(dim);
                    world = DimensionManager.getWorld(dim);
                    if (world == null) {
                        this.sender.sendMessage(new TextComponentString("Unable to load dimension " + dim));
                        active = false;
                        return false;
                    }
                }
                this.world = world;
                this.keepingLoaded = DimensionManager.keepDimensionLoaded(dim, true);
            }

            ICubeProviderInternal.Server provider = (ICubeProviderInternal.Server) ((ICubicWorldServer) this.world).getCubeCache();
            int saveQueueSize = provider.getCubeIO().getPendingCubeCount();

            if (this.lastMsg + PregenConfig.notificationInterval < System.currentTimeMillis()) {
                this.lastMsg = System.currentTimeMillis();
                this.sender.sendMessage(new TextComponentString(String.format(
                        "Generated %s/%s cubes, current block position: (%d, %d, %d), save queue size: %d",
                        generated, total, x << 4, y << 4, z << 4, saveQueueSize
                )));
            }
            if (saveQueueSize > PregenConfig.maxSaveQueueSize) {
                return false;
            }

            if (this.hasWork()) {
                //generate the chunk at the current position
                ICube cube = ((ICubeProviderServer) provider).getCube(x, y, z, PregenConfig.requirement);
                if (!cube.isFullyPopulated()) {
                    throw new IllegalStateException("Cube isn't fully populated!");
                }

                provider.getCubeIO().saveCube((Cube) cube);
                if (parseUnsignedLong(generated) % PregenConfig.unloadCubesInterval == 0L) {
                    ((ICubicWorldServer) this.world).unloadOldCubes(); //avoid OOM
                }

                if (++z > maxZ) {
                    if (++x > maxX) {
                        if (--y < minY) {
                            if (parseUnsignedLong(generated) < parseUnsignedLong(total) - 1L) {
                                throw new IllegalStateException(String.format("Iteration finished, but we only generated %s/%s cubes?!?", generated, total));
                            }
                        }
                        x = minX;
                    }
                    z = minZ;
                }
            }

            generated = String.valueOf(parseUnsignedLong(generated) + 1);
            if (parseUnsignedLong(generated) % PregenConfig.saveStateInterval == 0) {
                persistState();
            }
        }

        boolean hasWork = active && parseUnsignedLong(generated) < parseUnsignedLong(total);
        if (!hasWork) {
            this.sender.sendMessage(new TextComponentString("Generation complete."));
            if (this.world != null && this.keepingLoaded) {
                DimensionManager.keepDimensionLoaded(dim, false);
            }
            active = false;
            persistState();
            ((ICubicWorldServer) this.world).unloadOldCubes();
        }
        return hasWork;
    }
}
