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

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.daporkchop.ccpregen.util.Hilbert;
import net.minecraft.util.math.ChunkPos;
import org.junit.Test;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * @author DaPorkchop_
 */
public class TestHilbert {
    @Test
    public void test2dIsFull() {
        new SplittableRandom(1337L).longs(4096L).parallel()
                .mapToObj(SplittableRandom::new)
                .forEach(rng -> {
                    int x = rng.nextInt(-10000000, 10000000);
                    int z = rng.nextInt(-10000000, 10000000);

                    //randomly make the area shorter along some axes
                    int i = rng.nextInt() & 3;
                    int sizeX = rng.nextInt(1, (i & 1) != 0 ? 10 : 100);
                    int sizeZ = rng.nextInt(1, (i & 2) != 0 ? 10 : 100);

                    Set<ChunkPos> reference = this.all2d(x, z, sizeX, sizeZ);

                    Set<ChunkPos> hilbert = new ObjectOpenHashSet<>(sizeX * sizeZ);
                    Hilbert.hilbert2d(x, z, sizeX, sizeZ).forEach(hilbert::add);

                    if (!Objects.equals(reference, hilbert)) {
                        throw new IllegalStateException();
                    }
                });
    }

    @Test
    public void test2dIsOrderReliable() {
        new SplittableRandom(1337L).longs(4096L).parallel()
                .mapToObj(SplittableRandom::new)
                .forEach(rng -> {
                    int x = rng.nextInt(-10000000, 10000000);
                    int z = rng.nextInt(-10000000, 10000000);

                    //randomly make the area shorter along some axes
                    int i = rng.nextInt() & 3;
                    int sizeX = rng.nextInt(1, (i & 1) != 0 ? 10 : 100);
                    int sizeZ = rng.nextInt(1, (i & 2) != 0 ? 10 : 100);

                    Iterator<ChunkPos> i0 = Hilbert.hilbert2d(x, z, sizeX, sizeZ).iterator();
                    Iterator<ChunkPos> i1 = Hilbert.hilbert2d(x, z, sizeX, sizeZ).iterator();
                    while (i0.hasNext() && i1.hasNext()) {
                        ChunkPos p0 = i0.next();
                        ChunkPos p1 = i1.next();
                        if (!Objects.equals(p0, p1)) {
                            throw new IllegalStateException();
                        }
                    }

                    if (i0.hasNext() || i1.hasNext()) { //one of the iterators still has positions remaining
                        throw new IllegalStateException();
                    }
                });
    }

    @Test
    public void test3dIsFull() {
        new SplittableRandom(1337L).longs(4096L).parallel()
                .mapToObj(SplittableRandom::new)
                .forEach(rng -> {
                    int x = rng.nextInt(-10000000, 10000000);
                    int y = rng.nextInt(-10000000, 10000000);
                    int z = rng.nextInt(-10000000, 10000000);

                    //randomly make the volume shorter along at least one axis
                    int i = (rng.nextInt() & 7) | (1 << rng.nextInt(3));
                    int sizeX = rng.nextInt(1, (i & 1) != 0 ? 10 : 100);
                    int sizeY = rng.nextInt(1, (i & 2) != 0 ? 10 : 100);
                    int sizeZ = rng.nextInt(1, (i & 4) != 0 ? 10 : 100);

                    Set<CubePos> reference = this.all3d(x, y, z, sizeX, sizeY, sizeZ);

                    Set<CubePos> hilbert = new ObjectOpenHashSet<>(sizeX * sizeY * sizeZ);
                    Hilbert.hilbert3d(x, y, z, sizeX, sizeY, sizeZ).forEach(hilbert::add);

                    if (!Objects.equals(reference, hilbert)) {
                        throw new IllegalStateException();
                    }
                });
    }

    @Test
    public void test3dIsOrderReliable() {
        new SplittableRandom(1337L).longs(4096L).parallel()
                .mapToObj(SplittableRandom::new)
                .forEach(rng -> {
                    int x = rng.nextInt(-10000000, 10000000);
                    int y = rng.nextInt(-10000000, 10000000);
                    int z = rng.nextInt(-10000000, 10000000);

                    //randomly make the volume shorter along at least one axis
                    int i = (rng.nextInt() & 7) | (1 << rng.nextInt(3));
                    int sizeX = rng.nextInt(1, (i & 1) != 0 ? 10 : 100);
                    int sizeY = rng.nextInt(1, (i & 2) != 0 ? 10 : 100);
                    int sizeZ = rng.nextInt(1, (i & 4) != 0 ? 10 : 100);

                    Iterator<CubePos> i0 = Hilbert.hilbert3d(x, y, z, sizeX, sizeY, sizeZ).iterator();
                    Iterator<CubePos> i1 = Hilbert.hilbert3d(x, y, z, sizeX, sizeY, sizeZ).iterator();
                    while (i0.hasNext() && i1.hasNext()) {
                        CubePos p0 = i0.next();
                        CubePos p1 = i1.next();
                        if (!Objects.equals(p0, p1)) {
                            throw new IllegalStateException();
                        }
                    }

                    if (i0.hasNext() || i1.hasNext()) { //one of the iterators still has positions remaining
                        throw new IllegalStateException();
                    }
                });
    }

    private Set<ChunkPos> all2d(int x, int z, int sizeX, int sizeZ) {
        Set<ChunkPos> out = new ObjectOpenHashSet<>(sizeX * sizeZ);
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                out.add(new ChunkPos(x + dx, z + dz));
            }
        }
        return out;
    }

    private Set<CubePos> all3d(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        Set<CubePos> out = new ObjectOpenHashSet<>(sizeX * sizeY * sizeZ);
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    out.add(new CubePos(x + dx, y + dy, z + dz));
                }
            }
        }
        return out;
    }
}
