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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.streams.SparseStream;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Stream;


public class Directory implements Closeable {

    private Stream dirStream;

    private Directory parent;

    private long parentId;

    private long endOfEntries;

    private Map<Long, DirectoryEntry> entries;

    private List<Long> freeEntries;

    private DirectoryEntry parentEntry;

    private long parentEntryLocation;

    private DirectoryEntry selfEntry;

    private long selfEntryLocation;

    /**
     * Initializes a new instance of the Directory class. Use this constructor
     * to represent non-root directories.
     *
     * @param parent The parent directory.
     * @param parentId The identity of the entry representing this directory in
     *            the parent.
     */
    Directory(Directory parent, long parentId) {
        fileSystem = parent.fileSystem;
        this.parent = parent;
        this.parentId = parentId;
        DirectoryEntry dirEntry = getParentsChildEntry();
        dirStream = new ClusterStream(fileSystem, FileAccess.ReadWrite, dirEntry.getFirstCluster(), 0xffffffff);
        loadEntries();
    }

    /**
     * Initializes a new instance of the Directory class. Use this constructor
     * to represent the root directory.
     *
     * @param fileSystem The file system.
     * @param dirStream The stream containing the directory info.
     */
    Directory(FatFileSystem fileSystem, Stream dirStream) {
        this.fileSystem = fileSystem;
        this.dirStream = dirStream;
        loadEntries();
    }

    public List<DirectoryEntry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    private FatFileSystem fileSystem;

    public FatFileSystem getFileSystem() {
        return fileSystem;
    }

    public boolean isEmpty() {
        return entries.size() == 0;
    }

    public List<DirectoryEntry> getDirectories() {
        List<DirectoryEntry> dirs = new ArrayList<>(entries.size());
        for (DirectoryEntry dirEntry : entries.values()) {
            if (dirEntry.getAttributes().contains(FatAttributes.Directory)) {
                dirs.add(dirEntry);
            }

        }
        return dirs;
    }

    public List<DirectoryEntry> getFiles() {
        List<DirectoryEntry> files = new ArrayList<>(entries.size());
        for (DirectoryEntry dirEntry : entries.values()) {
            if (Collections.disjoint(dirEntry.getAttributes(), EnumSet.of(FatAttributes.Directory, FatAttributes.VolumeId))) {
                files.add(dirEntry);
            }
        }
        return files;
    }

    public DirectoryEntry getEntry(long id) {
        return id < 0 ? null : entries.get(id);
    }

    public Directory getChildDirectory(FileName name) {
        long id = findEntry(name);
        if (id < 0) {
            return null;
        }

        if (!entries.get(id).getAttributes().contains(FatAttributes.Directory)) {
            return null;
        }

        return fileSystem.getDirectory(this, id);
    }

    Directory createChildDirectory(FileName name) {
        long id = findEntry(name);
        if (id >= 0) {
            if (!entries.get(id).getAttributes().contains(FatAttributes.Directory)) {
                throw new dotnet4j.io.IOException("A file exists with the same name: " +
                                                  name.getDisplayName(Charset.forName(System.getProperty("file.encoding"))));
            }

            return fileSystem.getDirectory(this, id);
        }

        try {
            int[] firstCluster = new int[1];
            boolean result = !fileSystem.getFat().tryGetFreeCluster(firstCluster);
            if (result) {
                throw new dotnet4j.io.IOException("Failed to allocate first cluster for new directory");
            }

            fileSystem.getFat().setEndOfChain(firstCluster[0]);
            DirectoryEntry newEntry = new DirectoryEntry(fileSystem.getFatOptions(),
                                                         name,
                                                         EnumSet.of(FatAttributes.Directory),
                                                         fileSystem.getFatVariant());
            newEntry.setFirstCluster(firstCluster[0]);
            newEntry.setCreationTime(fileSystem.convertFromUtc(System.currentTimeMillis()));
            newEntry.setLastWriteTime(newEntry.getCreationTime());
            id = addEntry(newEntry);
            populateNewChildDirectory(newEntry);
            return fileSystem.getDirectory(this, id);
        } finally {
            // Rather than just creating a new instance, pull it through the
            // fileSystem cache
            // to ensure the cache model is preserved.
            fileSystem.getFat().flush();
        }
    }

