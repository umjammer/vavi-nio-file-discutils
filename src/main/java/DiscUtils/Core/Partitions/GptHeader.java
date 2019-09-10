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

package DiscUtils.Core.Partitions;

import java.util.UUID;

import DiscUtils.Core.Internal.Crc32Algorithm;
import DiscUtils.Core.Internal.Crc32LittleEndian;
import DiscUtils.Streams.Util.EndianUtilities;


public class GptHeader {
    public static final String GptSignature = "EFI PART";

    public long AlternateHeaderLba;

    public byte[] Buffer;

    public int Crc;

    public UUID DiskGuid;

    public int EntriesCrc;

    public long FirstUsable;

    public long HeaderLba;

    public int HeaderSize;

    public long LastUsable;

    public long PartitionEntriesLba;

    public int PartitionEntryCount;

    public int PartitionEntrySize;

    public String Signature;

    public int Version;

    public GptHeader(int sectorSize) {
        Signature = GptSignature;
        Version = 0x00010000;
        HeaderSize = 92;
        Buffer = new byte[sectorSize];
    }

    public GptHeader(GptHeader toCopy) {
        Signature = toCopy.Signature;
        Version = toCopy.Version;
        HeaderSize = toCopy.HeaderSize;
        Crc = toCopy.Crc;
        HeaderLba = toCopy.HeaderLba;
        AlternateHeaderLba = toCopy.AlternateHeaderLba;
        FirstUsable = toCopy.FirstUsable;
        LastUsable = toCopy.LastUsable;
        DiskGuid = toCopy.DiskGuid;
        PartitionEntriesLba = toCopy.PartitionEntriesLba;
        PartitionEntryCount = toCopy.PartitionEntryCount;
        PartitionEntrySize = toCopy.PartitionEntrySize;
        EntriesCrc = toCopy.EntriesCrc;
        Buffer = new byte[toCopy.Buffer.length];
        System.arraycopy(toCopy.Buffer, 0, Buffer, 0, Buffer.length);
    }

    public boolean readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.bytesToString(buffer, offset + 0, 8);
        Version = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        HeaderSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 12);
        Crc = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        HeaderLba = EndianUtilities.toInt64LittleEndian(buffer, offset + 24);
        AlternateHeaderLba = EndianUtilities.toInt64LittleEndian(buffer, offset + 32);
        FirstUsable = EndianUtilities.toInt64LittleEndian(buffer, offset + 40);
        LastUsable = EndianUtilities.toInt64LittleEndian(buffer, offset + 48);
        DiskGuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 56);
        PartitionEntriesLba = EndianUtilities.toInt64LittleEndian(buffer, offset + 72);
        PartitionEntryCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 80);
        PartitionEntrySize = EndianUtilities.toInt32LittleEndian(buffer, offset + 84);
        EntriesCrc = EndianUtilities.toUInt32LittleEndian(buffer, offset + 88);
        // In case the header has new fields unknown to us, store the entire header
        // as a byte array
        Buffer = new byte[HeaderSize];
        System.arraycopy(buffer, offset, Buffer, 0, HeaderSize);
        // Reject obviously invalid data
        if (!Signature.equals(GptSignature) || HeaderSize == 0) {
            return false;
        }

        return Crc == calcCrc(buffer, offset, HeaderSize);
    }

    public void writeTo(byte[] buffer, int offset) {
        // First, copy the cached header to allow for unknown fields
        System.arraycopy(Buffer, 0, buffer, offset, Buffer.length);
        // Next, write the fields
        EndianUtilities.stringToBytes(Signature, buffer, offset + 0, 8);
        EndianUtilities.writeBytesLittleEndian(Version, buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(HeaderSize, buffer, offset + 12);
        EndianUtilities.writeBytesLittleEndian(0, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(HeaderLba, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(AlternateHeaderLba, buffer, offset + 32);
        EndianUtilities.writeBytesLittleEndian(FirstUsable, buffer, offset + 40);
        EndianUtilities.writeBytesLittleEndian(LastUsable, buffer, offset + 48);
        EndianUtilities.writeBytesLittleEndian(DiskGuid, buffer, offset + 56);
        EndianUtilities.writeBytesLittleEndian(PartitionEntriesLba, buffer, offset + 72);
        EndianUtilities.writeBytesLittleEndian(PartitionEntryCount, buffer, offset + 80);
        EndianUtilities.writeBytesLittleEndian(PartitionEntrySize, buffer, offset + 84);
        EndianUtilities.writeBytesLittleEndian(EntriesCrc, buffer, offset + 88);
        // Calculate & write the CRC
        EndianUtilities.writeBytesLittleEndian(calcCrc(buffer, offset, HeaderSize), buffer, offset + 16);
        // Update the cached copy - re-allocate the buffer to allow for HeaderSize potentially having changed
        Buffer = new byte[HeaderSize];
        System.arraycopy(buffer, offset, Buffer, 0, HeaderSize);
    }

    public static int calcCrc(byte[] buffer, int offset, int count) {
        byte[] temp = new byte[count];
        System.arraycopy(buffer, offset, temp, 0, count);
        // Reset CRC field
        EndianUtilities.writeBytesLittleEndian(0, temp, 16);
        return Crc32LittleEndian.compute(Crc32Algorithm.Common, temp, 0, count);
    }

}
