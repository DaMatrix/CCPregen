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

package net.daporkchop.ccpregen.util;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.ChunkPos;

import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;

/**
 * Ported from <a href="https://github.com/jakubcerveny/gilbert/>https://github.com/jakubcerveny/gilbert/</a>.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class Hilbert {
    public static Stream<ChunkPos> hilbert2d(int x, int z, int sizeX, int sizeZ) {
        if (sizeX >= sizeZ) {
            return generate2d(x, z, sizeX, 0, 0, sizeZ).get();
        } else {
            return generate2d(x, z, 0, sizeZ, sizeX, 0).get();
        }
    }

    private static Supplier<Stream<ChunkPos>> generate2d(int x, int y, int ax, int ay, int bx, int by) {
        int w = abs(ax + ay);
        int h = abs(bx + by);

        int dax = sgn(ax);
        int day = sgn(ay);
        int dbx = sgn(bx);
        int dby = sgn(by);

        //trivial row/column fills
        if (h == 1) {
            return () -> IntStream.range(0, w).mapToObj(i -> new ChunkPos(x + i * dax, y + i * day));
        } else if (w == 1) {
            return () -> IntStream.range(0, h).mapToObj(i -> new ChunkPos(x + i * dbx, y + i * dby));
        }

        int ax2 = ax >> 1;
        int ay2 = ay >> 1;
        int bx2 = bx >> 1;
        int by2 = by >> 1;

        int w2 = abs(ax2 + ay2);
        int h2 = abs(bx2 + by2);

        //prefer even steps
        //TODO: the original implementation placed these inside the if{}else{} blocks, but didn't for 3d. figure out if this is necessary
        if ((w2 & 1) != 0 && w > 2) {
            ax2 += dax;
            ay2 += day;
        }
        if ((h2 & 1) != 0 && h > 2) {
            bx2 += dbx;
            by2 += dby;
        }

        //i need to make these final to access them inside the lambda
        //damn you java
        int f_ax2 = ax2;
        int f_ay2 = ay2;
        int f_bx2 = bx2;
        int f_by2 = by2;

        if (w * 2 > h * 3) {
            return () -> concat(
                    generate2d(x, y, f_ax2, f_ay2, bx, by),
                    generate2d(x + f_ax2, y + f_ay2, ax - f_ax2, ay - f_ay2, bx, by));
        } else {
            return () -> concat(
                    generate2d(x, y, f_bx2, f_by2, f_ax2, f_ay2),
                    generate2d(x + f_bx2, y + f_by2, ax, ay, bx - f_bx2, by - f_by2),
                    generate2d(x + (ax - dax) + (f_bx2 - dbx), y + (ay - day) + (f_by2 - dby),
                            -f_bx2, -f_by2, -(ax - f_ax2), -(ay - f_ay2)));
        }
    }

    public static Stream<CubePos> hilbert3d(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        if (sizeX >= max(sizeY, sizeZ)) {
            return generate3d(x, y, z,
                    sizeX, 0, 0,
                    0, sizeY, 0,
                    0, 0, sizeZ).get();
        } else if (sizeY >= max(sizeX, sizeZ)) {
            return generate3d(x, y, z,
                    0, sizeY, 0,
                    sizeX, 0, 0,
                    0, 0, sizeZ).get();
        } else {
            return generate3d(x, y, z,
                    0, 0, sizeZ,
                    sizeX, 0, 0,
                    0, sizeY, 0).get();
        }
    }

    private static Supplier<Stream<CubePos>> generate3d(int x, int y, int z, int ax, int ay, int az, int bx, int by, int bz, int cx, int cy, int cz) {
        int w = abs(ax + ay + az);
        int h = abs(bx + by + bz);
        int d = abs(cx + cy + cz);

        int dax = sgn(ax);
        int day = sgn(ay);
        int daz = sgn(az);
        int dbx = sgn(bx);
        int dby = sgn(by);
        int dbz = sgn(bz);
        int dcx = sgn(cx);
        int dcy = sgn(cy);
        int dcz = sgn(cz);

        //trivial row/column fills
        if (h == 1 && d == 1) {
            return () -> IntStream.range(0, w).mapToObj(i -> new CubePos(x + i * dax, y + i * day, z + i * daz));
        } else if (w == 1 && d == 1) {
            return () -> IntStream.range(0, h).mapToObj(i -> new CubePos(x + i * dbx, y + i * dby, z + i * dbz));
        } else if (w == 1 && h == 1) {
            return () -> IntStream.range(0, d).mapToObj(i -> new CubePos(x + i * dcx, y + i * dcy, z + i * dcz));
        }

        int ax2 = ax >> 1;
        int ay2 = ay >> 1;
        int az2 = az >> 1;
        int bx2 = bx >> 1;
        int by2 = by >> 1;
        int bz2 = bz >> 1;
        int cx2 = cx >> 1;
        int cy2 = cy >> 1;
        int cz2 = cz >> 1;

        int w2 = abs(ax2 + ay2 + az2);
        int h2 = abs(bx2 + by2 + bz2);
        int d2 = abs(cx2 + cy2 + cz2);

        //prefer even steps
        if ((w2 & 1) != 0 && w > 2) {
            ax2 += dax;
            ay2 += day;
            az2 += daz;
        }
        if ((h2 & 1) != 0 && h > 2) {
            bx2 += dbx;
            by2 += dby;
            bz2 += dbz;
        }
        if ((d2 & 1) != 0 && d > 2) {
            cx2 += dcx;
            cy2 += dcy;
            cz2 += dcz;
        }

        //i need to make these final to access them inside the lambda
        //damn you java
        int f_ax2 = ax2;
        int f_ay2 = ay2;
        int f_az2 = az2;
        int f_bx2 = bx2;
        int f_by2 = by2;
        int f_bz2 = bz2;
        int f_cx2 = cx2;
        int f_cy2 = cy2;
        int f_cz2 = cz2;

        if (w * 2 > h * 3 && w * 2 > d * 3) { //wide case, split in w only
            return () -> concat(
                    generate3d(x, y, z,
                            f_ax2, f_ay2, f_az2,
                            bx, by, bz,
                            cx, cy, cz),
                    generate3d(x + f_ax2, y + f_ay2, z + f_az2,
                            ax - f_ax2, ay - f_ay2, az - f_az2,
                            bx, by, bz,
                            cx, cy, cz));
        } else if (h * 3 > d * 4) { //do not split in d
            return () -> concat(
                    generate3d(x, y, z,
                            f_bx2, f_by2, f_bz2,
                            cx, cy, cz,
                            f_ax2, f_ay2, f_az2),
                    generate3d(x + f_bx2, y + f_by2, z + f_bz2,
                            ax, ay, az,
                            bx - f_bx2, by - f_by2, bz - f_bz2,
                            cx, cy, cz),
                    generate3d(x + (ax - dax) + (f_bx2 - dbx),
                            y + (ay - day) + (f_by2 - dby),
                            z + (az - daz) + (f_bz2 - dbz),
                            -f_bx2, -f_by2, -f_bz2,
                            cx, cy, cz,
                            -(ax - f_ax2), -(ay - f_ay2), -(az - f_az2)));
        } else if (d * 3 > h * 4) { //do not split in h
            return () -> concat(
                    generate3d(x, y, z,
                            f_cx2, f_cy2, f_cz2,
                            f_ax2, f_ay2, f_az2,
                            bx, by, bz),
                    generate3d(x + f_cx2, y + f_cy2, z + f_cz2,
                            ax, ay, az,
                            bx, by, bz,
                            cx - f_cx2, cy - f_cy2, cz - f_cz2),
                    generate3d(x + (ax - dax) + (f_cx2 - dcx),
                            y + (ay - day) + (f_cy2 - dcy),
                            z + (az - daz) + (f_cz2 - dcz),
                            -f_cx2, -f_cy2, -f_cz2,
                            -(ax - f_ax2), -(ay - f_ay2), -(az - f_az2),
                            bx, by, bz));
        } else { //regular case, split in all w/h/d
            return () -> concat(
                    generate3d(x, y, z,
                            f_bx2, f_by2, f_bz2,
                            f_cx2, f_cy2, f_cz2,
                            f_ax2, f_ay2, f_az2),
                    generate3d(x + f_bx2, y + f_by2, z + f_bz2,
                            cx, cy, cz,
                            f_ax2, f_ay2, f_az2,
                            bx - f_bx2, by - f_by2, bz - f_bz2),
                    generate3d(x + (f_bx2 - dbx) + (cx - dcx),
                            y + (f_by2 - dby) + (cy - dcy),
                            z + (f_bz2 - dbz) + (cz - dcz),
                            ax, ay, az,
                            -f_bx2, -f_by2, -f_bz2,
                            -(cx - f_cx2), -(cy - f_cy2), -(cz - f_cz2)),
                    generate3d(x + (ax - dax) + f_bx2 + (cx - dcx),
                            y + (ay - day) + f_by2 + (cy - dcy),
                            z + (az - daz) + f_bz2 + (cz - dcz),
                            -cx, -cy, -cz,
                            -(ax - f_ax2), -(ay - f_ay2), -(az - f_az2),
                            bx - f_bx2, by - f_by2, bz - f_bz2),
                    generate3d(x + (ax - dax) + (f_bx2 - dbx),
                            y + (ay - day) + (f_by2 - dby),
                            z + (az - daz) + (f_bz2 - dbz),
                            -f_bx2, -f_by2, -f_bz2,
                            f_cx2, f_cy2, f_cz2,
                            -(ax - f_ax2), -(ay - f_ay2), -(az - f_az2)));
        }
    }

    private static int sgn(int i) {
        return min(max(i, -1), 1);
    }

    @SafeVarargs
    private static <T> Stream<T> concat(Supplier<Stream<T>>... args) {
        return Stream.of(args).flatMap(Supplier::get);
    }
}
