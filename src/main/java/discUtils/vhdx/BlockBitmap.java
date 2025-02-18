//
// Copyright (c) 2008-2012, Kenneth Bell
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

package discUtils.vhdx;

public final class BlockBitmap {

    private final byte[] data;

    private final int length;

    private final int offset;

    public BlockBitmap(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    /**
     * @param state {@cs out}
     */
    public int contiguousSectors(int first, boolean[] state) {
        int matched = 0;
        int bitPos = first % 8;
        int bytePos = first / 8;
        state[0] = (data[offset + bytePos] & (1 << bitPos)) != 0;
        byte matchByte = state[0] ? (byte) 0xFF : (byte) 0;
        while (bytePos < length) {
            if (data[offset + bytePos] == matchByte) {
                matched += 8 - bitPos;
                bytePos++;
                bitPos = 0;
            } else if ((data[offset + bytePos] & (1 << bitPos)) != 0 == state[0]) {
                matched++;
                bitPos++;
                if (bitPos == 8) {
                    bitPos = 0;
                    bytePos++;
                }
            } else {
                break;
            }
        }
        return matched;
    }

    public boolean markSectorsPresent(int first, int count) {
        boolean changed = false;
        int marked = 0;
        int bitPos = first % 8;
        int bytePos = first / 8;
        while (marked < count) {
            if (bitPos == 0 && count - marked >= 8) {
                if ((data[offset + bytePos] & 0xff) != 0xFF) {
                    data[offset + bytePos] = (byte) 0xFF;
                    changed = true;
                }

                marked += 8;
                bytePos++;
            } else {
                if ((data[offset + bytePos] & (1 << bitPos)) == 0) {
                    data[offset + bytePos] = (byte) (data[offset + bytePos] | (byte) (1 << bitPos));
                    changed = true;
                }

                marked++;
                bitPos++;
                if (bitPos == 8) {
                    bitPos = 0;
                    bytePos++;
                }
            }
        }
        return changed;
    }
}
