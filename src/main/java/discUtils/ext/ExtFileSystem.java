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

package discUtils.ext;

import discUtils.core.FileSystemParameters;
import discUtils.core.IUnixFileSystem;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.vfs.VfsFileSystemFacade;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Read-only access to ext file system.
 */
public final class ExtFileSystem extends VfsFileSystemFacade implements IUnixFileSystem {
    /**
     * Initializes a new instance of the ExtFileSystem class.
     *
     * @param stream The stream containing the ext file system.
     */
    public ExtFileSystem(Stream stream) {
        super(new VfsExtFileSystem(stream, null));
    }

    /**
     * Initializes a new instance of the ExtFileSystem class.
     *
     * @param stream The stream containing the ext file system.
     * @param parameters The generic file system parameters (only file name
     *            encoding is honoured).
     */
    public ExtFileSystem(Stream stream, FileSystemParameters parameters) {
        super(new VfsExtFileSystem(stream, parameters));
    }

    /**
     * Retrieves Unix-specific information about a file or directory.
     *
     * @param path Path to the file or directory.
     * @return Information about the owner, group, permissions and type of the
     *         file or directory.
     */
    public UnixFileSystemInfo getUnixFileInfo(String path) {
        return VfsExtFileSystem.class.cast(getRealFileSystem()).getUnixFileInfo(path);
    }

    public static boolean detect(Stream stream) {
        if (stream.getLength() < 2048) {
            return false;
        }

        stream.setPosition(1024);
        byte[] superblockData = StreamUtilities.readExact(stream, 1024);
        SuperBlock superblock = new SuperBlock();
        superblock.readFrom(superblockData, 0);
        return superblock.magic == SuperBlock.Ext2Magic;
    }
}
