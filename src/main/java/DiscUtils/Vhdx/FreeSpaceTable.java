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

package DiscUtils.Vhdx;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Util.Sizes;


public final class FreeSpaceTable {
    private long _fileSize;

    private List<StreamExtent> _freeExtents;

    public FreeSpaceTable(long fileSize) {
        // There used to be a check here that the file size is a multiple of 1 MB.
        // However, the
        // VHDX Format Specification 1.00 25-August-2012 only states this:
        //     "the only restriction being that all objects have 1 MB alignment within the file"
        // Which does not mean the file size has to be multiple of 1MiB.
        // (The last extent can be less than 1MiB and still all extent have 1 MiB alignment.)

        _freeExtents = new ArrayList<>();
        _freeExtents.add(new StreamExtent(0, fileSize));
        _fileSize = fileSize;
    }

    public void extendTo(long fileSize, boolean isFree) {
        if (fileSize % Sizes.OneMiB != 0) {
            throw new IllegalArgumentException("VHDX space must be allocated on 1MB boundaries");
        }

        if (fileSize < _fileSize) {
            throw new IndexOutOfBoundsException("Attempt to extend file to smaller size: " + fileSize);
        }

        _fileSize = fileSize;

        if (isFree) {
            _freeExtents = StreamExtent.union(_freeExtents, new StreamExtent(_fileSize, fileSize - _fileSize));
        }
    }

    public void release(long start, long length) {
        validateRange(start, length, "release");
        _freeExtents = StreamExtent.union(_freeExtents, new StreamExtent(start, length));
    }

    public void reserve(long start, long length) {
        validateRange(start, length, "reserve");
        _freeExtents = StreamExtent.subtract(_freeExtents, new StreamExtent(start, length));
    }

    public void reserve(List<StreamExtent> extents) {
        _freeExtents = StreamExtent.subtract(_freeExtents, extents);
    }

    /**
     * @param start {@cs out}
     */
    public boolean tryAllocate(long length, long[] start) {
        if (length % Sizes.OneMiB != 0) {
            throw new IllegalArgumentException("VHDX free space must be managed on 1MB boundaries");
        }

        for (int i = 0; i < _freeExtents.size(); ++i) {
            StreamExtent extent = _freeExtents.get(i);
            if (extent.getLength() == length) {
                _freeExtents.remove(i);
                start[0] = extent.getStart();
                return true;
            }

            if (extent.getLength() > length) {
                _freeExtents.set(i, new StreamExtent(extent.getStart() + length, extent.getLength() - length));
                start[0] = extent.getStart();
                return true;
            }
        }

        start[0] = 0;
        return false;
    }

    private void validateRange(long start, long length, String method) {
        if (start % Sizes.OneMiB != 0) {
            throw new IllegalArgumentException("VHDX free space must be managed on 1MB boundaries");
        }

        if (length % Sizes.OneMiB != 0) {
            throw new IllegalArgumentException("VHDX free space must be managed on 1MB boundaries");
        }

        if (start < 0 || start > _fileSize || length > _fileSize - start) {
//Debug.println(length + ", " + _fileSize + ", " + start);
            throw new IndexOutOfBoundsException("Attempt to " + method + " space outside of file range");
        }
    }
}
