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

package DiscUtils.Iscsi;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.Util.EndianUtilities;


public class ScsiReportLunsResponse extends ScsiResponse {
    private int _availableLuns;

    private List<Long> __Luns;

    public List<Long> getLuns() {
        return __Luns;
    }

    public void setLuns(List<Long> value) {
        __Luns = value;
    }

    public int getNeededDataLength() {
        return _availableLuns * 8 + 8;
    }

    public boolean getTruncated() {
        return _availableLuns != getLuns().size();
    }

    public void readFrom(byte[] buffer, int offset, int count) {
        setLuns(new ArrayList<Long>());
        if (count == 0) {
            return;
        }

        if (count < 8) {
            throw new IllegalArgumentException("Data truncated too far");
        }

        _availableLuns = EndianUtilities.toUInt32BigEndian(buffer, offset) / 8;
        int pos = 8;
        while (pos <= count - 8 && getLuns().size() < _availableLuns) {
            getLuns().add(EndianUtilities.toUInt64BigEndian(buffer, offset + pos));
            pos += 8;
        }
    }
}
