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

    public int Flags;

    public int FreeSector;

    public int NumGdEntries;

    public int SavedGeneration;

    public int UncleanShutdown;

    public ServerSparseExtentHeader() {
        MagicNumber = CowdMagicNumber;
        Version = 1;
        GrainSize = 512;
        NumGTEsPerGT = 4096;
        Flags = 3;
    }

    public static ServerSparseExtentHeader read(byte[] buffer, int offset) {
        ServerSparseExtentHeader hdr = new ServerSparseExtentHeader();
        hdr.MagicNumber = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x00);
        hdr.Version = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x04);
        hdr.Flags = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x08);
        hdr.Capacity = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x0C);
        hdr.GrainSize = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x10);
        hdr.GdOffset = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x14);
        hdr.NumGdEntries = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x18);
        hdr.FreeSector = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x1C);
        hdr.SavedGeneration = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x660);
        hdr.UncleanShutdown = EndianUtilities.toUInt32LittleEndian(buffer, offset + 0x66C);
        hdr.NumGTEsPerGT = 4096;
        return hdr;
    }

    public byte[] getBytes() {
        byte[] buffer = new byte[Sizes.Sector * 4];
        EndianUtilities.writeBytesLittleEndian(MagicNumber, buffer, 0x00);
        EndianUtilities.writeBytesLittleEndian(Version, buffer, 0x04);
        EndianUtilities.writeBytesLittleEndian(Flags, buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian((int) Capacity, buffer, 0x0C);
        EndianUtilities.writeBytesLittleEndian((int) GrainSize, buffer, 0x10);
        EndianUtilities.writeBytesLittleEndian((int) GdOffset, buffer, 0x14);
        EndianUtilities.writeBytesLittleEndian(NumGdEntries, buffer, 0x18);
        EndianUtilities.writeBytesLittleEndian(FreeSector, buffer, 0x1C);
        EndianUtilities.writeBytesLittleEndian(SavedGeneration, buffer, 0x660);
        EndianUtilities.writeBytesLittleEndian(UncleanShutdown, buffer, 0x66C);
        return buffer;
    }
}
