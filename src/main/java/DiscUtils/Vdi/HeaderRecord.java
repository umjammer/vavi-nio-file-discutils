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

import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


public class HeaderRecord {
    private FileVersion _fileVersion = new FileVersion();

    public int BlockCount;

    public int BlockExtraSize;

    public int BlocksAllocated;

    public int BlockSize;

    public int BlocksOffset;

    public String Comment;

    public int DataOffset;

    public long DiskSize;

    public ImageFlags Flags = ImageFlags.None;

    public int HeaderSize;

    public ImageType imageType;

    public GeometryRecord LChsGeometry;

    public GeometryRecord LegacyGeometry;

    public UUID ModificationId;

    public UUID ParentId;

    public UUID ParentModificationId;

    public UUID UniqueId;

    public static HeaderRecord initialized(ImageType type,
                                           ImageFlags flags,
                                           long size,
                                           int blockSize,
                                           int blockExtra) {
        HeaderRecord result = new HeaderRecord();
        result._fileVersion = new FileVersion(0x00010001);
        result.HeaderSize = 400;
        result.imageType = type;
        result.Flags = flags;
        result.Comment = "Created by .NET DiscUtils";
        result.LegacyGeometry = new GeometryRecord();
        result.DiskSize = size;
        result.BlockSize = blockSize;
        result.BlockExtraSize = blockExtra;
        result.BlockCount = (int) ((size + blockSize - 1) / blockSize);
        result.BlocksAllocated = 0;
        result.BlocksOffset = (PreHeaderRecord.Size + result.HeaderSize + 511) / 512 * 512;
        result.DataOffset = (result.BlocksOffset + result.BlockCount * 4 + 511) / 512 * 512;
        result.UniqueId = UUID.randomUUID();
        result.ModificationId = UUID.randomUUID();
        result.LChsGeometry = new GeometryRecord();
        return result;
    }

    public void read(FileVersion version, Stream s) {
        int headerSize;
        _fileVersion = version;
        // Determine header size...
        if (version.getMajor() == 0) {
            headerSize = 348;
        } else {
            long savedPos = s.getPosition();
            headerSize = EndianUtilities.toInt32LittleEndian(StreamUtilities.readExact(s, 4), 0);
            s.setPosition(savedPos);
        }
        byte[] buffer = StreamUtilities.readExact(s, headerSize);
        read(version, buffer, 0);
    }

