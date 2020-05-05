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

package net.daporkchop.ccpregen.command;

import net.daporkchop.ccpregen.CCPregen;
import net.daporkchop.ccpregen.PregenState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.server.permission.PermissionAPI;

/**
 * @author DaPorkchop_
 */
public class PregenCommand extends CommandBase {
    public static BlockPos parseBlockPos(String[] args, int startIndex) throws NumberInvalidException {
        return new BlockPos(
                parseDouble(0.0d, args[startIndex], -30000000, 30000000, false),
                parseDouble(0.0d, args[startIndex + 1], Integer.MIN_VALUE, Integer.MAX_VALUE, false),
                parseDouble(0.0d, args[startIndex + 2], -30000000, 30000000, false));
    }

    @Override
    public String getName() {
        return "ccpregen";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ccpregen <minX> <minY> <minZ> <maxX> <maxY> <maxZ> [dimension]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 6)    {
            sender.sendMessage(new TextComponentString(this.getUsage(sender)));
            return;
        }
        BlockPos min = parseBlockPos(args, 0);
        BlockPos max = parseBlockPos(args, 3);
        int dimension = args.length == 6 ? sender.getEntityWorld().provider.getDimension() : parseInt(args[7]);
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ())  {
            sender.sendMessage(new TextComponentString("Min coordinates may not be greater than max coordinates!"));
        } else if (!PregenState.startPregeneration(sender, min, max, dimension))   {
            sender.sendMessage(new TextComponentString("A pregeneration task is already running!"));
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer) sender, CCPregen.MODID + ".command.ccpregen");
        } else {
            return super.checkPermission(server, sender);
        }
    }
}
