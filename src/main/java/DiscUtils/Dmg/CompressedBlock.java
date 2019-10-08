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

package DiscUtils.Dmg;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class CompressedBlock implements IByteArraySerializable {
    public int BlocksDescriptor;

    public UdifChecksum CheckSum;

    public long DataStart;

    public int DecompressBufferRequested;

    public long FirstSector;

    public int InfoVersion;

    public List<CompressedRun> Runs;

    public long SectorCount;

    public int Signature;

    public long getSize() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        Signature = EndianUtilities.toUInt32BigEndian(buffer, offset + 0);
        InfoVersion = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        FirstSector = EndianUtilities.toInt64BigEndian(buffer, offset + 8);
        SectorCount = EndianUtilities.toInt64BigEndian(buffer, offset + 16);
        DataStart = EndianUtilities.toUInt64BigEndian(buffer, offset + 24);
        DecompressBufferRequested = EndianUtilities.toUInt32BigEndian(buffer, offset + 32);
        BlocksDescriptor = EndianUtilities.toUInt32BigEndian(buffer, offset + 36);
        CheckSum = EndianUtilities.<UdifChecksum> toStruct(UdifChecksum.class, buffer, offset + 60);
        Runs = new ArrayList<>();
        int numRuns = EndianUtilities.toInt32BigEndian(buffer, offset + 200);
        for (int i = 0; i < numRuns; ++i) {
            Runs.add(EndianUtilities.<CompressedRun> toStruct(CompressedRun.class, buffer, offset + 204 + i * 40));
        }
        return 0;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
