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

package discUtils.nfs;


public final class Nfs3FileHandle implements Comparable<Nfs3FileHandle> {
    public Nfs3FileHandle() {
    }

    public Nfs3FileHandle(XdrDataReader reader) {
        setValue(reader.readBuffer(Nfs3Mount.MaxFileHandleSize));
    }

    private byte[] value;

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public int compareTo(Nfs3FileHandle other) {
        if (other.getValue() == null) {
            return getValue() == null ? 0 : 1;
        }

        if (getValue() == null) {
            return -1;
        }

        int maxIndex = Math.min(getValue().length, other.getValue().length);
        for (int i = 0; i < maxIndex; ++i) {
            int diff = getValue()[i] - other.getValue()[i];
            if (diff != 0) {
                return diff;
            }

        }
        return getValue().length - other.getValue().length;
    }

    public boolean equals(Nfs3FileHandle other) {
        if (other == null) {
            return false;
        }

        if (getValue() == null) {
            return other.getValue() == null;
        }

        if (other.getValue() == null) {
            return false;
        }

        if (getValue().length != other.getValue().length) {
            return false;
        }

        for (int i = 0; i < getValue().length; ++i) {
            if (getValue()[i] != other.getValue()[i]) {
                return false;
            }

        }
        return true;
    }

    public boolean equals(Object obj) {
        Nfs3FileHandle other = obj instanceof Nfs3FileHandle ? (Nfs3FileHandle) obj : null;
        return equals(other);
    }

    public int hashCode() {
        int value = 0;
        if (getValue() != null) {
            for (int i = 0; i < getValue().length; ++i) {
                value = (value << 1) ^ getValue()[i];
            }
        }

        return value;
    }

    public void write(XdrDataWriter writer) {
        writer.writeBuffer(getValue());
    }

    public String toString() {
        int value = 0;
        if (getValue() != null) {
            for (int i = getValue().length - 1; i >= 0; i--) {
                value = (value << 1) | getValue()[i];
            }
        }

        return String.valueOf(value);
    }
}
