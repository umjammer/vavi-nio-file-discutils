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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import vavi.util.Debug;

import DiscUtils.Streams.Util.MathUtilities;
import dotnet4j.io.IOException;


class IndexNode {
    private final List<IndexEntry> _entries;

    private final Index _index;

    private final boolean _isRoot;

    private final int _storageOverhead;

    private final IndexNodeSaveFn _store;

    public IndexNode(IndexNodeSaveFn store, int storeOverhead, Index index, boolean isRoot, int allocatedSize) {
        _store = store;
        _storageOverhead = storeOverhead;
        _index = index;
        _isRoot = isRoot;
        _header = new IndexHeader(allocatedSize);
        _totalSpaceAvailable = allocatedSize;

        IndexEntry endEntry = new IndexEntry(_index.isFileIndex());
        endEntry.getFlags().add(IndexEntryFlags.End);

        _entries = new ArrayList<>();
        _entries.add(endEntry);

        _header._offsetToFirstEntry = IndexHeader.Size + storeOverhead;
        _header._totalSizeOfEntries = _header._offsetToFirstEntry + endEntry.getSize();
    }

    public IndexNode(IndexNodeSaveFn store, int storeOverhead, Index index, boolean isRoot, byte[] buffer, int offset) {
        _store = store;
        _storageOverhead = storeOverhead;
        _index = index;
        _isRoot = isRoot;
        _header = new IndexHeader(buffer, offset + 0);
        setTotalSpaceAvailable(_header._allocatedSizeOfEntries);

        _entries = new ArrayList<>();
        int pos = _header._offsetToFirstEntry;
        while (pos < _header._totalSizeOfEntries) {
            IndexEntry entry = new IndexEntry(index.isFileIndex());
            entry.read(buffer, offset + pos);
            _entries.add(entry);

            if (entry.getFlags().contains(IndexEntryFlags.End)) {
                break;
            }

            pos += entry.getSize();
        }
    }

    public List<IndexEntry> getEntries() {
        return _entries;
    }

    private IndexHeader _header;

    public IndexHeader getHeader() {
        return _header;
    }

    private long getSpaceFree() {
        long entriesTotal = 0;
        for (int i = 0; i < _entries.size(); ++i) {
            entriesTotal += _entries.get(i).getSize();
        }
        int firstEntryOffset = MathUtilities.roundUp(IndexHeader.Size + _storageOverhead, 8);
        return _totalSpaceAvailable - (entriesTotal + firstEntryOffset);
    }

    private long _totalSpaceAvailable;

    long getTotalSpaceAvailable() {
        return _totalSpaceAvailable;
    }

    void setTotalSpaceAvailable(long value) {
        _totalSpaceAvailable = value;
    }

    public void addEntry(byte[] key, byte[] data) {
        IndexEntry overflowEntry = addEntry(new IndexEntry(key, data, _index.isFileIndex()));
        if (overflowEntry != null) {
            throw new IOException("Error adding entry - root overflowed");
        }
    }

    public void updateEntry(byte[] key, byte[] data) {
        for (int i = 0; i < _entries.size(); ++i) {
            IndexEntry focus = _entries.get(i);
            int compVal = _index.compare(key, focus.getKeyBuffer());
            if (compVal == 0) {
                IndexEntry newEntry = new IndexEntry(focus, key, data);
                if (_entries.get(i).getSize() != newEntry.getSize()) {
                    throw new UnsupportedOperationException("Changing index entry sizes");
                }

                _entries.set(i, newEntry);
                _store.invoke();
                return;
            }
        }
        throw new IOException("No such index entry");
    }

    /**
     * @param entry {@cs out}
     * @param node {@cs out}
     */
    public boolean tryFindEntry(byte[] key, IndexEntry[] entry, IndexNode[] node) {
        for (IndexEntry focus : _entries) {
            if (focus.getFlags().contains(IndexEntryFlags.End)) {
                if (focus.getFlags().contains(IndexEntryFlags.Node)) {
                    IndexBlock subNode = _index.getSubBlock(focus);
                    return subNode.getNode().tryFindEntry(key, entry, node);
                }

                break;
            }
            int compVal = _index.compare(key, focus.getKeyBuffer());
            if (compVal == 0) {
                entry[0] = focus;
                node[0] = this;
                return true;
            }
            if (compVal < 0 && !Collections.disjoint(focus.getFlags(), EnumSet.of(IndexEntryFlags.End, IndexEntryFlags.Node))) {
                IndexBlock subNode = _index.getSubBlock(focus);
                return subNode.getNode().tryFindEntry(key, entry, node);
            }
        }

        entry[0] = null;
        node[0] = null;
        return false;
    }

