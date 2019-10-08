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

package DiscUtils.Iso9660;

import java.io.IOException;

import DiscUtils.Streams.Builder.BuilderExtent;


public abstract class VolumeDescriptorDiskRegion extends BuilderExtent {
    private byte[] _readCache;

    public VolumeDescriptorDiskRegion(long start) {
        super(start, IsoUtilities.SectorSize);
    }

    public void close() throws IOException {
    }

    public void prepareForRead() {
        _readCache = getBlockData();
    }

    public int read(long diskOffset, byte[] buffer, int offset, int count) {
        long relPos = diskOffset - getStart();
        int numRead = (int) Math.min(count, _readCache.length - relPos);
        System.arraycopy(_readCache, (int) relPos, buffer, offset, numRead);
        return numRead;
    }

    public void disposeReadState() {
        _readCache = null;
    }

    protected abstract byte[] getBlockData();
}