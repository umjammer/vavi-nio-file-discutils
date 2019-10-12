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

public class DataRun {
    public DataRun() {
    }

    public DataRun(long offset, long length, boolean isSparse) {
        setRunOffset(offset);
        setRunLength(length);
        setIsSparse(isSparse);
    }

    private boolean __IsSparse;

    public boolean isSparse() {
        return __IsSparse;
    }

    public void setIsSparse(boolean value) {
        __IsSparse = value;
    }

    private long __RunLength;

    public long getRunLength() {
        return __RunLength;
    }

    public void setRunLength(long value) {
        __RunLength = value;
    }

    private long __RunOffset;

    public long getRunOffset() {
        return __RunOffset;
    }

    public void setRunOffset(long value) {
        __RunOffset = value;
    }

    public long getSize() {
        int runLengthSize = varLongSize(getRunLength());
        int runOffsetSize = varLongSize(getRunOffset());
        return 1 + runLengthSize + runOffsetSize;
    }

    public int read(byte[] buffer, int offset) {
        int runOffsetSize = (buffer[offset] >>> 4) & 0x0F;
        int runLengthSize = buffer[offset] & 0x0F;
        setRunLength(readVarLong(buffer, offset + 1, runLengthSize));
        setRunOffset(readVarLong(buffer, offset + 1 + runLengthSize, runOffsetSize));
        setIsSparse(runOffsetSize == 0);
        return 1 + runLengthSize + runOffsetSize;
    }

    public String toString() {
        try {
            return String.format("{0:+##;-##;0}[+{1}]", getRunOffset(), getRunLength());
        } catch (RuntimeException __dummyCatchVar0) {
            throw __dummyCatchVar0;
        } catch (Exception __dummyCatchVar0) {
            throw new RuntimeException(__dummyCatchVar0);
        }

    }

    public int write(byte[] buffer, int offset) {
        int runLengthSize = writeVarLong(buffer, offset + 1, getRunLength());
        int runOffsetSize = isSparse() ? 0 : writeVarLong(buffer, offset + 1 + runLengthSize, getRunOffset());
        buffer[offset] = (byte) ((runLengthSize & 0x0F) | ((runOffsetSize << 4) & 0xF0));
        return 1 + runLengthSize + runOffsetSize;
    }

    private static long readVarLong(byte[] buffer, int offset, int size) {
        long val = 0;
        boolean signExtend = false;
        for (int i = 0; i < size; ++i) {
            byte b = buffer[offset + i];
            val = val | ((long) b << (i * 8));
            signExtend = (b & 0x80) != 0;
        }
        if (signExtend) {
            for (int i = size; i < 8; ++i) {
                val = val | ((long) 0xFF << (i * 8));
            }
        }

        return val;
    }

    private static int writeVarLong(byte[] buffer, int offset, long val) {
        boolean isPositive = val >= 0;
        int pos = 0;
        do {
            buffer[offset + pos] = (byte) (val & 0xFF);
            val >>>= 8;
            pos++;
        } while (val != 0 && val != -1);
        // Avoid appearing to have a negative number that is actually positive,
        // record an extra empty byte if needed.
        if (isPositive && (buffer[offset + pos - 1] & 0x80) != 0) {
            buffer[offset + pos] = 0;
            pos++;
        } else if (!isPositive && (buffer[offset + pos - 1] & 0x80) != 0x80) {
            buffer[offset + pos] = (byte) 0xFF;
            pos++;
        }

        return pos;
    }

    private static int varLongSize(long val) {
        boolean isPositive = val >= 0;
        boolean lastByteHighBitSet = false;
        int len = 0;
        do {
            lastByteHighBitSet = (val & 0x80) != 0;
            val >>>= 8;
            len++;
        } while (val != 0 && val != -1);
        if ((isPositive && lastByteHighBitSet) || (!isPositive && !lastByteHighBitSet)) {
            len++;
        }

        return len;
    }
}
