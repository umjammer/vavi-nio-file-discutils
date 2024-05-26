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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import discUtils.streams.util.MathUtilities;
import dotnet4j.io.IOException;

import static java.lang.System.getLogger;


class IndexNode {

    private static final Logger logger = getLogger(IndexNode.class.getName());

    private final List<IndexEntry> entries;

    private final Index index;

    private final boolean isRoot;

    private final int storageOverhead;

    private final IndexNodeSaveFn store;

    public IndexNode(IndexNodeSaveFn store, int storeOverhead, Index index, boolean isRoot, int allocatedSize) {
        this.store = store;
        storageOverhead = storeOverhead;
        this.index = index;
        this.isRoot = isRoot;
        header = new IndexHeader(allocatedSize);
        totalSpaceAvailable = allocatedSize;

        IndexEntry endEntry = new IndexEntry(this.index.isFileIndex());
        endEntry.getFlags().add(IndexEntryFlags.End);

        entries = new ArrayList<>();
        entries.add(endEntry);

        header.offsetToFirstEntry = IndexHeader.Size + storeOverhead;
        header.totalSizeOfEntries = header.offsetToFirstEntry + endEntry.getSize();
    }

    public IndexNode(IndexNodeSaveFn store, int storeOverhead, Index index, boolean isRoot, byte[] buffer, int offset) {
        this.store = store;
        storageOverhead = storeOverhead;
        this.index = index;
        this.isRoot = isRoot;
        header = new IndexHeader(buffer, offset + 0);
        setTotalSpaceAvailable(header.allocatedSizeOfEntries);

        entries = new ArrayList<>();
        int pos = header.offsetToFirstEntry;
        while (pos < header.totalSizeOfEntries) {
            IndexEntry entry = new IndexEntry(index.isFileIndex());
            entry.read(buffer, offset + pos);
            entries.add(entry);

            if (entry.getFlags().contains(IndexEntryFlags.End)) {
                break;
            }

            pos += entry.getSize();
        }
    }

    public List<IndexEntry> getEntries() {
        return entries;
    }

    private IndexHeader header;

    public IndexHeader getHeader() {
        return header;
    }

    private long getSpaceFree() {
        long entriesTotal = 0;
        for (IndexEntry entry : entries) {
            entriesTotal += entry.getSize();
        }
        int firstEntryOffset = MathUtilities.roundUp(IndexHeader.Size + storageOverhead, 8);
        return totalSpaceAvailable - (entriesTotal + firstEntryOffset);
    }

    private long totalSpaceAvailable;

    long getTotalSpaceAvailable() {
        return totalSpaceAvailable;
    }

    void setTotalSpaceAvailable(long value) {
        totalSpaceAvailable = value;
    }

    public void addEntry(byte[] key, byte[] data) {
        IndexEntry overflowEntry = addEntry(new IndexEntry(key, data, index.isFileIndex()));
        if (overflowEntry != null) {
            throw new IOException("Error adding entry - root overflowed");
        }
    }