    public int read(FileVersion version, byte[] buffer, int offset) {
        if (version.getMajor() == 0) {
            imageType = ImageType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0));
            Flags = ImageFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 4));
            Comment = EndianUtilities.bytesToString(buffer, offset + 8, 256).replaceFirst("\0*$", "");
            LegacyGeometry = new GeometryRecord();
            LegacyGeometry.read(buffer, offset + 264);
            DiskSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 280);
            BlockSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 288);
            BlockCount = EndianUtilities.toInt32LittleEndian(buffer, offset + 292);
            BlocksAllocated = EndianUtilities.toInt32LittleEndian(buffer, offset + 296);
            UniqueId = EndianUtilities.toGuidLittleEndian(buffer, offset + 300);
            ModificationId = EndianUtilities.toGuidLittleEndian(buffer, offset + 316);
            ParentId = EndianUtilities.toGuidLittleEndian(buffer, offset + 332);
            HeaderSize = 348;
            BlocksOffset = HeaderSize + PreHeaderRecord.Size;
            DataOffset = BlocksOffset + BlockCount * 4;
            BlockExtraSize = 0;
            ParentModificationId = UUID.fromString("");
        } else if (version.getMajor() == 1 && version.getMinor() == 1) {
            HeaderSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
            imageType = ImageType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 4));
            Flags = ImageFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 8));
            Comment = EndianUtilities.bytesToString(buffer, offset + 12, 256).replaceFirst("\0*$", "");
            BlocksOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 268);
            DataOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 272);
            LegacyGeometry = new GeometryRecord();
            LegacyGeometry.read(buffer, offset + 276);
            DiskSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 296);
            BlockSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 304);
            BlockExtraSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 308);
            BlockCount = EndianUtilities.toInt32LittleEndian(buffer, offset + 312);
            BlocksAllocated = EndianUtilities.toInt32LittleEndian(buffer, offset + 316);
            UniqueId = EndianUtilities.toGuidLittleEndian(buffer, offset + 320);
            ModificationId = EndianUtilities.toGuidLittleEndian(buffer, offset + 336);
            ParentId = EndianUtilities.toGuidLittleEndian(buffer, offset + 352);
            ParentModificationId = EndianUtilities.toGuidLittleEndian(buffer, offset + 368);
            if (HeaderSize > 384) {
                LChsGeometry = new GeometryRecord();
                LChsGeometry.read(buffer, offset + 384);
            }

        } else {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unrecognized file version: " + version);
        }
        return HeaderSize;
    }

    public void write(Stream s) {
        byte[] buffer = new byte[HeaderSize];
        write(buffer, 0);
        s.write(buffer, 0, buffer.length);
    }

    public int write(byte[] buffer, int offset) {
        if (_fileVersion.getMajor() == 0) {
            EndianUtilities.writeBytesLittleEndian(imageType.ordinal(), buffer, offset + 0);
            EndianUtilities.writeBytesLittleEndian(Flags.ordinal(), buffer, offset + 4);
            EndianUtilities.stringToBytes(Comment, buffer, offset + 8, 256);
            LegacyGeometry.write(buffer, offset + 264);
            EndianUtilities.writeBytesLittleEndian(DiskSize, buffer, offset + 280);
            EndianUtilities.writeBytesLittleEndian(BlockSize, buffer, offset + 288);
            EndianUtilities.writeBytesLittleEndian(BlockCount, buffer, offset + 292);
            EndianUtilities.writeBytesLittleEndian(BlocksAllocated, buffer, offset + 296);
            EndianUtilities.writeBytesLittleEndian(UniqueId, buffer, offset + 300);
            EndianUtilities.writeBytesLittleEndian(ModificationId, buffer, offset + 316);
            EndianUtilities.writeBytesLittleEndian(ParentId, buffer, offset + 332);
        } else if (_fileVersion.getMajor() == 1 && _fileVersion.getMinor() == 1) {
            EndianUtilities.writeBytesLittleEndian(HeaderSize, buffer, offset + 0);
            EndianUtilities.writeBytesLittleEndian(imageType.ordinal(), buffer, offset + 4);
            EndianUtilities.writeBytesLittleEndian(Flags.ordinal(), buffer, offset + 8);
            EndianUtilities.stringToBytes(Comment, buffer, offset + 12, 256);
            EndianUtilities.writeBytesLittleEndian(BlocksOffset, buffer, offset + 268);
            EndianUtilities.writeBytesLittleEndian(DataOffset, buffer, offset + 272);
            LegacyGeometry.write(buffer, offset + 276);
            EndianUtilities.writeBytesLittleEndian(DiskSize, buffer, offset + 296);
            EndianUtilities.writeBytesLittleEndian(BlockSize, buffer, offset + 304);
            EndianUtilities.writeBytesLittleEndian(BlockExtraSize, buffer, offset + 308);
            EndianUtilities.writeBytesLittleEndian(BlockCount, buffer, offset + 312);
            EndianUtilities.writeBytesLittleEndian(BlocksAllocated, buffer, offset + 316);
            EndianUtilities.writeBytesLittleEndian(UniqueId, buffer, offset + 320);
            EndianUtilities.writeBytesLittleEndian(ModificationId, buffer, offset + 336);
            EndianUtilities.writeBytesLittleEndian(ParentId, buffer, offset + 352);
            EndianUtilities.writeBytesLittleEndian(ParentModificationId, buffer, offset + 368);
            if (HeaderSize > 384) {
                LChsGeometry.write(buffer, offset + 384);
            }

        } else {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unrecognized file version: " + _fileVersion);
        }
        return HeaderSize;
    }
}
