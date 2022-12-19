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

package discUtils.net.dns;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import vavi.util.ByteUtil;


public final class PacketWriter {

    private final byte[] data;

    private int pos;

    public PacketWriter(int maxSize) {
        data = new byte[maxSize];
    }

    public void writeName(String name) {
        // TODO: Implement compression
        String[] labels = Arrays.stream(name.split("\\.")).filter(e -> !e.isEmpty()).toArray(String[]::new);

        for (String label : labels) {
            byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
            if (labelBytes.length > 63) {
                throw new IllegalArgumentException("Invalid DNS label - more than 63 octets '" + label + "' in '" + name + "'");
            }

            data[pos++] = (byte) labelBytes.length;
            System.arraycopy(labelBytes, 0, data, pos, labelBytes.length);
            pos += labelBytes.length;
        }

        data[pos++] = 0;
    }

    public void write(short val) {
        ByteUtil.writeBeShort(val, data, pos);
        pos += 2;
    }

    public byte[] getBytes() {
        byte[] result = new byte[pos];
        System.arraycopy(data, 0, result, 0, pos);
        return result;
    }
}
