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

import DiscUtils.Streams.ReaderWriter.BigEndianDataReader;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public final class XdrDataReader extends BigEndianDataReader {

    public XdrDataReader(Stream stream) {
        super(stream);
    }

    public boolean readBool() {
        return readUInt32() != 0;
    }

    public byte[] readBytes(int count) {
        byte[] buffer = StreamUtilities.readExact(_stream, count);
        if ((count & 0x3) != 0) {
            StreamUtilities.readExact(_stream, 4 - (count & 0x3));
        }

        return buffer;
    }

    public byte[] readBuffer() {
        int length = readUInt32();
        return readBytes(length);
    }

    public byte[] readBuffer(int maxLength) {
        int length = readUInt32();
        if (length <= maxLength) {
            return readBytes(length);
        }

        throw new IOException("Attempt to read buffer that is too long");
    }

    public String readString() {
        byte[] data = readBuffer();
        return new String(data, Charset.forName("ASCII"));
    }

    public String readString(int maxLength) {
        byte[] data = readBuffer(maxLength);
        return new String(data, Charset.forName("ASCII"));
    }
}