    void attachChildDirectory(FileName name, Directory newChild) {
        long id = findEntry(name);
        if (id >= 0) {
            throw new dotnet4j.io.IOException("Directory entry already exists");
        }

        DirectoryEntry newEntry = new DirectoryEntry(newChild.getParentsChildEntry());
        newEntry.setName(name);
        addEntry(newEntry);
        DirectoryEntry newParentEntry = new DirectoryEntry(getSelfEntry());
        newParentEntry.setName(FileName.ParentEntryName);
        newChild.setParentEntry(newParentEntry);
    }

    long findVolumeId() {
        for (long id : entries.keySet()) {
            DirectoryEntry focus = entries.get(id);
            if (focus.getAttributes().contains(FatAttributes.VolumeId)) {
                return id;
            }
        }

        return -1;
    }

    long findEntry(FileName name) {
        for (long id : entries.keySet()) {
            DirectoryEntry focus = entries.get(id);
            if (focus.getName().equals(name) && !focus.getAttributes().contains(FatAttributes.VolumeId)) {
                return id;
            }
        }

        return -1;
    }

    SparseStream openFile(FileName name, FileMode mode, FileAccess fileAccess) {
        if (mode == FileMode.Append || mode == FileMode.Truncate) {
            throw new UnsupportedOperationException();
        }

        long fileId = findEntry(name);
        boolean exists = fileId != -1;
        if (mode == FileMode.CreateNew && exists) {
            throw new dotnet4j.io.IOException("File already exists");
        }

        if (mode == FileMode.Open && !exists) {
            throw new FileNotFoundException("File not found " +
                                            name.getDisplayName(fileSystem.getFatOptions().getFileNameEncoding()));
        }

        if ((mode == FileMode.Open || mode == FileMode.OpenOrCreate || mode == FileMode.Create) && exists) {
            SparseStream stream = new FatFileStream(fileSystem, this, fileId, fileAccess);
            if (mode == FileMode.Create) {
                stream.setLength(0);
            }

            handleAccessed(false);
            return stream;
        }

        if ((mode == FileMode.OpenOrCreate || mode == FileMode.CreateNew || mode == FileMode.Create) && !exists) {
            // Create new file
            DirectoryEntry newEntry = new DirectoryEntry(fileSystem.getFatOptions(),
                                                         name,
                                                         EnumSet.of(FatAttributes.Archive),
                                                         fileSystem.getFatVariant());
            newEntry.setFirstCluster(0);
            // i.e. Zero-length
            newEntry.setCreationTime(fileSystem.convertFromUtc(System.currentTimeMillis()));
            newEntry.setLastWriteTime(newEntry.getCreationTime());
            fileId = addEntry(newEntry);
            return new FatFileStream(fileSystem, this, fileId, fileAccess);
        }

        throw new UnsupportedOperationException();
    }

    // Should never get here...
    long addEntry(DirectoryEntry newEntry) {
        // Unlink an entry from the free list (or add to the end of the existing
        // directory)
        long pos;
        if (freeEntries.size() > 0) {
            pos = freeEntries.get(0);
            freeEntries.remove(0);
        } else {
            pos = endOfEntries;
            endOfEntries += 32;
        }
        // Put the new entry into it's slot
        dirStream.setPosition(pos);
        newEntry.writeTo(dirStream);
        // Update internal structures to reflect new entry (as if read from
        // disk)
        entries.put(pos, newEntry);
        handleAccessed(true);
        return pos;
    }

    void deleteEntry(long id, boolean releaseContents) {
        if (id < 0) {
            throw new dotnet4j.io.IOException("Attempt to delete unknown directory entry");
        }

        try {
            DirectoryEntry entry = entries.get(id);
            DirectoryEntry copy = new DirectoryEntry(entry);
            copy.setName(entry.getName().deleted());
            dirStream.setPosition(id);
            copy.writeTo(dirStream);
            if (releaseContents) {
                fileSystem.getFat().freeChain(entry.getFirstCluster());
            }

            entries.remove(id);
            freeEntries.add(id);
            handleAccessed(true);
        } finally {
            fileSystem.getFat().flush();
        }
    }

    void updateEntry(long id, DirectoryEntry entry) {
        if (id < 0) {
            throw new dotnet4j.io.IOException("Attempt to update unknown directory entry");
        }

        dirStream.setPosition(id);
        entry.writeTo(dirStream);
        entries.put(id, entry);
    }

