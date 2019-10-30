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
import dotnet4j.io.Stream;


public class HeaderRecord {
    private FileVersion fileVersion;

    public int blockCount;

    public int blockExtraSize;

    public int blocksAllocated;

    public int blockSize;

    public int blocksOffset;

    public String comment;

    public int dataOffset;

    public long diskSize;

    public ImageFlags flags = ImageFlags.None;

    public int headerSize;

    public ImageType imageType;

    public GeometryRecord lchsGeometry;

    public GeometryRecord legacyGeometry;

    public UUID modificationId;

    public UUID parentId = new UUID(0, 0);

    public UUID parentModificationId = new UUID(0, 0);

    public UUID uniqueId;

    public static HeaderRecord initialized(ImageType type, ImageFlags flags, long size, int blockSize, int blockExtra) {
        HeaderRecord result = new HeaderRecord();

        result.fileVersion = new FileVersion(0x00010001);
        result.headerSize = 400;
        result.imageType = type;
        result.flags = flags;
        result.comment = "Created by .NET DiscUtils";
        result.legacyGeometry = new GeometryRecord();
        result.diskSize = size;
        result.blockSize = blockSize;
        result.blockExtraSize = blockExtra;
        result.blockCount = (int) ((size + blockSize - 1) / blockSize);
        result.blocksAllocated = 0;

        result.blocksOffset = (PreHeaderRecord.Size + result.headerSize + 511) / 512 * 512;
        result.dataOffset = (result.blocksOffset + result.blockCount * 4 + 511) / 512 * 512;

        result.uniqueId = UUID.randomUUID();
        result.modificationId = UUID.randomUUID();

        result.lchsGeometry = new GeometryRecord();

        return result;
    }

    public void read(FileVersion version, Stream s) {
        int headerSize;
        fileVersion = version;
        // Determine header size...
        if (version.getMajor() == 0) {
            headerSize = 348;
        } else {
            long savedPos = s.getPosition();
//Debug.println("savedPos: " + savedPos);
            headerSize = EndianUtilities.toInt32LittleEndian(StreamUtilities.readExact(s, 4), 0);
//Debug.printf("headerSize: %1$8x, %1$d\n", headerSize);
            s.setPosition(savedPos);
//Debug.println("getPosition: " + s.getPosition());
        }
        byte[] buffer = StreamUtilities.readExact(s, headerSize);
//Debug.println("R:\n" + StringUtil.getDump(buffer, 64));
        read(version, buffer, 0);
    }

