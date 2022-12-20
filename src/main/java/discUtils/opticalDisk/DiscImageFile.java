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

package discUtils.opticalDisk;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskLayer;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamBuffer;
import discUtils.streams.buffer.BufferStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


/**
 * Represents a single optical disc image file.
 */
public final class DiscImageFile extends VirtualDiskLayer {

    public static final int Mode1SectorSize = 2048;

    public static final int Mode2SectorSize = 2352;

    private final OpticalFormat format;

    private Closeable toDispose;

    /**
     * Initializes a new instance of the DiscImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiscImageFile(Stream stream) {
        this(stream, Ownership.None, OpticalFormat.None);
    }

    /**
     * Initializes a new instance of the DiscImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     * @param format The disc image format.
     */
    public DiscImageFile(Stream stream, Ownership ownsStream, OpticalFormat format) {
        if (ownsStream == Ownership.Dispose) {
            toDispose = stream;
        }

        if (format == OpticalFormat.None) {
            if (stream.getLength() % Mode1SectorSize == 0 && stream.getLength() % Mode2SectorSize != 0) {
                this.format = OpticalFormat.Mode1;
            } else if (stream.getLength() % Mode1SectorSize != 0 && stream.getLength() % Mode2SectorSize == 0) {
                this.format = OpticalFormat.Mode2;
            } else {
                throw new dotnet4j.io.IOException("Unable to detect optical disk format");
            }
        } else {
            this.format = format;
        }

        setContent(stream instanceof SparseStream ? (SparseStream) stream : null);
        if (getContent() == null) {
            setContent(SparseStream.fromStream(stream, Ownership.None));
        }

        if (this.format == OpticalFormat.Mode2) {
            Mode2Buffer converter = new Mode2Buffer(new StreamBuffer(getContent(), Ownership.None));
            setContent(new BufferStream(converter, FileAccess.Read));
        }
    }

    public long getCapacity() {
        return getContent().getLength();
    }

    private SparseStream content;

    SparseStream getContent() {
        return content;
    }

    void setContent(SparseStream value) {
        content = value;
    }

    /**
     * Gets the Geometry of the disc.
     *
     * Optical discs don't fit the CHS model, so dummy CHS data provided, but
     * sector size is accurate.
     */
    public Geometry getGeometry() {
        // Note external sector size is always 2048 - 2352 just has extra header
        // & error-correction info
        return new Geometry(1, 1, 1, Mode1SectorSize);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    public boolean isSparse() {
        return false;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return false;
    }

    public FileLocator getRelativeFileLocator() {
        return null;
    }

    /**
     * Gets the content of this layer.
     *
     * @param parent The parent stream (if any).
     * @param ownsParent Controls ownership of the parent stream.
     * @return The content as a stream.
     */
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (ownsParent == Ownership.Dispose && parent != null) {
            try {
                parent.close();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        return SparseStream.fromStream(getContent(), Ownership.None);
    }

    /**
     * Gets the possible locations of the parent file (if any).
     *
     * @return list of strings, empty if no parent.
     */
    public List<String> getParentLocations() {
        return Collections.emptyList();
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        if (toDispose != null) {
            toDispose.close();
            toDispose = null;
        }

        if (getContent() != null) {
            getContent().close();
            setContent(null);
        }
    }
}
