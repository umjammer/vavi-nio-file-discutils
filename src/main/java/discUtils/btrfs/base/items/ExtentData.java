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

package discUtils.btrfs.base.items;

import java.util.ArrayList;
import java.util.List;

import discUtils.btrfs.Context;
import discUtils.btrfs.base.ExtentDataCompression;
import discUtils.btrfs.base.ExtentDataType;
import discUtils.btrfs.base.Key;
import discUtils.core.compression.ZlibStream;
import discUtils.streams.ConcatStream;
import discUtils.streams.LengthWrappingStream;
import discUtils.streams.PositionWrappingStream;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.lzo.SeekableLzoStream;


/**
 * The contents of a file
 */
public class ExtentData extends BaseItem {

    public ExtentData(Key key) {
        super(key);
    }

    /**
     * generation
     */
    private long generation;

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long value) {
        generation = value;
    }

    /**
     * (n) size of decoded extent
     */
    private long decodedSize;

    public long getDecodedSize() {
        return decodedSize;
    }

    public void setDecodedSize(long value) {
        decodedSize = value;
    }

    /**
     * compression (0=none, 1=zlib, 2=LZO)
     */
    private ExtentDataCompression compression = ExtentDataCompression.None;

    public ExtentDataCompression getCompression() {
        return compression;
    }

    public void setCompression(ExtentDataCompression value) {
        compression = value;
    }

    /**
     * encryption (0=none)
     */
    private boolean encryption;

    public boolean getEncryption() {
        return encryption;
    }

    public void setEncryption(boolean value) {
        encryption = value;
    }

    /**
     * type (0=inline, 1=regular, 2=prealloc)
     */
    private ExtentDataType type = ExtentDataType.Inline;

    public ExtentDataType getType() {
        return type;
    }

    public void setType(ExtentDataType value) {
        type = value;
    }

    /**
     * If the extent is inline, the bytes are the data bytes (n bytes in case no
     * compression/encryption/other encoding is used)
     */
    private byte[] inlineData;

    public byte[] getInlineData() {
        return inlineData;
    }

    public void setInlineData(byte[] value) {
        inlineData = value;
    }

    /**
     * (ea) logical address of extent. If this is zero, the extent is sparse and
     * consists of all zeroes.
     */
    private long extentAddress;

    public long getExtentAddress() {
        return extentAddress;
    }

    public void setExtentAddress(long value) {
        extentAddress = value;
    }

    /**
     * (es) size of extent
     */
    private long extentSize;

    public long getExtentSize() {
        return extentSize;
    }

    public void setExtentSize(long value) {
        extentSize = value;
    }

    /**
     * (o) offset within the extent
     */
    private long extentOffset;

    public long getExtentOffset() {
        return extentOffset;
    }

    public void setExtentOffset(long value) {
        extentOffset = value;
    }

    /**
     * (s) logical number of bytes in file
     */
    private long logicalSize;

    public long getLogicalSize() {
        return logicalSize;
    }

    public void setLogicalSize(long value) {
        logicalSize = value;
    }

    public int size() {
        return getType() == ExtentDataType.Inline ? getInlineData().length + 0x15 : 0x35;
    }

    public int readFrom(byte[] buffer, int offset) {
        generation = EndianUtilities.toUInt64LittleEndian(buffer, offset);
        decodedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8);
        compression = ExtentDataCompression.values()[buffer[offset + 0x10]];
        encryption = buffer[offset + 0x11] != 0;
        //12 2 UINT other encoding (0=none)
        type = ExtentDataType.values()[buffer[offset + 0x14]];
        if (type == ExtentDataType.Inline) {
            inlineData = EndianUtilities.toByteArray(buffer, offset + 0x15, buffer.length - (offset + 0x15));
        } else {
            extentAddress = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x15);
            extentSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x1d);
            extentOffset = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x25);
            logicalSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x2d);
        }
        return size();
    }

    public Stream getStream(Context context) {
        if (encryption)
            throw new IOException("Extent encryption is not supported");
        Stream stream;
        switch (type) {
        case Inline:
            byte[] data = inlineData;
            stream = new MemoryStream(data);
            break;
        case Regular:
            long address = extentAddress;
            if (address == 0) {
                stream = new ZeroStream(logicalSize);
            } else {
                long physicalAddress = context.mapToPhysical(address);
                stream = new SubStream(context.getRawStream(),
                                       Ownership.None,
                                       physicalAddress + getExtentOffset(),
                                       getExtentSize());
            }
            break;
        case PreAlloc:
            throw new UnsupportedOperationException();
        default:
            throw new IOException("invalid extent type");
        }
        switch (compression) {
        case None:
            break;
        case Zlib:
            ZlibStream zlib = new ZlibStream(stream, CompressionMode.Decompress, false);
            SparseStream sparse = SparseStream.fromStream(zlib, Ownership.Dispose);
            LengthWrappingStream length = new LengthWrappingStream(sparse, logicalSize, Ownership.Dispose);
            stream = new PositionWrappingStream(length, 0, Ownership.Dispose);
            break;
        case Lzo:
            byte[] buffer = StreamUtilities.readExact(stream, 4); // sizeof(int)
            int totalLength = EndianUtilities.toUInt32LittleEndian(buffer, 0);
            long processed = 4; // sizeof(int)
            List<SparseStream> parts = new ArrayList<>();
            long remaining = logicalSize;
//Debug.println("remaining: " + remaining + ", " + stream);
            while (processed < totalLength) {
                stream.position(processed);
                StreamUtilities.readExact(stream, buffer, 0, 4); // sizeof(int)
                int partLength = EndianUtilities.toUInt32LittleEndian(buffer, 0);
                processed += 4; // sizeof(int)
//Debug.println("processed: " + processed + ", partLength: " + partLength + ", remaining: " + remaining);
                SubStream part = new SubStream(stream, Ownership.Dispose, processed, partLength);
                SeekableLzoStream uncompressed = new SeekableLzoStream(part, CompressionMode.Decompress, false);
                uncompressed.setLength(Math.min(Sizes.OneKiB * 4, remaining));
                remaining -= uncompressed.getLength();
                parts.add(SparseStream.fromStream(uncompressed, Ownership.Dispose));
                processed += partLength;
            }
            stream = new ConcatStream(Ownership.Dispose, parts);
            break;
        default:
            throw new IOException("Unsupported extent compression ({compression})");
        }
        return stream;
    }
}