    public void updateEntry(byte[] key, byte[] data) {
        for (int i = 0; i < entries.size(); ++i) {
            IndexEntry focus = entries.get(i);
            int compVal = index.compare(key, focus.getKeyBuffer());
            if (compVal == 0) {
                IndexEntry newEntry = new IndexEntry(focus, key, data);
                if (entries.get(i).getSize() != newEntry.getSize()) {
                    throw new UnsupportedOperationException("Changing index entry sizes");
                }

                entries.set(i, newEntry);
                store.invoke();
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
        for (IndexEntry focus : entries) {
            if (focus.getFlags().contains(IndexEntryFlags.End)) {
                if (focus.getFlags().contains(IndexEntryFlags.Node)) {
                    IndexBlock subNode = index.getSubBlock(focus);
                    return subNode.getNode().tryFindEntry(key, entry, node);
                }

                break;
            }
            int compVal = index.compare(key, focus.getKeyBuffer());
            if (compVal == 0) {
                entry[0] = focus;
                node[0] = this;
                return true;
            }
            if (compVal < 0 && !Collections.disjoint(focus.getFlags(), EnumSet.of(IndexEntryFlags.End, IndexEntryFlags.Node))) {
                IndexBlock subNode = index.getSubBlock(focus);
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
        for (IndexEntry entry : entries) {
            totalEntriesSize += entry.getSize();
            haveSubNodes |= entry.getFlags().contains(IndexEntryFlags.Node);
        }

        header.offsetToFirstEntry = MathUtilities.roundUp(IndexHeader.Size + storageOverhead, 8);
        header.totalSizeOfEntries = totalEntriesSize + header.offsetToFirstEntry;
        header.hasChildNodes = (byte) (haveSubNodes ? 1 : 0);
        header.writeTo(buffer, offset + 0);

        int pos = header.offsetToFirstEntry;
        for (IndexEntry entry : entries) {
            entry.writeTo(buffer, offset + pos);
            pos += entry.getSize();
        }
        return IndexHeader.Size;
    }

    public int calcEntriesSize() {
        int totalEntriesSize = 0;
        for (IndexEntry entry : entries) {
            totalEntriesSize += entry.getSize();
        }
        return totalEntriesSize;
    }

    public int calcSize() {
        int firstEntryOffset = MathUtilities.roundUp(IndexHeader.Size + storageOverhead, 8);
        return firstEntryOffset + calcEntriesSize();
    }

    /**
     * @param exactMatch {@cs out}
     */
    public int getEntry(byte[] key, boolean[] exactMatch) {
        for (int i = 0; i < entries.size(); ++i) {
            IndexEntry focus = entries.get(i);
            int compVal;
            if (focus.getFlags().contains(IndexEntryFlags.End)) {
                exactMatch[0] = false;
                return i;
            }

            compVal = index.compare(key, focus.getKeyBuffer());
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
        IndexEntry entry = entries.get(entryIndex);

        if (exactMatch[0]) {
            if (entry.getFlags().contains(IndexEntryFlags.Node)) {
                IndexNode childNode = index.getSubBlock(entry).getNode();
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
                entries.remove(entryIndex);
                newParentEntry[0] = null;
            }

            store.invoke();
            return true;
        }
        if (entry.getFlags().contains(IndexEntryFlags.Node)) {
            IndexNode childNode = index.getSubBlock(entry).getNode();
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

                store.invoke();
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
        if (!isRoot) {
            throw new UnsupportedOperationException("Only valid on root node");
        }

        if (entries.size() == 1) {
            return false;
        }

        IndexEntry newRootEntry = new IndexEntry(index.isFileIndex());
        newRootEntry.getFlags().add(IndexEntryFlags.End);

        IndexBlock newBlock = index.allocateBlock(newRootEntry);

        // Set the deposed entries into the new node. Note we updated the parent
        // pointers first, because it's possible SetEntries may need to further
        // divide the entries to fit into nodes. We mustn't overwrite any changes.
        newBlock.getNode().setEntries(entries, 0, entries.size());

        entries.clear();
        entries.add(newRootEntry);

        return true;
    }

    /**
     * Removes redundant nodes (that contain only an 'End' entry).
     *
     * @param entryIndex The index of the entry that may have a redundant child.
     * @return An entry that needs to be promoted to the parent node (if any).
     */
    private IndexEntry liftNode(int entryIndex) {
        if (entries.get(entryIndex).getFlags().contains(IndexEntryFlags.Node)) {
            IndexNode childNode = index.getSubBlock(entries.get(entryIndex)).getNode();
            if (childNode.entries.size() == 1) {
                long freeBlock = entries.get(entryIndex).getChildrenVirtualCluster();
                entries.get(entryIndex).getFlags().remove(IndexEntryFlags.Node);
                if (childNode.entries.get(0).getFlags().contains(IndexEntryFlags.Node)) {
                    entries.get(entryIndex).getFlags().add(IndexEntryFlags.Node);
                }
                entries.get(entryIndex).setChildrenVirtualCluster(childNode.entries.get(0).getChildrenVirtualCluster());

                index.freeBlock(freeBlock);
            }

            if (Collections.disjoint(entries.get(entryIndex).getFlags(), EnumSet.of(IndexEntryFlags.Node, IndexEntryFlags.End))) {
                IndexEntry entry = entries.get(entryIndex);
                entries.remove(entryIndex);

                IndexNode nextNode = index.getSubBlock(entries.get(entryIndex)).getNode();
                return nextNode.addEntry(entry);
            }
        }

        return null;
    }

    private IndexEntry populateEnd() {
        if (entries.size() > 1 && entries.get(entries.size() - 1).getFlags().equals(EnumSet.of(IndexEntryFlags.End)) &&
            entries.get(entries.size() - 2).getFlags().contains(IndexEntryFlags.Node)) {
            IndexEntry old = entries.get(entries.size() - 2);
            entries.remove(entries.size() - 2);
            entries.get(entries.size() - 1).setChildrenVirtualCluster(old.getChildrenVirtualCluster());
            entries.get(entries.size() - 1).getFlags().add(IndexEntryFlags.Node);
            old.setChildrenVirtualCluster(0);
            old.setFlags(EnumSet.noneOf(IndexEntryFlags.class));
            return index.getSubBlock(entries.get(entries.size() - 1)).getNode().addEntry(old);
        }

        return null;
    }

    private void insertEntryThisNode(IndexEntry newEntry) {
        boolean[] exactMatch = new boolean[1];
        int index = getEntry(newEntry.getKeyBuffer(), exactMatch);
        if (exactMatch[0]) {
            throw new IOException("Entry already exists");
        }

        entries.add(index, newEntry);
    }

    private IndexEntry addEntry(IndexEntry newEntry) {
        boolean[] exactMatch = new boolean[1];
        int index = getEntry(newEntry.getKeyBuffer(), exactMatch);

        if (exactMatch[0]) {
logger.log(Level.DEBUG, newEntry + " / " + entries);
            throw new IOException("Entry already exists");
        }

        if (entries.get(index).getFlags().contains(IndexEntryFlags.Node)) {
            IndexEntry ourNewEntry = this.index.getSubBlock(entries.get(index)).getNode().addEntry(newEntry);
            if (ourNewEntry == null) {
                // No change to this node
                return null;
            }

            insertEntryThisNode(ourNewEntry);
        } else {
            entries.add(index, newEntry);
        }

        // If there wasn't enough space, we may need to
        // divide this node
        IndexEntry newParentEntry = ensureNodeSize();

        store.invoke();

        return newParentEntry;
    }

    private IndexEntry ensureNodeSize() {
        // While the node is too small to hold the entries, we need to reduce
        // the number of entries.
        if (getSpaceFree() < 0) {
            if (isRoot) {
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
        if (entries.get(entries.size() - 1).getFlags().contains(IndexEntryFlags.Node)) {
            return index.getSubBlock(entries.get(entries.size() - 1)).getNode().findLargestLeaf();
        }

        if (entries.size() > 1 && !entries.get(entries.size() - 2).getFlags().contains(IndexEntryFlags.Node)) {
            return entries.get(entries.size() - 2);
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
        int midEntryIdx = entries.size() / 2;
        IndexEntry midEntry = entries.get(midEntryIdx);

        // The terminating entry (aka end) for the new node
        IndexEntry newTerm = new IndexEntry(index.isFileIndex());
        newTerm.getFlags().add(IndexEntryFlags.End);

        // The set of entries in the new node
        List<IndexEntry> newEntries = new ArrayList<>(midEntryIdx + 1);
        for (int i = 0; i < midEntryIdx; ++i) {
            newEntries.add(entries.get(i));
        }

        newEntries.add(newTerm);

        // Copy the node pointer from the elected 'mid' entry to the new node
        if (midEntry.getFlags().contains(IndexEntryFlags.Node)) {
            newTerm.setChildrenVirtualCluster(midEntry.getChildrenVirtualCluster());
            newTerm.getFlags().add(IndexEntryFlags.Node);
        }

        // Set the new entries into the new node
        IndexBlock newBlock = index.allocateBlock(midEntry);

        // Set the entries into the new node. Note we updated the parent
        // pointers first, because it's possible SetEntries may need to further
        // divide the entries to fit into nodes. We mustn't overwrite any changes.
        newBlock.getNode().setEntries(newEntries, 0, newEntries.size());

        // Forget about the entries moved into the new node, and the entry about
        // to be promoted as the new node's pointer
        entries.subList(0, midEntryIdx + 1).clear();

        // Promote the old mid entry
        return midEntry;
    }

    private void setEntries(List<IndexEntry> newEntries, int offset, int count) {
        entries.clear();
        for (int i = 0; i < count; ++i) {
            entries.add(newEntries.get(i + offset));
        }

        // Add an end entry, if not present
        if (count == 0 || !entries.get(entries.size() - 1).getFlags().contains(IndexEntryFlags.End)) {
            IndexEntry end = new IndexEntry(index.isFileIndex());
            end.getFlags().add(IndexEntryFlags.End);
            entries.add(end);
        }

        // Ensure the node isn't over-filled
        if (getSpaceFree() < 0) {
            throw new IOException("Error setting node entries - oversized for node");
        }

        // Persist the new entries to disk
        store.invoke();
    }
}
