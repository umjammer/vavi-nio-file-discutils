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

import discUtils.streams.util.EndianUtilities;


public class CompressionResourceHeader {

    private int dataSize;

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int value) {
        dataSize = value;
    }

    private int flags;

    public int getFlags() {
        return flags;
    }

    public void setFlags(int value) {
        flags = value;
    }

    private int headerSize;

    public int getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(int value) {
        headerSize = value;
    }

    public static int getSize() {
        return 16;
    }

    private int totalSize;

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int value) {
        totalSize = value;
    }

    public int readFrom(byte[] buffer, int offset) {
        headerSize = EndianUtilities.toUInt32BigEndian(buffer, offset);
        totalSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 4);
        dataSize = EndianUtilities.toUInt32BigEndian(buffer, offset + 8);
        flags = EndianUtilities.toUInt32BigEndian(buffer, offset + 12);

        return getSize();
    }
}
