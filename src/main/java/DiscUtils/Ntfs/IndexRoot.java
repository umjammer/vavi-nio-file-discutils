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

    private int _attributeType;

    public int getAttributeType() {
        return _attributeType;
    }

    public void setAttributeType(int value) {
        _attributeType = value;
    }

    private AttributeCollationRule _collationRule = AttributeCollationRule.Binary;

    public AttributeCollationRule getCollationRule() {
        return _collationRule;
    }

    public void setCollationRule(AttributeCollationRule value) {
        _collationRule = value;
    }

    private int _indexAllocationSize;

    public int getIndexAllocationSize() {
        return _indexAllocationSize;
    }

    public void setIndexAllocationSize(int value) {
        _indexAllocationSize = value;
    }

    private byte _rawClustersPerIndexRecord;

    public int getRawClustersPerIndexRecord() {
        return _rawClustersPerIndexRecord & 0xff;
    }

    public void setRawClustersPerIndexRecord(byte value) {
        _rawClustersPerIndexRecord = value;
    }

    public int size() {
        return 16;
    }

    public int readFrom(byte[] buffer, int offset) {
        _attributeType = EndianUtilities.toUInt32LittleEndian(buffer, 0x00);
        _collationRule = AttributeCollationRule.valueOf(EndianUtilities.toUInt32LittleEndian(buffer, 0x04));
        _indexAllocationSize = EndianUtilities.toUInt32LittleEndian(buffer, 0x08);
        _rawClustersPerIndexRecord = buffer[0x0C];
        return 16;
    }

    public void writeTo(byte[] buffer, int offset) {
        EndianUtilities.writeBytesLittleEndian(_attributeType, buffer, 0);
        EndianUtilities.writeBytesLittleEndian(_collationRule.getValue(), buffer, 0x04);
        EndianUtilities.writeBytesLittleEndian(_indexAllocationSize, buffer, 0x08);
        EndianUtilities.writeBytesLittleEndian(_rawClustersPerIndexRecord, buffer, 0x0C);
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "                Attr Type: " + _attributeType);
        writer.println(indent + "           Collation Rule: " + _collationRule);
        writer.println(indent + "         Index Alloc Size: " + _indexAllocationSize);
        writer.println(indent + "  Raw Clusters Per Record: " + _rawClustersPerIndexRecord);
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

            long xHash = EndianUtilities.toUInt32LittleEndian(x, 0) & 0xffffffffl;
            long yHash = EndianUtilities.toUInt32LittleEndian(y, 0) & 0xffffffffl;

            if (xHash < yHash) {
                return -1;
            }
            if (xHash > yHash) {
                return 1;
            }

            long xId = EndianUtilities.toUInt32LittleEndian(x, 4) & 0xffffffffl;
            long yId = EndianUtilities.toUInt32LittleEndian(y, 4) & 0xffffffffl;

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

            long xVal = EndianUtilities.toUInt32LittleEndian(x, 0) & 0xffffffffl;
            long yVal = EndianUtilities.toUInt32LittleEndian(y, 0) & 0xffffffffl;

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
                long xVal = EndianUtilities.toUInt32LittleEndian(x, i * 4) & 0xffffffffl;
                long yVal = EndianUtilities.toUInt32LittleEndian(y, i * 4) & 0xffffffffl;

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

            int xFnLen = x[0x40] & 0xff;
            int yFnLen = y[0x40] & 0xff;

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
