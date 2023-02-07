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

package discUtils.udf;

import vavi.util.ByteUtil;


public final class SparablePartitionMap extends PartitionMap {

    public int[] locationsOfSparingTables;

    private byte numSparingTables;

    public int getNumSparingTables() {
        return numSparingTables & 0xff;
    }

    public short packetLength;

    public short partitionNumber;

    public int sparingTableSize;

    public short volumeSequenceNumber;

    @Override
    public int size() {
        return 64;
    }

    @Override
    protected int parse(byte[] buffer, int offset) {
        volumeSequenceNumber = ByteUtil.readLeShort(buffer, offset + 36);
        partitionNumber = ByteUtil.readLeShort(buffer, offset + 38);
        packetLength = ByteUtil.readLeShort(buffer, offset + 40);
        numSparingTables = buffer[offset + 42];
        sparingTableSize = ByteUtil.readLeInt(buffer, offset + 44);
        locationsOfSparingTables = new int[getNumSparingTables()];
        for (int i = 0; i < getNumSparingTables(); ++i) {
            locationsOfSparingTables[i] = ByteUtil.readLeInt(buffer, offset + 48 + 4 * i);
        }
        return 64;
    }
}
