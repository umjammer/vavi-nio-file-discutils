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

package discUtils.hfsPlus;

import discUtils.core.IUnixFileSystem;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.vfs.VfsFileSystemFacade;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Class that interprets Apple's HFS+ file system, found in DMG files.
 */
public class HfsPlusFileSystem extends VfsFileSystemFacade implements IUnixFileSystem {

    /**
     * Initializes a new instance of the HfsPlusFileSystem class.
     *
     * @param stream A stream containing the file system.
     */
    public HfsPlusFileSystem(Stream stream) {
        super(new HfsPlusFileSystemImpl(stream));
    }

    /**
    * Gets the Unix (BSD) file information about a file or directory.
    *
    *  @param path The path of the file or directory.
    *  @return Unix file information.
    */
    @Override public UnixFileSystemInfo getUnixFileInfo(String path) {
        return HfsPlusFileSystemImpl.class.cast(getRealFileSystem()).getUnixFileInfo(path);
    }

    public static boolean detect(Stream stream) {
        if (stream.getLength() < 1536) {
            return false;
        }

        stream.position(1024);

        byte[] headerBuf = StreamUtilities.readExact(stream, 512);
        VolumeHeader hdr = new VolumeHeader();
        hdr.readFrom(headerBuf, 0);

        return hdr.isValid();
    }
}
