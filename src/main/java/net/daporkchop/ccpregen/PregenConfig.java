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

import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import net.daporkchop.ccpregen.util.CoordinateOrder;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author DaPorkchop_
 */
@Config(modid = CCPregen.MODID)
@Mod.EventBusSubscriber(modid = CCPregen.MODID)
public class PregenConfig {
    @Config.Comment("The period (in milliseconds) between progress notification messages.")
    @Config.RangeInt(min = 1)
    public static int notificationInterval = 5000;

    @Config.Comment({
            "The maximum number of cubes that may be queued for saving at any one time.",
            "If more than this number of cubes are queued, pregeneration will be paused until the number drops below this threshold again."
    })
    @Config.RangeInt(min = 0)
    public static int maxSaveQueueSize = 10000;

    @Config.Comment("Defines how much work the pregenerator will do before considering a cube to be generated.")
    public static ICubeProviderServer.Requirement requirement = ICubeProviderServer.Requirement.LIGHT;

    @Config.Comment("The period (in generated cubes) between saves of the current state.")
    @Config.RangeInt(min = 1)
    public static int saveStateInterval = 30000;

    @Config.Comment({
            "The period (in generated cubes) between unloading of all cubes.",
            "Setting this value too low can seriously degrade performance, setting it too high can cause the server to run out of memory."
    })
    @Config.RangeInt(min = 1)
    public static int unloadCubesInterval = 8000;

    @Config.Comment({
            "Whether or not columns should also be automatically unloaded every unloadCubesInterval.",
            "This can provide significant performance benefits when pregenerating very tall areas, since serialization of columns becomes increasingly",
            "expensive as the heightmap grows. However, for wider areas this can cause high memory usage, and even cause the JVM to completely run out",
            "of memory."
    })
    public static boolean unloadColumns = true;

    @Config.Comment({
            "The order in which cubes will be generated.",
            "Slices are 1-cube-tall horizontal planes, columns should be self-explanatory :P",
            "Cannot be updated retroactively on an already running task."
    })
    public static CoordinateOrder order = CoordinateOrder.SLICES_TOP_TO_BOTTOM;

    @Config.Comment({
            "Whether or not cubes should be unloaded immediately after they are generated.",
            "This will cause significant slowdown if the generation requirement is set to anything other than GENERATE, and will likely cause significant",
            "benefits for GENERATE."
    })
    public static boolean immediateCubeUnload = false;

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(CCPregen.MODID)) {
            ConfigManager.sync(CCPregen.MODID, Config.Type.INSTANCE);
        }
    }
}
