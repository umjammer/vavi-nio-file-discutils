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

package discUtils.iso9660;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public class BootValidationEntry {

    private byte[] data;

    public byte headerId;

    public String manfId;

    public byte platformId;

    public BootValidationEntry() {
        headerId = 1;
        platformId = 0;
        manfId = ".Net DiscUtils";
    }

    public BootValidationEntry(byte[] src, int offset) {
        data = new byte[32];
        System.arraycopy(src, offset, data, 0, 32);
        headerId = data[0];
        platformId = data[1];
        manfId = new String(data, 4, 24, StandardCharsets.US_ASCII).replaceFirst("[\0 ]*$", "");
    }

    public boolean getChecksumValid() {
        short total = 0;
        for (int i = 0; i < 16; ++i) {
            total += ByteUtil.readLeShort(data, i * 2);
        }
        return total == 0;
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, offset + 0x20, (byte) 0);
        buffer[offset + 0x00] = headerId;
        buffer[offset + 0x01] = platformId;
        EndianUtilities.stringToBytes(manfId, buffer, offset + 0x04, 24);
        buffer[offset + 0x1E] = 0x55;
        buffer[offset + 0x1F] = (byte) 0xAA;
        ByteUtil.writeLeShort(calcChecksum(buffer, offset), buffer, offset + 0x1C);
    }

    private static short calcChecksum(byte[] buffer, int offset) {
        short total = 0;
        for (int i = 0; i < 16; ++i) {
            total = (short) (total + ByteUtil.readLeShort(buffer, offset + i * 2) & 0xffff);
        }
        return (short) (0 - total);
    }
}
