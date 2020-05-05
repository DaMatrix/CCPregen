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

package net.daporkchop.ccpregen.util;

import static java.lang.Long.*;
import static net.daporkchop.ccpregen.PregenState.*;

/**
 * @author DaPorkchop_
 */
public enum CoordinateOrder {
    SLICES_TOP_TO_BOTTOM {
        @Override
        public void init() {
            x = minX;
            y = maxY;
            z = minZ;
        }

        @Override
        public void next() {
            if (++x > maxX) {
                if (++z > maxZ) {
                    if (--y < minY) {
                        if (parseUnsignedLong(generated) < parseUnsignedLong(total) - 1L) {
                            throw new IllegalStateException(String.format("Iteration finished, but we only generated %s/%s cubes?!?", generated, total));
                        }
                    }
                    z = minZ;
                }
                x = minX;
            }
        }
    },
    SLICES_BOTTOM_TO_TOP {
        @Override
        public void init() {
            x = minX;
            y = minY;
            z = minZ;
        }

        @Override
        public void next() {
            if (++x > maxX) {
                if (++z > maxZ) {
                    if (++y > maxY) {
                        if (parseUnsignedLong(generated) < parseUnsignedLong(total) - 1L) {
                            throw new IllegalStateException(String.format("Iteration finished, but we only generated %s/%s cubes?!?", generated, total));
                        }
                    }
                    z = minZ;
                }
                x = minX;
            }
        }
    },
    COLUMNS_TOP_TO_BOTTOM {
        @Override
        public void init() {
            x = minX;
            y = maxY;
            z = minZ;
        }

        @Override
        public void next() {
            if (--y < minY) {
                if (++x > maxX) {
                    if (++z > maxZ) {
                        if (parseUnsignedLong(generated) < parseUnsignedLong(total) - 1L) {
                            throw new IllegalStateException(String.format("Iteration finished, but we only generated %s/%s cubes?!?", generated, total));
                        }
                    }
                    x = minX;
                }
                y = maxY;
            }
        }
    },
    COLUMNS_BOTTOM_TO_TOP {
        @Override
        public void init() {
            x = minX;
            y = minY;
            z = minZ;
        }

        @Override
        public void next() {
            if (++y > maxY) {
                if (++x > maxX) {
                    if (++z > maxZ) {
                        if (parseUnsignedLong(generated) < parseUnsignedLong(total) - 1L) {
                            throw new IllegalStateException(String.format("Iteration finished, but we only generated %s/%s cubes?!?", generated, total));
                        }
                    }
                    x = minX;
                }
                y = minY;
            }
        }
    };

    public abstract void init();

    public abstract void next();
}
