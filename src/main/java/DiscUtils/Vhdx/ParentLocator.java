//
// Copyright (c) 2008-2012, Kenneth Bell
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

package DiscUtils.Vhdx;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


class ParentLocator implements IByteArraySerializable {

    private static final UUID LocatorTypeGuid = UUID.fromString("B04AEFB7-D19E-4A81-B789-25B8E9445913");

    public short Count;

    public UUID LocatorType = LocatorTypeGuid;

    public short Reserved = 0;

    public Map<String, String> getEntries() {
        return Entries;
    }

    private Map<String, String> Entries = new HashMap<>();

    public long getSize() {
        if (Entries.size() != 0) {
            throw new UnsupportedOperationException();
        }

        return 20;
    }

    public int readFrom(byte[] buffer, int offset) {
        LocatorType = EndianUtilities.toGuidLittleEndian(buffer, offset + 0);
        if (LocatorType != LocatorTypeGuid) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Unrecognized Parent Locator type: " + LocatorType);
        }

        Entries = new HashMap<>();

        Count = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 18);
        for (short i = 0; i < Count; ++i) {
            int kvOffset = offset + 20 + i * 12;
            int keyOffset = EndianUtilities.toInt32LittleEndian(buffer, kvOffset + 0);
            int valueOffset = EndianUtilities.toInt32LittleEndian(buffer, kvOffset + 4);
            int keyLength = EndianUtilities.toUInt16LittleEndian(buffer, kvOffset + 8);
            int valueLength = EndianUtilities.toUInt16LittleEndian(buffer, kvOffset + 10);

            String key = new String(buffer, keyOffset, keyLength, Charset.forName("utf-8"));
            String value = new String(buffer, valueOffset, valueLength, Charset.forName("utf-8"));

            Entries.put(key, value);
        }

        return 0;
    }

    public void writeTo(byte[] buffer, int offset) {
        if (Entries.size() != 0) {
            throw new UnsupportedOperationException();
        }

        Count = (short) Entries.size();

        EndianUtilities.writeBytesLittleEndian(LocatorType, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(Reserved, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(Count, buffer, offset + 18);
    }
}
