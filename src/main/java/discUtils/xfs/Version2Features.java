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

enum Version2Features {
    /**
     * Additional version flags if {@code VersionFlags#MOREBITSBIT}
     * is set in {@link SuperBlock#getVersion()}.
     */
    Reserved1,
    /**
     * Lazy global counters. Making a filesystem with this
     * bit set can improve performance. The global free
     * space and inode counts are only updated in the
     * primary superblock when the filesystem is cleanly
     * unmounted.
     */
    LazySbBitCount,
    Reserved4,
    /**
     * Extended attributes version 2. Making a filesystem
     * with this optimises the inode layout of extended
     * attributes. If this bit is set and the noattr2 mount
     * flag is not specified, the di_forkoff inode field
     * will be dynamically adjusted.
     */
    ExtendedAttributeVersion2,
    /**
     * Parent pointers. All inodes must have an extended
     * attribute that points back to its parent inode. The
     * primary purpose for this information is in backup
     * systems.
     */
    Parent,
    _dummy_0020,
    _dummy_0040,
    /**
     * 32-bit Project ID. Inodes can be associated with a
     * project ID number, which can be used to enforce disk
     * space usage quotas for a particular group of
     * directories. This flag indicates that project IDs can be
     * 32 bits in size.
     */
    ProjectId32Bit,
    /**
     * Metadata checksumming. All metadata blocks have
     * an extended header containing the block checksum,
     * a copy of the metadata UUID, the log sequence
     * number of the last update to prevent stale replays,
     * and a back pointer to the owner of the block. This
     * feature must be and can only be set if the lowest
     * nibble of sb_versionnum is set to 5.
     */
    Crc,
    /**
     * Directory file type. Each directory entry records the
     * type of the inode to which the entry points. This
     * speeds up directory iteration by removing the need
     * to load every inode into memory.
     */
    FType;

    private final int value = 1 << ordinal();

    public Supplier<Integer> supplier() {
        return () -> value;
    }

    public Function<Integer, Boolean> function() {
        return v -> (v & value) != 0;
    }

    public static EnumSet<Version2Features> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Version2Features.class)));
    }

    public static long valueOf(EnumSet<Version2Features> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.supplier().get())).getSum();
    }
}
