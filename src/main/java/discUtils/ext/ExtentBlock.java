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

package discUtils.ext;

import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.IOException;


public class ExtentBlock implements IByteArraySerializable {

    public Extent[] extents;

    public ExtentHeader header;

    public ExtentIndex[] index;

    public int size() {
        return 12 + header.getMaxEntries() * 12;
    }

    public int readFrom(byte[] buffer, int offset) {
        header = EndianUtilities.toStruct(ExtentHeader.class, buffer, offset + 0);
        if (header.magic != ExtentHeader.HeaderMagic) {
            throw new IOException("Invalid extent header reading inode");
        }

        if (header.depth == 0) {
            index = null;
            extents = new Extent[header.getEntries()];
            for (int i = 0; i < extents.length; ++i) {
                extents[i] = EndianUtilities.toStruct(Extent.class, buffer, offset + 12 + i * 12);
            }
        } else {
            extents = null;
            index = new ExtentIndex[header.getEntries()];
            for (int i = 0; i < index.length; ++i) {
                index[i] = EndianUtilities.toStruct(ExtentIndex.class, buffer, offset + 12 + i * 12);
            }
        }
        return 12 + header.getMaxEntries() * 12;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
