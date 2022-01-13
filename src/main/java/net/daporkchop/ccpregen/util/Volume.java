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

import lombok.EqualsAndHashCode;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@EqualsAndHashCode
public final class Volume {
    public final int minX;
    public final int minY;
    public final int minZ;
    public final int maxX;
    public final int maxY;
    public final int maxZ;

    public final long total;

    public Volume(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = min(x1, x2);
        this.minY = min(y1, y2);
        this.minZ = min(z1, z2);
        this.maxX = max(x1, x2);
        this.maxY = max(y1, y2);
        this.maxZ = max(z1, z2);

        this.total = ((long) this.maxX - this.minX + 1L) * ((long) this.maxY - this.minY + 1L) * ((long) this.maxZ - this.minZ + 1L);
    }

    public int sizeX() {
        return this.maxX - this.minX + 1;
    }

    public int sizeY() {
        return this.maxY - this.minY + 1;
    }

    public int sizeZ() {
        return this.maxZ - this.minZ + 1;
    }
}
