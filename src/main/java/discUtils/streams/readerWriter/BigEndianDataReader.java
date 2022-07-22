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

package discUtils.streams.readerWriter;

import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.Stream;


public class BigEndianDataReader extends DataReader {

    public BigEndianDataReader(Stream stream) {
        super(stream);
    }

    public short readUInt16() {
        readToBuffer(2);
        return EndianUtilities.toUInt16BigEndian(buffer, 0);
    }

    public int readInt32() {
        readToBuffer(4);
        return EndianUtilities.toInt32BigEndian(buffer, 0);
    }

    public int readUInt32() {
        readToBuffer(4);
        return EndianUtilities.toUInt32BigEndian(buffer, 0);
    }

    public long readInt64() {
        readToBuffer(8);
        return EndianUtilities.toInt64BigEndian(buffer, 0);
    }

    public long readUInt64() {
        readToBuffer(8);
        return EndianUtilities.toUInt64BigEndian(buffer, 0);
    }
}
