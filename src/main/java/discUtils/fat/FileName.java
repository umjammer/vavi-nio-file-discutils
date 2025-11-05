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

package discUtils.fat;

import java.nio.charset.Charset;

import discUtils.core.internal.Utilities;


public final class FileName {

    private static final byte SpaceByte = 0x20;

    public static final FileName SelfEntryName = new FileName(new byte[] {
        0x2E, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20
    }, 0);

    public static final FileName ParentEntryName = new FileName(new byte[] {
        0x2E, 0x2E, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20
    }, 0);

    public static final FileName Null = new FileName(new byte[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    }, 0);

    private static final byte[] InvalidBytes = {
        0x22, 0x2A, 0x2B, 0x2C, 0x2E, 0x2F, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x5B, 0x5C, 0x5D, 0x7C
    };

    private final byte[] raw;

    public FileName(byte[] data, int offset) {
        raw = new byte[11];
        System.arraycopy(data, offset, raw, 0, 11);
    }

    /**
     * @throws IllegalArgumentException wrong name, or name length
     */
    public FileName(String name, Charset encoding) {
        raw = new byte[11];
        byte[] bytes = name.toUpperCase().getBytes(encoding);

        int nameIdx = 0;
        int rawIdx = 0;
        while (nameIdx < bytes.length && bytes[nameIdx] != '.' && rawIdx < raw.length) {
            byte b = bytes[nameIdx++];
            if ((b & 0xff) < 0x20 || contains(InvalidBytes, b)) {
//logger.log(Level.DEBUG, name + ", " + encoding + ", " + Arrays.toString(bytes));
                throw new IllegalArgumentException("Invalid character in file name '%1$c', %1$02x: %2$s".formatted(b, name));
            }

            raw[rawIdx++] = b;
        }

        if (rawIdx > 8) {
            throw new IllegalArgumentException("File name too long '" + name + "'");
        }
        if (rawIdx == 0) {
            throw new IllegalArgumentException("File name too short '" + name + "'");
        }

        while (rawIdx < 8) {
            raw[rawIdx++] = SpaceByte;
        }

        if (nameIdx < bytes.length && bytes[nameIdx] == '.') {
            ++nameIdx;
        }

        while (nameIdx < bytes.length && rawIdx < raw.length) {
            byte b = bytes[nameIdx++];
            if (b < 0x20 || contains(InvalidBytes, b)) {
                throw new IllegalArgumentException("Invalid character in file extension '" + (char) b + "': " + name);
            }

            raw[rawIdx++] = b;
        }

        while (rawIdx < 11) {
            raw[rawIdx++] = SpaceByte;
        }

        if (nameIdx != bytes.length) {
            throw new IllegalArgumentException("File extension too long '" + name + "': " + name);
        }
    }

    public boolean equals(FileName other) {
        if (other == null) {
            return false;
        }

        return compareRawNames(this, other) == 0;
    }

    public static FileName fromPath(String path, Charset encoding) {
        return new FileName(Utilities.getFileFromPath(path), encoding);
    }

    public String getDisplayName(Charset encoding) {
        return getSearchName(encoding).replaceFirst("\\.*$", "");
    }

    public String getSearchName(Charset encoding) {
        return new String(raw, 0, 8, encoding).replaceFirst(" *$", "") + "." +
               new String(raw, 8, 3, encoding).replaceFirst(" *$", "");
    }

    public String getRawName(Charset encoding) {
        return new String(raw, 0, 11, encoding).replaceFirst(" *$", "");
    }

    public FileName deleted() {
        byte[] data = new byte[11];
        System.arraycopy(raw, 0, data, 0, 11);
        data[0] = (byte) 0xe5;
        return new FileName(data, 0);
    }

    public boolean isDeleted() {
        return (raw[0] & 0xff) == 0xe5;
    }

    public boolean isEndMarker() {
        return raw[0] == 0x00;
    }

    public void getBytes(byte[] data, int offset) {
        System.arraycopy(raw, 0, data, offset, 11);
    }

    public boolean equals(Object other) {
        return equals(other instanceof FileName ? (FileName) other : null);
    }

    public int hashCode() {
        int val = 0x1a8d3c4e;
        for (int i = 0; i < 11; ++i) {
            val = (val << 2) ^ raw[i];
        }
        return val;
    }

    public String toString() {
        return getRawName(Charset.forName(System.getProperty("file.encoding")));
    }

    private static int compareRawNames(FileName a, FileName b) {
        for (int i = 0; i < 11; ++i) {
            if (a.raw[i] != b.raw[i]) {
                return a.raw[i] - b.raw[i];
            }
        }
        return 0;
    }

    private static boolean contains(byte[] array, byte val) {
        for (byte b : array) {
            if (b == val) {
                return true;
            }
        }
        return false;
    }
}
