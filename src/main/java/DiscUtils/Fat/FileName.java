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

package DiscUtils.Fat;

import java.nio.charset.Charset;

import DiscUtils.Core.Internal.Utilities;


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

    private final byte[] _raw;

    public FileName(byte[] data, int offset) {
        _raw = new byte[11];
        System.arraycopy(data, offset, _raw, 0, 11);
    }

    /**
     * @throws IllegalArgumentException
     */
    public FileName(String name, Charset encoding) {
        _raw = new byte[11];
        byte[] bytes = name.toUpperCase().getBytes(encoding);
        int nameIdx = 0;
        int rawIdx = 0;
        while (nameIdx < bytes.length && bytes[nameIdx] != '.' && rawIdx < _raw.length) {
            byte b = bytes[nameIdx++];
            if ((b & 0xff) < 0x20 || contains(InvalidBytes, b)) {
                throw new IllegalArgumentException("Invalid character in file name '" + (char) b + "', 0x" + Integer.toHexString(b & 0xff));
            }

            _raw[rawIdx++] = b;
        }
        if (rawIdx > 8) {
            throw new IllegalArgumentException("File name too long '" + name + "'");
        }

        if (rawIdx == 0) {
            throw new IllegalArgumentException("File name too short '" + name + "'");
        }

        while (rawIdx < 8) {
            _raw[rawIdx++] = SpaceByte;
        }
        if (nameIdx < bytes.length && bytes[nameIdx] == '.') {
            ++nameIdx;
        }

        while (nameIdx < bytes.length && rawIdx < _raw.length) {
            byte b = bytes[nameIdx++];
            if (b < 0x20 || contains(InvalidBytes, b)) {
                throw new IllegalArgumentException("Invalid character in file extension '" + (char) b + "'");
            }

            _raw[rawIdx++] = b;
        }
        while (rawIdx < 11) {
            _raw[rawIdx++] = SpaceByte;
        }
        if (nameIdx != bytes.length) {
            throw new IllegalArgumentException("File extension too long '" + name + "'");
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
        return new String(_raw, 0, 8, encoding).replaceFirst(" *$", "") + "." + new String(_raw, 8, 3, encoding).replaceFirst(" *$", "");
    }

    public String getRawName(Charset encoding) {
        return new String(_raw, 0, 11, encoding).replaceFirst(" *$", "");
    }

    public FileName deleted() {
        byte[] data = new byte[11];
        System.arraycopy(_raw, 0, data, 0, 11);
        data[0] = (byte) 0xE5;
        return new FileName(data, 0);
    }

    public boolean isDeleted() {
        return (_raw[0] & 0xff) == 0xE5;
    }

    public boolean isEndMarker() {
        return _raw[0] == 0x00;
    }

    public void getBytes(byte[] data, int offset) {
        System.arraycopy(_raw, 0, data, offset, 11);
    }

    public boolean equals(Object other) {
        return equals(other instanceof FileName ? (FileName) other : (FileName) null);
    }

    public int hashCode() {
        int val = 0x1A8D3C4E;
        for (int i = 0; i < 11; ++i) {
            val = (val << 2) ^ _raw[i];
        }
        return val;
    }

    private static int compareRawNames(FileName a, FileName b) {
        for (int i = 0; i < 11; ++i) {
            if (a._raw[i] != b._raw[i]) {
                return a._raw[i] - b._raw[i];
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
