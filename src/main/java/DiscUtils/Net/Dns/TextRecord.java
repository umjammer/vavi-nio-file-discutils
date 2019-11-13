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

package DiscUtils.Net.Dns;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents a DNS TXT record.
 */
public final class TextRecord extends ResourceRecord {
    public TextRecord(String name, RecordType type, RecordClass rClass, long expiry, PacketReader reader) {
        super(name, type, rClass, expiry);
        _values = new HashMap<>();
        short dataLen = reader.readUShort();
        int pos = reader.getPosition();
        while (reader.getPosition() < pos + dataLen) {
            int valueLen = reader.readByte();
            byte[] valueBinary = reader.readBytes(valueLen);
            storeValue(valueBinary);
        }
    }

    /**
     * Gets the values encoded in this record.
     * For data fidelity, the data is returned in byte form - typically
     * the encoded data is actually ASCII or UTF-8.
     */
    private Map<String, byte[]> _values;

    public Map<String, byte[]> getValues() {
        return _values;
    }

    private void storeValue(byte[] value) {
        int i = 0;
        while (i < value.length && value[i] != '=') {
            ++i;
        }
        if (i < value.length) {
            byte[] data = new byte[value.length - (i + 1)];
            System.arraycopy(value, i + 1, data, 0, data.length);
            getValues().put(new String(value, 0, i, Charset.forName("ASCII")), data);
        } else {
            getValues().put(new String(value, Charset.forName("ASCII")), null);
        }
    }
}
