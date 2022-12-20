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

package discUtils.ntfs;

import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public class FileRecordReference implements IByteArraySerializable, Comparable<FileRecordReference> {

    public FileRecordReference() {
    }

    public FileRecordReference(long val) {
        value = val;
    }

    public FileRecordReference(long mftIndex, short sequenceNumber) {
        this(mftIndex & 0x0000_FFFF_FFFF_FFFFL | ((long) sequenceNumber << 48 & 0xFFFF_0000_0000_0000L));
    }

    private long value;

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getMftIndex() {
        return value & 0x0000_FFFF_FFFF_FFFFL;
    }

    public int getSequenceNumber() {
        return (int) (value >>> 48) & 0xFFFF;
    }

    @Override public int size() {
        return 8;
    }

    public boolean isNull() {
        return getSequenceNumber() == 0;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        setValue(ByteUtil.readLeLong(buffer, offset));
        return 8;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeLong(value, buffer, offset);
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof FileRecordReference)) {
            return false;
        }

        return value == ((FileRecordReference) obj).value;
    }

    @Override public int hashCode() {
        return Long.hashCode(value);
    }

    @Override public int compareTo(FileRecordReference other) {
        return Long.compare(value, other.value);

    }

    @Override public String toString() {
        return "MFT:" + getMftIndex() + " (ver: " + getSequenceNumber() + ")";
    }
}
