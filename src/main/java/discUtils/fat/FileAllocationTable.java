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

package discUtils.fat;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;

import static java.lang.System.getLogger;


public class FileAllocationTable {

    private static final Logger logger = getLogger(FileAllocationTable.class.getName());

    private final FatBuffer buffer;

    private final int firstFatSector;

    private final int numFats;

    private final Stream stream;

    public FileAllocationTable(FatType type, Stream stream, int firstFatSector, int fatSize, int numFats, byte activeFat) {
        this.stream = stream;
        this.firstFatSector = firstFatSector;
        this.numFats = numFats;

logger.log(Level.TRACE, "%d, %016x".formatted(firstFatSector, (firstFatSector + fatSize * activeFat) * Sizes.Sector));
        this.stream.position((firstFatSector + (long) fatSize * activeFat) * Sizes.Sector);
        buffer = new FatBuffer(type, StreamUtilities.readExact(this.stream, fatSize * Sizes.Sector));
    }

    public boolean isFree(int val) {
        return buffer.isFree(val);
    }

    public boolean isEndOfChain(int val) {
        return buffer.isEndOfChain(val);
    }

    public boolean isBadCluster(int val) {
        return buffer.isBadCluster(val);
    }

    public int getNext(int cluster) {
        return buffer.getNext(cluster);
    }

    public void setEndOfChain(int cluster) {
        buffer.setEndOfChain(cluster);
    }

    public void setBadCluster(int cluster) {
        buffer.setBadCluster(cluster);
    }

    public void setNext(int cluster, int next) {
        buffer.setNext(cluster, next);
    }

    public void flush() {
        for (int i = 0; i < numFats; i++) {
            buffer.writeDirtyRegions(stream, (long) firstFatSector * Sizes.Sector + buffer.getSize() * i);
        }
        buffer.clearDirtyRegions();
    }

    public boolean tryGetFreeCluster(int[] cluster) {
        boolean result = buffer.tryGetFreeCluster(cluster);
        return result;
    }

    public void freeChain(int head) {
        buffer.freeChain(head);
    }

    public int getNumEntries() {
        return buffer.getNumEntries();
    }
}
