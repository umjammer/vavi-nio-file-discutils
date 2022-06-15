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

package DiscUtils.Ntfs;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import vavi.util.win32.DateUtil;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class FileNameRecord implements IByteArraySerializable, IDiagnosticTraceable {
    public long _allocatedSize;

    public long _creationTime;

    public int _eaSizeOrReparsePointTag;

    public String _fileName;

    public FileNameNamespace _fileNameNamespace;

    public EnumSet<FileAttributeFlags> _flags;

    public long _lastAccessTime;

    public long _mftChangedTime;

    public long _modificationTime;

    public FileRecordReference _parentDirectory;

    public long _realSize;

    public FileNameRecord() {
    }

    public FileNameRecord(FileNameRecord toCopy) {
        _parentDirectory = toCopy._parentDirectory;
        _creationTime = toCopy._creationTime;
        _modificationTime = toCopy._modificationTime;
        _mftChangedTime = toCopy._mftChangedTime;
        _lastAccessTime = toCopy._lastAccessTime;
        _allocatedSize = toCopy._allocatedSize;
        _realSize = toCopy._realSize;
        _flags = toCopy._flags;
        _eaSizeOrReparsePointTag = toCopy._eaSizeOrReparsePointTag;
        _fileNameNamespace = toCopy._fileNameNamespace;
        _fileName = toCopy._fileName;
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return convertFlags(_flags);
    }

    public int size() {
        return 0x42 + _fileName.length() * 2;
    }

    public int readFrom(byte[] buffer, int offset) {
        _parentDirectory = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x00));
        _creationTime = readDateTime(buffer, offset + 0x08);
        _modificationTime = readDateTime(buffer, offset + 0x10);
        _mftChangedTime = readDateTime(buffer, offset + 0x18);
        _lastAccessTime = readDateTime(buffer, offset + 0x20);
        _allocatedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        _realSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30);
        _flags = FileAttributeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x38));
        _eaSizeOrReparsePointTag = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x3C);
        int fnLen = buffer[offset + 0x40] & 0xff;
        _fileNameNamespace = FileNameNamespace.valueOf(buffer[offset + 0x41]);
        _fileName = new String(buffer, offset + 0x42, fnLen * 2, StandardCharsets.UTF_16LE);
        return 0x42 + fnLen * 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(_parentDirectory.getValue(), buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_creationTime), buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_modificationTime), buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_mftChangedTime), buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_lastAccessTime), buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(_allocatedSize, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(_realSize, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian((int) FileAttributeFlags.valueOf(_flags), buffer, offset + 0x38);
        EndianUtilities.writeBytesLittleEndian(_eaSizeOrReparsePointTag, buffer, offset + 0x3C);
        buffer[offset + 0x40] = (byte) _fileName.length();
        buffer[offset + 0x41] = (byte) _fileNameNamespace.ordinal();
        byte[] bytes = _fileName.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, buffer, offset + 0x42, bytes.length);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "FILE NAME RECORD");
        writer.println(indent + "   Parent Directory: " + _parentDirectory);
        writer.println(indent + "      Creation Time: " + _creationTime);
        writer.println(indent + "  Modification Time: " + _modificationTime);
        writer.println(indent + "   MFT Changed Time: " + _mftChangedTime);
        writer.println(indent + "   Last Access Time: " + _lastAccessTime);
        writer.println(indent + "     Allocated Size: " + _allocatedSize);
        writer.println(indent + "          Real Size: " + _realSize);
        writer.println(indent + "              Flags: " + _flags);

        if (_flags.contains(FileAttributeFlags.ReparsePoint)) {
            writer.println(indent + "  Reparse Point Tag: " + _eaSizeOrReparsePointTag);
        } else {
            writer.println(indent + "      Ext Attr Size: " + (_eaSizeOrReparsePointTag & 0xFFFF));
        }

        writer.println(indent + "          Namespace: " + _fileNameNamespace);
        writer.println(indent + "          File Name: " + _fileName);
    }

    public boolean equals(FileNameRecord other) {
        if (other == null) {
            return false;
        }

        return _parentDirectory.equals(other._parentDirectory) && _fileNameNamespace == other._fileNameNamespace &&
               _fileName.equals(other._fileName);
    }

    public String toString() {
        return _fileName;
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
