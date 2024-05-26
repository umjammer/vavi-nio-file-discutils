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
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public class DescriptorTag implements IByteArraySerializable {

    public short descriptorCrc;

    private short descriptorCrcLength;

    public int getDescriptorCrcLength() {
        return descriptorCrcLength;
    }

    public short descriptorVersion;

    public byte tagChecksum;

    public TagIdentifier tagIdentifier;

    public int tagLocation;

    public short tagSerialNumber;

    @Override public int size() {
        return 16;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        tagIdentifier = TagIdentifier.valueOf(ByteUtil.readLeShort(buffer, offset));
        descriptorVersion = ByteUtil.readLeShort(buffer, offset + 2);
        tagChecksum = buffer[offset + 4];
        tagSerialNumber = ByteUtil.readLeShort(buffer, offset + 6);
        descriptorCrc = ByteUtil.readLeShort(buffer, offset + 8);
        descriptorCrcLength = ByteUtil.readLeShort(buffer, offset + 10);
        tagLocation = ByteUtil.readLeInt(buffer, offset + 12);
        return 16;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }

    public static boolean isValid(byte[] buffer, int offset) {
        byte checkSum = 0;
        if (ByteUtil.readLeShort(buffer, offset) == 0) {
            return false;
        }

        for (int i = 0; i < 4; ++i) {
            checkSum = (byte) (checkSum + buffer[offset + i]);
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
