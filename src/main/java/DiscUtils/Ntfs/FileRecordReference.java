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

package DiscUtils.Ntfs;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public class FileRecordReference implements IByteArraySerializable, Comparable<FileRecordReference> {
    public FileRecordReference() {
    }

    public FileRecordReference(long val) {
        setValue(val);
    }

    public FileRecordReference(long mftIndex, short sequenceNumber) {
        setValue(mftIndex & 0x0000FFFFFFFFFFFFL | ((long) sequenceNumber << 48 & 0xFFFF000000000000L));
    }

    private long __Value;

    public long getValue() {
        return __Value;
    }

    public void setValue(long value) {
        __Value = value;
    }

    public long getMftIndex() {
        return getValue() & 0x0000FFFFFFFFFFFFL;
    }

    public short getSequenceNumber() {
        return (short) ((getValue() >>> 48) & 0xFFFF);
    }

    public long getSize() {
        return 8;
    }

    public boolean getIsNull() {
        return getSequenceNumber() == 0;
    }

    public int readFrom(byte[] buffer, int offset) {
        setValue(EndianUtilities.toUInt64LittleEndian(buffer, offset));
        return 8;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(getValue(), buffer, offset);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FileRecordReference)) {
            return false;
        }

        return getValue() == ((FileRecordReference) obj).getValue();
    }

    public int hashCode() {
        return Long.hashCode(getValue());
    }

    public int compareTo(FileRecordReference other) {
        if (getValue() < other.getValue()) {
            return -1;
        }

        if (getValue() > other.getValue()) {
            return 1;
        }

        return 0;
    }

    public String toString() {
        return "MFT:" + getMftIndex() + " (ver: " + getSequenceNumber() + ")";
    }
}
