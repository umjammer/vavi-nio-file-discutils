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

package DiscUtils.Fat;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.SparseStream;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.Stream;


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
    public Directory(Directory parent, long parentId) {
        __FileSystem = parent.getFileSystem();
        _parent = parent;
        _parentId = parentId;
        DirectoryEntry dirEntry = getParentsChildEntry();
        _dirStream = new ClusterStream(getFileSystem(), FileAccess.ReadWrite, dirEntry.getFirstCluster(), Integer.MAX_VALUE);
        loadEntries();
    }

    /**
     * Initializes a new instance of the Directory class. Use this constructor
     * to represent the root directory.
     *
     * @param fileSystem The file system.
     * @param dirStream The stream containing the directory info.
     */
    public Directory(FatFileSystem fileSystem, Stream dirStream) {
        __FileSystem = fileSystem;
        _dirStream = dirStream;
        loadEntries();
    }

    public List<DirectoryEntry> getEntries() {
        return new ArrayList<>(_entries.values());
    }

    private FatFileSystem __FileSystem;

    public FatFileSystem getFileSystem() {
        return __FileSystem;
    }

    public boolean getIsEmpty() {
        return _entries.size() == 0;
    }

    public void close() {
        System.gc();
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
            if (dirEntry.getAttributes().containsAll(EnumSet.of(FatAttributes.Directory, FatAttributes.VolumeId))) {
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

        if (_entries.get(id).getAttributes().contains(FatAttributes.Directory)) {
            return null;
        }

        return getFileSystem().getDirectory(this, id);
    }

    public Directory createChildDirectory(FileName name) {
        long id = findEntry(name);
        if (id >= 0) {
            if (_entries.get(id).getAttributes().contains(FatAttributes.Directory)) {
                throw new moe.yo3explorer.dotnetio4j.IOException("A file exists with the same name");
            }

            return getFileSystem().getDirectory(this, id);
        }

        try {
            int[] firstCluster = new int[1];
            boolean result = !getFileSystem().getFat().tryGetFreeCluster(firstCluster);
            if (result) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Failed to allocate first cluster for new directory");
            }

            getFileSystem().getFat().setEndOfChain(firstCluster[0]);
            DirectoryEntry newEntry = new DirectoryEntry(getFileSystem().getFatOptions(),
                                                         name,
                                                         EnumSet.of(FatAttributes.Directory),
                                                         getFileSystem().getFatVariant());
            newEntry.setFirstCluster(firstCluster[0]);
            newEntry.setCreationTime(getFileSystem().convertFromUtc(System.currentTimeMillis()));
            newEntry.setLastWriteTime(newEntry.getCreationTime());
            id = addEntry(newEntry);
            populateNewChildDirectory(newEntry);
            return getFileSystem().getDirectory(this, id);
        } finally {
            // Rather than just creating a new instance, pull it through the fileSystem cache
            // to ensure the cache model is preserved.
            getFileSystem().getFat().flush();
        }
    }

    public void attachChildDirectory(FileName name, Directory newChild) {
        long id = findEntry(name);
        if (id >= 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Directory entry already exists");
        }

        DirectoryEntry newEntry = new DirectoryEntry(newChild.getParentsChildEntry());
        newEntry.setName(name);
        addEntry(newEntry);
        DirectoryEntry newParentEntry = new DirectoryEntry(getSelfEntry());
        newParentEntry.setName(FileName.ParentEntryName);
        newChild.setParentEntry(newParentEntry);
    }

    public long findVolumeId() {
        for (long id : _entries.keySet()) {
            DirectoryEntry focus = _entries.get(id);
            if (focus.getAttributes().contains(FatAttributes.VolumeId)) {
                return id;
            }

        }
        return -1;
    }

    public long findEntry(FileName name) {
        for (long id : _entries.keySet()) {
            DirectoryEntry focus = _entries.get(id);
            if (focus.getName() == name && focus.getAttributes().contains(FatAttributes.VolumeId)) {
                return id;
            }

        }
        return -1;
    }

    public SparseStream openFile(FileName name, FileMode mode, FileAccess fileAccess) {
        if (mode == FileMode.Append || mode == FileMode.Truncate) {
            throw new UnsupportedOperationException();
        }

        long fileId = findEntry(name);
        boolean exists = fileId != -1;
        if (mode == FileMode.CreateNew && exists) {
            throw new moe.yo3explorer.dotnetio4j.IOException("File already exists");
        }

        if (mode == FileMode.Open && !exists) {
            throw new FileNotFoundException("File not found " +
                                            name.getDisplayName(getFileSystem().getFatOptions().getFileNameEncoding()));
        }

        if ((mode == FileMode.Open || mode == FileMode.OpenOrCreate || mode == FileMode.Create) && exists) {
            SparseStream stream = new FatFileStream(getFileSystem(), this, fileId, fileAccess);
            if (mode == FileMode.Create) {
                stream.setLength(0);
            }

            handleAccessed(false);
            return stream;
        }

        if ((mode == FileMode.OpenOrCreate || mode == FileMode.CreateNew || mode == FileMode.Create) && !exists) {
            // Create new file
            DirectoryEntry newEntry = new DirectoryEntry(getFileSystem().getFatOptions(),
                                                         name,
                                                         EnumSet.of(FatAttributes.Archive),
                                                         getFileSystem().getFatVariant());
            newEntry.setFirstCluster(0);
            // i.e. Zero-length
            newEntry.setCreationTime(getFileSystem().convertFromUtc(System.currentTimeMillis()));
            newEntry.setLastWriteTime(newEntry.getCreationTime());
            fileId = addEntry(newEntry);
            return new FatFileStream(getFileSystem(), this, fileId, fileAccess);
        }

        throw new UnsupportedOperationException();
    }

    // Should never get here...
    public long addEntry(DirectoryEntry newEntry) {
        // Unlink an entry from the free list (or add to the end of the existing directory)
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
        // Update internal structures to reflect new entry (as if read from disk)
        _entries.put(pos, newEntry);
        handleAccessed(true);
        return pos;
    }

    public void deleteEntry(long id, boolean releaseContents) {
        if (id < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to delete unknown directory entry");
        }

        try {
            DirectoryEntry entry = _entries.get(id);
            DirectoryEntry copy = new DirectoryEntry(entry);
            copy.setName(entry.getName().deleted());
            _dirStream.setPosition(id);
            copy.writeTo(_dirStream);
            if (releaseContents) {
                getFileSystem().getFat().freeChain(entry.getFirstCluster());
            }

            _entries.remove(id);
            _freeEntries.add(id);
            handleAccessed(true);
        } finally {
            getFileSystem().getFat().flush();
        }
    }

    public void updateEntry(long id, DirectoryEntry entry) {
        if (id < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("Attempt to update unknown directory entry");
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
            DirectoryEntry entry = new DirectoryEntry(getFileSystem().getFatOptions(),
                                                      _dirStream,
                                                      getFileSystem().getFatVariant());
            if (entry.getAttributes()
                    .containsAll(EnumSet
                            .of(FatAttributes.ReadOnly, FatAttributes.Hidden, FatAttributes.System, FatAttributes.VolumeId))) {
            } else // Long File Name entry
            if (entry.getName().isDeleted()) {
                // E5 = Free Entry
                _freeEntries.add(streamPos);
            } else if (entry.getName() == FileName.SelfEntryName) {
                _selfEntry = entry;
                _selfEntryLocation = streamPos;
            } else if (entry.getName() == FileName.ParentEntryName) {
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
        if (getFileSystem().canWrite() && _parent != null) {
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
        // Populate new directory with initial (special) entries.  First one is easy, just change the name!
        ClusterStream stream = new ClusterStream(getFileSystem(),
                                                 FileAccess.Write,
                                                 newEntry.getFirstCluster(),
                                                 Integer.MAX_VALUE);
        try {
            {
                // First is the self-referencing entry...
                DirectoryEntry selfEntry = new DirectoryEntry(newEntry);
                selfEntry.setName(FileName.SelfEntryName);
                selfEntry.writeTo(stream);
                // Second is a clone of our self entry (i.e. parent) - though dates are odd...
                DirectoryEntry parentEntry = new DirectoryEntry(getSelfEntry());
                parentEntry.setName(FileName.ParentEntryName);
                parentEntry.setCreationTime(newEntry.getCreationTime());
                parentEntry.setLastWriteTime(newEntry.getLastWriteTime());
                parentEntry.writeTo(stream);
            }
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
        }
    }

    public void cloase() throws IOException {
        _dirStream.close();
    }

    public DirectoryEntry getParentsChildEntry() {
        if (_parent == null) {
            return new DirectoryEntry(getFileSystem().getFatOptions(),
                                      FileName.ParentEntryName,
                                      EnumSet.of(FatAttributes.Directory),
                                      getFileSystem().getFatVariant());
        }

        return _parent.getEntry(_parentId);
    }

    public void setParentsChildEntry(DirectoryEntry value) {
        if (_parent != null) {
            _parent.updateEntry(_parentId, value);
        }

    }

    public DirectoryEntry getSelfEntry() {
        if (_parent == null) {
            return new DirectoryEntry(getFileSystem().getFatOptions(),
                                      FileName.Null,
                                      EnumSet.of(FatAttributes.Directory),
                                      getFileSystem().getFatVariant());
        }

        return _selfEntry;
    }

    // If we're the root directory, simulate the parent entry with a dummy record
    public void setSelfEntry(DirectoryEntry value) {
        if (_selfEntryLocation >= 0) {
            _dirStream.setPosition(_selfEntryLocation);
            value.writeTo(_dirStream);
            _selfEntry = value;
        }
    }

    public DirectoryEntry getParentEntry() {
        return _parentEntry;
    }

    public void setParentEntry(DirectoryEntry value) {
        if (_parentEntryLocation < 0) {
            throw new moe.yo3explorer.dotnetio4j.IOException("No parent entry on disk to update");
        }

        _dirStream.setPosition(_parentEntryLocation);
        value.writeTo(_dirStream);
        _parentEntry = value;
    }
}
