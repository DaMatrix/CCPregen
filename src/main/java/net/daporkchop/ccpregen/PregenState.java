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

import net.daporkchop.ccpregen.util.CoordinateOrder;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

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

    public static int dim;
    public static int minX;
    public static int minY;
    public static int minZ;
    public static int maxX;
    public static int maxY;
    public static int maxZ;
    public static int y;
    public static int x;
    public static int z;

    //these fields are a hack because forge doesn't support long fields for config
    public static String generated = "";
    public static String total = "";

    public static CoordinateOrder order = CoordinateOrder.SLICES_TOP_TO_BOTTOM;

    public static boolean startPregeneration(ICommandSender sender, BlockPos min, BlockPos max, int dimension) {
        if (active) {
            return false;
        } else {
            active = true;
        }

        dim = dimension;
        minX = (min.getX() >> 4) - 1;
        minY = (min.getY() >> 4) - 1;
        minZ = (min.getZ() >> 4) - 1;
        maxX = (max.getX() >> 4) + 1;
        maxY = (max.getY() >> 4) + 1;
        maxZ = (max.getZ() >> 4) + 1;
        generated = "0";
        total = String.valueOf((long) (maxX - minX) * (long) (maxY - minY) * (long) (maxZ - minZ));
        (order = PregenConfig.order).init();

        persistState();
        WorldWorkerManager.addWorker(new PregenerationWorker(sender));
        return true;
    }

    public static void loadState(ICommandSender sender) {
        if (active) {
            sender.sendMessage(new TextComponentString("Resuming pregeneration..."));
            WorldWorkerManager.addWorker(new PregenerationWorker(sender));
        }
    }

    public static void persistState() {
        ConfigManager.sync(CCPregen.MODID, Config.Type.INSTANCE);
    }
}
