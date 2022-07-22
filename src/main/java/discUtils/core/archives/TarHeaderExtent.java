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

package discUtils.core.archives;

import java.util.EnumSet;

import discUtils.core.UnixFilePermissions;
import discUtils.streams.builder.BuilderBufferExtent;


public final class TarHeaderExtent extends BuilderBufferExtent {

    private final long fileLength;

    private final String fileName;

    private final int groupId;

    private final EnumSet<UnixFilePermissions> mode;

    private final long modificationTime;

    private final int ownerId;

    public TarHeaderExtent(long start,
            String fileName,
            long fileLength,
            EnumSet<UnixFilePermissions> mode,
            int ownerId,
            int groupId,
            long modificationTime) {
        super(start, 512);
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.mode = mode;
        this.ownerId = ownerId;
        this.groupId = groupId;
        this.modificationTime = modificationTime;
    }

    public TarHeaderExtent(long start, String fileName, long fileLength) {
        this(start, fileName, fileLength, EnumSet.noneOf(UnixFilePermissions.class), 0, 0, 0L /* UnixEpoch */);
    }

    protected byte[] getBuffer() {
        byte[] buffer = new byte[TarHeader.Length];
        TarHeader header = new TarHeader();
        header.fileName = fileName;
        header.fileLength = fileLength;
        header.fileMode = mode;
        header.ownerId = ownerId;
        header.groupId = groupId;
        header.modificationTime = modificationTime;
        header.writeTo(buffer, 0);
        return buffer;
    }

}
