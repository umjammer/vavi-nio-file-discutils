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
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import vavi.util.win32.DateUtil;

import discUtils.core.IDiagnosticTraceable;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class FileNameRecord implements IByteArraySerializable, IDiagnosticTraceable {

    public long allocatedSize;

    public long creationTime;

    public int eaSizeOrReparsePointTag;

    public String fileName;

    public FileNameNamespace fileNameNamespace;

    public EnumSet<FileAttributeFlags> flags;

    public long lastAccessTime;

    public long mftChangedTime;

    public long modificationTime;

    public FileRecordReference parentDirectory;

    public long realSize;

    public FileNameRecord() {
    }

    public FileNameRecord(FileNameRecord toCopy) {
        parentDirectory = toCopy.parentDirectory;
        creationTime = toCopy.creationTime;
        modificationTime = toCopy.modificationTime;
        mftChangedTime = toCopy.mftChangedTime;
        lastAccessTime = toCopy.lastAccessTime;
        allocatedSize = toCopy.allocatedSize;
        realSize = toCopy.realSize;
        flags = toCopy.flags;
        eaSizeOrReparsePointTag = toCopy.eaSizeOrReparsePointTag;
        fileNameNamespace = toCopy.fileNameNamespace;
        fileName = toCopy.fileName;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return convertFlags(flags);
    }

    public int size() {
        return 0x42 + fileName.length() * 2;
    }

    public int readFrom(byte[] buffer, int offset) {
        parentDirectory = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x00));
        creationTime = readDateTime(buffer, offset + 0x08);
        modificationTime = readDateTime(buffer, offset + 0x10);
        mftChangedTime = readDateTime(buffer, offset + 0x18);
        lastAccessTime = readDateTime(buffer, offset + 0x20);
        allocatedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        realSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30);
        flags = FileAttributeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x38));
        eaSizeOrReparsePointTag = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x3C);
        int fnLen = buffer[offset + 0x40] & 0xff;
        fileNameNamespace = FileNameNamespace.valueOf(buffer[offset + 0x41]);
        fileName = new String(buffer, offset + 0x42, fnLen * 2, StandardCharsets.UTF_16LE);
        return 0x42 + fnLen * 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(parentDirectory.getValue(), buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(creationTime), buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(modificationTime), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(mftChangedTime), buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(lastAccessTime), buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(allocatedSize, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(realSize, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian((int) FileAttributeFlags.valueOf(flags), buffer, offset + 0x38);
        EndianUtilities.writeBytesLittleEndian(eaSizeOrReparsePointTag, buffer, offset + 0x3C);
        buffer[offset + 0x40] = (byte) fileName.length();
        buffer[offset + 0x41] = (byte) fileNameNamespace.ordinal();
        byte[] bytes = fileName.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, buffer, offset + 0x42, bytes.length);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "FILE NAME RECORD");
        writer.println(indent + "   Parent Directory: " + parentDirectory);
        writer.println(indent + "      Creation Time: " + creationTime);
        writer.println(indent + "  Modification Time: " + modificationTime);
        writer.println(indent + "   MFT Changed Time: " + mftChangedTime);
        writer.println(indent + "   Last Access Time: " + lastAccessTime);
        writer.println(indent + "     Allocated Size: " + allocatedSize);
        writer.println(indent + "          Real Size: " + realSize);
        writer.println(indent + "              flags: " + flags);

        if (flags.contains(FileAttributeFlags.ReparsePoint)) {
            writer.println(indent + "  Reparse Point Tag: " + eaSizeOrReparsePointTag);
        } else {
            writer.println(indent + "      ext Attr Size: " + (eaSizeOrReparsePointTag & 0xFFFF));
        }

        writer.println(indent + "          Namespace: " + fileNameNamespace);
        writer.println(indent + "          File Name: " + fileName);
    }

    public boolean equals(FileNameRecord other) {
        if (other == null) {
            return false;
        }

        return parentDirectory.equals(other.parentDirectory) && fileNameNamespace == other.fileNameNamespace &&
               fileName.equals(other.fileName);
    }

    public String toString() {
        return fileName;
    }

    public static EnumSet<FileAttributeFlags> setAttributes(EnumSet<FileAttributes> attrs, EnumSet<FileAttributeFlags> flags) {
        EnumSet<FileAttributes> attrMask = EnumSet.allOf(FileAttributes.class);
        attrMask.remove(FileAttributes.Directory);
        EnumSet<FileAttributeFlags> newFlags = FileAttributeFlags.and(flags, FileAttributeFlags.valueOf(0xFFFF0000));
        newFlags.addAll(FileAttributes.cast(FileAttributeFlags.class, FileAttributes.and(attrs, attrMask)));
        return newFlags;
    }

    public static EnumSet<FileAttributes> convertFlags(EnumSet<FileAttributeFlags> flags) {
        EnumSet<FileAttributes> result = FileAttributeFlags
                .cast(FileAttributes.class, FileAttributeFlags.and(flags, FileAttributeFlags.valueOf(0xFFFF)));

        if (flags.contains(FileAttributeFlags.Directory)) {
            result.add(FileAttributes.Directory);
        }

        return result;
    }

    /** @return epoch millis */
    private static long readDateTime(byte[] buffer, int offset) {
        return DateUtil.fromFileTime(EndianUtilities.toInt64LittleEndian(buffer, offset));
    }
}
