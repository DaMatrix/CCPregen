/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.AsyncBatchingCubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.WorldWorkerManager;

import java.lang.reflect.Field;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.DoubleStream;

import static net.daporkchop.ccpregen.SurfaceTrackingState.*;

/**
 * @author DaPorkchop_
 */
public class SurfaceTrackingWorker implements WorldWorkerManager.IWorker {
    private final ICommandSender sender;
    private long lastMsg = System.currentTimeMillis();
    private final double[] speeds = new double[10];
    private int gennedSinceLastNotification = 0;
    private WorldServer world;
    private boolean keepingLoaded;

    private NavigableSet<CubePos> allCubePositions;
    private int skipped;
    private int totalCount;

    public SurfaceTrackingWorker(ICommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasWork() {
        return active && (this.allCubePositions == null || !this.allCubePositions.isEmpty());
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

            if (this.allCubePositions == null) {
                this.sender.sendMessage(new TextComponentString("Loading list of cubes to consider for surface tracking..."));
                this.allCubePositions = new TreeSet<>((a, b) -> {
                    int d = Integer.compare(a.getX(), b.getX());
                    if (d == 0 && (d = Integer.compare(a.getZ(), b.getZ())) == 0) {
                        d = -Integer.compare(a.getY(), b.getY());
                    }
                    return d;
                });

                try {
                    Field field = AsyncBatchingCubeIO.class.getDeclaredField("storage");
                    field.setAccessible(true);

                    ((ICubicStorage) field.get(provider.getCubeIO())).forEachCube(this.allCubePositions::add);
                    this.totalCount = this.allCubePositions.size();
                } catch (Throwable t) {
                    t.printStackTrace();
                    this.sender.sendMessage(new TextComponentString("Unable to list all cubes in the world! You may have to update to the latest version of Cubic Chunks! See the log for more information.")
                            .setStyle(new Style().setColor(TextFormatting.RED)));
                }
            }

            int saveQueueSize = provider.getCubeIO().getPendingCubeCount();
            if (this.lastMsg + PregenConfig.notificationInterval < System.currentTimeMillis()) {
                System.arraycopy(this.speeds, 0, this.speeds, 1, this.speeds.length - 1);
                this.speeds[0] = this.gennedSinceLastNotification * 1000.0d / (double) (System.currentTimeMillis() - this.lastMsg);

                this.sender.sendMessage(new TextComponentString(String.format(
                        "Surface tracked %d/%d cubes (%.1f cubes/s), skipped %d, save queue: %d",
                        this.totalCount - this.allCubePositions.size(), this.totalCount, DoubleStream.of(this.speeds).sum() / this.speeds.length, this.skipped, saveQueueSize
                )));

                this.gennedSinceLastNotification = 0;
                this.lastMsg = System.currentTimeMillis();
            }
            if (saveQueueSize > PregenConfig.maxSaveQueueSize) { //don't do anything until the save queue can be flushed a bit
                return false;
            }

            if (!paused && this.hasWork()) {
                CubePos pos = this.allCubePositions.pollFirst();
                Cube cube = provider.getCube(pos);

                if (cube.isFullyPopulated() && !cube.isSurfaceTracked()) { //force the cube to be surface tracked
                    cube.trackSurface();
                    PregenerationWorker.postGenerateCube(this.world, provider, cube, (this.totalCount - this.allCubePositions.size()) % PregenConfig.unloadCubesInterval == 0L);
                } else {
                    this.skipped++;
                }
                this.gennedSinceLastNotification++;
            }
        }

        boolean hasWork = this.hasWork();
        if (!hasWork) {
            this.sender.sendMessage(new TextComponentString("Surface tracking complete."));
            if (this.world != null) {
                ((ICubicWorldServer) this.world).unloadOldCubes();
                if (this.keepingLoaded) {
                    //allow world to be unloaded
                    DimensionManager.keepDimensionLoaded(dim, false);
                }
            }
            active = false;
            persistState();
        }
        return !paused && hasWork;
    }
}
