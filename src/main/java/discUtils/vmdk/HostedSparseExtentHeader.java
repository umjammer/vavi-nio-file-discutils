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

package discUtils.vmdk;

import java.util.EnumSet;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;


public class HostedSparseExtentHeader extends CommonSparseExtentHeader {

    public static final int VmdkMagicNumber = 0x564d444b;

    public short compressAlgorithm;

    public long descriptorOffset;

    public long descriptorSize;

    public byte doubleEndLineChar1;

    public byte doubleEndLineChar2;

    public EnumSet<HostedSparseExtentFlags> flags;

    public byte nonEndLineChar;

    public long overhead;

    public long rgdOffset;

    public byte singleEndLineChar;

    public byte uncleanShutdown;

    public HostedSparseExtentHeader() {
        magicNumber = VmdkMagicNumber;
        version = 1;
        singleEndLineChar = (byte) '\n';
        nonEndLineChar = (byte) ' ';
        doubleEndLineChar1 = (byte) '\r';
        doubleEndLineChar2 = (byte) '\n';
    }

    public static HostedSparseExtentHeader read(byte[] buffer, int offset) {
        HostedSparseExtentHeader hdr = new HostedSparseExtentHeader();
        hdr.magicNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0);
        hdr.version = EndianUtilities.toUInt32LittleEndian(buffer, offset + 4);
        hdr.flags = HostedSparseExtentFlags.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, offset + 8));
        hdr.capacity = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x0C);
        hdr.grainSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x14);
        hdr.descriptorOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x1C);
        hdr.descriptorSize = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x24);
        hdr.numGTEsPerGT = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x2C);
        hdr.rgdOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x30);
        hdr.gdOffset = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x38);
        hdr.overhead = EndianUtilities.toInt64LittleEndian(buffer, offset + 0x40);
        hdr.uncleanShutdown = buffer[offset + 0x48];
        hdr.singleEndLineChar = buffer[offset + 0x49];
        hdr.nonEndLineChar = buffer[offset + 0x4A];
        hdr.doubleEndLineChar1 = buffer[offset + 0x4B];
        hdr.doubleEndLineChar2 = buffer[offset + 0x4C];
        hdr.compressAlgorithm = EndianUtilities.toUInt16LittleEndian(buffer, offset + 0x4D);
        return hdr;
    }

    public byte[] getBytes() {
        byte[] buffer = new byte[Sizes.Sector];
        EndianUtilities.writeBytesLittleEndian(magicNumber, buffer, 0x00);
        EndianUtilities.writeBytesLittleEndian(version, buffer, 0x04);
        EndianUtilities.writeBytesLittleEndian((int) HostedSparseExtentFlags.valueOf(flags), buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian(capacity, buffer, 0x0C);
        EndianUtilities.writeBytesLittleEndian(grainSize, buffer, 0x14);
        EndianUtilities.writeBytesLittleEndian(descriptorOffset, buffer, 0x1C);
        EndianUtilities.writeBytesLittleEndian(descriptorSize, buffer, 0x24);
        EndianUtilities.writeBytesLittleEndian(numGTEsPerGT, buffer, 0x2C);
        EndianUtilities.writeBytesLittleEndian(rgdOffset, buffer, 0x30);
        EndianUtilities.writeBytesLittleEndian(gdOffset, buffer, 0x38);
        EndianUtilities.writeBytesLittleEndian(overhead, buffer, 0x40);
        buffer[0x48] = uncleanShutdown;
        buffer[0x49] = singleEndLineChar;
        buffer[0x4A] = nonEndLineChar;
        buffer[0x4B] = doubleEndLineChar1;
        buffer[0x4C] = doubleEndLineChar2;
        EndianUtilities.writeBytesLittleEndian(compressAlgorithm, buffer, 0x4D);
        return buffer;
    }
}
