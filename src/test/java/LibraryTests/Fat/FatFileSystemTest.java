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

package LibraryTests.Fat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.DiscDirectoryInfo;
import DiscUtils.Core.DiscFileInfo;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.DiscFileSystemInfo;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.FloppyDiskType;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.CoreCompat.EncodingHelper;
import DiscUtils.Fat.FatFileSystem;
import DiscUtils.Streams.SparseMemoryStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.MemoryStream;
import moe.yo3explorer.dotnetio4j.Stream;
import moe.yo3explorer.dotnetio4j.StreamWriter;


public class FatFileSystemTest {
    @Test
    public void formatFloppy() throws Exception {
        MemoryStream ms = new MemoryStream();
        FatFileSystem fs = FatFileSystem.formatFloppy(ms, FloppyDiskType.HighDensity, "KBFLOPPY   ");
    }

    @Test
    public void cyrillic() throws Exception {
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
            assertEquals(upperDE, dirs.get(0));
            // Uppercase
            assertTrue(fs.directoryExists(lowerDE));
            assertTrue(fs.directoryExists(upperDE));
            fs.createDirectory(lowerDE + lowerDE + lowerDE);
            assertEquals(2, fs.getDirectories("").size());
            fs.deleteDirectory(lowerDE + lowerDE + lowerDE);
            assertEquals(1, fs.getDirectories("").size());
        }
        List<FileSystemInfo> detectDefaultFileSystems = FileSystemManager.detectFileSystems(ms);
        DiscFileSystem fs2 = detectDefaultFileSystems.get(0).open(ms, new FileSystemParameters());
        assertTrue(fs2.directoryExists(lowerDE));
        assertTrue(fs2.directoryExists(upperDE));
        assertEquals(1, fs2.getDirectories("").size());
    }

    @Test
    public void defaultCodepage() throws Exception {
        String graphicChar = "\u255D";
        MemoryStream ms = new MemoryStream();
        FatFileSystem fs = FatFileSystem.formatFloppy(ms, FloppyDiskType.HighDensity, "KBFLOPPY   ");
        fs.getFatOptions().setFileNameEncoding(EncodingHelper.forCodePage(855));
        String name = graphicChar;
        fs.createDirectory(name);
        List<String> dirs = fs.getDirectories("");
        assertEquals(1, dirs.size());
        assertEquals(graphicChar, dirs.get(0));
        // Uppercase
        assertTrue(fs.directoryExists(graphicChar));
    }

    @Test
    public void formatPartition() throws Exception {
        MemoryStream ms = new MemoryStream();
        Geometry g = Geometry.fromCapacity(1024 * 1024 * 32);
        FatFileSystem fs = FatFileSystem.formatPartition(ms, "KBPARTITION", g, 0, (int) g.getTotalSectorsLong(), (short) 13);
        fs.createDirectory("DIRB\\DIRC");
        FatFileSystem fs2 = new FatFileSystem(ms);
        assertEquals(1, fs2.getRoot().getDirectories().size());
    }

    @Test
    public void createDirectory() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        fs.createDirectory("UnItTeSt");
        assertEquals("UNITTEST", fs.getRoot().getDirectories("UNITTEST").get(0).getName());
        fs.createDirectory("folder\\subflder");
        assertEquals("FOLDER", fs.getRoot().getDirectories("FOLDER").get(0).getName());
        fs.createDirectory("folder\\subflder");
        assertEquals("SUBFLDER", fs.getRoot().getDirectories("FOLDER").get(0).getDirectories("SUBFLDER").get(0).getName());
    }

    @Test
    public void canWrite() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        assertEquals(true, fs.canWrite());
    }

    @Test
    public void label() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        assertEquals("FLOPPY_IMG ", fs.getVolumeLabel());
        fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, null);
        assertEquals("NO NAME    ", fs.getVolumeLabel());
    }

    @Test
    public void fileInfo() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        DiscFileInfo fi = fs.getFileInfo("SOMEDIR\\SOMEFILE.TXT");
        assertNotNull(fi);
    }

    @Test
    public void directoryInfo() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        DiscDirectoryInfo fi = fs.getDirectoryInfo("SOMEDIR");
        assertNotNull(fi);
    }

    @Test
    public void fileSystemInfo() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        DiscFileSystemInfo fi = fs.getFileSystemInfo("SOMEDIR\\SOMEFILE");
        assertNotNull(fi);
    }

    @Test
    public void root() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        assertNotNull(fs.getRoot());
        assertTrue(fs.getRoot().exists());
        assertTrue(fs.getRoot().getName().isEmpty());
        assertNull(fs.getRoot().getParent());
    }

    @Test
    public void openFileAsDir() throws Exception {
        FatFileSystem fs = FatFileSystem.formatFloppy(new MemoryStream(), FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        try (Stream s = fs.openFile("FOO.TXT", FileMode.Create, FileAccess.ReadWrite)) {
            StreamWriter w = new StreamWriter(s);
            w.writeLine("FOO - some sample text");
            w.flush();
        }
        assertThrows(FileNotFoundException.class, () -> {
            fs.getFiles("FOO.TXT");
        });
    }

    @Test
    public void honoursReadOnly() throws Exception {
        SparseMemoryStream diskStream = new SparseMemoryStream();
        FatFileSystem fs = FatFileSystem.formatFloppy(diskStream, FloppyDiskType.HighDensity, "FLOPPY_IMG ");
        fs.createDirectory("AAA");
        fs.createDirectory("BAR");
        try (Stream t = fs.openFile("BAR\\AAA.TXT", FileMode.Create, FileAccess.ReadWrite)) {
        }
        try (Stream s = fs.openFile("BAR\\FOO.TXT", FileMode.Create, FileAccess.ReadWrite)) {
            StreamWriter w = new StreamWriter(s);
            w.writeLine("FOO - some sample text");
            w.flush();
        }
        fs.setLastAccessTimeUtc("BAR", ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli());
        fs.setLastAccessTimeUtc("BAR\\FOO.TXT",
                                ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli());
        // Check we can access a file without any errors
        SparseStream roDiskStream = SparseStream.readOnly(diskStream, Ownership.None);
        FatFileSystem fatFs = new FatFileSystem(roDiskStream);
        try (Stream fileStream = fatFs.openFile("BAR\\FOO.TXT", FileMode.Open)) {
            fileStream.readByte();
        }
    }

    @Test
    public void invalidImageThrowsException() throws Exception {
        SparseMemoryStream stream = new SparseMemoryStream();
        byte[] buffer = new byte[1024 * 1024];
        stream.write(buffer, 0, 1024 * 1024);
        stream.setPosition(0);
        assertThrows(IllegalStateException.class, () -> { // InvalidFileSystemException
            return new FatFileSystem(stream);
        });
    }
}
