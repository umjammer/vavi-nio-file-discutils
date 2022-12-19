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

package libraryTests.squashFs;

import java.io.File;
import java.util.EnumSet;

import discUtils.core.UnixFilePermissions;
import discUtils.squashFs.SquashFileSystemBuilder;
import discUtils.squashFs.SquashFileSystemReader;
import dotnet4j.io.FileMode;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public final class SquashFileSystemBuilderTest {

    private static final String FS = File.separator;

    @Test
    public void singleFile() throws Exception {
        MemoryStream fsImage = new MemoryStream();
        SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
        builder.addFile("file", new MemoryStream(new byte[] { 1, 2, 3, 4 }));
        builder.build(fsImage);

//Debug.println("\n" + StringUtil.getDump(fsImage.toArray()));
        SquashFileSystemReader reader = new SquashFileSystemReader(fsImage);
        assertEquals(1, reader.getFileSystemEntries(FS).size());
        assertEquals(4, reader.getFileLength("file"));
        assertTrue(reader.fileExists("file"));
        assertFalse(reader.directoryExists("file"));
        assertFalse(reader.fileExists("otherfile"));
    }

    @Test
    public void createDirs() throws Exception {
        MemoryStream fsImage = new MemoryStream();
        SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
        builder.addFile(FS + "adir" + FS + "anotherdir" + FS + "file",
                        new MemoryStream(new byte[] { 1, 2, 3, 4 }));
        builder.build(fsImage);
        SquashFileSystemReader reader = new SquashFileSystemReader(fsImage);
        assertTrue(reader.directoryExists("adir"));
        assertTrue(reader.directoryExists("adir" + FS + "anotherdir"));
        assertTrue(reader.fileExists("adir" + FS + "anotherdir" + FS + "file"));
    }

    @Test
    public void defaults() throws Exception {
        MemoryStream fsImage = new MemoryStream();
        SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
        builder.addFile("file", new MemoryStream(new byte[] { 1, 2, 3, 4 }));
        builder.addDirectory("dir");
        builder.setDefaultUser(1000);
        builder.setDefaultGroup(1234);
        builder.setDefaultFilePermissions(UnixFilePermissions.OwnerAll);
        builder.setDefaultDirectoryPermissions(UnixFilePermissions.GroupAll);
        builder.addFile("file2", new MemoryStream());
        builder.addDirectory("dir2");
        builder.build(fsImage);
        SquashFileSystemReader reader = new SquashFileSystemReader(fsImage);
        assertEquals(0, reader.getUnixFileInfo("file").getUserId());
        assertEquals(0, reader.getUnixFileInfo("file").getGroupId());
        assertEquals(EnumSet.of(UnixFilePermissions.OwnerRead,
                                UnixFilePermissions.OwnerWrite,
                                UnixFilePermissions.GroupRead,
                                UnixFilePermissions.GroupWrite),
                     reader.getUnixFileInfo("file").getPermissions());
        assertEquals(0, reader.getUnixFileInfo("dir").getUserId());
        assertEquals(0, reader.getUnixFileInfo("dir").getGroupId());
        EnumSet<UnixFilePermissions> flag = UnixFilePermissions.OwnerAll;
        flag.addAll(EnumSet.of(UnixFilePermissions.GroupRead,
                               UnixFilePermissions.GroupExecute,
                               UnixFilePermissions.OthersRead,
                               UnixFilePermissions.OthersExecute));
        assertEquals(flag, reader.getUnixFileInfo("dir").getPermissions());
        assertEquals(1000, reader.getUnixFileInfo("file2").getUserId());
        assertEquals(1234, reader.getUnixFileInfo("file2").getGroupId());
        assertEquals(UnixFilePermissions.OwnerAll, reader.getUnixFileInfo("file2").getPermissions());
        assertEquals(1000, reader.getUnixFileInfo("dir2").getUserId());
        assertEquals(1234, reader.getUnixFileInfo("dir2").getGroupId());
        assertEquals(UnixFilePermissions.GroupAll, reader.getUnixFileInfo("dir2").getPermissions());
    }

    @Test
    public void fragmentData() throws Exception {
        MemoryStream fsImage = new MemoryStream();

        SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
        builder.addFile("file", new MemoryStream(new byte[] { 1, 2, 3, 4 }));
        builder.build(fsImage);

        SquashFileSystemReader reader = new SquashFileSystemReader(fsImage);

        try (Stream fs = reader.openFile("file", FileMode.Open)) {
            byte[] buffer = new byte[100];
            int numRead = fs.read(buffer, 0, 100);

            assertEquals(4, numRead);
            assertEquals(1, buffer[0]);
            assertEquals(2, buffer[1]);
            assertEquals(3, buffer[2]);
            assertEquals(4, buffer[3]);
        }
    }

    @Test
    public void blockData() throws Exception {
        byte[] testData = new byte[(512 * 1024) + 21];
        for (int i = 0; i < testData.length; ++i) {
            testData[i] = (byte) (i % 33);
        }

        MemoryStream fsImage = new MemoryStream();

        SquashFileSystemBuilder builder = new SquashFileSystemBuilder();
        builder.addFile("file", new MemoryStream(testData));
        builder.build(fsImage);

        SquashFileSystemReader reader = new SquashFileSystemReader(fsImage);

        try (Stream fs = reader.openFile("file", FileMode.Open)) {
            byte[] buffer = new byte[(512 * 1024) + 1024];
            int numRead = fs.read(buffer, 0, buffer.length);
            assertEquals(testData.length, numRead);
            for (int i = 0; i < testData.length; ++i) {
                assertEquals(testData[i], buffer[i] /* , "Data differs at index " + i */);
            }
        }
    }
}
