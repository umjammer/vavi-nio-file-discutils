//
// Copyright (c) 2008-2011, Kenneth Bell
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

package DiscUtils.Ext;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Feature flags for features backwards compatible with read-only mounting.
 */
public enum ReadOnlyCompatibleFeatures {
    /**
     * Indicates that not all block groups contain a backup superblock.
     */
    SparseSuperblock,
    /**
     * Indicates file system contains files greater than 0x7FFFFFFF in size
     * (limit of unsigned uints).
     */
    LargeFiles,
    /**
     * Indicates BTree-style directories present (not used in mainline?).
     */
    BtreeDirectory,
    /**
     * Ext4 feature - support for storing huge files.
     */
    HugeFile,
    /**
     * Ext4 feature - checksum block group structures.
     */
    GdtChecksum,
    /**
     * Ext4 feature - Unknown.
     */
    DirNlink,
    /**
     * Ext4 feature - extra inode size.
     */
    ExtraInodeSize;

    // TODO
    public Supplier<Integer> supplier() {
        return () -> 1 << ordinal();
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<ReadOnlyCompatibleFeatures> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ReadOnlyCompatibleFeatures.class)));
    }
}
