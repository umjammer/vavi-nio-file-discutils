//
// Copyright (c) 2019, Bianco Veigel
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
 * Read-write incompatible feature flags.
 */
enum IncompatibleFeatures {
//    None,
    /**
     * Directory file type. Each directory entry tracks the
     * type of the inode to which the entry points. This is a
     * performance optimization to remove the need to
     * load every inode into memory to iterate a directory.
     */
    FType,
    /**
     * Sparse inodes. This feature relaxes the requirement
     * to allocate inodes in chunks of 64. When the free
     * space is heavily fragmented, there might exist plenty
     * of free space but not enough contiguous free space to
     * allocate a new inode chunk. With this feature, the
     * user can continue to create files until all free space is
     * exhausted.
     * Unused space in the inode B+tree records are used to
     * track which parts of the inode chunk are not inodes.
     */
    SparseInodes,
    /**
     * Metadata UUID. The UUID stamped into each
     * metadata block must match the value in
     * sb_meta_uuid. This enables the administrator to
     * change sb_uuid at will without having to rewrite
     * the entire filesystem.
     */
    MetaUUID;

    public static final EnumSet<IncompatibleFeatures > Supported = EnumSet.of(FType);

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<IncompatibleFeatures> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(IncompatibleFeatures.class)));
    }

    public static long valueOf(EnumSet<IncompatibleFeatures> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
