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

package discUtils.ntfs;

class CookedDataRun {

    public CookedDataRun(DataRun raw, long startVcn, long prevLcn, NonResidentAttributeRecord attributeExtent) {
        dataRun = raw;
        this.startVcn = startVcn;
        startLcn = prevLcn + raw.getRunOffset();
        this.attributeExtent = attributeExtent;

        if (startVcn < 0) {
            throw new IndexOutOfBoundsException("startVcn: VCN must be >= 0: " + startVcn);
        }

        if (startLcn < 0) {
            throw new IndexOutOfBoundsException("prevLcn: LCN must be >= 0: " + prevLcn);
        }
    }

    private NonResidentAttributeRecord attributeExtent;

    public NonResidentAttributeRecord getAttributeExtent() {
        return attributeExtent;
    }

    private DataRun dataRun;

    public DataRun getDataRun() {
        return dataRun;
    }

    public boolean isSparse() {
        return dataRun.isSparse();
    }

    public long getLength() {
        return dataRun.getRunLength();
    }

    public void setLength(long value) {
        dataRun.setRunLength(value);
    }

    private long startLcn;

    public long getStartLcn() {
        return startLcn;
    }

    public void setStartLcn(long value) {
        startLcn = value;
    }

    private long startVcn;

    public long getStartVcn() {
        return startVcn;
    }

    public String toString() {
        return "🍳{" + startVcn + ", " + getLength() + "}";
    }
}
