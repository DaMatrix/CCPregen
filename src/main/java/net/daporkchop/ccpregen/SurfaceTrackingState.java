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

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

/**
 * @author DaPorkchop_
 */
@Config(modid = CCPregen.MODID, category = "state_surfacetracking")
public class SurfaceTrackingState {
    @Config.Comment({
            "Whether or not the surface tracker is currently running.",
            "Set to false to abort an ongoing surface tracking task."
    })
    public static boolean active = false;
    public static boolean paused = false;

    public static int dim;

    public static boolean startSurfaceTracking(ICommandSender sender, int dimension) {
        if (active) {
            return false;
        } else {
            active = true;
        }

        paused = false;
        dim = dimension;

        persistState();
        WorldWorkerManager.addWorker(new SurfaceTrackingWorker(sender));
        return true;
    }

    public static void loadState(ICommandSender sender) {
        if (active) {
            sender.sendMessage(new TextComponentString("Resuming surface tracking..."));
            WorldWorkerManager.addWorker(new SurfaceTrackingWorker(sender));
        }
    }

    public static void persistState() {
        ConfigManager.sync(CCPregen.MODID, Config.Type.INSTANCE);
    }
}
