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

package discUtils.btrfs;

import discUtils.core.vfs.VfsFileSystemFacade;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Read-only access to btrfs file system.
 */
public final class BtrfsFileSystem extends VfsFileSystemFacade {

    public static final long[] SuperblockOffsets = {
        0x10000L, 0x4000000L, 0x4000000000L, 0x4000000000000L
    };

    /**
     * Initializes a new instance of the BtrfsFileSystem class.
     *
     * @param stream The stream containing the btrfs file system.
     */
    public BtrfsFileSystem(Stream stream) {
        super(new VfsBtrfsFileSystem(stream));
    }

    /**
     * Initializes a new instance of the BtrfsFileSystem class.
     *
     * @param stream  The stream containing the btrfs file system.
     * @param options Options for opening the file system
     */
    public BtrfsFileSystem(Stream stream, BtrfsFileSystemOptions options) {
        super(new VfsBtrfsFileSystem(stream, options));
    }

    public static boolean detect(Stream stream) {
        if (stream.getLength() < SuperBlock.Length + SuperblockOffsets[0]) {
            return false;
        }

        stream.position(SuperblockOffsets[0]);
        byte[] superblockData = StreamUtilities.readExact(stream, SuperBlock.Length);
        SuperBlock superblock = new SuperBlock();
        superblock.readFrom(superblockData, 0);
        return superblock.getMagic() == SuperBlock.BtrfsMagic;
    }

    /**
     * retrieve all subvolumes
     *
     * @return a list of subvolumes with id and name
     */
    public Subvolume[] getSubvolumes() {
        return VfsBtrfsFileSystem.class.cast(getRealFileSystem()).getSubvolumes();
    }
}
