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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import discUtils.core.UnixFilePermissions;
import discUtils.streams.builder.BuilderBufferExtent;
import discUtils.streams.builder.BuilderExtent;
import discUtils.streams.builder.StreamBuilder;
import discUtils.streams.util.MathUtilities;
import dotnet4j.io.Stream;


/**
 * builder to create UNIX Tar archive files.
 */
public final class TarFileBuilder extends StreamBuilder {

    private final List<UnixBuildFileRecord> files;

    /**
     * Initializes a new instance of the {@link #TarFileBuilder}
     * class.
     */
    public TarFileBuilder() {
        files = new ArrayList<>();
    }

    /**
     * Add a file to the tar archive.
     *
     * @param name The name of the file.
     * @param buffer The file data.
     */
    public void addFile(String name, byte[] buffer) {
        files.add(new UnixBuildFileRecord(name, buffer));
    }

    /**
     * Add a file to the tar archive.
     *
     * @param name The name of the file.
     * @param buffer The file data.
     * @param fileMode The access mode of the file.
     * @param ownerId The uid of the owner.
     * @param groupId The gid of the owner.
     * @param modificationTime The modification time for the file.
     */
    public void addFile(String name,
                        byte[] buffer,
                        EnumSet<UnixFilePermissions> fileMode,
                        int ownerId,
                        int groupId,
                        long modificationTime) {
        files.add(new UnixBuildFileRecord(name, buffer, fileMode, ownerId, groupId, modificationTime));
    }

    /**
     * Add a file to the tar archive.
     *
     * @param name The name of the file.
     * @param stream The file data.
     */
    public void addFile(String name, Stream stream) {
        files.add(new UnixBuildFileRecord(name, stream));
    }

    /**
     * Add a file to the tar archive.
     *
     * @param name The name of the file.
     * @param stream The file data.
     * @param fileMode The access mode of the file.
     * @param ownerId The uid of the owner.
     * @param groupId The gid of the owner.
     * @param modificationTime The modification time for the file.
     */
    public void addFile(String name,
                        Stream stream,
                        EnumSet<UnixFilePermissions> fileMode,
                        int ownerId,
                        int groupId,
                        long modificationTime) {
        files.add(new UnixBuildFileRecord(name, stream, fileMode, ownerId, groupId, modificationTime));
    }

    /**
     * @param totalLength {@cs out}
     */
    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        List<BuilderExtent> result = new ArrayList<>(files.size() * 2 + 2);
        long pos = 0;
        for (UnixBuildFileRecord file : files) {
            BuilderExtent fileContentExtent = file.fix(pos + TarHeader.Length);
            result.add(new TarHeaderExtent(pos,
                                           file.getName(),
                                           fileContentExtent.getLength(),
                                           file.getFileMode(),
                                           file.getOwnerId(),
                                           file.getGroupId(),
                                           file.getModificationTime()));
            pos += TarHeader.Length;
            result.add(fileContentExtent);
            pos += MathUtilities.roundUp(fileContentExtent.getLength(), 512);
        }
        // Two empty 512-byte blocks at end of tar file.
        result.add(new BuilderBufferExtent(pos, new byte[1024]));
        totalLength[0] = pos + 1024;
        return result;
    }
}
