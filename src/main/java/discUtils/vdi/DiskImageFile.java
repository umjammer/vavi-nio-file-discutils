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

package discUtils.vdi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskLayer;
import discUtils.streams.SparseStream;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;


/**
 * Represents a single VirtualBox disk (.vdi file).
 */
public final class DiskImageFile extends VirtualDiskLayer {

    private HeaderRecord header;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private final Ownership ownsStream;

    private PreHeaderRecord preHeader;

    private Stream stream;

    /**
     * Indicates if a write occurred, indicating the marker in the header needs
     * to be updated.
     */
    private boolean writeOccurred;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        this.stream = stream;
        ownsStream = Ownership.None;
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
        this.stream = stream;
        this.ownsStream = ownsStream;
        readHeader();
    }

    public long getCapacity() {
        return header.diskSize;
    }

    /**
     * Gets (a guess at) the geometry of the virtual disk.
     */
    public Geometry getGeometry() {
        if (header.lchsGeometry != null && header.lchsGeometry.cylinders != 0) {
            return header.lchsGeometry.toGeometry(header.diskSize);
        }

        if (header.legacyGeometry.cylinders != 0) {
            return header.legacyGeometry.toGeometry(header.diskSize);
        }

        return GeometryRecord.fromCapacity(header.diskSize).toGeometry(header.diskSize);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    public boolean isSparse() {
        return header.imageType != ImageType.Fixed;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return header.imageType == ImageType.Differencing || header.imageType == ImageType.Undo;
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
        stream.position(0);
        preHeader.write(stream);
        header.write(stream);
        stream.position(header.blocksOffset);
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
        Arrays.fill(blockTable, (byte) 0xFF);
        header.blocksAllocated = 0;
        stream.position(0);
        preHeader.write(stream);
        header.write(stream);
        stream.position(header.blocksOffset);
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

        DiskStream stream = new DiskStream(this.stream, Ownership.None, header);
        stream.writeOccurred = onWriteOccurred;
        return stream;
    }

    /**
     * Gets the possible locations of the parent file (if any).
     *
     * @return Array of strings, empty if no parent.
     */
    public List<String> getParentLocations() {
        // Until diff/undo supported
        return Collections.emptyList();
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        if (writeOccurred && stream != null) {
            header.modificationId = UUID.randomUUID();
            stream.position(PreHeaderRecord.Size);
            header.write(stream);
        }

        if (ownsStream == Ownership.Dispose && stream != null) {
            stream.close();
            stream = null;
        }
    }

    private void readHeader() {
        stream.position(0);
        preHeader = new PreHeaderRecord();
        preHeader.read(stream);
        header = new HeaderRecord();
        header.read(preHeader.version, stream);
    }

    private BiConsumer<Object, Object[]> onWriteOccurred = (sender, e) -> writeOccurred = true;
}
