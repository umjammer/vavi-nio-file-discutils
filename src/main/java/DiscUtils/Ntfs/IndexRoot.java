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

import java.io.PrintWriter;
import java.util.Comparator;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class IndexRoot implements IByteArraySerializable, IDiagnosticTraceable {
    public static final int HeaderOffset = 0x10;

    private int __AttributeType;

    public int getAttributeType() {
        return __AttributeType;
    }

    public void setAttributeType(int value) {
        __AttributeType = value;
    }

    private AttributeCollationRule __CollationRule = AttributeCollationRule.Binary;

    public AttributeCollationRule getCollationRule() {
        return __CollationRule;
    }

    public void setCollationRule(AttributeCollationRule value) {
        __CollationRule = value;
    }

    private int __IndexAllocationSize;

    public int getIndexAllocationSize() {
        return __IndexAllocationSize;
    }

    public void setIndexAllocationSize(int value) {
        __IndexAllocationSize = value;
    }

    private byte __RawClustersPerIndexRecord;

    public byte getRawClustersPerIndexRecord() {
        return __RawClustersPerIndexRecord;
    }

    public void setRawClustersPerIndexRecord(byte value) {
        __RawClustersPerIndexRecord = value;
    }

    public int sizeOf() {
        return 16;
    }

    public int readFrom(byte[] buffer, int offset) {
        setAttributeType(EndianUtilities.toUInt32LittleEndian(buffer, 0x00));
        setCollationRule(AttributeCollationRule.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, 0x04)));
        setIndexAllocationSize(EndianUtilities.toUInt32LittleEndian(buffer, 0x08));
        setRawClustersPerIndexRecord(buffer[0x0C]);
        return 16;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(getAttributeType(), buffer, 0);
        EndianUtilities.writeBytesLittleEndian(getCollationRule().ordinal(), buffer, 0x04);
        EndianUtilities.writeBytesLittleEndian(getIndexAllocationSize(), buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian(getRawClustersPerIndexRecord(), buffer, 0x0C);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "                Attr Type: " + getAttributeType());
        writer.println(indent + "           Collation Rule: " + getCollationRule());
        writer.println(indent + "         Index Alloc Size: " + getIndexAllocationSize());
        writer.println(indent + "  Raw Clusters Per Record: " + getRawClustersPerIndexRecord());
    }

    public Comparator<byte[]> getCollator(UpperCase upCase) {
        switch (getCollationRule()) {
        case Filename:
            return new FileNameComparer(upCase);
        case SecurityHash:
            return new SecurityHashComparer();
        case UnsignedLong:
            return new UnsignedLongComparer();
        case MultipleUnsignedLongs:
            return new MultipleUnsignedLongComparer();
        case Sid:
            return new SidComparer();
        default:
            throw new UnsupportedOperationException();
        }
    }

    private final static class SecurityHashComparer implements Comparator<byte[]> {
        public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }

            if (y == null) {
                return -1;
            }

            if (x == null) {
                return 1;
            }

            int xHash = EndianUtilities.toUInt32LittleEndian(x, 0);
            int yHash = EndianUtilities.toUInt32LittleEndian(y, 0);
            if (xHash < yHash) {
                return -1;
            }

            if (xHash > yHash) {
                return 1;
            }

            int xId = EndianUtilities.toUInt32LittleEndian(x, 4);
            int yId = EndianUtilities.toUInt32LittleEndian(y, 4);
            if (xId < yId) {
                return -1;
            }

            if (xId > yId) {
                return 1;
            }

            return 0;
        }
    }

    private final static class UnsignedLongComparer implements Comparator<byte[]> {
        public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }

            if (y == null) {
                return -1;
            }

            if (x == null) {
                return 1;
            }

            int xVal = EndianUtilities.toUInt32LittleEndian(x, 0);
            int yVal = EndianUtilities.toUInt32LittleEndian(y, 0);
            if (xVal < yVal) {
                return -1;
            }

            if (xVal > yVal) {
                return 1;
            }

            return 0;
        }
    }

    private final static class MultipleUnsignedLongComparer implements Comparator<byte[]> {
        public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }

            if (y == null) {
                return -1;
            }

            if (x == null) {
                return 1;
            }

            for (int i = 0; i < x.length / 4; ++i) {
                int xVal = EndianUtilities.toUInt32LittleEndian(x, i * 4);
                int yVal = EndianUtilities.toUInt32LittleEndian(y, i * 4);
                if (xVal < yVal) {
                    return -1;
                }

                if (xVal > yVal) {
                    return 1;
                }
            }
            return 0;
        }
    }

    private final static class FileNameComparer implements Comparator<byte[]> {
        private final UpperCase _stringComparer;

        public FileNameComparer(UpperCase upCase) {
            _stringComparer = upCase;
        }

        public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }

            if (y == null) {
                return -1;
            }

            if (x == null) {
                return 1;
            }

            byte xFnLen = x[0x40];
            byte yFnLen = y[0x40];
            return _stringComparer.compare(x, 0x42, xFnLen * 2, y, 0x42, yFnLen * 2);
        }
    }

    private final static class SidComparer implements Comparator<byte[]> {
        public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }

            if (y == null) {
                return -1;
            }

            if (x == null) {
                return 1;
            }

            int toComp = Math.min(x.length, y.length);
            for (int i = 0; i < toComp; ++i) {
                int val = x[i] - y[i];
                if (val != 0) {
                    return val;
                }

            }
            if (x.length < y.length) {
                return -1;
            }

            if (x.length > y.length) {
                return 1;
            }

            return 0;
        }
    }
}
