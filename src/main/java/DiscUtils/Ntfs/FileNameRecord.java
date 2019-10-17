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
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;

import vavi.util.win32.DateUtil;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class FileNameRecord implements IByteArraySerializable, IDiagnosticTraceable {
    public long AllocatedSize;

    public long CreationTime;

    public int EASizeOrReparsePointTag;

    public String FileName;

    public FileNameNamespace _FileNameNamespace;

    public EnumSet<FileAttributeFlags> Flags;

    public long LastAccessTime;

    public long MftChangedTime;

    public long ModificationTime;

    public FileRecordReference ParentDirectory;

    public long RealSize;

    public FileNameRecord() {
    }

    public FileNameRecord(FileNameRecord toCopy) {
        ParentDirectory = toCopy.ParentDirectory;
        CreationTime = toCopy.CreationTime;
        ModificationTime = toCopy.ModificationTime;
        MftChangedTime = toCopy.MftChangedTime;
        LastAccessTime = toCopy.LastAccessTime;
        AllocatedSize = toCopy.AllocatedSize;
        RealSize = toCopy.RealSize;
        Flags = toCopy.Flags;
        EASizeOrReparsePointTag = toCopy.EASizeOrReparsePointTag;
        _FileNameNamespace = toCopy._FileNameNamespace;
        FileName = toCopy.FileName;
    }

    public Map<String, Object> getFileAttributes() {
        return convertFlags(Flags);
    }

    public long getSize() {
        return 0x42 + FileName.length() * 2;
    }

    public int readFrom(byte[] buffer, int offset) {
        ParentDirectory = new FileRecordReference(EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x00));
        CreationTime = readDateTime(buffer, offset + 0x08);
        ModificationTime = readDateTime(buffer, offset + 0x10);
        MftChangedTime = readDateTime(buffer, offset + 0x18);
        LastAccessTime = readDateTime(buffer, offset + 0x20);
        AllocatedSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x28);
        RealSize = EndianUtilities.toUInt64LittleEndian(buffer, offset + 0x30);
        Flags = FileAttributeFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x38));
        EASizeOrReparsePointTag = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x3C);
        byte fnLen = buffer[offset + 0x40];
        _FileNameNamespace = FileNameNamespace.valueOf(buffer[offset + 0x41]);
        FileName = new String(buffer, offset + 0x42, fnLen * 2, Charset.forName("UTF-16LE"));
        return 0x42 + fnLen * 2;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(ParentDirectory.getValue(), buffer, offset + 0x00);
        EndianUtilities.writeBytesLittleEndian(CreationTime, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(ModificationTime, buffer, offset + 0x10);
        EndianUtilities.writeBytesLittleEndian(MftChangedTime, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(LastAccessTime, buffer, offset + 0x20);
        EndianUtilities.writeBytesLittleEndian(AllocatedSize, buffer, offset + 0x28);
        EndianUtilities.writeBytesLittleEndian(RealSize, buffer, offset + 0x30);
        EndianUtilities.writeBytesLittleEndian((int) FileAttributeFlags.valueOf(Flags), buffer, offset + 0x38);
        EndianUtilities.writeBytesLittleEndian(EASizeOrReparsePointTag, buffer, offset + 0x3C);
        buffer[offset + 0x40] = (byte) FileName.length();
        buffer[offset + 0x41] = (byte) _FileNameNamespace.ordinal();
        byte[] bytes = FileName.getBytes(Charset.forName("UTF-16LE"));
        System.arraycopy(bytes, 0, buffer, offset + 0x42, bytes.length);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "FILE NAME RECORD");
        writer.println(indent + "   Parent Directory: " + ParentDirectory);
        writer.println(indent + "      Creation Time: " + CreationTime);
        writer.println(indent + "  Modification Time: " + ModificationTime);
        writer.println(indent + "   MFT Changed Time: " + MftChangedTime);
        writer.println(indent + "   Last Access Time: " + LastAccessTime);
        writer.println(indent + "     Allocated Size: " + AllocatedSize);
        writer.println(indent + "          Real Size: " + RealSize);
        writer.println(indent + "              Flags: " + Flags);
        if (Flags.contains(FileAttributeFlags.ReparsePoint)) {
            writer.println(indent + "  Reparse Point Tag: " + EASizeOrReparsePointTag);
        } else {
            writer.println(indent + "      Ext Attr Size: " + (EASizeOrReparsePointTag & 0xFFFF));
        }
        writer.println(indent + "          Namespace: " + _FileNameNamespace);
        writer.println(indent + "          File Name: " + FileName);
    }

    public boolean equals(FileNameRecord other) {
        if (other == null) {
            return false;
        }

        return ParentDirectory == other.ParentDirectory && _FileNameNamespace == other._FileNameNamespace &&
               FileName.equals(other.FileName);
    }

    public String toString() {
        return FileName;
    }

    public static EnumSet<FileAttributeFlags> setAttributes(Map<String, Object> attrs, EnumSet<FileAttributeFlags> flags) {
        Map<String, Object> attrMask = FileAttributes.all();
        FileAttributes.not(attrMask, FileAttributes.Directory);
        EnumSet<FileAttributeFlags> newFlags = FileAttributeFlags.and(flags, 0xFFFF0000);
        newFlags.addAll(FileAttributeFlags.and(attrs, attrMask));
        return newFlags;
    }

    public static Map<String, Object> convertFlags(EnumSet<FileAttributeFlags> flags) {
        Map<String, Object> result = FileAttributeFlags.toMap(flags); // (int) flags & 0xFFFF
        if (flags.contains(FileAttributeFlags.Directory)) {
            result.put("Directory", true);
        }

        return result;
    }

    /** @return epoch millis */
    private static long readDateTime(byte[] buffer, int offset) {
        return DateUtil.filetimeToLong(EndianUtilities.toInt64LittleEndian(buffer, offset));
    }
}
