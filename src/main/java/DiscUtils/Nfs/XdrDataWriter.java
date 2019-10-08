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

package DiscUtils.Nfs;

import java.nio.charset.Charset;

import DiscUtils.Streams.ReaderWriter.BigEndianDataWriter;
import moe.yo3explorer.dotnetio4j.Stream;


public final class XdrDataWriter extends BigEndianDataWriter {
    public XdrDataWriter(Stream stream) {
        super(stream);
    }

    public void write(boolean value) {
        write(value ? 1 : 0);
    }

    public void writeBytes(byte[] buffer, int offset, int count) {
        _stream.write(buffer, offset, count);
        if ((count & 0x3) != 0) {
            int padding = 4 - (buffer.length & 0x3);
            _stream.write(new byte[padding], 0, padding);
        }
    }

    public void writeBuffer(byte[] buffer) {
        writeBuffer(buffer, 0, buffer.length);
    }

    public void writeBuffer(byte[] buffer, int offset, int count) {
        if (buffer == null || count == 0) {
            write(0);
        } else {
            write(count);
            writeBytes(buffer, offset, count);
        }
    }

    public void write(String value) {
        writeBuffer(value.getBytes(Charset.forName("ASCII")));
    }
}
