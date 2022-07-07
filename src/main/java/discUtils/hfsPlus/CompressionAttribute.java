//
// Copyright (c) 2014, Quamotion
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

package discUtils.hfsPlus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import discUtils.streams.util.EndianUtilities;


class CompressionAttribute {
    @SuppressWarnings("unused")
    private byte _attrData1;

    @SuppressWarnings("unused")
    private byte _attrData2;

    private int _compressionMagic;

    @SuppressWarnings("unused")
    private int _recordType;

    @SuppressWarnings("unused")
    private int _reserved1;

    @SuppressWarnings("unused")
    private int _reserved2;

    @SuppressWarnings("unused")
    private int _reserved3;

    private int _attrSize;

    public int getAttrSize() {
        return _attrSize;
    }

    public void setAttrSize(int value) {
        _attrSize = value;
    }

    public String getCompressionMagic() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(_compressionMagic);
        return new String(buffer.array(), StandardCharsets.US_ASCII);
    }

    private int _compressionType;

    public int getCompressionType() {
        return _compressionType;
    }

    public void setCompressionType(int value) {
        _compressionType = value;
    }

    public static int getSize() {
        return 32;
    }

    private int _uncompressedSize;

    public int getUncompressedSize() {
        return _uncompressedSize;
    }

    public void setUncompressedSize(int value) {
        _uncompressedSize = value;
    }

    public int readFrom(byte[] buffer, int offset) {
        _recordType = EndianUtilities.toUInt32BigEndian(buffer, offset + 0);
        _reserved1 = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        _reserved1 = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        setAttrSize(EndianUtilities.toUInt32BigEndian(buffer, offset + 12));
        _compressionMagic = EndianUtilities.toUInt32BigEndian(buffer, offset + 16);
        setCompressionType(EndianUtilities.toUInt32LittleEndian(buffer, offset + 20));
        setUncompressedSize(EndianUtilities.toUInt32LittleEndian(buffer, offset + 24));
        _reserved3 = EndianUtilities.toUInt32BigEndian(buffer, offset + 28);

        return getSize();
    }
}
