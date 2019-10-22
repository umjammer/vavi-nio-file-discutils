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

package DiscUtils.Btrfs;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import DiscUtils.Btrfs.Base.ItemType;
import DiscUtils.Btrfs.Base.Key;
import DiscUtils.Btrfs.Base.Items.ExtentData;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.IVfsFile;
import DiscUtils.Streams.BuiltStream;
import DiscUtils.Streams.StreamBuffer;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Builder.BuilderExtent;
import DiscUtils.Streams.Builder.BuilderStreamExtent;
import DiscUtils.Streams.Util.Ownership;


public class File implements IVfsFile {

    protected final DirEntry DirEntry;

    protected final Context Context;

    public File(DirEntry dirEntry, Context context) {
        DirEntry = dirEntry;
        Context = context;
    }

    public long getCreationTimeUtc() {
        return DirEntry.getCreationTimeUtc();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return DirEntry.getFileAttributes();
    }

    public void setFileAttributes(Map<String, Object> value) {
        throw new UnsupportedOperationException();
    }

    public IBuffer getFileContent() {
        List<ExtentData> extents = Context.findKey__(DirEntry.getTreeId(), new Key(DirEntry.getObjectId(), ItemType.ExtentData));
        return bufferFromExtentList(extents);
    }

    private IBuffer bufferFromExtentList(List<ExtentData> extents) {
        List<BuilderExtent> builderExtents = new ArrayList<>(extents.size());
        for (ExtentData extent : extents) {
            long offset = extent.getKey().getOffset();
            BuilderExtent builderExtent = new BuilderStreamExtent(offset, extent.getStream(Context), Ownership.Dispose);
            builderExtents.add(builderExtent);
        }
        return new StreamBuffer(new BuiltStream(DirEntry.getFileSize(), builderExtents), Ownership.Dispose);
    }

    public long getFileLength() {
        throw new UnsupportedOperationException();
    }

    public long getLastAccessTimeUtc() {
        return DirEntry.getLastAccessTimeUtc();
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return DirEntry.getLastWriteTimeUtc();
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }
}
