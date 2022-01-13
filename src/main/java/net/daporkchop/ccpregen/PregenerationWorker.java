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
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import lombok.SneakyThrows;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.stream.DoubleStream;

import static net.daporkchop.ccpregen.PregenState.*;

/**
 * @author DaPorkchop_
 */
public class PregenerationWorker implements WorldWorkerManager.IWorker {
    private static final MethodHandle CUBEPROVIDERSERVER_TRYUNLOADCUBE;
    private static final MethodHandle CUBEPROVIDERSERVER_CUBESITERATOR;
    private static final MethodHandle CUBEPROVIDERSERVER_CUBEMAP;
    private static final Object[] SINGLETON_ARRAY = new Object[1];

    private static final boolean ASYNC_TERRAIN;

    static {
        try {
            Method cubeProviderServer_tryUnloadCube = CubeProviderServer.class.getDeclaredMethod("tryUnloadCube", Cube.class);
            cubeProviderServer_tryUnloadCube.setAccessible(true);
            CUBEPROVIDERSERVER_TRYUNLOADCUBE = MethodHandles.lookup().unreflect(cubeProviderServer_tryUnloadCube);

            Method cubeProviderServer_cubesIterator = CubeProviderServer.class.getDeclaredMethod("cubesIterator");
            cubeProviderServer_cubesIterator.setAccessible(true);
            CUBEPROVIDERSERVER_CUBESITERATOR = MethodHandles.lookup().unreflect(cubeProviderServer_cubesIterator);

            Field cubeProviderServer_cubeMap = CubeProviderServer.class.getDeclaredField("cubeMap");
            cubeProviderServer_cubeMap.setAccessible(true);
            CUBEPROVIDERSERVER_CUBEMAP = MethodHandles.lookup().unreflectGetter(cubeProviderServer_cubeMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ModContainer cubicchunks = Loader.instance().getIndexedModList().get(CubicChunks.MODID);
        String asyncVersion = "1.12.2-0.0.1175.0"; //the version at which the async terrain gen api was added
        ASYNC_TERRAIN = asyncVersion.compareTo(cubicchunks.getVersion()) <= 0;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected static void postGenerateCube(WorldServer world, CubeProviderServer provider, Cube cube, boolean unloadAll) {
        //save the cube if configured
        if (PregenConfig.immediateCubeSave) {
            provider.getCubeIO().saveCube(cube);
        }

        //unload the cube if configured
        if (PregenConfig.immediateCubeUnload) {
            try {
                SINGLETON_ARRAY[0] = cube;
                if ((boolean) CUBEPROVIDERSERVER_TRYUNLOADCUBE.invokeExact(provider, cube)) {
                    ((XYZMap<Cube>) CUBEPROVIDERSERVER_CUBEMAP.invokeExact(provider)).remove(cube);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                SINGLETON_ARRAY[0] = null;
            }
        }

        //unload everything if requested
        if (unloadAll) {
            if (PregenConfig.unloadColumns) {
                ((ICubicWorldServer) world).unloadOldCubes();
            } else {
                try {
                    for (Iterator<Cube> itr = (Iterator<Cube>) CUBEPROVIDERSERVER_CUBESITERATOR.invokeExact(provider); itr.hasNext(); ) {
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
    }

    private final ICommandSender sender;
    private long lastMsg = System.currentTimeMillis();
    private final double[] speeds = new double[10];
    private int gennedSinceLastNotification = 0;
    private WorldServer world;
    private boolean keepingLoaded;
    private CubePos printedFailWarning;

    private final Queue<CubePos> waitingPositions = new ArrayDeque<>();

    public PregenerationWorker(ICommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean hasWork() {
        return active && (!this.waitingPositions.isEmpty() || iterator.hasNext());
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

            CubeProviderServer provider = (CubeProviderServer) ((ICubicWorldServer) this.world).getCubeCache();
            int saveQueueSize = provider.getCubeIO().getPendingCubeCount();

            if (this.lastMsg + PregenConfig.notificationInterval < System.currentTimeMillis()) {
                System.arraycopy(this.speeds, 0, this.speeds, 1, this.speeds.length - 1);
                this.speeds[0] = this.gennedSinceLastNotification * 1000.0d / (double) (System.currentTimeMillis() - this.lastMsg);

                this.sender.sendMessage(new TextComponentString(String.format(
                        "Generated %d/%d cubes (%.1f cubes/s), save queue: %d",
                        PregenState.generated, volume.total, DoubleStream.of(this.speeds).sum() / this.speeds.length, saveQueueSize
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

        boolean hasWork = this.hasWork();
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

    private boolean generateCubeAsync(CubeProviderServer provider) {
        ICubeGenerator generator = ((ICubicWorldServer) this.world).getCubeGenerator();

        //fill up the queue if it isn't already full
        while (this.waitingPositions.size() < PregenConfig.asyncPrefetchCount && iterator.hasNext()) {
            //add the position to the queue
            CubePos pos = iterator.next();
            this.waitingPositions.add(pos);

            //poll the generator to prefetch it
            this.poll(generator, pos.getX(), pos.getY(), pos.getZ());
        }

        //check if the first queued position is ready to be generated
        CubePos pos = this.waitingPositions.peek();
        switch (this.poll(generator, pos.getX(), pos.getY(), pos.getZ())) {
            case READY: //generator reports the cube is ready to be generated
                //remove position from the queue
                this.waitingPositions.poll();

                //generate the cube
                this.generateCube(provider, pos);
                return true;
            case WAITING: //do nothing
                return false;
            case FAIL: //the cube failed?!?
                if (!pos.equals(this.printedFailWarning)) {
                    this.printedFailWarning = pos;
                    this.sender.sendMessage(new TextComponentString("The generator reported that async prefetching of the cube at " + pos + " failed!")
                            .setStyle(new Style().setColor(TextFormatting.RED)));
                    this.sender.sendMessage(new TextComponentString("This is very likely to cause pregeneration to hang/stall indefinitely!")
                            .setStyle(new Style().setColor(TextFormatting.RED)));
                }
                return false;
            default:
                throw new IllegalStateException();
        }
    }

    private ICubeGenerator.GeneratorReadyState poll(ICubeGenerator generator, int x, int y, int z) {
        if (PregenConfig.requirement.ordinal() >= ICubeProviderServer.Requirement.POPULATE.ordinal()) {
            return generator.pollAsyncCubePopulator(x, y, z);
        } else {
            return generator.pollAsyncCubeGenerator(x, y, z);
        }
    }

    private boolean generateCubeBlocking(CubeProviderServer provider) {
        //generate the chunk at the current position
        this.generateCube(provider, iterator.next());
        return true;
    }

    private void generateCube(CubeProviderServer provider, CubePos pos) {
        Cube cube = provider.getCube(pos.getX(), pos.getY(), pos.getZ(), PregenConfig.requirement);
        this.postGenerateCube(provider, cube);
    }

    private void postGenerateCube(CubeProviderServer provider, Cube cube) {
        postGenerateCube(this.world, provider, cube, generated % PregenConfig.unloadCubesInterval == 0L);

        this.gennedSinceLastNotification++;

        if (++generated % PregenConfig.saveStateInterval == 0) {
            persistState();
        }
    }
}
