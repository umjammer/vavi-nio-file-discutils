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

package discUtils.ntfs;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import discUtils.core.internal.Utilities;
import dotnet4j.io.IOException;
import dotnet4j.util.compat.Tuple;


public class Directory extends File {

    private IndexView<FileNameRecord, FileRecordReference> index;

    public Directory(INtfsContext context, FileRecord baseRecord) {
        super(context, baseRecord);
    }

    private IndexView<FileNameRecord, FileRecordReference> getIndex() {
        if (index == null && streamExists(AttributeType.IndexRoot, "$I30")) {
            index = new IndexView<>(FileNameRecord.class, FileRecordReference.class, getIndex("$I30"));
        }

        return index;
    }

    public boolean isEmpty() {
        return getIndex().getCount() == 0;
    }

    public List<DirectoryEntry> getAllEntries(boolean filter) {
        List<DirectoryEntry> result = new ArrayList<>();
        List<Tuple<FileNameRecord, FileRecordReference>> entries = filter ? filterEntries(getIndex().getEntries()) : getIndex().getEntries();
        for (Tuple<FileNameRecord, FileRecordReference> entry : entries) {
            result.add(new DirectoryEntry(this, entry.getValue(), entry.getKey()));
        }
        return result;
    }

    public void updateEntry(DirectoryEntry entry) {
        getIndex().put(entry.getDetails(), entry.getReference());
        updateRecordInMft();
    }

    @Override public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "DIRECTORY (" + super.toString() + ")");
        writer.println(indent + "  File Number: " + getIndexInMft());
        if (getIndex() != null) {
            for (Tuple<FileNameRecord, FileRecordReference> entry : getIndex().getEntries()) {
                writer.println(indent + "  DIRECTORY ENTRY (" + entry.getKey().fileName + ")");
                writer.println(indent + "    MFT Ref: " + entry.getValue());
                entry.getKey().dump(writer, indent + "    ");
            }
        }
    }

    @Override public String toString() {
        return super.toString() + java.io.File.separator;
    }

    public static Directory createNew(INtfsContext context, EnumSet<FileAttributeFlags> parentDirFlags) {
        Directory dir = (Directory) context.getAllocateFile().invoke(EnumSet.of(FileRecordFlags.IsDirectory));
        EnumSet<FileAttributeFlags> flags = EnumSet.of(FileAttributeFlags.Archive);
        if (parentDirFlags.contains(FileAttributeFlags.Compressed)) {
            flags.add(FileAttributeFlags.Compressed);
        }
        StandardInformation.initializeNewFile(dir, flags);
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

        Tuple<FileNameRecord, FileRecordReference> entry = getIndex().findFirst_(new FileNameQuery(searchName, context.getUpperCase()));
        if (entry != null && entry.getKey() != null) {
            return new DirectoryEntry(this, entry.getValue(), entry.getKey());
        }

        return null;
    }

    public DirectoryEntry addEntry(File file, String name, FileNameNamespace nameNamespace) {
        if (name.length() > 255) {
            throw new IOException("Invalid file name, more than 255 characters: " + name);
        }

        if ((name.indexOf('\0') & name.indexOf('/')) != -1) {
            throw new IOException("Invalid file name, contains '\\0' or '/': " + name);
        }

        FileNameRecord newNameRecord = file.getFileNameRecord(null, true);
        newNameRecord.fileNameNamespace = nameNamespace;
        newNameRecord.fileName = name;
        newNameRecord.parentDirectory = getMftReference();

        NtfsStream nameStream = file.createStream(AttributeType.FileName, null);
        nameStream.setContent(newNameRecord);

        file.setHardLinkCount((short) (file.getHardLinkCount() + 1));
        file.updateRecordInMft();

        getIndex().put(newNameRecord, file.getMftReference());

        modified();
        updateRecordInMft();

        return new DirectoryEntry(this, file.getMftReference(), newNameRecord);
    }

    public void removeEntry(DirectoryEntry dirEntry) {
        File file = context.getGetFileByRef().invoke(dirEntry.getReference());
        FileNameRecord nameRecord = dirEntry.getDetails();
        getIndex().remove(dirEntry.getDetails());
        for (NtfsStream stream : file.getStreams(AttributeType.FileName, null)) {
            FileNameRecord streamName = stream.getContent(FileNameRecord.class);
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
        StringBuilder baseName = new StringBuilder();
        StringBuilder ext = new StringBuilder();
        int lastPeriod = name.lastIndexOf('.');
        int i = 0;
        while (baseName.length() < 6 && i < name.length() && i != lastPeriod) {
            char upperChar = Character.toUpperCase(name.charAt(i));
            if (Utilities.is8Dot3Char(upperChar)) {
                baseName.append(upperChar);
            }

            ++i;
        }
        if (lastPeriod >= 0) {
            i = lastPeriod + 1;
            while (ext.length() < 3 && i < name.length()) {
                char upperChar = Character.toUpperCase(name.charAt(i));
                if (Utilities.is8Dot3Char(upperChar)) {
                    ext.append(upperChar);
                }

                ++i;
            }
        }

        i = 1;
        String candidate;
        do {
            String suffix = String.format("~%d", i);
            candidate = baseName.substring(0, Math.min(8 - suffix.length(), baseName.length())) + suffix +
                        (!ext.isEmpty() ? "." + ext : "");
            i++;
        } while (getEntryByName(candidate) != null);
        return candidate;
    }

    private List<Tuple<FileNameRecord, FileRecordReference>> filterEntries(List<Tuple<FileNameRecord, FileRecordReference>> entriesIter) {
        List<Tuple<FileNameRecord, FileRecordReference>> entries = new ArrayList<>();
        for (Tuple<FileNameRecord, FileRecordReference> entry : entriesIter) {
            // Weed out short-name entries for files and any hidden / system / metadata files.
            if (entry.getKey().flags.contains(FileAttributeFlags.Hidden) && context.getOptions().hideHiddenFiles()) {
                continue;
            }

            if (entry.getKey().flags.contains(FileAttributeFlags.System) && context.getOptions().hideSystemFiles()) {
                continue;
            }

            if (entry.getValue().getMftIndex() < 24 && context.getOptions().hideMetafiles()) {
                continue;
            }

            if (entry.getKey().fileNameNamespace == FileNameNamespace.Dos && context.getOptions().hideDosFileNames()) {
                continue;
            }

            entries.add(entry);
        }
        return entries;
    }

    private final static class FileNameQuery implements Comparable<byte[]> {

        private final byte[] query;

        private final UpperCase upperCase;

        public FileNameQuery(String query, UpperCase upperCase) {
            this.query = query.getBytes(StandardCharsets.UTF_16LE);
            this.upperCase = upperCase;
        }

        @Override public int compareTo(byte[] buffer) {
            // Note: this is internal knowledge of FileNameRecord structure - but for performance
            // reasons, we don't want to decode the entire structure.  In fact can avoid the string
            // conversion as well.
            byte fnLen = buffer[0x40];
            return upperCase.compare(query, 0, query.length, buffer, 0x42, fnLen * 2);
        }

        @Override public String toString() {
            return new String(query, StandardCharsets.UTF_16LE) + ": " + upperCase;
        }
    }
}