    public short writeTo(byte[] buffer, int offset) {
        boolean haveSubNodes = false;
        int totalEntriesSize = 0;
        for (IndexEntry entry : _entries) {
            totalEntriesSize += entry.getSize();
            haveSubNodes |= entry.getFlags().contains(IndexEntryFlags.Node);
        }

        _header._offsetToFirstEntry = MathUtilities.roundUp(IndexHeader.Size + _storageOverhead, 8);
        _header._totalSizeOfEntries = totalEntriesSize + _header._offsetToFirstEntry;
        _header._hasChildNodes = (byte) (haveSubNodes ? 1 : 0);
        _header.writeTo(buffer, offset + 0);

        int pos = _header._offsetToFirstEntry;
        for (IndexEntry entry : _entries) {
            entry.writeTo(buffer, offset + pos);
            pos += entry.getSize();
        }
        return IndexHeader.Size;
    }

    public int calcEntriesSize() {
        int totalEntriesSize = 0;
        for (IndexEntry entry : _entries) {
            totalEntriesSize += entry.getSize();
        }
        return totalEntriesSize;
    }

    public int calcSize() {
        int firstEntryOffset = MathUtilities.roundUp(IndexHeader.Size + _storageOverhead, 8);
        return firstEntryOffset + calcEntriesSize();
    }

    /**
     * @param exactMatch {@cs out}
     */
    public int getEntry(byte[] key, boolean[] exactMatch) {
        for (int i = 0; i < _entries.size(); ++i) {
            IndexEntry focus = _entries.get(i);
            int compVal;
            if (focus.getFlags().contains(IndexEntryFlags.End)) {
                exactMatch[0] = false;
                return i;
            }

            compVal = _index.compare(key, focus.getKeyBuffer());
            if (compVal <= 0) {
                exactMatch[0] = compVal == 0;
                return i;
            }
        }
        throw new IOException("Corrupt index node - no End entry");
    }

    /**
     * @param newParentEntry {@cs out}
     */
    public boolean removeEntry(byte[] key, IndexEntry[] newParentEntry) {
        boolean[] exactMatch = new boolean[1];
        int entryIndex = getEntry(key, exactMatch);
        IndexEntry entry = _entries.get(entryIndex);

        if (exactMatch[0]) {
            if (entry.getFlags().contains(IndexEntryFlags.Node)) {
                IndexNode childNode = _index.getSubBlock(entry).getNode();
                IndexEntry rLeaf = childNode.findLargestLeaf();

                byte[] newKey = rLeaf.getKeyBuffer();
                byte[] newData = rLeaf.getDataBuffer();

                IndexEntry[] newEntry = new IndexEntry[1];
                childNode.removeEntry(newKey, newEntry);
                entry.setKeyBuffer(newKey);
                entry.setDataBuffer(newData);

                if (newEntry[0] != null) {
                    insertEntryThisNode(newEntry[0]);
                }

                newEntry[0] = liftNode(entryIndex);
                if (newEntry[0] != null) {
                    insertEntryThisNode(newEntry[0]);
                }

                newEntry[0] = populateEnd();
                if (newEntry[0] != null) {
                    insertEntryThisNode(newEntry[0]);
                }

                // New entry could be larger than old, so may need
                // to divide this node...
                newParentEntry[0] = ensureNodeSize();
            } else {
                _entries.remove(entryIndex);
                newParentEntry[0] = null;
            }

            _store.invoke();
            return true;
        }
        if (entry.getFlags().contains(IndexEntryFlags.Node)) {
            IndexNode childNode = _index.getSubBlock(entry).getNode();
            IndexEntry[] newEntry = new IndexEntry[1];
            boolean r = childNode.removeEntry(key, newEntry);
            if (r) {
                if (newEntry[0] != null) {
                    insertEntryThisNode(newEntry[0]);
                }

                newEntry[0] = liftNode(entryIndex);
                if (newEntry[0] != null) {
                    insertEntryThisNode(newEntry[0]);
                }

                newEntry[0] = populateEnd();
                if (newEntry[0] != null) {
                    insertEntryThisNode(newEntry[0]);
                }

                // New entry could be larger than old, so may need
                // to divide this node...
                newParentEntry[0] = ensureNodeSize();

                _store.invoke();
                return true;
            }
        }

        newParentEntry[0] = null;
        return false;
    }

