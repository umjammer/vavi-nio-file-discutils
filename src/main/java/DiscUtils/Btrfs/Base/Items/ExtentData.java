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

package DiscUtils.Btrfs.Base.Items;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Btrfs.Context;
import DiscUtils.Btrfs.Base.ExtentDataCompression;
import DiscUtils.Btrfs.Base.ExtentDataType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Core.Compression.ZlibStream;
import DiscUtils.Streams.ConcatStream;
import DiscUtils.Streams.LengthWrappingStream;
import DiscUtils.Streams.PositionWrappingStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
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
    private long _generation;

    public long getGeneration() {
        return _generation;
    }

    public void setGeneration(long value) {
        _generation = value;
    }

    /**
     * (n) size of decoded extent
     */
    private long _decodedSize;

    public long getDecodedSize() {
        return _decodedSize;
    }

    public void setDecodedSize(long value) {
        _decodedSize = value;
    }

    /**
     * compression (0=none, 1=zlib, 2=LZO)
     */
    private ExtentDataCompression _compression = ExtentDataCompression.None;

    public ExtentDataCompression getCompression() {
        return _compression;
    }

    public void setCompression(ExtentDataCompression value) {
        _compression = value;
    }

    /**
     * encryption (0=none)
     */
    private boolean _encryption;

    public boolean getEncryption() {
        return _encryption;
    }

    public void setEncryption(boolean value) {
        _encryption = value;
    }

    /**
     * type (0=inline, 1=regular, 2=prealloc)
     */
    private ExtentDataType _type = ExtentDataType.Inline;

    public ExtentDataType getType() {
        return _type;
    }

    public void setType(ExtentDataType value) {
        _type = value;
    }

    /**
     * If the extent is inline, the bytes are the data bytes (n bytes in case no
     * compression/encryption/other encoding is used)
     */
    private byte[] _inlineData;

    public byte[] getInlineData() {
        return _inlineData;
    }

    public void setInlineData(byte[] value) {
        _inlineData = value;
    }

    /**
     * (ea) logical address of extent. If this is zero, the extent is sparse and
     * consists of all zeroes.
     */
    private long _extentAddress;

    public long getExtentAddress() {
        return _extentAddress;
    }

    public void setExtentAddress(long value) {
        _extentAddress = value;
    }

    /**
     * (es) size of extent
     */
    private long _extentSize;

    public long getExtentSize() {
        return _extentSize;
    }

    public void setExtentSize(long value) {
        _extentSize = value;
    }

    /**
     * (o) offset within the extent
     */
    private long _extentOffset;

    public long getExtentOffset() {
        return _extentOffset;
    }

    public void setExtentOffset(long value) {
        _extentOffset = value;
    }

    /**
     * (s) logical number of bytes in file
     */
    private long _logicalSize;

    public long getLogicalSize() {
        return _logicalSize;
    }

    public void setLogicalSize(long value) {
        _logicalSize = value;
    }

    public int size() {
        return getType() == ExtentDataType.Inline ? getInlineData().length + 0x15 : 0x35;
    }

    public int readFrom(byte[] buffer, int offset) {
        setGeneration(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        setDecodedSize(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x8));
        setCompression(ExtentDataCompression.values()[buffer[offset + 0x10]]);
        setEncryption(buffer[offset + 0x11] != 0);
        //12 2 UINT other encoding (0=none)
        setType(ExtentDataType.values()[buffer[offset + 0x14]]);
        if (getType() == ExtentDataType.Inline) {
            setInlineData(EndianUtilities.toByteArray(buffer, offset + 0x15, buffer.length - (offset + 0x15)));
        } else {
            setExtentAddress(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x15));
            setExtentSize(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x1d));
            setExtentOffset(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x25));
            setLogicalSize(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x2d));
        }
        return size();
    }

    public Stream getStream(Context context) {
        if (getEncryption())
            throw new IOException("Extent encryption is not supported");
        Stream stream;
        switch (getType()) {
        case Inline:
            byte[] data = getInlineData();
            stream = new MemoryStream(data);
            break;
        case Regular:
            long address = getExtentAddress();
            if (address == 0) {
                stream = new ZeroStream(getLogicalSize());
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
        switch (getCompression()) {
        case None:
            break;
        case Zlib:
            ZlibStream zlib = new ZlibStream(stream, CompressionMode.Decompress, false);
            SparseStream sparse = SparseStream.fromStream(zlib, Ownership.Dispose);
            LengthWrappingStream length = new LengthWrappingStream(sparse, getLogicalSize(), Ownership.Dispose);
            stream = new PositionWrappingStream(length, 0, Ownership.Dispose);
            break;
        case Lzo:
            byte[] buffer = StreamUtilities.readExact(stream, 4); // sizeof(int)
            int totalLength = EndianUtilities.toUInt32LittleEndian(buffer, 0);
            long processed = 4; // sizeof(int)
            List<SparseStream> parts = new ArrayList<>();
            long remaining = getLogicalSize();
//Debug.println("remaining: " + remaining + ", " + stream);
            while (processed < totalLength) {
                stream.setPosition(processed);
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
            throw new IOException("Unsupported extent compression ({Compression})");
        }
        return stream;
    }
}
