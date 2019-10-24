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

package LibraryTests;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.CoreCompat.FileAttributes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class DiscFileSystemFileTest {

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void createFile(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        try (Stream s = fs.getFileInfo("foo.txt").open(FileMode.Create, FileAccess.ReadWrite)) {
            s.writeByte((byte) 1);
        }
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        assertTrue(fi.exists());
        assertEquals(EnumSet.of(FileAttributes.Archive), fi.getAttributes());
        assertEquals(1, fi.getLength());

        try (Stream s = fs.openFile("Foo.txt", FileMode.Open, FileAccess.Read)) {
            assertEquals(1, s.readByte());
        }
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void createFileInvalid_Long(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertThrows(IOException.class, () -> {
            try (Stream s = fs.getFileInfo(new String(new char[256]).replace('\0', 'X'))
                    .open(FileMode.Create, FileAccess.ReadWrite)) {
                s.writeByte((byte) 1);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void createFileInvalid_Characters(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertThrows(IOException.class, () -> {
            try (Stream s = fs.getFileInfo("A\0File").open(FileMode.Create, FileAccess.ReadWrite)) {
                s.writeByte((byte) 1);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void deleteFile(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        try (Stream s = fs.getFileInfo("foo.txt").open(FileMode.Create, FileAccess.ReadWrite)) {
        }
        assertEquals(1, fs.getRoot().getFiles().size());
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        fi.delete();
        assertEquals(0, fs.getRoot().getFiles().size());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void length(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        try (Stream s = fs.getFileInfo("foo.txt").open(FileMode.Create, FileAccess.ReadWrite)) {
            s.setLength(3128);
        }
        assertEquals(3128, fs.getFileInfo("foo.txt").getLength());
        try (Stream s = fs.openFile("foo.txt", FileMode.Open, FileAccess.ReadWrite)) {
            s.setLength(3);
            assertEquals(3, s.getLength());
        }
        assertEquals(3, fs.getFileInfo("foo.txt").getLength());
        try (Stream s = fs.openFile("foo.txt", FileMode.Open, FileAccess.ReadWrite)) {
            s.setLength(3333);
            byte[] buffer = new byte[512];
            for (int i = 0; i < buffer.length; ++i) {
                buffer[i] = (byte) i;
            }
            s.write(buffer, 0, buffer.length);
            s.write(buffer, 0, buffer.length);
            assertEquals(1024, s.getPosition());
            assertEquals(3333, s.getLength());
            s.setLength(512);
            assertEquals(512, s.getLength());
        }
        try (Stream s = fs.openFile("foo.txt", FileMode.Open, FileAccess.ReadWrite)) {
            byte[] buffer = new byte[512];
            int numRead = s.read(buffer, 0, buffer.length);
            int totalRead = 0;
            while (numRead != 0) {
                totalRead += numRead;
                numRead = s.read(buffer, totalRead, buffer.length - totalRead);
            }
            for (int i = 0; i < buffer.length; ++i) {
                assertEquals((byte) i, buffer[i]);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_FileNotFound(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        assertThrows(FileNotFoundException.class, () -> {
            try (Stream s = di.open(FileMode.Open)) {
            }
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_FileExists(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        try (Stream s = di.open(FileMode.Create)) {
            s.writeByte((byte) 1);
        }
        assertThrows(IOException.class, () -> {
            try (Stream s = di.open(FileMode.CreateNew)) {
            }
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_DirExists(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("FOO.TXT");
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        assertThrows(IOException.class, () -> {
            return di.open(FileMode.Create);
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_Read(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        try (Stream s = di.open(FileMode.Create)) {
            s.writeByte((byte) 1);
        }

        try (Stream s = di.open(FileMode.Open, FileAccess.Read)) {
            assertFalse(s.canWrite());
            assertTrue(s.canRead());
            assertEquals(1, s.readByte());
        }
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_Read_Fail(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        try (Stream s = di.open(FileMode.Create, FileAccess.Read)) {
            assertThrows(IOException.class, () -> {
                s.writeByte((byte) 1);
            });
        }
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_Write(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        try (Stream s = di.open(FileMode.Create, FileAccess.Write)) {
            assertTrue(s.canWrite());
            assertFalse(s.canRead());
            s.writeByte((byte) 1);
        }
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void open_Write_Fail(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo di = fs.getFileInfo("foo.txt");
        try (Stream s = di.open(FileMode.Create, FileAccess.ReadWrite)) {
            s.writeByte((byte) 1);
        }

        try (Stream s = di.open(FileMode.Open, FileAccess.Write)) {
            assertTrue(s.canWrite());
            assertFalse(s.canRead());
            assertThrows(IOException.class, () -> {
                return s.readByte();
            });
        }
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void name(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertEquals("foo.txt", fs.getFileInfo("foo.txt").getName());
        assertEquals("foo.txt", fs.getFileInfo("path\\foo.txt").getName());
        assertEquals("foo.txt", fs.getFileInfo("\\foo.txt").getName());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void attributes(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo fi = fs.getFileInfo("foo.txt");

        try (Stream s = fi.open(FileMode.Create)) {
        }
        // Check default attributes
        assertEquals(EnumSet.of(FileAttributes.Archive), fi.getAttributes());
        // Check round-trip
        EnumSet<FileAttributes> newAttrs = EnumSet.of(FileAttributes.Hidden, FileAttributes.ReadOnly, FileAttributes.System);
        fi.setAttributes(newAttrs);
        assertEquals(newAttrs, fi.getAttributes());
        // And check persistence to disk
        assertEquals(newAttrs, fs.getFileInfo("foo.txt").getAttributes());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void attributes_ChangeType(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        try (Stream s = fi.open(FileMode.Create)) {
        }
        assertThrows(IllegalArgumentException.class, () -> {
            EnumSet<FileAttributes> flags = fi.getAttributes();
            flags.add(FileAttributes.Directory);
            fi.setAttributes(flags);
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void exists(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        assertFalse(fi.exists());

        try (Stream s = fi.open(FileMode.Create)) {
        }
        assertTrue(fi.exists());
        fs.createDirectory("dir.txt");
        assertFalse(fs.getFileInfo("dir.txt").exists());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void creationTimeUtc(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();

        try (Stream s = fs.openFile("foo.txt", FileMode.Create)) {
        }
        assertTrue(Instant.now().toEpochMilli() >= fs.getFileInfo("foo.txt").getCreationTimeUtc());
        assertTrue(Instant.now().minus(Duration.ofSeconds(10)).toEpochMilli() <= fs.getFileInfo("foo.txt")
                .getCreationTimeUtc());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void creationTime(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();

        try (Stream s = fs.openFile("foo.txt", FileMode.Create)) {
        }
        assertTrue(Instant.now().toEpochMilli() >= fs.getFileInfo("foo.txt").getCreationTime());
        assertTrue(Instant.now().minus(Duration.ofSeconds(10)).toEpochMilli() <= fs.getFileInfo("foo.txt").getCreationTime());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void lastAccessTime(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        try (Stream s = fs.openFile("foo.txt", FileMode.Create)) {
        }
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        long baseTime = Instant.now().minus(Duration.ofDays(2)).toEpochMilli();
        fi.setLastAccessTime(baseTime);

        try (Stream s = fs.openFile("foo.txt", FileMode.Open, FileAccess.Read)) {
        }
        assertTrue(baseTime < fi.getLastAccessTime());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void lastWriteTime(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        try (Stream s = fs.openFile("foo.txt", FileMode.Create)) {
        }
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        long baseTime = Instant.now().minus(Duration.ofMinutes(10)).toEpochMilli();
        fi.setLastWriteTime(baseTime);

        try (Stream s = fs.openFile("foo.txt", FileMode.Open)) {
            s.writeByte((byte) 1);
        }
        assertTrue(baseTime < fi.getLastWriteTime());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void delete(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        try (Stream s = fs.openFile("foo.txt", FileMode.Create)) {
        }
        fs.getFileInfo("foo.txt").delete();
        assertFalse(fs.fileExists("foo.txt"));
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void delete_Dir(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("foo.txt");
        assertThrows(FileNotFoundException.class, () -> {
            fs.getFileInfo("foo.txt").delete();
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void delete_NoFile(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertThrows(FileNotFoundException.class, () -> {
            fs.getFileInfo("foo.txt").delete();
        });
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void copyFile(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        try (Stream s = fi.create()) {
            for (int i = 0; i < 10; ++i) {
                s.write(new byte[111], 0, 111);
            }
        }
        fi.setAttributes(EnumSet.of(FileAttributes.Hidden, FileAttributes.System));
        fi.copyTo("foo2.txt");
        fi = fs.getFileInfo("foo2.txt");
        assertTrue(fi.exists());
        assertEquals(1110, fi.getLength());
        assertEquals(EnumSet.of(FileAttributes.Hidden, FileAttributes.System), fi.getAttributes());
        fi = fs.getFileInfo("foo.txt");
        assertTrue(fi.exists());
        fi = fs.getFileInfo("foo2.txt");
        assertTrue(fi.exists());
        assertEquals(1110, fi.getLength());
        assertEquals(EnumSet.of(FileAttributes.Hidden, FileAttributes.System), fi.getAttributes());
        fi = fs.getFileInfo("foo.txt");
        assertTrue(fi.exists());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void moveFile(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo fi = fs.getFileInfo("foo.txt");

        try (Stream s = fi.create()) {
            for (int i = 0; i < 10; ++i) {
                s.write(new byte[111], 0, 111);
            }
        }
        fi.setAttributes(EnumSet.of(FileAttributes.Hidden, FileAttributes.System));
        fi.moveTo("foo2.txt");
        fi = fs.getFileInfo("foo2.txt");
        assertTrue(fi.exists());
        assertEquals(1110, fi.getLength());
        assertEquals(EnumSet.of(FileAttributes.Hidden, FileAttributes.System), fi.getAttributes());
        fi = fs.getFileInfo("foo.txt");
        assertFalse(fi.exists());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void moveFile_Overwrite(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        DiscFileInfo fi = fs.getFileInfo("foo.txt");
        try (Stream s = fi.create()) {
            s.writeByte((byte) 1);
        }
        DiscFileInfo fi2 = fs.getFileInfo("foo2.txt");

        try (Stream s = fi2.create()) {
        }
        fs.moveFile("foo.txt", "foo2.txt", true);
        assertFalse(fi.exists());
        assertTrue(fi2.exists());
        assertEquals(1, fi2.getLength());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void equals(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        assertEquals(fs.getFileInfo("foo.txt"), fs.getFileInfo("foo.txt"));
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void parent(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        fs.createDirectory("SOMEDIR\\ADIR");
        try (Stream s = fs.openFile("SOMEDIR\\ADIR\\FILE.TXT", FileMode.Create)) {
        }
        DiscFileInfo fi = fs.getFileInfo("SOMEDIR\\ADIR\\FILE.TXT");
        assertEquals(fs.getDirectoryInfo("SOMEDIR\\ADIR"), fi.getParent());
        assertEquals(fs.getDirectoryInfo("SOMEDIR\\ADIR"), fi.getDirectory());
    }

    @ParameterizedTest
    @MethodSource("LibraryTests.FileSystemSource#getReadWriteFileSystems")
    public void volumeLabel(NewFileSystemDelegate fsFactory) throws Exception {
        DiscFileSystem fs = fsFactory.invoke();
        String volLabel = fs.getVolumeLabel();
        assertNotNull(volLabel);
    }
}
