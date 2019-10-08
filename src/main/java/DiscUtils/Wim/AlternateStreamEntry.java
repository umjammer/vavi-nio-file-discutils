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

package DiscUtils.Wim;

import java.nio.charset.Charset;

import DiscUtils.Streams.ReaderWriter.DataReader;


public class AlternateStreamEntry {
    public byte[] Hash;

    public long Length;

    public String Name;

    public static AlternateStreamEntry readFrom(DataReader reader) {
        long startPos = reader.getPosition();
        long length = reader.readInt64();
        if (length == 0) {
            return null;
        }

        reader.skip(8);
        AlternateStreamEntry result = new AlternateStreamEntry();
        result.Length = length;
        result.Hash = reader.readBytes(20);
        int nameLength = reader.readUInt16();
        if (nameLength > 0) {
            result.Name = new String(reader.readBytes(nameLength + 2), Charset.forName("Unicode")).replaceAll("\0*$", "");
        } else {
            result.Name = "";
        }
        if (startPos + length > reader.getPosition()) {
            int toRead = (int) (startPos + length - reader.getPosition());
            reader.skip(toRead);
        }

        return result;
    }
}