    /**
     * Only valid on the root node, this method moves all entries into a single
     * child node.
     *
     * @return Whether any changes were made.
     */
    boolean depose() {
        if (!_isRoot) {
            throw new UnsupportedOperationException("Only valid on root node");
        }

        if (_entries.size() == 1) {
            return false;
        }

        IndexEntry newRootEntry = new IndexEntry(_index.isFileIndex());
        newRootEntry.getFlags().add(IndexEntryFlags.End);

        IndexBlock newBlock = _index.allocateBlock(newRootEntry);

        // Set the deposed entries into the new node. Note we updated the parent
        // pointers first, because it's possible SetEntries may need to further
        // divide the entries to fit into nodes. We mustn't overwrite any changes.
        newBlock.getNode().setEntries(_entries, 0, _entries.size());

        _entries.clear();
        _entries.add(newRootEntry);

        return true;
    }

    /**
     * Removes redundant nodes (that contain only an 'End' entry).
     *
     * @param entryIndex The index of the entry that may have a redundant child.
     * @return An entry that needs to be promoted to the parent node (if any).
     */
    private IndexEntry liftNode(int entryIndex) {
        if (_entries.get(entryIndex).getFlags().contains(IndexEntryFlags.Node)) {
            IndexNode childNode = _index.getSubBlock(_entries.get(entryIndex)).getNode();
            if (childNode._entries.size() == 1) {
                long freeBlock = _entries.get(entryIndex).getChildrenVirtualCluster();
                _entries.get(entryIndex).getFlags().remove(IndexEntryFlags.Node);
                if (childNode._entries.get(0).getFlags().contains(IndexEntryFlags.Node)) {
                    _entries.get(entryIndex).getFlags().add(IndexEntryFlags.Node);
                }
                _entries.get(entryIndex).setChildrenVirtualCluster(childNode._entries.get(0).getChildrenVirtualCluster());

                _index.freeBlock(freeBlock);
            }

            if (Collections.disjoint(_entries.get(entryIndex).getFlags(), EnumSet.of(IndexEntryFlags.Node, IndexEntryFlags.End))) {
                IndexEntry entry = _entries.get(entryIndex);
                _entries.remove(entryIndex);

                IndexNode nextNode = _index.getSubBlock(_entries.get(entryIndex)).getNode();
                return nextNode.addEntry(entry);
            }
        }

        return null;
    }

    private IndexEntry populateEnd() {
        if (_entries.size() > 1 && _entries.get(_entries.size() - 1).getFlags().equals(EnumSet.of(IndexEntryFlags.End)) &&
            _entries.get(_entries.size() - 2).getFlags().contains(IndexEntryFlags.Node)) {
            IndexEntry old = _entries.get(_entries.size() - 2);
            _entries.remove(_entries.size() - 2);
            _entries.get(_entries.size() - 1).setChildrenVirtualCluster(old.getChildrenVirtualCluster());
            _entries.get(_entries.size() - 1).getFlags().add(IndexEntryFlags.Node);
            old.setChildrenVirtualCluster(0);
            old.setFlags(EnumSet.noneOf(IndexEntryFlags.class));
            return _index.getSubBlock(_entries.get(_entries.size() - 1)).getNode().addEntry(old);
        }

        return null;
    }

    private void insertEntryThisNode(IndexEntry newEntry) {
        boolean[] exactMatch = new boolean[1];
        int index = getEntry(newEntry.getKeyBuffer(), exactMatch);
        if (exactMatch[0]) {
            throw new IOException("Entry already exists");
        }

        _entries.add(index, newEntry);
    }

