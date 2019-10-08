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

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.Vfs.IVfsDirectory;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.Stream;


public class ReaderDirectory extends File implements IVfsDirectory<ReaderDirEntry, File> {
    private final List<ReaderDirEntry> _records;

    public ReaderDirectory(IsoContext context, ReaderDirEntry dirEntry) {
        super(context, dirEntry);
        byte[] buffer = new byte[IsoUtilities.SectorSize];
        Stream extent = new ExtentStream(_context
                .getDataStream(), dirEntry.getRecord().LocationOfExtent, Integer.MAX_VALUE, (byte) 0, (byte) 0);
        _records = new ArrayList<>();
        int totalLength = dirEntry.getRecord().DataLength;
        int totalRead = 0;
        while (totalRead < totalLength) {
            int bytesRead = Math.min(buffer.length, totalLength - totalRead);
            StreamUtilities.readExact(extent, buffer, 0, bytesRead);
            totalRead += bytesRead;
            int pos = 0;
            while (pos < bytesRead && buffer[pos] != 0) {
                DirectoryRecord[] dr = new DirectoryRecord[1];
                int length = DirectoryRecord.readFrom(buffer, pos, context.getVolumeDescriptor().CharacterEncoding, dr);
                if (!IsoUtilities.isSpecialDirectory(dr[0])) {
                    ReaderDirEntry childDirEntry = new ReaderDirEntry(_context, dr[0]);
                    if (context.getSuspDetected() && context.getRockRidgeIdentifier() != null &&
                        !context.getRockRidgeIdentifier().isEmpty()) {
                        if (childDirEntry.getSuspRecords() == null ||
                            !childDirEntry.getSuspRecords().hasEntry(context.getRockRidgeIdentifier(), "RE")) {
                            _records.add(childDirEntry);
                        }

                    } else {
                        _records.add(childDirEntry);
                    }
                } else if (dr[0].FileIdentifier.equals("\0")) {
                    __Self = new ReaderDirEntry(_context, dr[0]);
                }

                pos += length;
            }
        }
    }

    public byte[] getSystemUseData() {
        return getSelf().getRecord().SystemUseData;
    }

    public List<ReaderDirEntry> getAllEntries() {
        return _records;
    }

    private ReaderDirEntry __Self;

    public ReaderDirEntry getSelf() {
        return __Self;
    }

    public ReaderDirEntry getEntryByName(String name) {
        boolean anyVerMatch = name.indexOf(';') < 0;
        String normName = IsoUtilities.normalizeFileName(name).toUpperCase();
        if (anyVerMatch) {
            normName = normName.substring(0, normName.lastIndexOf(';') + 1);
        }

        for (ReaderDirEntry r : _records) {
            String toComp = IsoUtilities.normalizeFileName(r.getFileName()).toUpperCase();
            if (!anyVerMatch && toComp.equals(normName)) {
                return r;
            }

            if (anyVerMatch && toComp.startsWith(normName)) {
                return r;
            }
        }
        return null;
    }

    public ReaderDirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
