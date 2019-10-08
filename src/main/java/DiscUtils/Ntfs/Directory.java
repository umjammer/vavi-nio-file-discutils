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

package DiscUtils.Ntfs;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.Internal.Utilities;
import moe.yo3explorer.dotnetio4j.IOException;


public class Directory extends File {
    private IndexView<FileNameRecord, FileRecordReference> _index;

    public Directory(INtfsContext context, FileRecord baseRecord) {
        super(context, baseRecord);
    }

    private IndexView<FileNameRecord, FileRecordReference> getIndex() {
        if (_index == null && streamExists(AttributeType.IndexRoot, "$I30")) {
            _index = new IndexView<>(getIndex("$I30"));
        }

        return _index;
    }

    public boolean getIsEmpty() {
        return getIndex().getCount() == 0;
    }

    public List<DirectoryEntry> getAllEntries(boolean filter) {
        List<DirectoryEntry> result = new ArrayList<>();
        Map<FileNameRecord, FileRecordReference> entries = filter ? filterEntries(getIndex().getEntries()) : getIndex().getEntries();
        for (Map.Entry<FileNameRecord, FileRecordReference> entry : entries.entrySet()) {
            result.add(new DirectoryEntry(this, entry.getValue(), entry.getKey()));
        }
        return result;
    }