    private IndexEntry addEntry(IndexEntry newEntry) {
        boolean[] exactMatch = new boolean[1];
        int index = getEntry(newEntry.getKeyBuffer(), exactMatch);

        if (exactMatch[0]) {
Debug.println(newEntry + " / " + _entries);
            throw new IOException("Entry already exists");
        }

        if (_entries.get(index).getFlags().contains(IndexEntryFlags.Node)) {
            IndexEntry ourNewEntry = _index.getSubBlock(_entries.get(index)).getNode().addEntry(newEntry);
            if (ourNewEntry == null) {
                // No change to this node
                return null;
            }

            insertEntryThisNode(ourNewEntry);
        } else {
            _entries.add(index, newEntry);
        }

        // If there wasn't enough space, we may need to
        // divide this node
        IndexEntry newParentEntry = ensureNodeSize();

        _store.invoke();

        return newParentEntry;
    }

    private IndexEntry ensureNodeSize() {
        // While the node is too small to hold the entries, we need to reduce
        // the number of entries.
        if (getSpaceFree() < 0) {
            if (_isRoot) {
                depose();
            } else {
                return divide();
            }
        }

        return null;
    }

    /**
     * Finds the largest leaf entry in this tree.
     *
     * @return The index entry of the largest leaf.
     */
    private IndexEntry findLargestLeaf() {
        if (_entries.get(_entries.size() - 1).getFlags().contains(IndexEntryFlags.Node)) {
            return _index.getSubBlock(_entries.get(_entries.size() - 1)).getNode().findLargestLeaf();
        }

        if (_entries.size() > 1 && !_entries.get(_entries.size() - 2).getFlags().contains(IndexEntryFlags.Node)) {
            return _entries.get(_entries.size() - 2);
        }

        throw new IOException("Invalid index node found");
    }

    /**
     * Only valid on non-root nodes, this method divides the node in two, adding the
     * new node to the current parent.
     *
     * @return An entry that needs to be promoted to the parent node (if any).
     */
    private IndexEntry divide() {
        int midEntryIdx = _entries.size() / 2;
        IndexEntry midEntry = _entries.get(midEntryIdx);

        // The terminating entry (aka end) for the new node
        IndexEntry newTerm = new IndexEntry(_index.isFileIndex());
        newTerm.getFlags().add(IndexEntryFlags.End);

        // The set of entries in the new node
        List<IndexEntry> newEntries = new ArrayList<>(midEntryIdx + 1);
        for (int i = 0; i < midEntryIdx; ++i) {
            newEntries.add(_entries.get(i));
        }

        newEntries.add(newTerm);

        // Copy the node pointer from the elected 'mid' entry to the new node
        if (midEntry.getFlags().contains(IndexEntryFlags.Node)) {
            newTerm.setChildrenVirtualCluster(midEntry.getChildrenVirtualCluster());
            newTerm.getFlags().add(IndexEntryFlags.Node);
        }

        // Set the new entries into the new node
        IndexBlock newBlock = _index.allocateBlock(midEntry);

        // Set the entries into the new node. Note we updated the parent
        // pointers first, because it's possible SetEntries may need to further
        // divide the entries to fit into nodes. We mustn't overwrite any changes.
        newBlock.getNode().setEntries(newEntries, 0, newEntries.size());

        // Forget about the entries moved into the new node, and the entry about
        // to be promoted as the new node's pointer
        _entries.subList(0, midEntryIdx + 1).clear();

        // Promote the old mid entry
        return midEntry;
    }

    private void setEntries(List<IndexEntry> newEntries, int offset, int count) {
        _entries.clear();
        for (int i = 0; i < count; ++i) {
            _entries.add(newEntries.get(i + offset));
        }

        // Add an end entry, if not present
        if (count == 0 || !_entries.get(_entries.size() - 1).getFlags().contains(IndexEntryFlags.End)) {
            IndexEntry end = new IndexEntry(_index.isFileIndex());
            end.getFlags().add(IndexEntryFlags.End);
            _entries.add(end);
        }

        // Ensure the node isn't over-filled
        if (getSpaceFree() < 0) {
            throw new IOException("Error setting node entries - oversized for node");
        }

        // Persist the new entries to disk
        _store.invoke();
    }
}
