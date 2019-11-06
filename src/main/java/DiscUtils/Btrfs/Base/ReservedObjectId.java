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


/**
 * All root objectids between FIRST_FREE = 256ULL and LAST_FREE = -256ULL refer
 * to file trees.
 */
public enum ReservedObjectId {
    /**
     * The object id that refers to the ROOT_TREE itself
     */
    RootTree(1),
    /**
     * The objectid that refers to the EXTENT_TREE
     */
    ExtentTree(2),
    /**
     * The objectid that refers to the root of the CHUNK_TREE
     */
    ChunkTree(3),
    /**
     * The objectid that refers to the root of the DEV_TREE
     */
    DevTree(4),
    /**
     * The objectid that refers to the global FS_TREE root
     */
    FsTree(5),
    /**
     * The objectid that refers to the CSUM_TREE
     */
    CsumTree(7),
    /**
     * The objectid that refers to the QUOTA_TREE
     */
    QuotaTree(8),
    /**
     * The objectid that refers to the UUID_TREE
     */
    UuidTree(9),
    /**
     * The objectid that refers to the FREE_SPACE_TREE
     */
    FreeSpaceTree(10),
    /**
     * The objectid that refers to the TREE_LOG tree
     */
    TreeLog(-8),
    /**
     * The objectid that refers to the TREE_RELOC tree
     */
    TreeReloc(-9),
    /**
     * The objectid that refers to the DATA_RELOC tree
     */
    DataRelocTree(-10),
    /**
     * The objectid that refers to the directory within the root tree.
     * 
     * If it exists, it will have the usual items used to implement a directory
     * associated with it There will only be a single entry called default that
     * points to a key to be used as the root directory on the file system
     * instead of the FS_TREE
     */
    RootTreeDir(6),
    /**
     * The objectid used for orphan root tracking
     */
    Orphan(-6),
    CsumItem(-11),
    /**
     * This objectid indicates the first available objectid in this CHUNK_TREE.
     * In practice, it is the only objectid used in the tree. The offset field
     * of the key is the only component used to distinguish separate CHUNK_ITEM
     * items.
     */
    FirstChunkTree(256);

    private int value;

    public int getValue() {
        return value;
    }

    private ReservedObjectId(int value) {
        this.value = value;
    }

    public static ReservedObjectId valueOf(int value) {
        return Arrays.stream(values()).filter(v -> v.getValue() == value).findFirst().get();
    }
}
