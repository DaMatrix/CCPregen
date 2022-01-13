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
import net.daporkchop.ccpregen.util.CoordinateOrder;
import net.daporkchop.ccpregen.util.Volume;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

import java.util.Iterator;

/**
 * @author DaPorkchop_
 */
@Config(modid = CCPregen.MODID, category = "state")
public class PregenState {
    @Config.Comment({
            "Whether or not the pregenerator is currently running.",
            "Set to false to abort an ongoing pregeneration task."
    })
    public static boolean active = false;
    public static boolean paused = false;

    public static int dim;

    @Config.Name("minX")
    public static int _minX;
    @Config.Name("minY")
    public static int _minY;
    @Config.Name("minZ")
    public static int _minZ;
    @Config.Name("maxX")
    public static int _maxX;
    @Config.Name("maxY")
    public static int _maxY;
    @Config.Name("maxZ")
    public static int _maxZ;

    //these fields are a hack because forge doesn't support long fields for config
    @Config.Name("generated")
    public static String _generated_as_string = ""; //not to be used directly

    public static CoordinateOrder order = PregenConfig.order;

    @Config.Ignore
    public static Volume volume;
    @Config.Ignore
    public static Iterator<CubePos> iterator;
    @Config.Ignore
    public static long generated;

    public static boolean startPregeneration(ICommandSender sender, BlockPos min, BlockPos max, int dimension) {
        return startPregenerationCubes(sender,
                new CubePos(min.getX() >> 4, min.getY() >> 4, min.getZ() >> 4),
                new CubePos(max.getX() >> 4, max.getY() >> 4, max.getZ() >> 4).add(1, 1, 1),
                dimension);
    }

    public static boolean startPregenerationCubes(ICommandSender sender, CubePos min, CubePos max, int dimension) {
        if (active) {
            return false;
        } else {
            active = true;
        }

        paused = false;
        dim = dimension;

        volume = new Volume(
                _minX = min.getX(), _minY = min.getY(), _minZ = min.getZ(),
                _maxX = max.getX(), _maxY = max.getY(), _maxZ = max.getZ());
        order = PregenConfig.order;

        generated = 0L;
        iterator = order.iterator(volume);

        persistState();
        WorldWorkerManager.addWorker(new PregenerationWorker(sender));
        return true;
    }

    public static void loadState(ICommandSender sender) {
        if (active) {
            sender.sendMessage(new TextComponentString("Resuming pregeneration..."));

            //restore non-serialized objects
            volume = new Volume(_minX, _minY, _minZ, _maxX, _maxY, _maxZ);
            generated = Long.parseLong(_generated_as_string);

            //restore iterator and seek to last generated position
            iterator = order.iterator(volume);
            for (long l = 0L; l < generated; l++) {
                iterator.next();
            }

            WorldWorkerManager.addWorker(new PregenerationWorker(sender));
        }
    }

    public static void persistState() {
        _generated_as_string = String.valueOf(generated);

        ConfigManager.sync(CCPregen.MODID, Config.Type.INSTANCE);
    }
}
