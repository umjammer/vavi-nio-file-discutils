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

package discUtils.opticalDisk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.StreamUtilities;


/**
 * Interprets a Mode 2 image.
 *
 * Effectively just strips the additional header / footer from the Mode 2 sector
 * data - does not attempt to validate the information.
 */
class Mode2Buffer implements IBuffer {

    private final byte[] iobuffer;

    private final IBuffer wrapped;

    public Mode2Buffer(IBuffer toWrap) {
        wrapped = toWrap;
        iobuffer = new byte[DiscImageFile.Mode2SectorSize];
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return wrapped.getCapacity() / DiscImageFile.Mode2SectorSize * DiscImageFile.Mode1SectorSize;
    }

    public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, getCapacity()));
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        int totalToRead = (int) Math.min(getCapacity() - pos, count);
        int totalRead = 0;
        while (totalRead < totalToRead) {
            long thisPos = pos + totalRead;
            long sector = thisPos / DiscImageFile.Mode1SectorSize;
            int sectorOffset = (int) (thisPos - sector * DiscImageFile.Mode1SectorSize);
            StreamUtilities
                    .readExact(wrapped, sector * DiscImageFile.Mode2SectorSize, iobuffer, 0, DiscImageFile.Mode2SectorSize);
            int bytesToCopy = Math.min(DiscImageFile.Mode1SectorSize - sectorOffset, totalToRead - totalRead);
            System.arraycopy(iobuffer, 24 + sectorOffset, buffer, offset + totalRead, bytesToCopy);
            totalRead += bytesToCopy;
        }
        return totalRead;
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void clear(long pos, int count) {
        throw new UnsupportedOperationException();
    }

    public void flush() {
        throw new UnsupportedOperationException();
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        long capacity = getCapacity();
        if (start < capacity) {
            long end = Math.min(start + count, capacity);
            result.add(new StreamExtent(start, end - start));
        }
        return result;
    }
}
