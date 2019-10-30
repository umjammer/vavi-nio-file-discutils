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

package DiscUtils.Udf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.Vfs.IVfsDirectory;
import DiscUtils.Streams.Util.StreamUtilities;


public class Directory extends File implements IVfsDirectory<FileIdentifier, File> {
    private final List<FileIdentifier> _entries;

    public Directory(UdfContext context, LogicalPartition partition, FileEntry fileEntry) {
        super(context, partition, fileEntry, (int) partition.getLogicalBlockSize());

        if (getFileContent().getCapacity() > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Very large directory");
        }

        _entries = new ArrayList<>();

        byte[] contentBytes = StreamUtilities.readExact(getFileContent(), 0, (int) getFileContent().getCapacity());

        int pos = 0;
        while (pos < contentBytes.length) {
            FileIdentifier id = new FileIdentifier();
            int size = id.readFrom(contentBytes, pos);

            if (Collections.disjoint(id._FileCharacteristics, EnumSet.of(FileCharacteristic.Deleted, FileCharacteristic.Parent))) {
                _entries.add(id);
            }

            pos += size;
        }
    }

    public List<FileIdentifier> getAllEntries() {
        return _entries;
    }

    public FileIdentifier getSelf() {
        return null;
    }

    public FileIdentifier createNewFile(String name) {
        throw new UnsupportedOperationException();
    }

    public FileIdentifier getEntryByName(String name) {
        for (FileIdentifier entry : _entries) {
            if (entry.Name.compareTo(name) == 0) {
                return entry;
            }
        }

        return null;
    }
}
