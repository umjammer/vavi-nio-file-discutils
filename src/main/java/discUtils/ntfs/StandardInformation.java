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

package discUtils.ntfs;

import java.io.PrintWriter;
import java.util.EnumSet;

import discUtils.core.IDiagnosticTraceable;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;
import vavi.util.win32.DateUtil;


public final class StandardInformation implements IByteArraySerializable, IDiagnosticTraceable {

    private boolean haveExtraFields = true;

    public int classId;

    public long creationTime;

    public EnumSet<FileAttributeFlags> fileAttributeFlags;

    public long lastAccessTime;

    public int maxVersions;

    public long mftChangedTime;

    public long modificationTime;

    public int ownerId;

    public long quotaCharged;

    public int securityId;

    public long updateSequenceNumber;

    public int version;

    public int size() {
        return haveExtraFields ? 0x48 : 0x30;
    }

    public int readFrom(byte[] buffer, int offset) {
        creationTime = readDateTime(buffer, 0x00);
        modificationTime = readDateTime(buffer, 0x08);
        mftChangedTime = readDateTime(buffer, 0x10);
        lastAccessTime = readDateTime(buffer, 0x18);
        fileAttributeFlags = FileAttributeFlags.valueOf(ByteUtil.readLeInt(buffer, 0x20));
        maxVersions = ByteUtil.readLeInt(buffer, 0x24);
        version = ByteUtil.readLeInt(buffer, 0x28);
        classId = ByteUtil.readLeInt(buffer, 0x2C);
        if (buffer.length > 0x30) {
            ownerId = ByteUtil.readLeInt(buffer, 0x30);
            securityId = ByteUtil.readLeInt(buffer, 0x34);
            quotaCharged = ByteUtil.readLeLong(buffer, 0x38);
            updateSequenceNumber = ByteUtil.readLeLong(buffer, 0x40);
            haveExtraFields = true;
            return 0x48;
        }

        haveExtraFields = false;
        return 0x30;
    }

    public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeLong(DateUtil.toFileTime(creationTime), buffer, 0x00);
        ByteUtil.writeLeLong(DateUtil.toFileTime(modificationTime), buffer, 0x08);
        ByteUtil.writeLeLong(DateUtil.toFileTime(mftChangedTime), buffer, 0x10);
        ByteUtil.writeLeLong(DateUtil.toFileTime(lastAccessTime), buffer, 0x18);
        ByteUtil.writeLeInt((int) FileAttributeFlags.valueOf(fileAttributeFlags), buffer, 0x20);
        ByteUtil.writeLeInt(maxVersions, buffer, 0x24);
        ByteUtil.writeLeInt(version, buffer, 0x28);
        ByteUtil.writeLeInt(classId, buffer, 0x2C);
        if (haveExtraFields) {
            ByteUtil.writeLeInt(ownerId, buffer, 0x30);
            ByteUtil.writeLeInt(securityId, buffer, 0x34);
            ByteUtil.writeLeLong(quotaCharged, buffer, 0x38);
            ByteUtil.writeLeLong(updateSequenceNumber, buffer, 0x38);
        }

    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "      Creation Time: " + creationTime);
        writer.println(indent + "  Modification Time: " + modificationTime);
        writer.println(indent + "   MFT Changed Time: " + mftChangedTime);
        writer.println(indent + "   Last Access Time: " + lastAccessTime);
        writer.println(indent + "   File Permissions: " + fileAttributeFlags);
        writer.println(indent + "       Max Versions: " + maxVersions);
        writer.println(indent + "            Version: " + version);
        writer.println(indent + "           Class Id: " + classId);
        writer.println(indent + "        Security Id: " + securityId);
        writer.println(indent + "      Quota Charged: " + quotaCharged);
        writer.println(indent + "     Update Seq Num: " + updateSequenceNumber);
    }

    public static StandardInformation initializeNewFile(File file, EnumSet<FileAttributeFlags> flags) {
        long now = System.currentTimeMillis();
        NtfsStream siStream = file.createStream(AttributeType.StandardInformation, null);
        StandardInformation si = new StandardInformation();
        si.creationTime = now;
        si.modificationTime = now;
        si.mftChangedTime = now;
        si.lastAccessTime = now;
        si.fileAttributeFlags = flags;
        siStream.setContent(si);
        return si;
    }

    public static EnumSet<FileAttributes> convertFlags(EnumSet<FileAttributeFlags> flags, boolean isDirectory) {
        EnumSet<FileAttributes> result = FileAttributeFlags.cast(FileAttributes.class, flags);
        if (isDirectory) {
            result.add(FileAttributes.Directory);
        }

        return result;
    }

    public static EnumSet<FileAttributeFlags> setFileAttributes(EnumSet<FileAttributes> newAttributes,
                                                                EnumSet<FileAttributeFlags> existing) {
        int _newAttributes = (int) FileAttributes.valueOf(newAttributes);
        int _existing = (int) FileAttributeFlags.valueOf(existing);
        return FileAttributeFlags.valueOf((_existing & 0xFFFF_0000) | (_newAttributes & 0xFFFF));
    }

    private static long readDateTime(byte[] buffer, int offset) {
        return DateUtil.fromFileTime(ByteUtil.readLeLong(buffer, offset));
    }
}
