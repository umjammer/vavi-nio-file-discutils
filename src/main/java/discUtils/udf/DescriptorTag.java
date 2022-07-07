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

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class DescriptorTag implements IByteArraySerializable {
    public short DescriptorCrc;

    private short DescriptorCrcLength;

    public int getDescriptorCrcLength() {
        return DescriptorCrcLength;
    }

    public short DescriptorVersion;

    public byte TagChecksum;

    public TagIdentifier _TagIdentifier;

    public int TagLocation;

    public short TagSerialNumber;

    public int size() {
        return 16;
    }

    public int readFrom(byte[] buffer, int offset) {
        _TagIdentifier = TagIdentifier.valueOf(EndianUtilities.toUInt16LittleEndian(buffer, offset));
        DescriptorVersion = EndianUtilities.toUInt16LittleEndian(buffer, offset + 2);
        TagChecksum = buffer[offset + 4];
        TagSerialNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 6);
        DescriptorCrc = EndianUtilities.toUInt16LittleEndian(buffer, offset + 8);
        DescriptorCrcLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 10);
        TagLocation = EndianUtilities.toUInt32LittleEndian(buffer, offset + 12);
        return 16;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public static boolean isValid(byte[] buffer, int offset) {
        byte checkSum = 0;
        if (EndianUtilities.toUInt16LittleEndian(buffer, offset) == 0) {
            return false;
        }

        for (int i = 0; i < 4; ++i) {
            checkSum += buffer[offset + i];
        }
        for (int i = 5; i < 16; ++i) {
            checkSum += buffer[offset + i];
        }
        return checkSum == buffer[offset + 4];
    }

    /**
     * @param result {@cs out}
     */
    public static boolean tryFromStream(Stream stream, DescriptorTag[] result) {
        byte[] next = StreamUtilities.readExact(stream, 512);
        if (!isValid(next, 0)) {
            result[0] = null;
            return false;
        }

        DescriptorTag dt = new DescriptorTag();
        dt.readFrom(next, 0);
        result[0] = dt;
        return true;
    }
}
