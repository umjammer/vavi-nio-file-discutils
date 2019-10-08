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

package DiscUtils.Xfs;

import java.util.Arrays;

public enum InodeFlags {
    None,
    /**
     * The inode's data is located on the real-time device.
     */
    Realtime,
    /**
     * The inode's extents have been preallocated.
     */
    Prealloc,
    /**
     * Specifies the sb_rbmino uses the new real-time bitmap format
     */
    NewRtBitmap,
    /**
     * Specifies the inode cannot be modified.
     */
    Immutable,
    /**
     * The inode is in append only mode.
     */
    Append,
    /**
     * The inode is written synchronously.
     */
    Sync,
    /**
     * The inode's di_atime is not updated.
     */
    NoAtime,
    /**
     * Specifies the inode is to be ignored by xfsdump.
     */
    NoDump,
    /**
     * For directory inodes, new inodes inherit the XFS_DIFLAG_REALTIME bit.
     */
    RtInherit,
    /**
     * For directory inodes, new inodes inherit the
     * {@link #Inode.ProjectId}
     * value.
     */
    ProjInherit,
    /**
     * For directory inodes, symlinks cannot be created.
     */
    NoSymlinks,
    /**
     * Specifies the extent size for real-time files or a and extent size hint
     * for regular files.
     */
    ExtentSize,
    /**
     * For directory inodes, new inodes inherit the
     * {@link #Inode.ExtentSize}
     * value.
     */
    ExtentSizeInherit,
    /**
     * Specifies the inode is to be ignored when defragmenting the filesystem.
     */
    NoDefrag,
    /**
     * use filestream allocator
     */
    Filestream;

    public static InodeFlags valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.ordinal() == value).findFirst().get();
    }
}
