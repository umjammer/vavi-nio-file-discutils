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

import java.io.PrintWriter;
import java.util.Comparator;

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.IByteArraySerializable;
import vavi.util.ByteUtil;


public final class IndexRoot implements IByteArraySerializable, IDiagnosticTraceable {

    public static final int HeaderOffset = 0x10;

    private int attributeType;

    public int getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(int value) {
        attributeType = value;
    }

    private AttributeCollationRule collationRule = AttributeCollationRule.Binary;

    public AttributeCollationRule getCollationRule() {
        return collationRule;
    }

    public void setCollationRule(AttributeCollationRule value) {
        collationRule = value;
    }

    private int indexAllocationSize;

    public int getIndexAllocationSize() {
        return indexAllocationSize;
    }

    public void setIndexAllocationSize(int value) {
        indexAllocationSize = value;
    }

    private byte rawClustersPerIndexRecord;

    public int getRawClustersPerIndexRecord() {
        return rawClustersPerIndexRecord & 0xff;
    }

    public void setRawClustersPerIndexRecord(byte value) {
        rawClustersPerIndexRecord = value;
    }

    @Override public int size() {
        return 16;
    }

    @Override public int readFrom(byte[] buffer, int offset) {
        attributeType = ByteUtil.readLeInt(buffer, 0x00);
        collationRule = AttributeCollationRule.valueOf(ByteUtil.readLeInt(buffer, 0x04));
        indexAllocationSize = ByteUtil.readLeInt(buffer, 0x08);
        rawClustersPerIndexRecord = buffer[0x0C];
        return 16;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        ByteUtil.writeLeInt(attributeType, buffer, 0);
        ByteUtil.writeLeInt(collationRule.getValue(), buffer, 0x04);
        ByteUtil.writeLeInt(indexAllocationSize, buffer, 0x08);
        buffer[0x0C] = rawClustersPerIndexRecord;
    }

    @Override public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "                Attr Type: " + attributeType);
        writer.println(indent + "           Collation Rule: " + collationRule);
        writer.println(indent + "         Index Alloc Size: " + indexAllocationSize);
        writer.println(indent + "  raw Clusters Per Record: " + rawClustersPerIndexRecord);
    }

    public Comparator<byte[]> getCollator(UpperCase upCase) {
        return switch (collationRule) {
            case Filename -> new FileNameComparer(upCase);
            case SecurityHash -> new SecurityHashComparer();
            case UnsignedLong -> new UnsignedLongComparer();
            case MultipleUnsignedLongs -> new MultipleUnsignedLongComparer();
            case Sid -> new SidComparer();
            default -> throw new UnsupportedOperationException();
        };
    }

    private final static class SecurityHashComparer implements Comparator<byte[]> {
        @Override public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }
            if (y == null) {
                return -1;
            }
            if (x == null) {
                return 1;
            }

            long xHash = ByteUtil.readLeInt(x, 0) & 0xffffffffL;
            long yHash = ByteUtil.readLeInt(y, 0) & 0xffffffffL;

            if (xHash < yHash) {
                return -1;
            }
            if (xHash > yHash) {
                return 1;
            }

            long xId = ByteUtil.readLeInt(x, 4) & 0xffffffffL;
            long yId = ByteUtil.readLeInt(y, 4) & 0xffffffffL;

            return Long.compare(xId, yId);

        }
    }

    private final static class UnsignedLongComparer implements Comparator<byte[]> {
        @Override public int compare(byte[] x, byte[] y) {
            if (x == null && y == null) {
                return 0;
            }
            if (y == null) {
                return -1;
            }
            if (x == null) {
                return 1;
            }

            long xVal = ByteUtil.readLeInt(x, 0) & 0xffffffffL;
            long yVal = ByteUtil.readLeInt(y, 0) & 0xffffffffL;

            return Long.compare(xVal, yVal);

        }
    }

    private final static class MultipleUnsignedLongComparer implements Comparator<byte[]> {
        @Override public int compare(byte[] x, byte[] y) {
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
                long xVal = ByteUtil.readLeInt(x, i * 4) & 0xffffffffL;
                long yVal = ByteUtil.readLeInt(y, i * 4) & 0xffffffffL;

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

        private final UpperCase stringComparer;

        public FileNameComparer(UpperCase upCase) {
            stringComparer = upCase;
        }

        @Override public int compare(byte[] x, byte[] y) {
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

            return stringComparer.compare(x, 0x42, xFnLen * 2, y, 0x42, yFnLen * 2);
        }
    }

    private final static class SidComparer implements Comparator<byte[]> {
        @Override public int compare(byte[] x, byte[] y) {
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

            return Integer.compare(x.length, y.length);
        }
    }
}
