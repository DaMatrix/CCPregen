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

import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * @author DaPorkchop_
 */
public enum CoordinateOrder {
    SLICES_TOP_TO_BOTTOM {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return IntStream.rangeClosed(volume.minY, volume.maxY)
                    .map(y -> volume.maxY - y + volume.minY) //reverse order
                    .boxed()
                    .flatMap(y -> IntStream.rangeClosed(volume.minX, volume.maxX).boxed()
                            .flatMap(x -> IntStream.rangeClosed(volume.minZ, volume.maxZ)
                                    .mapToObj(z -> new CubePos(x, y, z))))
                    .iterator();
        }
    },
    SLICES_BOTTOM_TO_TOP {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return IntStream.rangeClosed(volume.minY, volume.maxY).boxed()
                    .flatMap(y -> IntStream.rangeClosed(volume.minX, volume.maxX).boxed()
                            .flatMap(x -> IntStream.rangeClosed(volume.minZ, volume.maxZ)
                                    .mapToObj(z -> new CubePos(x, y, z))))
                    .iterator();
        }
    },
    COLUMNS_TOP_TO_BOTTOM {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return IntStream.rangeClosed(volume.minX, volume.maxX).boxed()
                    .flatMap(x -> IntStream.rangeClosed(volume.minZ, volume.maxZ).boxed()
                            .flatMap(z -> IntStream.rangeClosed(volume.minY, volume.maxY)
                                    .map(y -> volume.maxY - y + volume.minY) //reverse order
                                    .mapToObj(y -> new CubePos(x, y, z))))
                    .iterator();
        }
    },
    COLUMNS_BOTTOM_TO_TOP {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return IntStream.rangeClosed(volume.minX, volume.maxX).boxed()
                    .flatMap(x -> IntStream.rangeClosed(volume.minZ, volume.maxZ).boxed()
                            .flatMap(z -> IntStream.rangeClosed(volume.minY, volume.maxY)
                                    .mapToObj(y -> new CubePos(x, y, z))))
                    .iterator();
        }
    },
    HILBERT_3D {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return Hilbert.hilbert3d(volume.minX, volume.minY, volume.minZ, volume.sizeX(), volume.sizeY(), volume.sizeZ())
                    .iterator();
        }
    },
    HILBERT_2D_TOP_TO_BOTTOM {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return Hilbert.hilbert2d(volume.minX, volume.minZ, volume.sizeX(), volume.sizeZ())
                    .flatMap(pos -> IntStream.rangeClosed(volume.minY, volume.maxY)
                            .map(y -> volume.maxY - y + volume.minY) //reverse order
                            .mapToObj(y -> new CubePos(pos.x, y, pos.z)))
                    .iterator();
        }
    },
    HILBERT_2D_BOTTOM_TO_TOP {
        @Override
        public Iterator<CubePos> iterator(Volume volume) {
            return Hilbert.hilbert2d(volume.minX, volume.minZ, volume.sizeX(), volume.sizeZ())
                    .flatMap(pos -> IntStream.rangeClosed(volume.minY, volume.maxY)
                            .mapToObj(y -> new CubePos(pos.x, y, pos.z)))
                    .iterator();
        }
    };

    public abstract Iterator<CubePos> iterator(Volume volume);
}
