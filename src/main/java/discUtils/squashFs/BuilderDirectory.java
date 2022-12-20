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

package discUtils.squashFs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class BuilderDirectory extends BuilderNode {

    private static final String FS = java.io.File.separator;

    private final List<Entry> children;

    private final Map<String, Entry> index;

    private DirectoryInode inode;

    public BuilderDirectory() {
        children = new ArrayList<>();
        index = new HashMap<>();
    }

    @Override
    public Inode getInode() {
        return inode;
    }

    public void addChild(String name, BuilderNode node) {
        if (name.contains(FS + FS)) {
            throw new IllegalArgumentException("Single level of path must be provided");
        }

        if (index.containsKey(name)) {
            throw new dotnet4j.io.IOException("The directory entry '" + name + "' already exists");
        }

        Entry newEntry = new Entry();
        newEntry.name = name;
        newEntry.node = node;
        children.add(newEntry);
        index.put(name, newEntry);
    }

    public BuilderNode getChild(String name) {
        if (index.containsKey(name)) {
            Entry result = index.get(name);
            return result.node;
        }

        return null;
    }

    @Override public void reset() {
        for (Entry entry : children) {
            entry.node.reset();
        }
        inode = new DirectoryInode();
    }

    @Override public void write(BuilderContext context) {
        if (written) {
            return;
        }

        Collections.sort(children);
        for (Entry entry : children) {
            entry.node.write(context);
        }
        writeDirectory(context);
        writeInode(context);
        written = true;
    }

    private void writeDirectory(BuilderContext context) {
        MetadataRef startPos = context.getDirectoryWriter().getPosition();

        int currentChild = 0;
        int numDirs = 0;
        while (currentChild < children.size()) {
            long thisBlock = children.get(currentChild).node.getInodeRef().getBlock();
            int firstInode = children.get(currentChild).node.getInodeNumber();

            int count = 1;
            while (currentChild + count < children.size()
                    && children.get(currentChild + count).node.getInodeRef().getBlock() == thisBlock
                    && children.get(currentChild + count).node.getInodeNumber() - firstInode < 0x7FFF) {
                ++count;
            }

            DirectoryHeader hdr = new DirectoryHeader();
            hdr.count = count - 1;
            hdr.inodeNumber = firstInode;
            hdr.startBlock = (int) thisBlock;

            hdr.writeTo(context.getIoBuffer(), 0);
            context.getDirectoryWriter().write(context.getIoBuffer(), 0, hdr.size());

            for (int i = 0; i < count; ++i) {
                Entry child = children.get(currentChild + i);
                DirectoryRecord record = new DirectoryRecord();
                record.setOffset((short) child.node.getInodeRef().getOffset());
                record.inodeNumber = (short) (child.node.getInodeNumber() - firstInode);
                record.type = child.node.getInode().type;
                record.name = child.name;

                record.writeTo(context.getIoBuffer(), 0);
                context.getDirectoryWriter().write(context.getIoBuffer(), 0, record.size());

                if (child.node.getInode().type == InodeType.Directory
                        || child.node.getInode().type == InodeType.ExtendedDirectory) {
                    ++numDirs;
                }
            }

            currentChild += count;
        }

        long size = context.getDirectoryWriter().distanceFrom(startPos);
        if (size > 0xffffffffL) {
            throw new UnsupportedOperationException("Writing large directories");
        }

        setNumLinks(numDirs + 2); // +1 for self, +1 for parent

        inode.setStartBlock((int) startPos.getBlock());
        inode.setOffset((short) startPos.getOffset());
        inode.setFileSize((int) size + 3);
    }

    // For some reason, always +3
    private void writeInode(BuilderContext context) {
        fillCommonInodeData(context);
        inode.type = InodeType.Directory;
        setInodeRef(context.getInodeWriter().getPosition());
        inode.writeTo(context.getIoBuffer(), 0);
        context.getInodeWriter().write(context.getIoBuffer(), 0, inode.size());
    }

    private static class Entry implements Comparable<Entry> {

        public String name;

        public BuilderNode node;

        @Override public int compareTo(Entry other) {
            return name.compareTo(other.name);
        }
    }
}
