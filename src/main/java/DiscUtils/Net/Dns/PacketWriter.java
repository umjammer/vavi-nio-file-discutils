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

package DiscUtils.Net.Dns;

import java.nio.charset.Charset;

import DiscUtils.Streams.Util.EndianUtilities;


public final class PacketWriter {
    private final byte[] _data;

    private int _pos;

    public PacketWriter(int maxSize) {
        _data = new byte[maxSize];
    }

    public void writeName(String name) {
        // TODO: Implement compression
        String[] labels = name.split("\\");
        for (String label : labels) {
            byte[] labelBytes = label.getBytes(Charset.forName("UTF8"));
            if (labelBytes.length > 63) {
                throw new IllegalArgumentException("Invalid DNS label - more than 63 octets '" + label + "' in '" + name + "'");
            }

            _data[_pos++] = (byte) labelBytes.length;
            System.arraycopy(labelBytes, 0, _data, _pos, labelBytes.length);
            _pos += labelBytes.length;
        }
        _data[_pos++] = 0;
    }

    public void write(short val) {
        EndianUtilities.writeBytesBigEndian(val, _data, _pos);
        _pos += 2;
    }

    public byte[] getBytes() {
        byte[] result = new byte[_pos];
        System.arraycopy(_data, 0, result, 0, _pos);
        return result;
    }
}
