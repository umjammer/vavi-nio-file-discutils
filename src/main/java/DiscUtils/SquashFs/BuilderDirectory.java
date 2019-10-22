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

package DiscUtils.SquashFs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class BuilderDirectory extends BuilderNode {
    private final List<Entry> _children;

    private final Map<String, Entry> _index;

    private DirectoryInode _inode;

    public BuilderDirectory() {
        _children = new ArrayList<>();
        _index = new HashMap<>();
    }

    public Inode getInode() {
        return _inode;
    }

    public void addChild(String name, BuilderNode node) {
        if (name.contains("\\\\")) {
            throw new IllegalArgumentException("Single level of path must be provided");
        }

        if (_index.containsKey(name)) {
            throw new moe.yo3explorer.dotnetio4j.IOException("The directory entry '" + name + "' already exists");
        }

        Entry newEntry = new Entry();
        newEntry.Name = name;
        newEntry.Node = node;
        _children.add(newEntry);
        _index.put(name, newEntry);
    }

    public BuilderNode getChild(String name) {
        if (_index.containsKey(name)) {
            Entry result = _index.get(name);
            return result.Node;
        }

        return null;
    }

    public void reset() {
        for (Entry entry : _children) {
            entry.Node.reset();
        }
        _inode = new DirectoryInode();
    }

    public void write(BuilderContext context) {
        if (_written) {
            return;
        }

        Collections.sort(_children);
        for (Entry entry : _children) {
            entry.Node.write(context);
        }
        writeDirectory(context);
        writeInode(context);
        _written = true;
    }

    private void writeDirectory(BuilderContext context) {
        MetadataRef startPos = context.getDirectoryWriter().getPosition();

        int currentChild = 0;
        int numDirs = 0;
        while (currentChild < _children.size()) {
            long thisBlock = _children.get(currentChild).Node.getInodeRef().getBlock();
            int firstInode = _children.get(currentChild).Node.getInodeNumber();

            int count = 1;
            while (currentChild + count < _children.size()
                    && _children.get(currentChild + count).Node.getInodeRef().getBlock() == thisBlock
                    && _children.get(currentChild + count).Node.getInodeNumber() - firstInode < 0x7FFF) {
                ++count;
            }

            DirectoryHeader hdr = new DirectoryHeader();
            hdr.Count = count - 1;
            hdr.InodeNumber = firstInode;
            hdr.StartBlock = (int) thisBlock;

            hdr.writeTo(context.getIoBuffer(), 0);
            context.getDirectoryWriter().write(context.getIoBuffer(), 0, (int) hdr.getSize());

            for (int i = 0; i < count; ++i) {
                Entry child = _children.get(currentChild + i);
                DirectoryRecord record = new DirectoryRecord();
                record.Offset = (short) child.Node.getInodeRef().getOffset();
                record.InodeNumber = (short) (child.Node.getInodeNumber() - firstInode);
                record.Type = child.Node.getInode().Type;
                record.Name = child.Name;

                record.writeTo(context.getIoBuffer(), 0);
                context.getDirectoryWriter().write(context.getIoBuffer(), 0, (int) record.getSize());

                if (child.Node.getInode().Type == InodeType.Directory
                        || child.Node.getInode().Type == InodeType.ExtendedDirectory) {
                    ++numDirs;
                }
            }

            currentChild += count;
        }

        long size = context.getDirectoryWriter().distanceFrom(startPos);
        if (size > 0xffffffffl) {
            throw new UnsupportedOperationException("Writing large directories");
        }

        setNumLinks(numDirs + 2); // +1 for self, +1 for parent

        _inode.setStartBlock((int) startPos.getBlock());
        _inode.setOffset((short) startPos.getOffset());
        _inode.setFileSize((int) size + 3);
    }

    // For some reason, always +3
    private void writeInode(BuilderContext context) {
        fillCommonInodeData(context);
        _inode.Type = InodeType.Directory;
        setInodeRef(context.getInodeWriter().getPosition());
        _inode.writeTo(context.getIoBuffer(), 0);
        context.getInodeWriter().write(context.getIoBuffer(), 0, (int) _inode.getSize());
    }

    private static class Entry implements Comparable<Entry> {
        public String Name;

        public BuilderNode Node;

        public int compareTo(Entry other) {
            return Name.compareTo(other.Name);
        }
    }
}
