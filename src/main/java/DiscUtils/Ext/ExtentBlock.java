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

package DiscUtils.Ext;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import dotnet4j.io.IOException;


public class ExtentBlock implements IByteArraySerializable {
    public Extent[] Extents;

    public ExtentHeader Header;

    public ExtentIndex[] Index;

    public int size() {
        return 12 + Header.getMaxEntries() * 12;
    }

    public int readFrom(byte[] buffer, int offset) {
        Header = EndianUtilities.<ExtentHeader> toStruct(ExtentHeader.class, buffer, offset + 0);
        if (Header.Magic != ExtentHeader.HeaderMagic) {
            throw new IOException("Invalid extent header reading inode");
        }

        if (Header.Depth == 0) {
            Index = null;
            Extents = new Extent[Header.getEntries()];
            for (int i = 0; i < Extents.length; ++i) {
                Extents[i] = EndianUtilities.<Extent> toStruct(Extent.class, buffer, offset + 12 + i * 12);
            }
        } else {
            Extents = null;
            Index = new ExtentIndex[Header.getEntries()];
            for (int i = 0; i < Index.length; ++i) {
                Index[i] = EndianUtilities.<ExtentIndex> toStruct(ExtentIndex.class, buffer, offset + 12 + i * 12);
            }
        }
        return 12 + Header.getMaxEntries() * 12;
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
