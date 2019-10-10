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

package DiscUtils.Dmg;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.Plist;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Block.BlockCacheStream;
import DiscUtils.Streams.Buffer.BufferStream;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;


public final class DiskImageFile extends VirtualDiskLayer {
    private final Ownership _ownsStream;

    private ResourceFork _resources;

    private Stream _stream;

    private final UdifResourceFile _udifHeader;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to read.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownsStream) {
        _udifHeader = new UdifResourceFile();
        _stream = stream;
        _ownsStream = ownsStream;
        stream.setPosition(stream.getLength() - _udifHeader.getSize());
        byte[] data = StreamUtilities.readExact(stream, (int) _udifHeader.getSize());
        _udifHeader.readFrom(data, 0);
        if (_udifHeader.getSignatureValid()) {
            stream.setPosition(_udifHeader.XmlOffset);
            byte[] xmlData = StreamUtilities.readExact(stream, (int) _udifHeader.XmlLength);
            Map<String, Object> plist = Plist.parse(new MemoryStream(xmlData));
            _resources = ResourceFork.fromPlist(plist);
            __Buffer = new UdifBuffer(stream, _resources, _udifHeader.SectorCount);
        }
    }

    private UdifBuffer __Buffer;

    public UdifBuffer getBuffer() {
        return __Buffer;
    }

    public long getCapacity() {
        return getBuffer() == null ? _stream.getLength() : getBuffer().getCapacity();
    }

    /**
     * Gets the geometry of the virtual disk layer.
     */
    public Geometry getGeometry() {
        return Geometry.fromCapacity(getCapacity());
    }

    public boolean isSparse() {
        return getBuffer() != null;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean getNeedsParent() {
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
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        }

        if (getBuffer() != null) {
            SparseStream rawStream = new BufferStream(getBuffer(), FileAccess.Read);
            return new BlockCacheStream(rawStream, Ownership.Dispose);
        }

        return SparseStream.fromStream(_stream, Ownership.None);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return Array of candidate file locations.
     */
    public List<String> getParentLocations() {
        return Arrays.asList();
    }

    public void close() throws IOException {
        try {
            if (_stream != null && _ownsStream == Ownership.Dispose) {
                _stream.close();
            }

            _stream = null;
        } finally {
            super.close();
        }
    }
}
