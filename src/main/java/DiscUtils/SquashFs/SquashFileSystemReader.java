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

package DiscUtils.SquashFs;

import DiscUtils.Core.IUnixFileSystem;
import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.Vfs.VfsFileSystemFacade;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Implementation of SquashFs file system reader.
 *
 * SquashFs is a read-only file system, it is not designed to be modified
 * after it is created.
 */
public class SquashFileSystemReader extends VfsFileSystemFacade implements IUnixFileSystem {
    /**
     * Initializes a new instance of the SquashFileSystemReader class.
     *
     * @param data The stream to read the file system image from.
     */
    public SquashFileSystemReader(Stream data) {
        super(new VfsSquashFileSystemReader(data));
    }

    /**
    * Gets Unix file information about a file or directory.
    *
    *  @param path Path to the file or directory.
    *  @return Unix information about the file or directory.
    */
    public UnixFileSystemInfo getUnixFileInfo(String path) {
        return VfsSquashFileSystemReader.class.cast(getRealFileSystem()).getUnixFileInfo(path);
    }

    /**
     * Detects if the stream contains a SquashFs file system.
     *
     * @param stream The stream to inspect.
     * @return
     *         {@code true}
     *         if stream appears to be a SquashFs file system.
     */
    public static boolean detect(Stream stream) {
        stream.setPosition(0);
        SuperBlock superBlock = new SuperBlock();
        if (stream.getLength() < superBlock.getSize()) {
            return false;
        }

        byte[] buffer = StreamUtilities.readExact(stream, (int) superBlock.getSize());
        superBlock.readFrom(buffer, 0);
        return superBlock.Magic == SuperBlock.SquashFsMagic;
    }
}
