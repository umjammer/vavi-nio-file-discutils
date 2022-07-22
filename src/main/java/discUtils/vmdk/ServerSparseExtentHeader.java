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

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Sizes;


public class ServerSparseExtentHeader extends CommonSparseExtentHeader {

    public static final int CowdMagicNumber = 0x44574f43;

    public int flags;

    public int freeSector;

    public int numGdEntries;

    public int savedGeneration;

    public int uncleanShutdown;

    public ServerSparseExtentHeader() {
        magicNumber = CowdMagicNumber;
        version = 1;
        grainSize = 512;
        numGTEsPerGT = 4096;
        flags = 3;
    }

    public static ServerSparseExtentHeader read(byte[] buffer, int offset) {
        ServerSparseExtentHeader hdr = new ServerSparseExtentHeader();
        hdr.magicNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
        hdr.version = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
        hdr.flags = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x08);
        hdr.capacity = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x0C);
        hdr.grainSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        hdr.gdOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x14);
        hdr.numGdEntries = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x18);
        hdr.freeSector = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x1C);
        hdr.savedGeneration = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x660);
        hdr.uncleanShutdown = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x66C);
        hdr.numGTEsPerGT = 4096;
        return hdr;
    }

    public byte[] getBytes() {
        byte[] buffer = new byte[Sizes.Sector * 4];
        EndianUtilities.writeBytesLittleEndian(magicNumber, buffer, 0x00);
        EndianUtilities.writeBytesLittleEndian(version, buffer, 0x04);
        EndianUtilities.writeBytesLittleEndian(flags, buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian((int) capacity, buffer, 0x0C);
        EndianUtilities.writeBytesLittleEndian((int) grainSize, buffer, 0x10);
        EndianUtilities.writeBytesLittleEndian((int) gdOffset, buffer, 0x14);
        EndianUtilities.writeBytesLittleEndian(numGdEntries, buffer, 0x18);
        EndianUtilities.writeBytesLittleEndian(freeSector, buffer, 0x1C);
        EndianUtilities.writeBytesLittleEndian(savedGeneration, buffer, 0x660);
        EndianUtilities.writeBytesLittleEndian(uncleanShutdown, buffer, 0x66C);
        return buffer;
    }
}
