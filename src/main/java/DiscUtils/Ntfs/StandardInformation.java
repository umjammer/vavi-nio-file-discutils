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
import java.util.Map;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class StandardInformation implements IByteArraySerializable, IDiagnosticTraceable {
    private boolean _haveExtraFields = true;

    public int ClassId;

    public long CreationTime;

    public EnumSet<FileAttributeFlags> _FileAttributes;

    public long LastAccessTime;

    public int MaxVersions;

    public long MftChangedTime;

    public long ModificationTime;

    public int OwnerId;

    public long QuotaCharged;

    public int SecurityId;

    public long UpdateSequenceNumber;

    public int Version;

    public long getSize() {
        return _haveExtraFields ? 0x48 : 0x30;
    }

    public int readFrom(byte[] buffer, int offset) {
        CreationTime = readDateTime(buffer, 0x00);
        ModificationTime = readDateTime(buffer, 0x08);
        MftChangedTime = readDateTime(buffer, 0x10);
        LastAccessTime = readDateTime(buffer, 0x18);
        _FileAttributes = FileAttributeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, 0x20));
        MaxVersions = EndianUtilities.toUInt32LittleEndian(buffer, 0x24);
        Version = EndianUtilities.toUInt32LittleEndian(buffer, 0x28);
        ClassId = EndianUtilities.toUInt32LittleEndian(buffer, 0x2C);
        if (buffer.length > 0x30) {
            OwnerId = EndianUtilities.toUInt32LittleEndian(buffer, 0x30);
            SecurityId = EndianUtilities.toUInt32LittleEndian(buffer, 0x34);
            QuotaCharged = EndianUtilities.toUInt64LittleEndian(buffer, 0x38);
            UpdateSequenceNumber = EndianUtilities.toUInt64LittleEndian(buffer, 0x40);
            _haveExtraFields = true;
            return 0x48;
        }

        _haveExtraFields = false;
        return 0x30;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(CreationTime, buffer, 0x00);
        EndianUtilities.writeBytesLittleEndian(ModificationTime, buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian(MftChangedTime, buffer, 0x10);
        EndianUtilities.writeBytesLittleEndian(LastAccessTime, buffer, 0x18);
        EndianUtilities.writeBytesLittleEndian((int) FileAttributeFlags.valueOf(_FileAttributes), buffer, 0x20);
        EndianUtilities.writeBytesLittleEndian(MaxVersions, buffer, 0x24);
        EndianUtilities.writeBytesLittleEndian(Version, buffer, 0x28);
        EndianUtilities.writeBytesLittleEndian(ClassId, buffer, 0x2C);
        if (_haveExtraFields) {
            EndianUtilities.writeBytesLittleEndian(OwnerId, buffer, 0x30);
            EndianUtilities.writeBytesLittleEndian(SecurityId, buffer, 0x34);
            EndianUtilities.writeBytesLittleEndian(QuotaCharged, buffer, 0x38);
            EndianUtilities.writeBytesLittleEndian(UpdateSequenceNumber, buffer, 0x38);
        }

    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "      Creation Time: " + CreationTime);
        writer.println(indent + "  Modification Time: " + ModificationTime);
        writer.println(indent + "   MFT Changed Time: " + MftChangedTime);
        writer.println(indent + "   Last Access Time: " + LastAccessTime);
        writer.println(indent + "   File Permissions: " + _FileAttributes);
        writer.println(indent + "       Max Versions: " + MaxVersions);
        writer.println(indent + "            Version: " + Version);
        writer.println(indent + "           Class Id: " + ClassId);
        writer.println(indent + "        Security Id: " + SecurityId);
        writer.println(indent + "      Quota Charged: " + QuotaCharged);
        writer.println(indent + "     Update Seq Num: " + UpdateSequenceNumber);
    }

    public static StandardInformation initializeNewFile(File file, EnumSet<FileAttributeFlags> flags) {
        long now = System.currentTimeMillis();
        NtfsStream siStream = file.createStream(AttributeType.StandardInformation,null);
        StandardInformation si = new StandardInformation();
        si.CreationTime = now;
        si.ModificationTime = now;
        si.MftChangedTime = now;
        si.LastAccessTime = now;
        si._FileAttributes = flags;
        siStream.setContent(si);
        return si;
    }

    public static Map<String, Object> convertFlags(EnumSet<FileAttributeFlags> flags, boolean isDirectory) {
        Map<String, Object> result = FileAttributeFlags.convert(flags);
        if (isDirectory) {
            result.put("Directory", true);
        }

        return result;
    }

    public static EnumSet<FileAttributeFlags> setFileAttributes(Map<String, Object> newAttributes, EnumSet<FileAttributeFlags> existing) {
        int _newAttributes = (int) FileAttributeFlags.valueOf(FileAttributeFlags.convert(newAttributes));
        int _existing = (int) FileAttributeFlags.valueOf(existing);
        return FileAttributeFlags.valueOf((_existing & 0xFFFF0000) | ( _newAttributes & 0xFFFF));
    }

    private static long readDateTime(byte[] buffer, int offset) {
        return EndianUtilities.toInt64LittleEndian(buffer, offset);
    }
}