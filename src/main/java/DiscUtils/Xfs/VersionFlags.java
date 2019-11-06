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

package DiscUtils.Xfs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Filesystem version number. This is a bitmask specifying the features
 * enabled when creating the filesystem.
 * Any disk checking tools or drivers that do not recognize any set bits
 * must not operate upon the filesystem.
 * Most of the flags indicate features introduced over time.
 */
public enum VersionFlags {
//    None,
    _dummy_0001,
    _dummy_0002,
    _dummy_0004,
    _dummy_0008,
    /**
     * Set if any inode have extended attributes. If this bit is
     * set; the XFS_SB_VERSION2_ATTR2BIT is not
     * set; and the attr2 mount flag is not specified, the
     * di_forkoff inode field will not be dynamically
     * adjusted.
     */
    ExtendedAttributes,
    /**
     * Set if any inodes use 32-bit di_nlink values.
     */
    NLink,
    /**
     * Quotas are enabled on the filesystem. This also
     * brings in the various quota fields in the superblock.
     */
    Quota,
    /**
     * Set if sb_inoalignmt is used.
     */
    Alignment,
    /**
     * Set if sb_unit and sb_width are used.
     */
    DAlignment,
    /**
     * Set if sb_shared_vn is used.
     */
    Shared,
    /**
     * Version 2 journaling logs are used.
     */
    LogV2,
    /**
     * Set if sb_sectsize is not 512.
     */
    Sector,
    /**
     * Unwritten extents are used. This is always set.
     */
    ExtentFlag,
    /**
     * Version 2 directories are used. This is always set.
     */
    DirV2,
    /**
     * ASCII only case-insens.
     */
    Borg,
    /**
     * Set if the sb_features2 field in the superblock
     * contains more flags.
     */
    Features2;

    /** 5.3, 6.0.1, 6.1 */
    public static final int Version1 = 1;
    /** 6.2 - attributes */
    public static final int Version2 = 2;
    /** 6.2 - new inode version */
    public static final int Version3 = 3;
    /** 6.2+ - bitmask version */
    public static final int Version4 = 4;
    /** CRC enabled filesystem */
    public static final int Version5 = 5;

    public static final int NumberFlag = 0x000f;

    // TODO
    public Supplier<Integer> supplier() {
        return () -> 1 << ordinal();
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<VersionFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(VersionFlags.class)));
    }

    public static long valueOf(EnumSet<VersionFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
