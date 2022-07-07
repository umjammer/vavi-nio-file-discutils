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
    private Stream _dirStream;

    private Directory _parent;

    private long _parentId;

    private long _endOfEntries;

    private Map<Long, DirectoryEntry> _entries;

    private List<Long> _freeEntries;

    private DirectoryEntry _parentEntry;

    private long _parentEntryLocation;

    private DirectoryEntry _selfEntry;

    private long _selfEntryLocation;

    /**
     * Initializes a new instance of the Directory class. Use this constructor
     * to represent non-root directories.
     *
     * @param parent The parent directory.
     * @param parentId The identity of the entry representing this directory in
     *            the parent.
     */
    Directory(Directory parent, long parentId) {
        _fileSystem = parent._fileSystem;
        _parent = parent;
        _parentId = parentId;
        DirectoryEntry dirEntry = getParentsChildEntry();
        _dirStream = new ClusterStream(_fileSystem, FileAccess.ReadWrite, dirEntry.getFirstCluster(), 0xffffffff);
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
        _fileSystem = fileSystem;
        _dirStream = dirStream;
        loadEntries();
    }

    public List<DirectoryEntry> getEntries() {
        return new ArrayList<>(_entries.values());
    }

    private FatFileSystem _fileSystem;

    public FatFileSystem getFileSystem() {
        return _fileSystem;
    }

    public boolean isEmpty() {
        return _entries.size() == 0;
    }

    public List<DirectoryEntry> getDirectories() {
        List<DirectoryEntry> dirs = new ArrayList<>(_entries.size());
        for (DirectoryEntry dirEntry : _entries.values()) {
            if (dirEntry.getAttributes().contains(FatAttributes.Directory)) {
                dirs.add(dirEntry);
            }

        }
        return dirs;
    }

    public List<DirectoryEntry> getFiles() {
        List<DirectoryEntry> files = new ArrayList<>(_entries.size());
        for (DirectoryEntry dirEntry : _entries.values()) {
            if (Collections.disjoint(dirEntry.getAttributes(), EnumSet.of(FatAttributes.Directory, FatAttributes.VolumeId))) {
                files.add(dirEntry);
            }
        }
        return files;
    }

    public DirectoryEntry getEntry(long id) {
        return id < 0 ? null : _entries.get(id);
    }

    public Directory getChildDirectory(FileName name) {
        long id = findEntry(name);
        if (id < 0) {
            return null;
        }

        if (!_entries.get(id).getAttributes().contains(FatAttributes.Directory)) {
            return null;
        }

        return _fileSystem.getDirectory(this, id);
    }

    Directory createChildDirectory(FileName name) {
        long id = findEntry(name);
        if (id >= 0) {
            if (!_entries.get(id).getAttributes().contains(FatAttributes.Directory)) {
                throw new dotnet4j.io.IOException("A file exists with the same name: " +
                                                  name.getDisplayName(Charset.forName(System.getProperty("file.encoding"))));
            }

            return _fileSystem.getDirectory(this, id);
        }

        try {
            int[] firstCluster = new int[1];
            boolean result = !_fileSystem.getFat().tryGetFreeCluster(firstCluster);
            if (result) {
                throw new dotnet4j.io.IOException("Failed to allocate first cluster for new directory");
            }

            _fileSystem.getFat().setEndOfChain(firstCluster[0]);
            DirectoryEntry newEntry = new DirectoryEntry(_fileSystem.getFatOptions(),
                                                         name,
                                                         EnumSet.of(FatAttributes.Directory),
                                                         _fileSystem.getFatVariant());
            newEntry.setFirstCluster(firstCluster[0]);
            newEntry.setCreationTime(_fileSystem.convertFromUtc(System.currentTimeMillis()));
            newEntry.setLastWriteTime(newEntry.getCreationTime());
            id = addEntry(newEntry);
            populateNewChildDirectory(newEntry);
            return _fileSystem.getDirectory(this, id);
        } finally {
            // Rather than just creating a new instance, pull it through the
            // fileSystem cache
            // to ensure the cache model is preserved.
            _fileSystem.getFat().flush();
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
        for (long id : _entries.keySet()) {
            DirectoryEntry focus = _entries.get(id);
            if (focus.getAttributes().contains(FatAttributes.VolumeId)) {
                return id;
            }
        }

        return -1;
    }

    long findEntry(FileName name) {
        for (long id : _entries.keySet()) {
            DirectoryEntry focus = _entries.get(id);
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
                                            name.getDisplayName(_fileSystem.getFatOptions().getFileNameEncoding()));
        }

        if ((mode == FileMode.Open || mode == FileMode.OpenOrCreate || mode == FileMode.Create) && exists) {
            SparseStream stream = new FatFileStream(_fileSystem, this, fileId, fileAccess);
            if (mode == FileMode.Create) {
                stream.setLength(0);
            }

            handleAccessed(false);
            return stream;
        }

        if ((mode == FileMode.OpenOrCreate || mode == FileMode.CreateNew || mode == FileMode.Create) && !exists) {
            // Create new file
            DirectoryEntry newEntry = new DirectoryEntry(_fileSystem.getFatOptions(),
                                                         name,
                                                         EnumSet.of(FatAttributes.Archive),
                                                         _fileSystem.getFatVariant());
            newEntry.setFirstCluster(0);
            // i.e. Zero-length
            newEntry.setCreationTime(_fileSystem.convertFromUtc(System.currentTimeMillis()));
            newEntry.setLastWriteTime(newEntry.getCreationTime());
            fileId = addEntry(newEntry);
            return new FatFileStream(_fileSystem, this, fileId, fileAccess);
        }

        throw new UnsupportedOperationException();
    }

    // Should never get here...
    long addEntry(DirectoryEntry newEntry) {
        // Unlink an entry from the free list (or add to the end of the existing
        // directory)
        long pos;
        if (_freeEntries.size() > 0) {
            pos = _freeEntries.get(0);
            _freeEntries.remove(0);
        } else {
            pos = _endOfEntries;
            _endOfEntries += 32;
        }
        // Put the new entry into it's slot
        _dirStream.setPosition(pos);
        newEntry.writeTo(_dirStream);
        // Update internal structures to reflect new entry (as if read from
        // disk)
        _entries.put(pos, newEntry);
        handleAccessed(true);
        return pos;
    }

    void deleteEntry(long id, boolean releaseContents) {
        if (id < 0) {
            throw new dotnet4j.io.IOException("Attempt to delete unknown directory entry");
        }

        try {
            DirectoryEntry entry = _entries.get(id);
            DirectoryEntry copy = new DirectoryEntry(entry);
            copy.setName(entry.getName().deleted());
            _dirStream.setPosition(id);
            copy.writeTo(_dirStream);
            if (releaseContents) {
                _fileSystem.getFat().freeChain(entry.getFirstCluster());
            }

            _entries.remove(id);
            _freeEntries.add(id);
            handleAccessed(true);
        } finally {
            _fileSystem.getFat().flush();
        }
    }

    void updateEntry(long id, DirectoryEntry entry) {
        if (id < 0) {
            throw new dotnet4j.io.IOException("Attempt to update unknown directory entry");
        }

        _dirStream.setPosition(id);
        entry.writeTo(_dirStream);
        _entries.put(id, entry);
    }

    private void loadEntries() {
        _entries = new HashMap<>();
        _freeEntries = new ArrayList<>();
        _selfEntryLocation = -1;
        _parentEntryLocation = -1;
        while (_dirStream.getPosition() < _dirStream.getLength()) {
            long streamPos = _dirStream.getPosition();
            DirectoryEntry entry = new DirectoryEntry(_fileSystem.getFatOptions(),
                                                      _dirStream,
                                                      _fileSystem.getFatVariant());
            if (entry.getAttributes()
                    .containsAll(EnumSet
                            .of(FatAttributes.ReadOnly, FatAttributes.Hidden, FatAttributes.System, FatAttributes.VolumeId))) {
                // Long File Name entry
            } else if (entry.getName().isDeleted()) {
                // E5 = Free Entry
                _freeEntries.add(streamPos);
            } else if (entry.getName().equals(FileName.SelfEntryName)) {
                _selfEntry = entry;
                _selfEntryLocation = streamPos;
            } else if (entry.getName().equals(FileName.ParentEntryName)) {
                _parentEntry = entry;
                _parentEntryLocation = streamPos;
            } else if (entry.getName().isEndMarker()) {
                // Free Entry, no more entries available
                _endOfEntries = streamPos;
                break;
            } else {
                _entries.put(streamPos, entry);
            }
        }
    }

    private void handleAccessed(boolean forWrite) {
        if (_fileSystem.canWrite() && _parent != null) {
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
        try (ClusterStream stream = new ClusterStream(_fileSystem,
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
        _dirStream.close();
    }

    DirectoryEntry getParentsChildEntry() {
        if (_parent == null) {
            return new DirectoryEntry(_fileSystem.getFatOptions(),
                                      FileName.ParentEntryName,
                                      EnumSet.of(FatAttributes.Directory),
                                      _fileSystem.getFatVariant());
        }

        return _parent.getEntry(_parentId);
    }

    void setParentsChildEntry(DirectoryEntry value) {
        if (_parent != null) {
            _parent.updateEntry(_parentId, value);
        }

    }

    DirectoryEntry getSelfEntry() {
        if (_parent == null) {
            // If we're the root directory, simulate the parent entry with a
            // dummy record
            return new DirectoryEntry(_fileSystem.getFatOptions(),
                                      FileName.Null,
                                      EnumSet.of(FatAttributes.Directory),
                                      _fileSystem.getFatVariant());
        }

        return _selfEntry;
    }

    void setSelfEntry(DirectoryEntry value) {
        if (_selfEntryLocation >= 0) {
            _dirStream.setPosition(_selfEntryLocation);
            value.writeTo(_dirStream);
            _selfEntry = value;
        }
    }

    DirectoryEntry getParentEntry() {
        return _parentEntry;
    }

    void setParentEntry(DirectoryEntry value) {
        if (_parentEntryLocation < 0) {
            throw new dotnet4j.io.IOException("No parent entry on disk to update");
        }

        _dirStream.setPosition(_parentEntryLocation);
        value.writeTo(_dirStream);
        _parentEntry = value;
    }
}