    public void updateEntry(DirectoryEntry entry) {
        getIndex().set___idx(entry.getDetails(), entry.getReference());
        updateRecordInMft();
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "DIRECTORY (" + super.toString() + ")");
        writer.println(indent + "  File Number: " + getIndexInMft());
        if (getIndex() != null) {
            for (Map.Entry<FileNameRecord, FileRecordReference> entry : getIndex().getEntries().entrySet()) {
                writer.println(indent + "  DIRECTORY ENTRY (" + entry.getKey().FileName + ")");
                writer.println(indent + "    MFT Ref: " + entry.getValue());
                entry.getKey().dump(writer, indent + "    ");
            }
        }
    }

    public String toString() {
        return super.toString() + "\\";
    }

    public static Directory createNew(INtfsContext context, EnumSet<FileAttributeFlags> parentDirFlags) {
        Directory dir = (Directory) context.getAllocateFile().invoke(EnumSet.of(FileRecordFlags.IsDirectory));
        StandardInformation
                .initializeNewFile(dir,
                                   EnumSet.of(FileAttributeFlags.Archive,
                                              parentDirFlags
                                                      .contains(FileAttributeFlags.Compressed) ? FileAttributeFlags.Compressed
                                                                                               : FileAttributeFlags.None));
        // Create the index root attribute by instantiating a new index
        dir.createIndex("$I30", AttributeType.FileName, AttributeCollationRule.Filename);
        dir.updateRecordInMft();
        return dir;
    }

    public DirectoryEntry getEntryByName(String name) {
        String searchName = name;
        int streamSepPos = name.indexOf(':');
        if (streamSepPos >= 0) {
            searchName = name.substring(0, streamSepPos);
        }

        Map.Entry<FileNameRecord, FileRecordReference> entry = getIndex().findFirst_(new FileNameQuery(searchName, _context.getUpperCase()));
        if (entry.getKey() != null) {
            return new DirectoryEntry(this, entry.getValue(), entry.getKey());
        }

        return null;
    }

    public DirectoryEntry addEntry(File file, String name, FileNameNamespace nameNamespace) {
        if (name.length() > 255) {
            throw new IOException("Invalid file name, more than 255 characters: " + name);
        }

        if ((name.indexOf('\0') & name.indexOf('/' )) != -1) {
            throw new IOException("Invalid file name, contains \'\\0\' or \'/\': " + name);
        }

        FileNameRecord newNameRecord = file.getFileNameRecord(null, true);
        newNameRecord._FileNameNamespace = nameNamespace;
        newNameRecord.FileName = name;
        newNameRecord.ParentDirectory = getMftReference();
        NtfsStream nameStream = file.createStream(AttributeType.FileName, null);
        nameStream.setContent(newNameRecord);
        file.setHardLinkCount((short) (file.getHardLinkCount() + 1));
        file.updateRecordInMft();
        getIndex().set___idx(newNameRecord, file.getMftReference());
        modified();
        updateRecordInMft();
        return new DirectoryEntry(this, file.getMftReference(), newNameRecord);
    }

    public void removeEntry(DirectoryEntry dirEntry) {
        File file = _context.getGetFileByRef().invoke(dirEntry.getReference());
        FileNameRecord nameRecord = dirEntry.getDetails();
        getIndex().remove(dirEntry.getDetails());
        for (NtfsStream stream : file.getStreams(AttributeType.FileName, null)) {
            FileNameRecord streamName = stream.getContent();
            if (nameRecord.equals(streamName)) {
                file.removeStream(stream);
                break;
            }

        }
        file.setHardLinkCount((short) (file.getHardLinkCount() - 1));
        file.updateRecordInMft();
        modified();
        updateRecordInMft();
    }

    public String createShortName(String name) {
        String baseName = "";
        String ext = "";
        int lastPeriod = name.lastIndexOf('.');
        int i = 0;
        while (baseName.length() < 6 && i < name.length() && i != lastPeriod) {
            char upperChar = Character.toUpperCase(name.charAt(i));
            if (Utilities.is8Dot3Char(upperChar)) {
                baseName += upperChar;
            }

            ++i;
        }
        if (lastPeriod >= 0) {
            i = lastPeriod + 1;
            while (ext.length() < 3 && i < name.length()) {
                char upperChar = Character.toUpperCase(name.charAt(i));
                if (Utilities.is8Dot3Char(upperChar)) {
                    ext += upperChar;
                }

                ++i;
            }
        }

        i = 1;
        String candidate;
        do {
            String suffix = String.format("~%d", i);
            candidate = baseName.substring(0, Math.min(8 - suffix.length(), baseName.length())) + suffix +
                        (ext.length() > 0 ? "." + ext : "");
            i++;
        } while (getEntryByName(candidate) != null);
        return candidate;
    }

    private Map<FileNameRecord, FileRecordReference> filterEntries(Map<FileNameRecord, FileRecordReference> entriesIter) {
        Map<FileNameRecord, FileRecordReference> entries = new HashMap<>();
        for (Map.Entry<FileNameRecord, FileRecordReference> entry : entriesIter.entrySet()) {
            // Weed out short-name entries for files and any hidden / system / metadata files.
            if (entry.getKey().Flags.contains(FileAttributeFlags.Hidden) && _context.getOptions().getHideHiddenFiles()) {
                continue;
            }

            if (entry.getKey().Flags.contains(FileAttributeFlags.System) && _context.getOptions().getHideSystemFiles()) {
                continue;
            }

            if (entry.getValue().getMftIndex() < 24 && _context.getOptions().getHideMetafiles()) {
                continue;
            }

            if (entry.getKey()._FileNameNamespace == FileNameNamespace.Dos && _context.getOptions().getHideDosFileNames()) {
                continue;
            }

            entries.put(entry.getKey(), entry.getValue());
        }
        return entries;
    }

    private final static class FileNameQuery implements Comparable<byte[]> {
        private final byte[] _query;

        private final UpperCase _upperCase;

        public FileNameQuery(String query, UpperCase upperCase) {
            _query = query.getBytes(Charset.forName("Unicode"));
            _upperCase = upperCase;
        }

        public int compareTo(byte[] buffer) {
            // Note: this is internal knowledge of FileNameRecord structure - but for performance
            // reasons, we don't want to decode the entire structure.  In fact can avoid the string
            // conversion as well.
            byte fnLen = buffer[0x40];
            return _upperCase.compare(_query, 0, _query.length, buffer, 0x42, fnLen * 2);
        }
    }
}
