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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Builder.BuilderExtent;


public class PathTable extends BuilderExtent {
    private final boolean _byteSwap;

    private final List<BuildDirectoryInfo> _dirs;

    private final Charset _enc;

    private final Map<BuildDirectoryMember, Integer> _locations;

    private byte[] _readCache;

    public PathTable(boolean byteSwap,
            Charset enc,
            List<BuildDirectoryInfo> dirs,
            Map<BuildDirectoryMember, Integer> locations,
            long start) {
        super(start, calcLength(enc, dirs));
        _byteSwap = byteSwap;
        _enc = enc;
        _dirs = dirs;
        _locations = locations;
    }

    public void close() throws IOException {
    }

    public void prepareForRead() {
        _readCache = new byte[(int) getLength()];
        int pos = 0;
        List<BuildDirectoryInfo> sortedList = new ArrayList<>(_dirs);
        Collections.sort(sortedList, BuildDirectoryInfo.PathTableSortComparison);
        Map<BuildDirectoryInfo, Short> dirNumbers = new HashMap<>(_dirs.size());
        short i = 1;
        for (BuildDirectoryInfo di : sortedList) {
            dirNumbers.put(di, i++);
            PathTableRecord ptr = new PathTableRecord();
            ptr.DirectoryIdentifier = di.pickName(null, _enc);
            ptr.LocationOfExtent = _locations.get(di);
            ptr.ParentDirectoryNumber = dirNumbers.get(di.getParent());
            pos += ptr.write(_byteSwap, _enc, _readCache, pos);
        }
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

    private static int calcLength(Charset enc, List<BuildDirectoryInfo> dirs) {
        int length = 0;
        for (BuildDirectoryInfo di : dirs) {
            length += di.getPathTableEntrySize(enc);
        }
        return length;
    }
}
