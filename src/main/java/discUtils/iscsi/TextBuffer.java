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

package discUtils.iscsi;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


public class TextBuffer {

    private final Map<String, String> records;

    public TextBuffer() {
        records = new LinkedHashMap<>();
    }

    public int getCount() {
        return records.size();
    }

    public String get(String key) {
        for (Map.Entry<String, String> entry : records.entrySet()) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void put(String key, String value) {
        for (Map.Entry<String, String> entry : records.entrySet()) {
            if (entry.getKey().equals(key)) {
                records.put(key, value);
                return;
            }

        }
        records.put(key, value);
    }

    public Map<String, String> getLines() {
        return records;
    }

    public int getSize() {
        int i = 0;
        for (Map.Entry<String, String> entry : records.entrySet()) {
            i += entry.getKey().length() + entry.getValue().length() + 2;
        }
        return i;
    }

    public void add(String key, String value) {
        records.put(key, value);
    }

    public void readFrom(byte[] buffer, int offset, int length) {
        if (buffer == null) {
            return;
        }

        int end = offset + length;
        int i = offset;
        while (i < end) {
            int nameStart = i;
            while (i < end && buffer[i] != '=') {
                ++i;
            }
            if (i >= end) {
                throw new IllegalArgumentException("Invalid text buffer");
            }

            String name = new String(buffer, nameStart, i - nameStart, StandardCharsets.US_ASCII);
            ++i;
            int valueStart = i;
            while (i < end && buffer[i] != '\0') {
                ++i;
            }
            String value = new String(buffer, valueStart, i - valueStart, StandardCharsets.US_ASCII);
            ++i;
            add(name, value);
        }
    }

    public int writeTo(byte[] buffer, int offset) {
        int i = offset;
        for (Map.Entry<String, String> entry : records.entrySet()) {
            byte[] bytes = entry.getKey().getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, buffer, i, bytes.length);
            i += bytes.length;
            buffer[i++] = (byte) '=';
            bytes = entry.getValue().getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, buffer, i, bytes.length);
            i += bytes.length;
            buffer[i++] = 0;
        }
        return i - offset;
    }

    public void remove(String key) {
        for (Map.Entry<String, String> entry : records.entrySet()) {
            if (entry.getKey().equals(key)) {
                records.remove(key);
                return;
            }
        }
    }
}
