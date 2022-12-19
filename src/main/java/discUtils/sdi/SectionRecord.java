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

package discUtils.sdi;

import java.nio.charset.StandardCharsets;

import vavi.util.ByteUtil;


public class SectionRecord {

    public static final int RecordSize = 64;

    public long attr;

    public long offset;

    public long partitionType;

    public String sectionType;

    public long size;

    public void readFrom(byte[] buffer, int offset) {
        sectionType = new String(buffer, offset, 8, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        attr = ByteUtil.readLeLong(buffer, offset + 8);
        this.offset = ByteUtil.readLeLong(buffer, offset + 16);
        size = ByteUtil.readLeLong(buffer, offset + 24);
        partitionType = ByteUtil.readLeLong(buffer, offset + 32);
    }
}
