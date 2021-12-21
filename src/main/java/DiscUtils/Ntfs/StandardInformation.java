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
import java.util.EnumSet;

import vavi.util.win32.DateUtil;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class StandardInformation implements IByteArraySerializable, IDiagnosticTraceable {
    private boolean _haveExtraFields = true;

    public int _classId;

    public long _creationTime;

    public EnumSet<FileAttributeFlags> _fileAttributes;

    public long _lastAccessTime;

    public int _maxVersions;

    public long _mftChangedTime;

    public long _modificationTime;

    public int _ownerId;

    public long _quotaCharged;

    public int _securityId;

    public long _updateSequenceNumber;

    public int _version;

    public int size() {
        return _haveExtraFields ? 0x48 : 0x30;
    }

    public int readFrom(byte[] buffer, int offset) {
        _creationTime = readDateTime(buffer, 0x00);
        _modificationTime = readDateTime(buffer, 0x08);
        _mftChangedTime = readDateTime(buffer, 0x10);
        _lastAccessTime = readDateTime(buffer, 0x18);
        _fileAttributes = FileAttributeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, 0x20));
        _maxVersions = EndianUtilities.toUInt32LittleEndian(buffer, 0x24);
        _version = EndianUtilities.toUInt32LittleEndian(buffer, 0x28);
        _classId = EndianUtilities.toUInt32LittleEndian(buffer, 0x2C);
        if (buffer.length > 0x30) {
            _ownerId = EndianUtilities.toUInt32LittleEndian(buffer, 0x30);
            _securityId = EndianUtilities.toUInt32LittleEndian(buffer, 0x34);
            _quotaCharged = EndianUtilities.toUInt64LittleEndian(buffer, 0x38);
            _updateSequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, 0x40);
            _haveExtraFields = true;
            return 0x48;
        }

        _haveExtraFields = false;
        return 0x30;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_creationTime), buffer, 0x00);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_modificationTime), buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_mftChangedTime), buffer, 0x10);
        EndianUtilities.writeBytesLittleEndian(DateUtil.toFileTime(_lastAccessTime), buffer, 0x18);
        EndianUtilities.writeBytesLittleEndian((int) FileAttributeFlags.valueOf(_fileAttributes), buffer, 0x20);
        EndianUtilities.writeBytesLittleEndian(_maxVersions, buffer, 0x24);
        EndianUtilities.writeBytesLittleEndian(_version, buffer, 0x28);
        EndianUtilities.writeBytesLittleEndian(_classId, buffer, 0x2C);
        if (_haveExtraFields) {
            EndianUtilities.writeBytesLittleEndian(_ownerId, buffer, 0x30);
            EndianUtilities.writeBytesLittleEndian(_securityId, buffer, 0x34);
            EndianUtilities.writeBytesLittleEndian(_quotaCharged, buffer, 0x38);
            EndianUtilities.writeBytesLittleEndian(_updateSequenceNumber, buffer, 0x38);
        }

    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "      Creation Time: " + _creationTime);
        writer.println(indent + "  Modification Time: " + _modificationTime);
        writer.println(indent + "   MFT Changed Time: " + _mftChangedTime);
        writer.println(indent + "   Last Access Time: " + _lastAccessTime);
        writer.println(indent + "   File Permissions: " + _fileAttributes);
        writer.println(indent + "       Max Versions: " + _maxVersions);
        writer.println(indent + "            Version: " + _version);
        writer.println(indent + "           Class Id: " + _classId);
        writer.println(indent + "        Security Id: " + _securityId);
        writer.println(indent + "      Quota Charged: " + _quotaCharged);
        writer.println(indent + "     Update Seq Num: " + _updateSequenceNumber);
    }

    public static StandardInformation initializeNewFile(File file, EnumSet<FileAttributeFlags> flags) {
        long now = System.currentTimeMillis();
        NtfsStream siStream = file.createStream(AttributeType.StandardInformation, null);
        StandardInformation si = new StandardInformation();
        si._creationTime = now;
        si._modificationTime = now;
        si._mftChangedTime = now;
        si._lastAccessTime = now;
        si._fileAttributes = flags;
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
        return FileAttributeFlags.valueOf((_existing & 0xFFFF0000) | (_newAttributes & 0xFFFF));
    }

    private static long readDateTime(byte[] buffer, int offset) {
        return DateUtil.fromFileTime(EndianUtilities.toInt64LittleEndian(buffer, offset));
    }
}