    private void loadEntries() {
        entries = new HashMap<>();
        freeEntries = new ArrayList<>();
        selfEntryLocation = -1;
        parentEntryLocation = -1;
        while (dirStream.getPosition() < dirStream.getLength()) {
            long streamPos = dirStream.getPosition();
            DirectoryEntry entry = new DirectoryEntry(fileSystem.getFatOptions(),
                    dirStream,
                                                      fileSystem.getFatVariant());
            if (entry.getAttributes()
                    .containsAll(EnumSet
                            .of(FatAttributes.ReadOnly, FatAttributes.Hidden, FatAttributes.System, FatAttributes.VolumeId))) {
                // Long File Name entry
            } else if (entry.getName().isDeleted()) {
                // E5 = Free Entry
                freeEntries.add(streamPos);
            } else if (entry.getName().equals(FileName.SelfEntryName)) {
                selfEntry = entry;
                selfEntryLocation = streamPos;
            } else if (entry.getName().equals(FileName.ParentEntryName)) {
                parentEntry = entry;
                parentEntryLocation = streamPos;
            } else if (entry.getName().isEndMarker()) {
                // Free Entry, no more entries available
                endOfEntries = streamPos;
                break;
            } else {
                entries.put(streamPos, entry);
            }
        }
    }

    private void handleAccessed(boolean forWrite) {
        if (fileSystem.canWrite() && parent != null) {
            long now = System.currentTimeMillis();
            DirectoryEntry entry = getSelfEntry();
            long oldAccessTime = entry.getLastAccessTime();
            long oldWriteTime = entry.getLastWriteTime();
            entry.setLastAccessTime(now);
            if (forWrite) {
                entry.setLastWriteTime(now);
            }

            if (entry.getLastAccessTime() != oldAccessTime || entry.getLastWriteTime() != oldWriteTime) {
                setSelfEntry(entry);
                DirectoryEntry parentEntry = getParentsChildEntry();
                parentEntry.setLastAccessTime(entry.getLastAccessTime());
                parentEntry.setLastWriteTime(entry.getLastWriteTime());
                setParentsChildEntry(parentEntry);
            }
        }
    }

    private void populateNewChildDirectory(DirectoryEntry newEntry) {
        // Populate new directory with initial (special) entries. First one is
        // easy, just change the name!
        try (ClusterStream stream = new ClusterStream(fileSystem,
                                                      FileAccess.Write,
                                                      newEntry.getFirstCluster(),
                                                      0xffffffff)) {
            // First is the self-referencing entry...
            DirectoryEntry selfEntry = new DirectoryEntry(newEntry);
            selfEntry.setName(FileName.SelfEntryName);
            selfEntry.writeTo(stream);
            // Second is a clone of our self entry (i.e. parent) - though dates
            // are odd...
            DirectoryEntry parentEntry = new DirectoryEntry(getSelfEntry());
            parentEntry.setName(FileName.ParentEntryName);
            parentEntry.setCreationTime(newEntry.getCreationTime());
            parentEntry.setLastWriteTime(newEntry.getLastWriteTime());
            parentEntry.writeTo(stream);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    public void close() throws IOException {
        dirStream.close();
    }

    DirectoryEntry getParentsChildEntry() {
        if (parent == null) {
            return new DirectoryEntry(fileSystem.getFatOptions(),
                                      FileName.ParentEntryName,
                                      EnumSet.of(FatAttributes.Directory),
                                      fileSystem.getFatVariant());
        }

        return parent.getEntry(parentId);
    }

    void setParentsChildEntry(DirectoryEntry value) {
        if (parent != null) {
            parent.updateEntry(parentId, value);
        }
    }

    DirectoryEntry getSelfEntry() {
        if (parent == null) {
            // If we're the root directory, simulate the parent entry with a
            // dummy record
            return new DirectoryEntry(fileSystem.getFatOptions(),
                                      FileName.Null,
                                      EnumSet.of(FatAttributes.Directory),
                                      fileSystem.getFatVariant());
        }

        return selfEntry;
    }

    void setSelfEntry(DirectoryEntry value) {
        if (selfEntryLocation >= 0) {
            dirStream.setPosition(selfEntryLocation);
            value.writeTo(dirStream);
            selfEntry = value;
        }
    }

    DirectoryEntry getParentEntry() {
        return parentEntry;
    }

    void setParentEntry(DirectoryEntry value) {
        if (parentEntryLocation < 0) {
            throw new dotnet4j.io.IOException("No parent entry on disk to update");
        }

        dirStream.setPosition(parentEntryLocation);
        value.writeTo(dirStream);
        parentEntry = value;
    }
}
