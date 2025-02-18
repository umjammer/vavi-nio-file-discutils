//
// Copyright (c) 2016, Bianco Veigel
// Copyright (c) 2017, Timo Walter
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

package discUtils.xfs;

import dotnet4j.io.IOException;


public class BTreeInodeLeaf extends BtreeHeader {

    private BTreeInodeRecord[] records;

    public BTreeInodeRecord[] getRecords() {
        return records;
    }

    public void setRecords(BTreeInodeRecord[] value) {
        records = value;
    }

    @Override
    public int size() {
        return super.size() + (getNumberOfRecords() * 0x10);
    }

    public BTreeInodeLeaf(int superBlockVersion) {
        super(superBlockVersion);
    }

    @Override
    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        offset += super.size();
        if (getLevel() != 0)
            throw new IOException("invalid B+tree level - expected 1");

        records = new BTreeInodeRecord[getNumberOfRecords()];
        for (int i = 0; i < getNumberOfRecords(); i++) {
            BTreeInodeRecord rec = new BTreeInodeRecord();
            offset += rec.readFrom(buffer, offset);
            records[i] = rec;
        }
        return size();
    }

    @Override
    public void loadBtree(AllocationGroup ag) {
    }
}
