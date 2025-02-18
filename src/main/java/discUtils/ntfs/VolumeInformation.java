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
import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class VolumeInformation implements IByteArraySerializable, IDiagnosticTraceable {

    public static final int VersionNt4 = 0x0102;

    public static final int VersionW2k = 0x0300;

    public static final int VersionXp = 0x0301;

    private byte majorVersion;

    private byte minorVersion;

    public VolumeInformation() {
    }

    public VolumeInformation(byte major, byte minor, EnumSet<VolumeInformationFlags> flags) {
        majorVersion = major;
        minorVersion = minor;
        setFlags(flags);
    }

    private EnumSet<VolumeInformationFlags> flags;

    public EnumSet<VolumeInformationFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<VolumeInformationFlags> value) {
        flags = value;
    }

    public int getVersion() {
        return (majorVersion & 0xff) << 8 | (minorVersion & 0xff);
    }

    @Override public int size() {
        return 0x0c;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        majorVersion = buffer[offset + 0x08];
        minorVersion = buffer[offset + 0x09];
        setFlags(VolumeInformationFlags.valueOf(ByteUtil.readLeShort(buffer, offset + 0x0a)));
        return 0x0c;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeLong(0, buffer, offset + 0x00);
        buffer[offset + 0x08] = majorVersion;
        buffer[offset + 0x09] = minorVersion;
        ByteUtil.writeLeShort((short) VolumeInformationFlags.valueOf(getFlags()), buffer, offset + 0x0a);
    }

    @Override public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "  Version: " + majorVersion + "." + minorVersion);
        writer.println(indent + "    Flags: " + flags);
    }

    @Override public String toString() {
        return "VolumeInformation: version: " + majorVersion + "." + minorVersion;
    }
}
