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

import net.daporkchop.ccpregen.command.PausePregenCommand;
import net.daporkchop.ccpregen.command.PregenCommand;
import net.daporkchop.ccpregen.command.PregenCubesCommand;
import net.daporkchop.ccpregen.command.ResumePregenCommand;
import net.daporkchop.ccpregen.command.StopPregenCommand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

@Mod(modid = CCPregen.MODID,
        useMetadata = true,
        dependencies = "required:cubicchunks@[1.12.2-0.0.1015.0,)",
        acceptableRemoteVersions = "*")
public class CCPregen {
    public static final String MODID = "ccpregen";

    public static Logger LOGGER;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        PermissionAPI.registerNode(MODID + ".command.ccpregen", DefaultPermissionLevel.OP, "Allows to run the /ccpregen command");
        PermissionAPI.registerNode(MODID + ".command.ccpregen_cubes", DefaultPermissionLevel.OP, "Allows to run the /ccpregen_cubes command");
        PermissionAPI.registerNode(MODID + ".command.ccpregen_stop", DefaultPermissionLevel.OP, "Allows to run the /ccpregen_stop command");
        PermissionAPI.registerNode(MODID + ".command.ccpregen_pause", DefaultPermissionLevel.OP, "Allows to run the /ccpregen_pause command");
        PermissionAPI.registerNode(MODID + ".command.ccpregen_resume", DefaultPermissionLevel.OP, "Allows to run the /ccpregen_resume command");

        event.registerServerCommand(new PregenCommand());
        event.registerServerCommand(new PregenCubesCommand());
        event.registerServerCommand(new StopPregenCommand());
        event.registerServerCommand(new PausePregenCommand());
        event.registerServerCommand(new ResumePregenCommand());

        PregenState.loadState(event.getServer());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event)    {
        PregenState.persistState();
    }
}
