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

import java.io.IOException;
import java.util.Comparator;

import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


public final class UpperCase implements Comparator<String> {
    private final char[] _table;

    public UpperCase(File file) {
        Stream s = file.openStream(AttributeType.Data, null, FileAccess.Read);
        try {
            _table = new char[(int) s.getLength() / 2];
            byte[] buffer = StreamUtilities.readExact(s, (int) s.getLength());
            for (int i = 0; i < _table.length; ++i) {
                _table[i] = (char) EndianUtilities.toUInt16LittleEndian(buffer, i * 2);
            }
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
        }
    }

    public int compare(String x, String y) {
        int compLen = Math.min(x.length(), y.length());
        for (int i = 0; i < compLen; ++i) {
            int result = _table[x.charAt(i)] - _table[y.charAt(i)];
            if (result != 0) {
                return result;
            }

        }
        return x.length() - y.length();
    }

    // Identical out to the shortest string, so length is now the
    // determining factor.
    public int compare(byte[] x, int xOffset, int xLength, byte[] y, int yOffset, int yLength) {
        int compLen = Math.min(xLength, yLength) / 2;
        for (int i = 0; i < compLen; ++i) {
            char xCh = (char) ((x[xOffset + i * 2] & 0xff) | (x[xOffset + i * 2 + 1] << 8));
            char yCh = (char) ((y[yOffset + i * 2] & 0xff) | (y[yOffset + i * 2 + 1] << 8));
            int result = _table[xCh] - _table[yCh];
            if (result != 0) {
                return result;
            }

        }
        return xLength - yLength;
    }

    // Identical out to the shortest string, so length is now the
    // determining factor.
    public static UpperCase initialize(File file) {
        byte[] buffer = new byte[(Character.MAX_VALUE + 1) * 2];
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; ++i) {
            EndianUtilities.writeBytesLittleEndian((short) Character.toUpperCase((char) i), buffer, i * 2);
        }
        Stream s = file.openStream(AttributeType.Data, null, FileAccess.ReadWrite);
        try {
            s.write(buffer, 0, buffer.length);
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
        }
        return new UpperCase(file);
    }
}
