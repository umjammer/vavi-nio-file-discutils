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

package discUtils.dmg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import vavi.util.Debug;

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.Plist;
import discUtils.core.VirtualDiskLayer;
import discUtils.streams.SparseStream;
import discUtils.streams.block.BlockCacheStream;
import discUtils.streams.buffer.BufferStream;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


public final class DiskImageFile extends VirtualDiskLayer {

    private final Ownership ownsStream;

    private ResourceFork resources;

    private Stream stream;

    private final UdifResourceFile udifHeader;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to read.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownsStream) {
        udifHeader = new UdifResourceFile();
        this.stream = stream;
        this.ownsStream = ownsStream;

Debug.println(Level.FINE, "stream.getLength(): " + stream.getLength());
Debug.println(Level.FINE, "udifHeader.size(): " + udifHeader.size());
        stream.setPosition(stream.getLength() - udifHeader.size());
        byte[] data = StreamUtilities.readExact(stream, udifHeader.size());

        udifHeader.readFrom(data, 0);

        if (udifHeader.getSignatureValid()) {
            stream.setPosition(udifHeader.xmlOffset);
            byte[] xmlData = StreamUtilities.readExact(stream, (int) udifHeader.xmlLength);
            Map<String, Object> plist = Plist.parse(new MemoryStream(xmlData));

            resources = ResourceFork.fromPlist(plist);
            buffer = new UdifBuffer(stream, resources, udifHeader.sectorCount);
        } else {
            // TODO fat32 dmg doesn't have udif header
Debug.printf(Level.WARNING, "udifHeader: %08x\n", udifHeader.signature);
        }
    }

    private UdifBuffer buffer;

    public UdifBuffer getBuffer() {
        return buffer;
    }

    public long getCapacity() {
        return buffer == null ? stream.getLength() : buffer.getCapacity();
    }

    /**
     * Gets the geometry of the virtual disk layer.
     */
    public Geometry getGeometry() {
        return Geometry.fromCapacity(getCapacity());
    }

    public boolean isSparse() {
        return buffer != null;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return false;
    }

    public FileLocator getRelativeFileLocator() {
        throw new UnsupportedOperationException();
    }

    public SparseStream openContent(SparseStream parentStream, Ownership ownsStream) {
        if (parentStream != null && ownsStream == Ownership.Dispose) {
            try {
                parentStream.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        if (buffer != null) {
            SparseStream rawStream = new BufferStream(buffer, FileAccess.Read);
            return new BlockCacheStream(rawStream, Ownership.Dispose);
        }

        return SparseStream.fromStream(stream, Ownership.None);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return Array of candidate file locations.
     */
    public List<String> getParentLocations() {
        return Collections.emptyList();
    }

    public void close() throws IOException {
        if (stream != null && ownsStream == Ownership.Dispose) {
            stream.close();
        }

        stream = null;
    }
}
