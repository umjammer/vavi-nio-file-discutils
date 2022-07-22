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

package discUtils.iso9660;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.streams.builder.BuilderExtent;


class PathTable extends BuilderExtent {

    private final boolean byteSwap;

    private final List<BuildDirectoryInfo> dirs;

    private final Charset enc;

    private final Map<BuildDirectoryMember, Integer> locations;

    private byte[] readCache;

    public PathTable(boolean byteSwap,
            Charset enc,
            List<BuildDirectoryInfo> dirs,
            Map<BuildDirectoryMember, Integer> locations,
            long start) {
        super(start, calcLength(enc, dirs));
        this.byteSwap = byteSwap;
        this.enc = enc;
        this.dirs = dirs;
        this.locations = locations;
    }

    public void close() throws IOException {
    }

    public void prepareForRead() {
        readCache = new byte[(int) getLength()];
        int pos = 0;
        List<BuildDirectoryInfo> sortedList = new ArrayList<>(dirs);
        sortedList.sort(BuildDirectoryInfo.PathTableSortComparison);
        Map<BuildDirectoryInfo, Short> dirNumbers = new HashMap<>(dirs.size());
        short i = 1;
        for (BuildDirectoryInfo di : sortedList) {
            dirNumbers.put(di, i++);
            PathTableRecord ptr = new PathTableRecord();
            ptr.directoryIdentifier = di.pickName(null, enc);
            ptr.locationOfExtent = locations.get(di);
            ptr.parentDirectoryNumber = dirNumbers.get(di.getParent());
            pos += ptr.write(byteSwap, enc, readCache, pos);
        }
    }

    public int read(long diskOffset, byte[] buffer, int offset, int count) {
        long relPos = diskOffset - getStart();
        int numRead = (int) Math.min(count, readCache.length - relPos);
        System.arraycopy(readCache, (int) relPos, buffer, offset, numRead);
        return numRead;
    }

    public void disposeReadState() {
        readCache = null;
    }

    private static int calcLength(Charset enc, List<BuildDirectoryInfo> dirs) {
        int length = 0;
        for (BuildDirectoryInfo di : dirs) {
            length += di.getPathTableEntrySize(enc);
        }
        return length;
    }
}
