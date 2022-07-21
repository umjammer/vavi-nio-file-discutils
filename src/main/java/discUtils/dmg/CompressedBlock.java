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

package discUtils.dmg;

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


public class CompressedBlock implements IByteArraySerializable {
    public int blocksDescriptor;

    public UdifChecksum checkSum;

    public long dataStart;

    public int decompressBufferRequested;

    public long firstSector;

    public int infoVersion;

    public List<CompressedRun> runs;

    public long sectorCount;

    public int signature;

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        signature = EndianUtilities.toUInt32BigEndian(buffer, offset + 0);
        infoVersion = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        firstSector = EndianUtilities.toInt64BigEndian(buffer, offset + 8);
        sectorCount = EndianUtilities.toInt64BigEndian(buffer, offset + 16);
        dataStart = EndianUtilities.toUInt64BigEndian(buffer, offset + 24);
        decompressBufferRequested = EndianUtilities.toUInt32BigEndian(buffer, offset + 32);
        blocksDescriptor = EndianUtilities.toUInt32BigEndian(buffer, offset + 36);
        checkSum = EndianUtilities.toStruct(UdifChecksum.class, buffer, offset + 60);
        runs = new ArrayList<>();
        int numRuns = EndianUtilities.toInt32BigEndian(buffer, offset + 200);
        for (int i = 0; i < numRuns; ++i) {
            runs.add(EndianUtilities.toStruct(CompressedRun.class, buffer, offset + 204 + i * 40));
        }
        return 0;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
