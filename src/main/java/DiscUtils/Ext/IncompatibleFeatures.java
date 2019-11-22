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
public enum IncompatibleFeatures {
    /**
     * File compression used (not used in mainline?).
     */
    Compression,
    /**
     * Indicates that directory entries contain a file type field (uses byte of
     * file name length field).
     */
    FileType,
    /**
     * Ext3 feature - indicates a dirty journal, that needs to be replayed (safe
     * for read-only access, not for read-write).
     */
    NeedsRecovery,
    /**
     * Ext3 feature - indicates the file system is a dedicated EXT3 journal, not
     * an actual file system.
     */
    IsJournalDevice,
    /**
     * Indicates the file system saves space by only allocating backup space for
     * the superblock in groups storing it (used with SparseSuperBlock).
     */
    MetaBlockGroup,
    /**
     * Ext4 feature to store files as extents.
     */
    Extents,
    /**
     * Ext4 feature to support some 64-bit fields.
     */
    SixtyFourBit,
    /**
     * Ext4 feature for storage of block groups.
     */
    FlexBlockGroups;

    private int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    };

    public static EnumSet<IncompatibleFeatures> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(IncompatibleFeatures.class)));
    }
}
