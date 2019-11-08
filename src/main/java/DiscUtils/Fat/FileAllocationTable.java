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

package DiscUtils.Fat;

import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


public class FileAllocationTable {
    private final FatBuffer _buffer;

    private final int _firstFatSector;

    private final int _numFats;

    private final Stream _stream;

    public FileAllocationTable(FatType type, Stream stream, int firstFatSector, int fatSize, int numFats, byte activeFat) {
        _stream = stream;
        _firstFatSector = firstFatSector;
        _numFats = numFats;
        _stream.setPosition((firstFatSector + fatSize * activeFat) * Sizes.Sector);
        _buffer = new FatBuffer(type, StreamUtilities.readExact(_stream, fatSize * Sizes.Sector));
    }

    public boolean isFree(int val) {
        return _buffer.isFree(val);
    }

    public boolean isEndOfChain(int val) {
        return _buffer.isEndOfChain(val);
    }

    public boolean isBadCluster(int val) {
        return _buffer.isBadCluster(val);
    }

    public int getNext(int cluster) {
        return _buffer.getNext(cluster);
    }

    public void setEndOfChain(int cluster) {
        _buffer.setEndOfChain(cluster);
    }

    public void setBadCluster(int cluster) {
        _buffer.setBadCluster(cluster);
    }

    public void setNext(int cluster, int next) {
        _buffer.setNext(cluster, next);
    }

    public void flush() {
        for (int i = 0; i < _numFats; ++i) {
            _buffer.writeDirtyRegions(_stream, _firstFatSector * Sizes.Sector + _buffer.getSize() * i);
        }
        _buffer.clearDirtyRegions();
    }

    public boolean tryGetFreeCluster(int[] cluster) {
        boolean result = _buffer.tryGetFreeCluster(cluster);
        return result;
    }

    public void freeChain(int head) {
        _buffer.freeChain(head);
    }

    public int getNumEntries() {
        return _buffer.getNumEntries();
    }
}
