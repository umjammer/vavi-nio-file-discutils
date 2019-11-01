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

package DiscUtils.Vdi;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.Stream;


/**
 * Represents a single VirtualBox disk (.vdi file).
 */
public final class DiskImageFile extends VirtualDiskLayer {
    private HeaderRecord _header;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private final Ownership _ownsStream;

    private PreHeaderRecord _preHeader;

    private Stream _stream;

    /**
     * Indicates if a write occurred, indicating the marker in the header needs
     * to be updated.
     */
    private boolean _writeOccurred;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        _stream = stream;
        _ownsStream = Ownership.None;
        readHeader();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownsStream) {
        _stream = stream;
        _ownsStream = ownsStream;
        readHeader();
    }

    public long getCapacity() {
        return _header.diskSize;
    }

    /**
     * Gets (a guess at) the geometry of the virtual disk.
     */
    public Geometry getGeometry() {
        if (_header.lchsGeometry != null && _header.lchsGeometry.Cylinders != 0) {
            return _header.lchsGeometry.toGeometry(_header.diskSize);
        }

        if (_header.legacyGeometry.Cylinders != 0) {
            return _header.legacyGeometry.toGeometry(_header.diskSize);
        }

        return GeometryRecord.fromCapacity(_header.diskSize).toGeometry(_header.diskSize);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    public boolean isSparse() {
        return _header.imageType != ImageType.Fixed;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return _header.imageType == ImageType.Differencing || _header.imageType == ImageType.Undo;
    }

    // Differencing disks not yet supported.
    public FileLocator getRelativeFileLocator() {
        return null;
    }

    /**
     * Initializes a stream as a fixed-sized VDI file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VDI file.
     */
    public static DiskImageFile initializeFixed(Stream stream,
                                                Ownership ownsStream,
                                                long capacity) {
        PreHeaderRecord preHeader = PreHeaderRecord.initialized();
        HeaderRecord header = HeaderRecord.initialized(ImageType.Fixed, ImageFlags.None, capacity, 1024 * 1024, 0);
        byte[] blockTable = new byte[header.blockCount * 4];
        for (int i = 0; i < header.blockCount; ++i) {
            EndianUtilities.writeBytesLittleEndian(i, blockTable, i * 4);
        }
        header.blocksAllocated = header.blockCount;
        stream.setPosition(0);
        preHeader.write(stream);
        header.write(stream);
        stream.setPosition(header.blocksOffset);
        stream.write(blockTable, 0, blockTable.length);
        long totalSize = header.dataOffset + header.blockSize * (long) header.blockCount;
        if (stream.getLength() < totalSize) {
            stream.setLength(totalSize);
        }

        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Initializes a stream as a dynamically-sized VDI file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VDI file.
     */
    public static DiskImageFile initializeDynamic(Stream stream,
                                                  Ownership ownsStream,
                                                  long capacity) {
        PreHeaderRecord preHeader = PreHeaderRecord.initialized();
        HeaderRecord header = HeaderRecord.initialized(ImageType.Dynamic, ImageFlags.None, capacity, 1024 * 1024, 0);
        byte[] blockTable = new byte[header.blockCount * 4];
        for (int i = 0; i < blockTable.length; ++i) {
            blockTable[i] = (byte) 0xFF;
        }
        header.blocksAllocated = 0;
        stream.setPosition(0);
        preHeader.write(stream);
        header.write(stream);
        stream.setPosition(header.blocksOffset);
        stream.write(blockTable, 0, blockTable.length);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Opens the content of the disk image file as a stream.
     *
     * @param parent The parent file's content (if any).
     * @param ownsParent Whether the created stream assumes ownership of parent
     *            stream.
     * @return The new content stream.
     */
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (parent != null && ownsParent == Ownership.Dispose) {
            try {
                // Not needed until differencing disks supported.
                parent.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        DiskStream stream = new DiskStream(_stream, Ownership.None, _header);
        stream.WriteOccurred = onWriteOccurred;
        return stream;
    }

    /**
     * Gets the possible locations of the parent file (if any).
     *
     * @return Array of strings, empty if no parent.
     */
    public List<String> getParentLocations() {
        // Until diff/undo supported
        return Arrays.asList();
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        if (_writeOccurred && _stream != null) {
            _header.modificationId = UUID.randomUUID();
            _stream.setPosition(PreHeaderRecord.Size);
            _header.write(_stream);
        }

        if (_ownsStream == Ownership.Dispose && _stream != null) {
            _stream.close();
            _stream = null;
        }
    }

    private void readHeader() {
        _stream.setPosition(0);
        _preHeader = new PreHeaderRecord();
        _preHeader.read(_stream);
        _header = new HeaderRecord();
        _header.read(_preHeader.Version, _stream);
    }

    private BiConsumer<Object, Object[]> onWriteOccurred = (sender, e) -> {
        _writeOccurred = true;
    };
}
