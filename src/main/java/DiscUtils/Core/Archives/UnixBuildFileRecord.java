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

package DiscUtils.Core.Archives;

import java.util.EnumSet;

import DiscUtils.Core.UnixFilePermissions;
import DiscUtils.Streams.Builder.BuilderBufferExtentSource;
import DiscUtils.Streams.Builder.BuilderExtent;
import DiscUtils.Streams.Builder.BuilderExtentSource;
import DiscUtils.Streams.Builder.BuilderStreamExtentSource;
import dotnet4j.io.Stream;


public final class UnixBuildFileRecord {
    private final BuilderExtentSource _source;

    public UnixBuildFileRecord(String name, byte[] buffer) {
        this(name, new BuilderBufferExtentSource(buffer), EnumSet.noneOf(UnixFilePermissions.class), 0, 0, 0L /* UnixEpoch */);
    }

    public UnixBuildFileRecord(String name, Stream stream) {
        this(name, new BuilderStreamExtentSource(stream), EnumSet.noneOf(UnixFilePermissions.class), 0, 0, 0L /* UnixEpoch */);
    }

    public UnixBuildFileRecord(String name,
            byte[] buffer,
            EnumSet<UnixFilePermissions> fileMode,
            int ownerId,
            int groupId,
            long modificationTime) {
        this(name, new BuilderBufferExtentSource(buffer), fileMode, ownerId, groupId, modificationTime);
    }

    public UnixBuildFileRecord(String name,
            Stream stream,
            EnumSet<UnixFilePermissions> fileMode,
            int ownerId,
            int groupId,
            long modificationTime) {
        this(name, new BuilderStreamExtentSource(stream), fileMode, ownerId, groupId, modificationTime);
    }

    public UnixBuildFileRecord(String name,
            BuilderExtentSource fileSource,
            EnumSet<UnixFilePermissions> fileMode,
            int ownerId,
            int groupId,
            long modificationTime) {
        __Name = name;
        _source = fileSource;
        __FileMode = fileMode;
        __OwnerId = ownerId;
        __GroupId = groupId;
        __ModificationTime = modificationTime;
    }

    private EnumSet<UnixFilePermissions> __FileMode;

    public EnumSet<UnixFilePermissions> getFileMode() {
        return __FileMode;
    }

    private int __GroupId;

    public int getGroupId() {
        return __GroupId;
    }

    private long __ModificationTime;

    public long getModificationTime() {
        return __ModificationTime;
    }

    private String __Name;

    public String getName() {
        return __Name;
    }

    private int __OwnerId;

    public int getOwnerId() {
        return __OwnerId;
    }

    public BuilderExtent fix(long pos) {
        return _source.fix(pos);
    }
}
