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

package discUtils.registry;

import java.nio.charset.StandardCharsets;

import discUtils.streams.IByteArraySerializable;


/**
 * base class for the different kinds of cell present in a hive.
 */
public abstract class Cell implements IByteArraySerializable {
    public Cell(int index) {
        setIndex(index);
    }

    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int value) {
        index = value;
    }

    @Override public abstract int size();

    @Override public abstract int readFrom(byte[] buffer, int offset);

    @Override public abstract void writeTo(byte[] buffer, int offset);

    public static Cell parse(RegistryHive hive, int index, byte[] buffer, int pos) {
        String type = new String(buffer, pos, 2, StandardCharsets.US_ASCII);
        Cell result;
        switch (type) {
        case "nk":
            result = new KeyNodeCell(index);
            break;
        case "sk":
            result = new SecurityCell(index);
            break;
        case "vk":
            result = new ValueCell(index);
            break;
        case "lh":
        case "lf":
            result = new SubKeyHashedListCell(hive, index);
            break;
        case "li":
        case "ri":
            result = new SubKeyIndirectListCell(hive, index);
            break;
        default:
            throw new IllegalArgumentException("Unknown cell type '" + type + "'");
        }
        result.readFrom(buffer, pos);
        return result;
    }
}
