/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.DoubleStream;

import static net.daporkchop.ccpregen.PregenState.*;

/**
 * @author DaPorkchop_
 */
public class PregenerationWorker implements WorldWorkerManager.IWorker {
    private static final Method CUBEPROVIDERSERVER_TRYUNLOADCUBE;
    private static final Method CUBEPROVIDERSERVER_CUBESITERATOR;
    private static final Field CUBEPROVIDERSERVER_CUBEMAP;
    private static final Object[] SINGLETON_ARRAY = new Object[1];

    private static final boolean ASYNC_TERRAIN;

    static {
        try {
            CUBEPROVIDERSERVER_TRYUNLOADCUBE = CubeProviderServer.class.getDeclaredMethod("tryUnloadCube", Cube.class);
            CUBEPROVIDERSERVER_TRYUNLOADCUBE.setAccessible(true);

            CUBEPROVIDERSERVER_CUBESITERATOR = CubeProviderServer.class.getDeclaredMethod("cubesIterator");
            CUBEPROVIDERSERVER_CUBESITERATOR.setAccessible(true);

            CUBEPROVIDERSERVER_CUBEMAP = CubeProviderServer.class.getDeclaredField("cubeMap");
            CUBEPROVIDERSERVER_CUBEMAP.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ModContainer cubicchunks = Loader.instance().getIndexedModList().get(CubicChunks.MODID);
        String asyncVersion = "1.12.2-0.0.1175.0"; //the version at which the async terrain gen api was added
        ASYNC_TERRAIN = asyncVersion.compareTo(cubicchunks.getVersion()) <= 0;
    }

    private final ICommandSender sender;
    private long lastMsg = System.currentTimeMillis();
    private final double[] speeds = new double[10];
    private int gennedSinceLastNotification = 0;
    private int pendingAsync;
    private WorldServer world;
    private boolean keepingLoaded;
    private CubePos printedFailWarning;

    private Set<CubePos> doneCubes = new ObjectOpenHashSet<>();

    public PregenerationWorker(ICommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasWork() {
        return active && pos != null;
    }

    @Override
    public boolean doWork() {
        boolean generated = true;

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
                System.arraycopy(this.speeds, 0, this.speeds, 1, this.speeds.length - 1);
                this.speeds[0] = this.gennedSinceLastNotification * 1000.0d / (double) (System.currentTimeMillis() - this.lastMsg);

                this.sender.sendMessage(new TextComponentString(String.format(
                        "Generated %d/%d cubes (%.1f cubes/s), position: %s, save queue: %d" + (ASYNC_TERRAIN && PregenConfig.asyncPrefetchCount > 0 ? ", pending (async): %d" : ""),
                        PregenState.generated, volume.total, DoubleStream.of(this.speeds).sum() / this.speeds.length, pos, saveQueueSize, this.pendingAsync
                )));

                this.gennedSinceLastNotification = 0;
                this.lastMsg = System.currentTimeMillis();
            }
            if (saveQueueSize > PregenConfig.maxSaveQueueSize) { //don't do anything until the save queue can be flushed a bit
                return false;
            }

            if (!paused && this.hasWork()) {
                if (ASYNC_TERRAIN && PregenConfig.asyncPrefetchCount > 0) {
                    generated = this.generateCubeAsync(provider);
                } else {
                    generated = this.generateCubeBlocking(provider);
                }
            }
        }

        boolean hasWork = active && pos != null;
        if (!hasWork) {
            this.sender.sendMessage(new TextComponentString("Generation complete."));
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
        return !paused && hasWork && generated;
    }

    private boolean generateCubeAsync(ICubeProviderInternal.Server provider) {
        boolean generated = false;

        ICubeGenerator generator = ((ICubicWorldServer) this.world).getCubeGenerator();
        switch (this.poll(generator, pos.getX(), pos.getY(), pos.getZ())) {
            case READY: //generator reports the cube is ready to be generated
                this.doneCubes.remove(pos);

                //generate the cube
                generated = this.generateCubeBlocking(provider);
                break;
            case WAITING: //do nothing
                break;
            case FAIL: //the cube failed?!?
                if (!pos.equals(this.printedFailWarning)) {
                    this.printedFailWarning = pos;
                    this.sender.sendMessage(new TextComponentString("The generator reported that async prefetching of the cube at " + pos + " failed!")
                            .setStyle(new Style().setColor(TextFormatting.RED)));
                    this.sender.sendMessage(new TextComponentString("This is very likely to cause pregeneration to hang/stall indefinitely!")
                            .setStyle(new Style().setColor(TextFormatting.RED)));
                }
        }

        CubePos nextPos = pos;
        this.pendingAsync = 0;
        for (int i = 0; i < PregenConfig.asyncPrefetchCount && (nextPos = order.next(volume, nextPos)) != null; i++) {
            if (this.doneCubes.contains(nextPos)) {
                continue;
            }

            if (this.poll(generator, nextPos.getX(), nextPos.getY(), nextPos.getZ()) == ICubeGenerator.GeneratorReadyState.READY) {
                this.doneCubes.add(nextPos);
            } else {
                this.pendingAsync++;
            }
        }

        return generated;
    }

    private ICubeGenerator.GeneratorReadyState poll(ICubeGenerator generator, int x, int y, int z) {
        if (PregenConfig.requirement.ordinal() >= ICubeProviderServer.Requirement.POPULATE.ordinal()) {
            return generator.pollAsyncCubePopulator(x, y, z);
        } else {
            return generator.pollAsyncCubeGenerator(x, y, z);
        }
    }

    private boolean generateCubeBlocking(ICubeProviderInternal.Server provider) {
        //generate the chunk at the current position
        Cube cube = (Cube) ((ICubeProviderServer) provider).getCube(pos.getX(), pos.getY(), pos.getZ(), PregenConfig.requirement);

        this.postGenerateCube(provider, cube);

        return true;
    }

    private void postGenerateCube(ICubeProviderInternal.Server provider, Cube cube) {
        provider.getCubeIO().saveCube(cube);
        if (PregenConfig.immediateCubeUnload) {
            try {
                SINGLETON_ARRAY[0] = cube;
                if ((boolean) CUBEPROVIDERSERVER_TRYUNLOADCUBE.invoke(provider, SINGLETON_ARRAY)) {
                    ((XYZMap<Cube>) CUBEPROVIDERSERVER_CUBEMAP.get(provider)).remove(cube);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                SINGLETON_ARRAY[0] = null;
            }
        }
        if (generated % PregenConfig.unloadCubesInterval == 0L) {
            if (PregenConfig.unloadColumns) {
                ((ICubicWorldServer) this.world).unloadOldCubes();
            } else {
                try {
                    for (Iterator<Cube> itr = (Iterator<Cube>) CUBEPROVIDERSERVER_CUBESITERATOR.invoke(provider); itr.hasNext(); ) {
                        SINGLETON_ARRAY[0] = itr.next();
                        if ((boolean) CUBEPROVIDERSERVER_TRYUNLOADCUBE.invoke(provider, SINGLETON_ARRAY)) {
                            itr.remove();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    SINGLETON_ARRAY[0] = null;
                }
            }
        }

        pos = order.next(volume, pos);
        this.gennedSinceLastNotification++;

        if (++generated % PregenConfig.saveStateInterval == 0) {
            persistState();
        }
    }
}
