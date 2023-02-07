//
// Copyright (c) 2016, Bianco Veigel
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

import java.util.ArrayList;
import java.util.List;

import dotnet4j.io.IOException;


public class BTreeExtentLeaf extends BTreeExtentHeader {

    private List<Extent> extents;

    @Override
    public List<Extent> getExtents() {
        return extents;
    }

    public void setExtents(List<Extent> value) {
        extents = value;
    }

    @Override
    public int size() {
        return super.size() + (getNumberOfRecords() * 0x10);
    }

    @Override
    public int readFrom(byte[] buffer, int offset) {
        offset += super.readFrom(buffer, offset);
        if (getLevel() != 0)
            throw new IOException("invalid B+tree level - expected 0");

        extents = new ArrayList<>(getNumberOfRecords());
        for (int i = 0; i < getNumberOfRecords(); i++) {
            Extent rec = new Extent();
            offset += rec.readFrom(buffer, offset);
            extents.add(rec);
        }
        return size();
    }

    /**
     *
     */
    @Override
    public void loadBtree(Context context) {
    }
}
