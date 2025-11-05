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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import discUtils.core.internal.Crc32Algorithm;
import discUtils.core.internal.Crc32LittleEndian;
import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


public class GptHeader {

    private static final Logger logger = getLogger(GptHeader.class.getName());

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
        version = 0x0001_0000;
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
        signature = new String(buffer, offset + 0, 8, StandardCharsets.US_ASCII);
        version = ByteUtil.readLeInt(buffer, offset + 8);
        headerSize = ByteUtil.readLeInt(buffer, offset + 12);
        crc = ByteUtil.readLeInt(buffer, offset + 16);
        headerLba = ByteUtil.readLeLong(buffer, offset + 24);
        alternateHeaderLba = ByteUtil.readLeLong(buffer, offset + 32);
        firstUsable = ByteUtil.readLeLong(buffer, offset + 40);
        lastUsable = ByteUtil.readLeLong(buffer, offset + 48);
        diskGuid = ByteUtil.readLeUUID(buffer, offset + 56);
        partitionEntriesLba = ByteUtil.readLeLong(buffer, offset + 72);
        partitionEntryCount = ByteUtil.readLeInt(buffer, offset + 80);
        partitionEntrySize = ByteUtil.readLeInt(buffer, offset + 84);
        entriesCrc = ByteUtil.readLeInt(buffer, offset + 88);
        // In case the header has new fields unknown to us, store the entire header
        // as a byte array
        this.buffer = new byte[headerSize];
        System.arraycopy(buffer, offset, this.buffer, 0, headerSize);
        // Reject obviously invalid data
logger.log(Level.TRACE, "signature: " + signature);
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
        ByteUtil.writeLeInt(version, buffer, offset + 8);
        ByteUtil.writeLeInt(headerSize, buffer, offset + 12);
        ByteUtil.writeLeInt(0, buffer, offset + 16);
        ByteUtil.writeLeLong(headerLba, buffer, offset + 24);
        ByteUtil.writeLeLong(alternateHeaderLba, buffer, offset + 32);
        ByteUtil.writeLeLong(firstUsable, buffer, offset + 40);
        ByteUtil.writeLeLong(lastUsable, buffer, offset + 48);
        ByteUtil.writeLeUUID(diskGuid, buffer, offset + 56);
        ByteUtil.writeLeLong(partitionEntriesLba, buffer, offset + 72);
        ByteUtil.writeLeInt(partitionEntryCount, buffer, offset + 80);
        ByteUtil.writeLeInt(partitionEntrySize, buffer, offset + 84);
        ByteUtil.writeLeInt(entriesCrc, buffer, offset + 88);
        // Calculate & write the CRC
        ByteUtil.writeLeInt(calcCrc(buffer, offset, headerSize), buffer, offset + 16);
        // Update the cached copy - re-allocate the buffer to allow for headerSize potentially having changed
        this.buffer = new byte[headerSize];
        System.arraycopy(buffer, offset, this.buffer, 0, headerSize);
    }

    public static int calcCrc(byte[] buffer, int offset, int count) {
        byte[] temp = new byte[count];
        System.arraycopy(buffer, offset, temp, 0, count);
        // Reset CRC field
        ByteUtil.writeLeInt(0, temp, 16);
        return Crc32LittleEndian.compute(Crc32Algorithm.Common, temp, 0, count);
    }
}
