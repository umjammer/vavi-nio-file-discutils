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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;
import vavi.util.Debug;


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
            long savedPos = s.position();
            headerSize = ByteUtil.readLeInt(StreamUtilities.readExact(s, 4), 0);
            s.position(savedPos);
        }

        byte[] buffer = StreamUtilities.readExact(s, headerSize);
        read(version, buffer, 0);
    }

    public int read(FileVersion version, byte[] buffer, int offset) {
        if (version.getMajor() == 0) {
            imageType = ImageType.values()[ByteUtil.readLeInt(buffer, offset + 0)];
            flags = ImageFlags.values()[ByteUtil.readLeInt(buffer, offset + 4)];
            comment = new String(buffer, offset + 8, 256, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
            legacyGeometry = new GeometryRecord();
            legacyGeometry.read(buffer, offset + 264);
            diskSize = ByteUtil.readLeLong(buffer, offset + 280);
            blockSize = ByteUtil.readLeInt(buffer, offset + 288);
            blockCount = ByteUtil.readLeInt(buffer, offset + 292);
            blocksAllocated = ByteUtil.readLeInt(buffer, offset + 296);
            uniqueId = ByteUtil.readLeUUID(buffer, offset + 300);
            modificationId = ByteUtil.readLeUUID(buffer, offset + 316);
            parentId = ByteUtil.readLeUUID(buffer, offset + 332);
            headerSize = 348;
            blocksOffset = headerSize + PreHeaderRecord.Size;
            dataOffset = blocksOffset + blockCount * 4;
            blockExtraSize = 0;
            parentModificationId = new UUID(0L, 0L);
        } else if (version.getMajor() == 1 && version.getMinor() == 1) {
            headerSize = ByteUtil.readLeInt(buffer, offset + 0);
            imageType = ImageType.values()[ByteUtil.readLeInt(buffer, offset + 4)];
            flags = ImageFlags.values()[ByteUtil.readLeInt(buffer, offset + 8)];
            comment = new String(buffer, offset + 12, 256, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
            blocksOffset = ByteUtil.readLeInt(buffer, offset + 268);
            dataOffset = ByteUtil.readLeInt(buffer, offset + 272);
            legacyGeometry = new GeometryRecord();
            legacyGeometry.read(buffer, offset + 276);
            diskSize = ByteUtil.readLeLong(buffer, offset + 296);
            blockSize = ByteUtil.readLeInt(buffer, offset + 304);
            blockExtraSize = ByteUtil.readLeInt(buffer, offset + 308);
            blockCount = ByteUtil.readLeInt(buffer, offset + 312);
            blocksAllocated = ByteUtil.readLeInt(buffer, offset + 316);
            uniqueId = ByteUtil.readLeUUID(buffer, offset + 320);
            modificationId = ByteUtil.readLeUUID(buffer, offset + 336);
            parentId = ByteUtil.readLeUUID(buffer, offset + 352);
            parentModificationId = ByteUtil.readLeUUID(buffer, offset + 368);

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
//Debug.println(s.position());
        s.write(buffer, 0, buffer.length);
    }

    public int write(byte[] buffer, int offset) {
        if (fileVersion.getMajor() == 0) {
            ByteUtil.writeLeInt(imageType.ordinal(), buffer, offset + 0);
            ByteUtil.writeLeInt(flags.ordinal(), buffer, offset + 4);
            EndianUtilities.stringToBytes(comment, buffer, offset + 8, 256);
            legacyGeometry.write(buffer, offset + 264);
            ByteUtil.writeLeLong(diskSize, buffer, offset + 280);
            ByteUtil.writeLeInt(blockSize, buffer, offset + 288);
            ByteUtil.writeLeInt(blockCount, buffer, offset + 292);
            ByteUtil.writeLeInt(blocksAllocated, buffer, offset + 296);
            ByteUtil.writeLeUUID(uniqueId, buffer, offset + 300);
            ByteUtil.writeLeUUID(modificationId, buffer, offset + 316);
            ByteUtil.writeLeUUID(parentId, buffer, offset + 332);
        } else if (fileVersion.getMajor() == 1 && fileVersion.getMinor() == 1) {
            ByteUtil.writeLeInt(headerSize, buffer, offset + 0);
Debug.println(Level.FINE, headerSize);
            ByteUtil.writeLeInt(imageType.ordinal(), buffer, offset + 4);
            ByteUtil.writeLeInt(flags.ordinal(), buffer, offset + 8);
            EndianUtilities.stringToBytes(comment, buffer, offset + 12, 256);
            ByteUtil.writeLeInt(blocksOffset, buffer, offset + 268);
            ByteUtil.writeLeInt(dataOffset, buffer, offset + 272);
            legacyGeometry.write(buffer, offset + 276);
            ByteUtil.writeLeLong(diskSize, buffer, offset + 296);
            ByteUtil.writeLeInt(blockSize, buffer, offset + 304);
            ByteUtil.writeLeInt(blockExtraSize, buffer, offset + 308);
            ByteUtil.writeLeInt(blockCount, buffer, offset + 312);
            ByteUtil.writeLeInt(blocksAllocated, buffer, offset + 316);
            ByteUtil.writeLeUUID(uniqueId, buffer, offset + 320);
            ByteUtil.writeLeUUID(modificationId, buffer, offset + 336);
            ByteUtil.writeLeUUID(parentId, buffer, offset + 352);
            ByteUtil.writeLeUUID(parentModificationId, buffer, offset + 368);

            if (headerSize > 384) {
                lchsGeometry.write(buffer, offset + 384);
            }
        } else {
            throw new dotnet4j.io.IOException("Unrecognized file version: " + fileVersion);
        }

        return headerSize;
    }
}
