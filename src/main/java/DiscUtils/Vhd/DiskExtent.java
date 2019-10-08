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

package DiscUtils.Vhd;

import DiscUtils.Core.VirtualDiskExtent;
import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;


public final class DiskExtent extends VirtualDiskExtent {
    private final DiskImageFile _file;

    public DiskExtent(DiskImageFile file) {
        _file = file;
    }

    public long getCapacity() {
        return _file.getCapacity();
    }

    public boolean getIsSparse() {
        return _file.getIsSparse();
    }

    public long getStoredSize() {
        return _file.getStoredSize();
    }

    public MappedStream openContent(SparseStream parent, Ownership ownsParent) {
        return _file.doOpenContent(parent, ownsParent);
    }

}