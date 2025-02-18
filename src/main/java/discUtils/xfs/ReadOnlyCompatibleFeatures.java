//
// Copyright (c) 2016, Bianco Veigel
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.xfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Feature flags for features backwards compatible with read-only mounting.
 */
enum ReadOnlyCompatibleFeatures {
    /**
     * Free inode B+tree. Each allocation group contains a
     * B+tree to track inode chunks containing free inodes.
     * This is a performance optimization to reduce the
     * time required to allocate inodes.
     */
    FINOBT,
    /**
     * Reverse mapping B+tree. Each allocation group
     * contains a B+tree containing records mapping AG
     * blocks to their owners.
     */
    RMAPBT,
    /**
     * Reference count B+tree. Each allocation group
     * contains a B+tree to track the reference counts of AG
     * blocks. This enables files to share data blocks safely.
     */
    REFLINK;

    public static final EnumSet<ReadOnlyCompatibleFeatures> ALL = EnumSet.of(FINOBT, RMAPBT, REFLINK);

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<ReadOnlyCompatibleFeatures> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ReadOnlyCompatibleFeatures.class)));
    }

    public static long valueOf(EnumSet<Version2Features> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
