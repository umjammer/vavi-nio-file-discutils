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

package discUtils.vhd;

import discUtils.streams.util.EndianUtilities;


public class ParentLocator {

    public static final String PlatformCodeWindowsRelativeUnicode = "W2ru";

    public static final String PlatformCodeWindowsAbsoluteUnicode = "W2ku";

    public String platformCode;

    public int platformDataLength;

    public long platformDataOffset;

    public int platformDataSpace;

    public ParentLocator() {
        platformCode = "";
    }

    public ParentLocator(ParentLocator toCopy) {
        platformCode = toCopy.platformCode;
        platformDataSpace = toCopy.platformDataSpace;
        platformDataLength = toCopy.platformDataLength;
        platformDataOffset = toCopy.platformDataOffset;
    }

    public static ParentLocator fromBytes(byte[] data, int offset) {
        ParentLocator result = new ParentLocator();
        result.platformCode = EndianUtilities.bytesToString(data, offset, 4);
        result.platformDataSpace = EndianUtilities.toInt32BigEndian(data, offset + 4);
        result.platformDataLength = EndianUtilities.toInt32BigEndian(data, offset + 8);
        result.platformDataOffset = EndianUtilities.toInt64BigEndian(data, offset + 16);
        return result;
    }

    public void toBytes(byte[] data, int offset) {
        EndianUtilities.stringToBytes(platformCode, data, offset, 4);
        EndianUtilities.writeBytesBigEndian(platformDataSpace, data, offset + 4);
        EndianUtilities.writeBytesBigEndian(platformDataLength, data, offset + 8);
        EndianUtilities.writeBytesBigEndian(0, data, offset + 12);
        EndianUtilities.writeBytesBigEndian(platformDataOffset, data, offset + 16);
    }
}
