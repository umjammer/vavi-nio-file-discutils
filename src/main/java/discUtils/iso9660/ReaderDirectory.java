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

import java.util.ArrayList;
import java.util.List;

import discUtils.core.vfs.IVfsDirectory;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;


public class ReaderDirectory extends File implements IVfsDirectory<ReaderDirEntry, File> {

    private final List<ReaderDirEntry> records;

    public ReaderDirectory(IsoContext context, ReaderDirEntry dirEntry) {
        super(context, dirEntry);
        byte[] buffer = new byte[IsoUtilities.SectorSize];
        Stream extent = new ExtentStream(this.context
                .getDataStream(), dirEntry.getRecord().locationOfExtent, 0xffff_ffffL, (byte) 0, (byte) 0);

        records = new ArrayList<>();

        int totalLength = dirEntry.getRecord().dataLength;
        int totalRead = 0;

        while (totalRead < totalLength) {
            int bytesRead = Math.min(buffer.length, totalLength - totalRead);

            StreamUtilities.readExact(extent, buffer, 0, bytesRead);
            totalRead += bytesRead;

            int pos = 0;
            while (pos < bytesRead && buffer[pos] != 0) {
                DirectoryRecord[] dr = new DirectoryRecord[1];
                int length = DirectoryRecord.readFrom(buffer, pos, context.getVolumeDescriptor().characterEncoding, dr);

                if (!IsoUtilities.isSpecialDirectory(dr[0])) {
                    ReaderDirEntry childDirEntry = new ReaderDirEntry(this.context, dr[0]);

                    if (context.getSuspDetected() && context.getRockRidgeIdentifier() != null &&
                        !context.getRockRidgeIdentifier().isEmpty()) {
                        if (childDirEntry.getSuspRecords() == null ||
                            !childDirEntry.getSuspRecords().hasEntry(context.getRockRidgeIdentifier(), "RE")) {
                            records.add(childDirEntry);
                        }
                    } else {
                        records.add(childDirEntry);
                    }
                } else if (dr[0].fileIdentifier.equals("\0")) {
                    self = new ReaderDirEntry(this.context, dr[0]);
                }

                pos += length;
            }
        }
    }

    @Override public byte[] getSystemUseData() {
        return getSelf().getRecord().systemUseData;
    }

    @Override public List<ReaderDirEntry> getAllEntries() {
        return records;
    }

    private ReaderDirEntry self;

    @Override public ReaderDirEntry getSelf() {
        return self;
    }

    @Override public ReaderDirEntry getEntryByName(String name) {
        boolean anyVerMatch = name.indexOf(';') < 0;
        String normName = IsoUtilities.normalizeFileName(name).toUpperCase();
        if (anyVerMatch) {
            normName = normName.substring(0, normName.lastIndexOf(';') + 1);
        }

        for (ReaderDirEntry r : records) {
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

    @Override public ReaderDirEntry createNewFile(String name) {
        throw new UnsupportedOperationException();
    }
}
