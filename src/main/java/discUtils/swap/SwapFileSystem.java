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

package discUtils.swap;

import discUtils.core.DiscFileSystemOptions;
import discUtils.core.vfs.IVfsDirectory;
import discUtils.core.vfs.IVfsFile;
import discUtils.core.vfs.VfsDirEntry;
import discUtils.core.vfs.VfsReadOnlyFileSystem;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


/**
 * Class for accessing swap file systems.
 */
public final class SwapFileSystem extends VfsReadOnlyFileSystem<VfsDirEntry, IVfsFile, IVfsDirectory<VfsDirEntry, IVfsFile>, SwapContext> {

    /**
     * Initializes a new instance of the SwapFileSystem class.
     *
     * @param stream The stream containing the file system.
     */
    public SwapFileSystem(Stream stream) {
        super(new DiscFileSystemOptions());

        setContext(new SwapContext());
        getContext().setHeader(readSwapHeader(stream));
        if (getContext().getHeader() == null)
            throw new dotnet4j.io.IOException("swap Header missing");
        if (!getContext().getHeader().getMagic().equals(SwapHeader.Magic1) &&
            !getContext().getHeader().getMagic().equals(SwapHeader.Magic2))
            throw new dotnet4j.io.IOException("Invalid swap header");
    }

    /**
     * Gets the friendly name for the file system.
     */
    @Override public String getFriendlyName() {
        return "swap";
    }

    /**
     * Gets the volume label.
     */
    @Override public String getVolumeLabel() {
        return getContext().getHeader().getVolume();
    }

    /**
     * Detects if a stream contains a swap file system.
     *
     * @param stream The stream to inspect.
     * @return {@code true} if the stream appears to be a swap file system, else
     *         {@code false} .
     */
    public static boolean detect(Stream stream) {
        SwapHeader header = readSwapHeader(stream);
        return header != null && (header.getMagic().equals(SwapHeader.Magic1) || header.getMagic().equals(SwapHeader.Magic2));
    }

    private static SwapHeader readSwapHeader(Stream stream) {
        if (stream.getLength() < SwapHeader.PageSize) {
            return null;
        }

        stream.position(0);
        byte[] headerData = StreamUtilities.readExact(stream, SwapHeader.PageSize);
        SwapHeader header = new SwapHeader();
        header.readFrom(headerData, 0);
        return header;
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        return getContext().getHeader().getLastPage() * SwapHeader.PageSize;
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override public long getUsedSpace() {
        return getSize();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        return 0;
    }

    @Override protected IVfsFile convertDirEntryToFile(VfsDirEntry dirEntry) {
        throw new UnsupportedOperationException();
    }
}