    public int read(FileVersion version, byte[] buffer, int offset) {
        if (version.getMajor() == 0) {
            imageType = ImageType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0));
            flags = ImageFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 4));
            comment = EndianUtilities.bytesToString(buffer, offset + 8, 256).replaceFirst("\0*$", "");
            legacyGeometry = new GeometryRecord();
            legacyGeometry.read(buffer, offset + 264);
            diskSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 280);
            blockSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 288);
            blockCount = EndianUtilities.toInt32LittleEndian(buffer, offset + 292);
            blocksAllocated = EndianUtilities.toInt32LittleEndian(buffer, offset + 296);
            uniqueId = EndianUtilities.toGuidLittleEndian(buffer, offset + 300);
            modificationId = EndianUtilities.toGuidLittleEndian(buffer, offset + 316);
            parentId = EndianUtilities.toGuidLittleEndian(buffer, offset + 332);
            headerSize = 348;
            blocksOffset = headerSize + PreHeaderRecord.Size;
            dataOffset = blocksOffset + blockCount * 4;
            blockExtraSize = 0;
            parentModificationId = new UUID(0L, 0L);
        } else if (version.getMajor() == 1 && version.getMinor() == 1) {
            headerSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
            imageType = ImageType.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 4));
            flags = ImageFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 8));
            comment = EndianUtilities.bytesToString(buffer, offset + 12, 256).replaceFirst("\0*$", "");
            blocksOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 268);
            dataOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 272);
            legacyGeometry = new GeometryRecord();
            legacyGeometry.read(buffer, offset + 276);
            diskSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 296);
            blockSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 304);
            blockExtraSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 308);
            blockCount = EndianUtilities.toInt32LittleEndian(buffer, offset + 312);
            blocksAllocated = EndianUtilities.toInt32LittleEndian(buffer, offset + 316);
            uniqueId = EndianUtilities.toGuidLittleEndian(buffer, offset + 320);
            modificationId = EndianUtilities.toGuidLittleEndian(buffer, offset + 336);
            parentId = EndianUtilities.toGuidLittleEndian(buffer, offset + 352);
            parentModificationId = EndianUtilities.toGuidLittleEndian(buffer, offset + 368);

            if (headerSize > 384) {
                lchsGeometry = new GeometryRecord();
                lchsGeometry.read(buffer, offset + 384);
            }

        } else {
            throw new dotnet4j.io.IOException("Unrecognized file version: " + version);
        }

        return headerSize;
    }

    public void write(Stream s) {
        byte[] buffer = new byte[headerSize];
        write(buffer, 0);
        s.write(buffer, 0, buffer.length);
    }

    public int write(byte[] buffer, int offset) {
        if (fileVersion.getMajor() == 0) {
            EndianUtilities.writeBytesLittleEndian(imageType.ordinal(), buffer, offset + 0);
            EndianUtilities.writeBytesLittleEndian(flags.ordinal(), buffer, offset + 4);
            EndianUtilities.stringToBytes(comment, buffer, offset + 8, 256);
            legacyGeometry.write(buffer, offset + 264);
            EndianUtilities.writeBytesLittleEndian(diskSize, buffer, offset + 280);
            EndianUtilities.writeBytesLittleEndian(blockSize, buffer, offset + 288);
            EndianUtilities.writeBytesLittleEndian(blockCount, buffer, offset + 292);
            EndianUtilities.writeBytesLittleEndian(blocksAllocated, buffer, offset + 296);
            EndianUtilities.writeBytesLittleEndian(uniqueId, buffer, offset + 300);
            EndianUtilities.writeBytesLittleEndian(modificationId, buffer, offset + 316);
            EndianUtilities.writeBytesLittleEndian(parentId, buffer, offset + 332);
        } else if (fileVersion.getMajor() == 1 && fileVersion.getMinor() == 1) {
            EndianUtilities.writeBytesLittleEndian(headerSize, buffer, offset + 0);
            EndianUtilities.writeBytesLittleEndian(imageType.ordinal(), buffer, offset + 4);
            EndianUtilities.writeBytesLittleEndian(flags.ordinal(), buffer, offset + 8);
            EndianUtilities.stringToBytes(comment, buffer, offset + 12, 256);
            EndianUtilities.writeBytesLittleEndian(blocksOffset, buffer, offset + 268);
            EndianUtilities.writeBytesLittleEndian(dataOffset, buffer, offset + 272);
            legacyGeometry.write(buffer, offset + 276);
            EndianUtilities.writeBytesLittleEndian(diskSize, buffer, offset + 296);
            EndianUtilities.writeBytesLittleEndian(blockSize, buffer, offset + 304);
            EndianUtilities.writeBytesLittleEndian(blockExtraSize, buffer, offset + 308);
            EndianUtilities.writeBytesLittleEndian(blockCount, buffer, offset + 312);
            EndianUtilities.writeBytesLittleEndian(blocksAllocated, buffer, offset + 316);
            EndianUtilities.writeBytesLittleEndian(uniqueId, buffer, offset + 320);
            EndianUtilities.writeBytesLittleEndian(modificationId, buffer, offset + 336);
            EndianUtilities.writeBytesLittleEndian(parentId, buffer, offset + 352);
            EndianUtilities.writeBytesLittleEndian(parentModificationId, buffer, offset + 368);

            if (headerSize > 384) {
                lchsGeometry.write(buffer, offset + 384);
            }

        } else {
            throw new dotnet4j.io.IOException("Unrecognized file version: " + fileVersion);
        }
//Debug.println("W:\n" + StringUtil.getDump(buffer, 64));
        return headerSize;
    }
}
