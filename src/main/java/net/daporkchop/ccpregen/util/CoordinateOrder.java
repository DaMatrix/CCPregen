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

package net.daporkchop.ccpregen.util;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;

/**
 * @author DaPorkchop_
 */
public enum CoordinateOrder {
    SLICES_TOP_TO_BOTTOM {
        @Override
        public CubePos startPos(Volume volume) {
            return new CubePos(volume.minX, volume.maxY, volume.minZ);
        }

        @Override
        public CubePos next(Volume volume, CubePos curr) {
            int x = curr.getX();
            int y = curr.getY();
            int z = curr.getZ();
            if (++x > volume.maxX) {
                x = volume.minX;
                if (++z > volume.maxZ) {
                    z = volume.minZ;
                    if (--y < volume.minY) {
                        return null;
                    }
                }
            }
            return new CubePos(x, y, z);
        }
    },
    SLICES_BOTTOM_TO_TOP {
        @Override
        public CubePos startPos(Volume volume) {
            return new CubePos(volume.minX, volume.minY, volume.minZ);
        }

        @Override
        public CubePos next(Volume volume, CubePos curr) {
            int x = curr.getX();
            int y = curr.getY();
            int z = curr.getZ();
            if (++x > volume.maxX) {
                x = volume.minX;
                if (++z > volume.maxZ) {
                    z = volume.minZ;
                    if (++y > volume.maxY) {
                        return null;
                    }
                }
            }
            return new CubePos(x, y, z);
        }
    },
    COLUMNS_TOP_TO_BOTTOM {
        @Override
        public CubePos startPos(Volume volume) {
            return new CubePos(volume.minX, volume.maxY, volume.minZ);
        }

        @Override
        public CubePos next(Volume volume, CubePos curr) {
            int x = curr.getX();
            int y = curr.getY();
            int z = curr.getZ();
            if (--y < volume.minY) {
                y = volume.maxY;
                if (++x > volume.maxX) {
                    x = volume.minX;
                    if (++z > volume.maxZ) {
                        return null;
                    }
                }
            }
            return new CubePos(x, y, z);
        }
    },
    COLUMNS_BOTTOM_TO_TOP {
        @Override
        public CubePos startPos(Volume volume) {
            return new CubePos(volume.minX, volume.minY, volume.minZ);
        }

        @Override
        public CubePos next(Volume volume, CubePos curr) {
            int x = curr.getX();
            int y = curr.getY();
            int z = curr.getZ();
            if (++y > volume.maxY) {
                y = volume.minY;
                if (++x > volume.maxX) {
                    x = volume.minX;
                    if (++z > volume.maxZ) {
                        return null;
                    }
                }
            }
            return new CubePos(x, y, z);
        }
    };

    public abstract CubePos startPos(Volume volume);

    public abstract CubePos next(Volume volume, CubePos curr);
}
