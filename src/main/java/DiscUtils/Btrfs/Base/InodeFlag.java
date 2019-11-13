//
// Copyright (c) 2017, Bianco Veigel
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

package DiscUtils.Btrfs.Base;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public enum InodeFlag {
    /**
     * Do not perform checksum operations on this inode.
     */
    NoDataSum,
    /**
     * Do not perform CoW for data extents on this inode when the reference
     * count is 1.
     */
    NoDataCow,
    /**
     * Inode is read-only regardless of UNIX permissions or ownership.
     *
     * This bit is still checked and returns EACCES but there is no way to set
     * it. That suggests that it has been superseded by IMMUTABLE.
     */
    Readonly,
    /**
     * Do not compress this inode.
     *
     * This flag may be changed by the kernel as compression ratios change. If
     * the compression ratio for data associated with an inode becomes
     * undesirable, this flag will be set. It may be cleared if the data changes
     * and the compression ratio is favorable again.
     */
    NoCompress,
    /**
     * Inode contains preallocated extents. This instructs the kernel to attempt
     * to avoid CoWing those extents.
     */
    Prealloc,
    /**
     * Operations on this inode will be performed synchronously. This flag is
     * converted to a VFS-level inode flag but is not handled anywhere.
     */
    Sync,
    /**
     * Inode is read-only regardless of UNIX permissions or ownership. Attempts
     * to modify this inode will result in EPERM being returned to the user.
     */
    Immutable,
    /**
     * This inode is append-only.
     */
    Append,
    /**
     * This inode is not a candidate for dumping using the dump(8) program.
     *
     * This flag will be accepted on all kernels but is not implemented
     */
    NoDump,
    /**
     * Do not update atime,when this inode is accessed.
     */
    NoATime,
    /**
     * Operations on directory operations will be performed synchronously.
     *
     * This flag is converted to a VFS-level inode flag but is not handled
     * anywhere.
     */
    DirSync,
    /**
     * Compression is enabled on this inode.
     */
    Compress;

    // TODO
    public Supplier<Integer> supplier() {
        return () -> 1 << ordinal();
    }

    // TODO
    public Function<Integer, Boolean> function() {
        return v -> (v & supplier().get()) != 0;
    };

    public static EnumSet<InodeFlag> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> v.function().apply(value))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(InodeFlag.class)));
    }
}
