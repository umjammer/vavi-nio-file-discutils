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

package discUtils.core.partitions;

import java.util.UUID;

import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.streams.util.EndianUtilities;


public class GptHeader {

    public static final String GptSignature = "EFI PART";

    public long alternateHeaderLba;

    public byte[] buffer;

    public int crc;

    public UUID diskGuid;

    public int entriesCrc;

    public long firstUsable;

    public long headerLba;

    public int headerSize;

    public long lastUsable;

    public long partitionEntriesLba;

    public int partitionEntryCount;

    public int partitionEntrySize;

    public String signature;

    public int version;

    public GptHeader(int sectorSize) {
        signature = GptSignature;
        version = 0x00010000;
        headerSize = 92;
        buffer = new byte[sectorSize];
    }

    public GptHeader(GptHeader toCopy) {
        signature = toCopy.signature;
        version = toCopy.version;
        headerSize = toCopy.headerSize;
        crc = toCopy.crc;
        headerLba = toCopy.headerLba;
        alternateHeaderLba = toCopy.alternateHeaderLba;
        firstUsable = toCopy.firstUsable;
        lastUsable = toCopy.lastUsable;
        diskGuid = toCopy.diskGuid;
        partitionEntriesLba = toCopy.partitionEntriesLba;
        partitionEntryCount = toCopy.partitionEntryCount;
        partitionEntrySize = toCopy.partitionEntrySize;
        entriesCrc = toCopy.entriesCrc;
        buffer = new byte[toCopy.buffer.length];
        System.arraycopy(toCopy.buffer, 0, buffer, 0, buffer.length);
    }

    public boolean readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.bytesToString(buffer, offset + 0, 8);
        version = EndianUtilities.toUInt32LittleEndian(buffer, offset + 8);
        headerSize = EndianUtilities.toInt32LittleEndian(buffer, offset + 12);
        crc = EndianUtilities.toUInt32LittleEndian(buffer, offset + 16);
        headerLba = EndianUtilities.toInt64LittleEndian(buffer, offset + 24);
        alternateHeaderLba = EndianUtilities.toInt64LittleEndian(buffer, offset + 32);
        firstUsable = EndianUtilities.toInt64LittleEndian(buffer, offset + 40);
        lastUsable = EndianUtilities.toInt64LittleEndian(buffer, offset + 48);
        diskGuid = EndianUtilities.toGuidLittleEndian(buffer, offset + 56);
        partitionEntriesLba = EndianUtilities.toInt64LittleEndian(buffer, offset + 72);
        partitionEntryCount = EndianUtilities.toUInt32LittleEndian(buffer, offset + 80);
        partitionEntrySize = EndianUtilities.toInt32LittleEndian(buffer, offset + 84);
        entriesCrc = EndianUtilities.toUInt32LittleEndian(buffer, offset + 88);
        // In case the header has new fields unknown to us, store the entire header
        // as a byte array
        this.buffer = new byte[headerSize];
        System.arraycopy(buffer, offset, this.buffer, 0, headerSize);
        // Reject obviously invalid data
        if (!signature.equals(GptSignature) || headerSize == 0) {
            return false;
        }

        return crc == calcCrc(buffer, offset, headerSize);
    }

    public void writeTo(byte[] buffer, int offset) {
        // First, copy the cached header to allow for unknown fields
        System.arraycopy(this.buffer, 0, buffer, offset, this.buffer.length);
        // Next, write the fields
        EndianUtilities.stringToBytes(signature, buffer, offset + 0, 8);
        EndianUtilities.writeBytesLittleEndian(version, buffer, offset + 8);
        EndianUtilities.writeBytesLittleEndian(headerSize, buffer, offset + 12);
        EndianUtilities.writeBytesLittleEndian(0, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(headerLba, buffer, offset + 24);
        EndianUtilities.writeBytesLittleEndian(alternateHeaderLba, buffer, offset + 32);
        EndianUtilities.writeBytesLittleEndian(firstUsable, buffer, offset + 40);
        EndianUtilities.writeBytesLittleEndian(lastUsable, buffer, offset + 48);
        EndianUtilities.writeBytesLittleEndian(diskGuid, buffer, offset + 56);
        EndianUtilities.writeBytesLittleEndian(partitionEntriesLba, buffer, offset + 72);
        EndianUtilities.writeBytesLittleEndian(partitionEntryCount, buffer, offset + 80);
        EndianUtilities.writeBytesLittleEndian(partitionEntrySize, buffer, offset + 84);
        EndianUtilities.writeBytesLittleEndian(entriesCrc, buffer, offset + 88);
        // Calculate & write the CRC
        EndianUtilities.writeBytesLittleEndian(calcCrc(buffer, offset, headerSize), buffer, offset + 16);
        // Update the cached copy - re-allocate the buffer to allow for headerSize potentially having changed
        this.buffer = new byte[headerSize];
        System.arraycopy(buffer, offset, this.buffer, 0, headerSize);
    }

    public static int calcCrc(byte[] buffer, int offset, int count) {
        byte[] temp = new byte[count];
        System.arraycopy(buffer, offset, temp, 0, count);
        // Reset CRC field
        EndianUtilities.writeBytesLittleEndian(0, temp, 16);
        return Crc32LittleEndian.compute(Crc32Algorithm.Common, temp, 0, count);
    }
}
