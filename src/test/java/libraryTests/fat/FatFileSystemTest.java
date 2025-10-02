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

package libraryTests.fat;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemInfo;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.FileSystemParameters;
import discUtils.core.FloppyDiskType;
import discUtils.core.Geometry;
import discUtils.core.coreCompat.EncodingHelper;
import discUtils.fat.FatFileSystem;
import discUtils.streams.SparseMemoryStream;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class FatFileSystemTest {

    private static final String FS = File.separator;

    @Test
    void formatFloppy() throws Exception {
        MemoryStream ms = new MemoryStream();
        FatFileSystem fs = FatFileSystem.formatFloppy(ms, FloppyDiskType.HighDensity, "KBFLOPPY   ");
    }

    @Test
    void cyrillic() throws Exception {
//        SetupHelper.RegisterAssembly(FatFileSystem.class.getTypeInfo().Assembly);

        String lowerDE = "\u0434";
        String upperDE = "\u0414";

        MemoryStream ms = new MemoryStream();
        try (FatFileSystem fs = FatFileSystem.formatFloppy(ms, FloppyDiskType.HighDensity, "KBFLOPPY   ")) {
            fs.getFatOptions().setFileNameEncoding(EncodingHelper.forCodePage(855));

            String name = lowerDE;
            fs.createDirectory(name);

            List<String> dirs = fs.getDirectories("");
            assertEquals(1, dirs.size());
            assertEquals(upperDE, dirs.get(0)); // Uppercase

            assertTrue(fs.directoryExists(lowerDE));
            assertTrue(fs.directoryExists(upperDE));

            fs.createDirectory(lowerDE + lowerDE + lowerDE);
            assertEquals(2, fs.getDirectories("").size());

            fs.deleteDirectory(lowerDE + lowerDE + lowerDE);
            assertEquals(1, fs.getDirectories("").size());
        }

        List<FileSystemInfo> detectDefaultFileSystems = FileSystemManager.detectFileSystems(ms);

        FileSystemParameters parameters = new FileSystemParameters();
        parameters.setFileNameEncoding(EncodingHelper.forCodePage(855));
        DiscFileSystem fs2 = detectDefaultFileSystems.get(0).open(ms, parameters);

        assertTrue(fs2.directoryExists(lowerDE));
        assertTrue(fs2.directoryExists(upperDE));
        assertEquals(1, fs2.getDirectories("").size());
    }

    @Test
    void defaultCodepage() throws Exception {
        String graphicChar = "\u255D";

        MemoryStream ms = new MemoryStream();
        FatFileSystem fs = FatFileSystem.formatFloppy(ms, FloppyDiskType.HighDensity, "KBFLOPPY   ");
        fs.getFatOptions().setFileNameEncoding(EncodingHelper.forCodePage(855));

        String name = graphicChar;
        fs.createDirectory(name);

        List<String> dirs = fs.getDirectories("");
        assertEquals(1, dirs.size());
        assertEquals(graphicChar, dirs.get(0)); // Uppercase

        assertTrue(fs.directoryExists(graphicChar));
    }

    @Test
    void formatPartition() throws Exception {
        MemoryStream ms = new MemoryStream();

        Geometry g = Geometry.fromCapacity(1024 * 1024 * 32);
        FatFileSystem fs = FatFileSystem.formatPartition(ms, "KBPARTITION", g, 0, (int) g.getTotalSectorsLong(), (short) 13);

        fs.createDirectory("DIRB" + FS + "DIRC");

        FatFileSystem fs2 = new FatFileSystem(ms);
        assertEquals(1, fs2.getRoot().getDirectories().size());
    }

    @Test
    void createDirectory() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");

        fs.createDirectory("UnItTeSt");
        assertEquals("UNITTEST", fs.getRoot().getDirectories("UNITTEST").get(0).getName());

        fs.createDirectory("folder" + FS + "subflder");
        assertEquals("FOLDER", fs.getRoot().getDirectories("FOLDER").get(0).getName());

        fs.createDirectory("folder" + FS + "subflder");
        assertEquals("SUBFLDER", fs.getRoot().getDirectories("FOLDER").get(0).getDirectories("SUBFLDER").get(0).getName());
    }

    @Test
    void canWrite() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        assertTrue(fs.canWrite());
    }

    @Test
    void label() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        assertEquals("FLOPPY_IMG ", fs.getVolumeLabel());

        fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, null);
        assertEquals("NO NAME    ", fs.getVolumeLabel());
    }

    @Test
    void fileInfo() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        DiscFileInfo fi = fs.getFileInfo("SOMEDIR" + FS + "SOMEFILE.TXT");
        assertNotNull(fi);
    }

    @Test
    void directoryInfo() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        DiscDirectoryInfo fi = fs.getDirectoryInfo("SOMEDIR");
        assertNotNull(fi);
    }

    @Test
    void fileSystemInfo() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        DiscFileSystemInfo fi = fs.getFileSystemInfo("SOMEDIR" + FS + "SOMEFILE");
        assertNotNull(fi);
    }

    @Test
    void root() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        assertNotNull(fs.getRoot());
        assertTrue(fs.getRoot().exists());
        assertTrue(fs.getRoot().getName().isEmpty());
        assertNull(fs.getRoot().getParent());
    }

    @Test
    void openFileAsDir() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");

        try (Stream s = fs.openFile("FOO.TXT", FileMode.Create, FileAccess.ReadWrite);
            StreamWriter w = new StreamWriter(s)) {
            w.writeLine("FOO - some sample text");
            w.flush();
        }

        assertThrows(FileNotFoundException.class, () -> fs.getFiles("FOO.TXT"));
    }

    @Test
    void honoursReadOnly() throws Exception {
        SparseMemoryStream diskStream = new SparseMemoryStream();
        FatFileSystem fs = FatFileSystem.formatFloppy(diskStream, FloppyDiskType.HighDensity, "FLOPPY_IMG ");

        fs.createDirectory("AAA");
        fs.createDirectory("BAR");

        try (Stream t = fs.openFile("BAR" + FS + "AAA.TXT", FileMode.Create, FileAccess.ReadWrite)) {
        }
        try (Stream s = fs.openFile("BAR" + FS + "FOO.TXT", FileMode.Create, FileAccess.ReadWrite);
            StreamWriter w = new StreamWriter(s)) {
            w.writeLine("FOO - some sample text");
            w.flush();
        }
        fs.setLastAccessTimeUtc("BAR", ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli());
        fs.setLastAccessTimeUtc("BAR" + FS + "FOO.TXT",
                                ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli());

        // Check we can access a file without any errors
        SparseStream roDiskStream = SparseStream.readOnly(diskStream, Ownership.None);
        try (FatFileSystem fatFs = new FatFileSystem(roDiskStream);
            Stream fileStream = fatFs.openFile("BAR" + FS + "FOO.TXT", FileMode.Open)) {
            fileStream.readByte();
        }
    }

    @Test
    void invalidImageThrowsException() throws Exception {
        SparseMemoryStream stream = new SparseMemoryStream();
        byte[] buffer = new byte[1024 * 1024];
        stream.write(buffer, 0, 1024 * 1024);
        stream.position(0);
        assertThrows(IllegalStateException.class, () -> { // InvalidFileSystemException
            new FatFileSystem(stream);
        });
    }
}
