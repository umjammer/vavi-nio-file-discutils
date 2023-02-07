//
// Copyright (c) 2017, Bianco Veigel
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

package discUtils.btrfs;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import discUtils.btrfs.base.ItemType;
import discUtils.btrfs.base.Key;
import discUtils.btrfs.base.items.ExtentData;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.BuiltStream;
import discUtils.streams.StreamBuffer;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.BuilderStreamExtent;
import discUtils.streams.util.Ownership;


public class File implements IVfsFile {

    protected final DirEntry dirEntry;

    protected final Context context;

    public File(DirEntry dirEntry, Context context) {
        this.dirEntry = dirEntry;
        this.context = context;
    }

    @Override public long getCreationTimeUtc() {
        return dirEntry.getCreationTimeUtc();
    }

    @Override public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public EnumSet<FileAttributes> getFileAttributes() {
        return dirEntry.getFileAttributes();
    }

    @Override public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    @Override public IBuffer getFileContent() {
        List<ExtentData> extents = context.findKey(dirEntry.getTreeId(), new Key(dirEntry.getObjectId(), ItemType.ExtentData));
        return bufferFromExtentList(extents);
    }

    private IBuffer bufferFromExtentList(List<ExtentData> extents) {
        List<BuilderExtent> builderExtents = new ArrayList<>(extents.size());
        for (ExtentData extent : extents) {
            long offset = extent.getKey().getOffset();
            BuilderExtent builderExtent = new BuilderStreamExtent(offset, extent.getStream(context), Ownership.Dispose);
            builderExtents.add(builderExtent);
        }
        return new StreamBuffer(new BuiltStream(dirEntry.getFileSize(), builderExtents), Ownership.Dispose);
    }

    @Override public long getFileLength() {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastAccessTimeUtc() {
        return dirEntry.getLastAccessTimeUtc();
    }

    @Override public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public long getLastWriteTimeUtc() {
        return dirEntry.getLastWriteTimeUtc();
    }

    @Override public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }
}
