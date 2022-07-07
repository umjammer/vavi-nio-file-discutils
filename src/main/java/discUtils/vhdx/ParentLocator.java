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

package discUtils.vhdx;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;


class ParentLocator implements IByteArraySerializable {

    private static final UUID LocatorTypeGuid = UUID.fromString("B04AEFB7-D19E-4A81-B789-25B8E9445913");

    public short count;

    public UUID locatorType = LocatorTypeGuid;

    public short reserved = 0;

    public Map<String, String> getEntries() {
        return entries;
    }

    private Map<String, String> entries;

    public int size() {
        if (entries.size() != 0) {
            throw new UnsupportedOperationException();
        }

        return 20;
    }

    public int readFrom(byte[] buffer, int offset) {
        locatorType = EndianUtilities.toGuidLittleEndian(buffer, offset + 0);
        if (!locatorType.equals(LocatorTypeGuid)) {
            throw new dotnet4j.io.IOException("Unrecognized Parent Locator type: " + locatorType);
        }

        entries = new HashMap<>();

        count = EndianUtilities.toUInt16LittleEndian(buffer, offset + 18);
        for (short i = 0; i < (count & 0xffff); ++i) {
            int kvOffset = offset + 20 + i * 12;
            int keyOffset = EndianUtilities.toInt32LittleEndian(buffer, kvOffset + 0);
            int valueOffset = EndianUtilities.toInt32LittleEndian(buffer, kvOffset + 4);
            int keyLength = EndianUtilities.toUInt16LittleEndian(buffer, kvOffset + 8);
            int valueLength = EndianUtilities.toUInt16LittleEndian(buffer, kvOffset + 10);

            String key = new String(buffer, keyOffset, keyLength, StandardCharsets.UTF_16LE);
            String value = new String(buffer, valueOffset, valueLength, StandardCharsets.UTF_16LE);

            entries.put(key, value);
        }

        return 0;
    }

    public void writeTo(byte[] buffer, int offset) {
        if (entries.size() != 0) {
            throw new UnsupportedOperationException();
        }

        count = (short) entries.size();

        EndianUtilities.writeBytesLittleEndian(locatorType, buffer, offset + 0);
        EndianUtilities.writeBytesLittleEndian(reserved, buffer, offset + 16);
        EndianUtilities.writeBytesLittleEndian(count, buffer, offset + 18);
    }
}
