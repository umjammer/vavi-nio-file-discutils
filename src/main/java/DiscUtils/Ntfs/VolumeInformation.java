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

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class VolumeInformation implements IByteArraySerializable, IDiagnosticTraceable {
    public static final int VersionNt4 = 0x0102;

    public static final int VersionW2k = 0x0300;

    public static final int VersionXp = 0x0301;

    private byte _majorVersion;

    private byte _minorVersion;

    public VolumeInformation() {
    }

    public VolumeInformation(byte major, byte minor, EnumSet<VolumeInformationFlags> flags) {
        _majorVersion = major;
        _minorVersion = minor;
        setFlags(flags);
    }

    private EnumSet<VolumeInformationFlags> _flags;

    public EnumSet<VolumeInformationFlags> getFlags() {
        return _flags;
    }

    public void setFlags(EnumSet<VolumeInformationFlags> value) {
        _flags = value;
    }

    public int getVersion() {
        return (_majorVersion & 0xff) << 8 | (_minorVersion & 0xff);
    }

    public int size() {
        return 0x0C;
    }

    public int readFrom(byte[] buffer, int offset) {
        _majorVersion = buffer[offset + 0x08];
        _minorVersion = buffer[offset + 0x09];
        setFlags(VolumeInformationFlags.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x0A)));
        return 0x0C;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian((long) 0, buffer, offset + 0x00);
        buffer[offset + 0x08] = _majorVersion;
        buffer[offset + 0x09] = _minorVersion;
        EndianUtilities.writeBytesLittleEndian((short) VolumeInformationFlags.valueOf(getFlags()), buffer, offset + 0x0A);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "  Version: " + _majorVersion + "." + _minorVersion);
        writer.println(indent + "    Flags: " + _flags);
    }
}
