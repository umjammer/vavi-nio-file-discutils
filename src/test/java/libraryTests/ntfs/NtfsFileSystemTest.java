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

package libraryTests.ntfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import discUtils.core.Geometry;
import discUtils.core.ReparsePoint;
import discUtils.core.coreCompat.FileAttributes;
import discUtils.ntfs.AttributeType;
import discUtils.ntfs.NtfsFileSystem;
import discUtils.streams.SparseMemoryStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.Range;
import libraryTests.FileSystemSource;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.AccessControlSections;
import dotnet4j.security.accessControl.RawSecurityDescriptor;


public class NtfsFileSystemTest {

    @Test
    public void aclInheritance() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        RawSecurityDescriptor sd = new RawSecurityDescriptor("O:BAG:BAD:(A;OICINP;GA;;;BA)");
        ntfs.createDirectory("dir");
        ntfs.setSecurity("dir", sd);
        ntfs.createDirectory("dir\\subdir");
        RawSecurityDescriptor inheritedSd = ntfs.getSecurity("dir\\subdir");
        assertNotNull(inheritedSd);
        assertEquals("O:BAG:BAD:(A;ID;GA;;;BA)", inheritedSd.getSddlForm(AccessControlSections.All));

        try (Closeable c = ntfs.openFile("dir\\subdir\\file", FileMode.Create, FileAccess.ReadWrite)) {
        }
        inheritedSd = ntfs.getSecurity("dir\\subdir\\file");
        assertNotNull(inheritedSd);
        assertEquals("O:BAG:BAD:", inheritedSd.getSddlForm(AccessControlSections.All));
    }

    @Test
    public void reparsePoints_Empty() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.createDirectory("dir");
        ntfs.setReparsePoint("dir", new ReparsePoint(12345, new byte[0]));
        ReparsePoint rp = ntfs.getReparsePoint("dir");
        assertEquals(12345, rp.getTag());
        assertNotNull(rp.getContent());
        assertEquals(0, rp.getContent().length);
    }

    @Test
    public void reparsePoints_NonEmpty() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.createDirectory("dir");
        ntfs.setReparsePoint("dir",
                             new ReparsePoint(123,
                                              new byte[] {
                                                  4, 5, 6
                                              }));
        ReparsePoint rp = ntfs.getReparsePoint("dir");
        assertEquals(123, rp.getTag());
        assertNotNull(rp.getContent());
        assertEquals(3, rp.getContent().length);
    }

    static class NullWriter extends Writer {
        public void write(char[] cbuf, int off, int len) throws IOException {
//            System.err.print(new String(cbuf, off, len));
        }
        public void flush() throws IOException {
        }
        public void close() throws IOException {
        }
    }

    @Test
    public void format_SmallDisk() throws Exception {
        long size = 8 * 1024 * 1024;
        SparseMemoryStream partStream = new SparseMemoryStream();
//        VirtualDisk disk = vhd.Disk.initializeDynamic(partStream, Ownership.Dispose, size);
        NtfsFileSystem.format(partStream, "New Partition", Geometry.fromCapacity(size), 0, size / 512);
        try (NtfsFileSystem ntfs = new NtfsFileSystem(partStream)) {
            ntfs.dump(new PrintWriter(new NullWriter()), "");
        }
    }

    @Test
    public void format_LargeDisk() throws Exception {
        long size = 1024L * 1024 * 1024L * 1024;
        // 1 TB
        SparseMemoryStream partStream = new SparseMemoryStream();
        NtfsFileSystem.format(partStream, "New Partition", Geometry.fromCapacity(size), 0, size / 512);
        try (NtfsFileSystem ntfs = new NtfsFileSystem(partStream)) {
            ntfs.dump(new PrintWriter(new NullWriter()), "");
        }
    }

    @Test
    public void clusterInfo() throws Exception {
        // 'Big' files have clusters
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();

        try (Stream s = ntfs.openFile("file", FileMode.Create, FileAccess.ReadWrite)) {
            s.write(new byte[(int) ntfs.getClusterSize()], 0, (int) ntfs.getClusterSize());
        }
        List<Range> ranges = ntfs.pathToClusters("file");
        assertEquals(1, ranges.size());
        assertEquals(1, ranges.get(0).getCount());
        // Short files have no clusters (stored in MFT)

        try (Stream s = ntfs.openFile("file2", FileMode.Create, FileAccess.ReadWrite)) {
            s.writeByte((byte) 1);
        }
        ranges = ntfs.pathToClusters("file2");
        assertEquals(0, ranges.size());
    }

    @Test
    public void extentInfo() throws Exception {
        try (SparseMemoryStream ms = new SparseMemoryStream()) {
            Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
            NtfsFileSystem ntfs = NtfsFileSystem.format(ms, "", diskGeometry, 0, diskGeometry.getTotalSectorsLong());
            // Check non-resident attribute
            try (Stream s = ntfs.openFile("file", FileMode.Create, FileAccess.ReadWrite)) {
                byte[] data = new byte[(int) ntfs.getClusterSize()];
                data[0] = (byte) 0xAE;
                data[1] = 0x3F;
                data[2] = (byte) 0x8D;
                s.write(data, 0, (int) ntfs.getClusterSize());
            }
            List<StreamExtent> extents = ntfs.pathToExtents("file");
            assertEquals(1, extents.size());
            assertEquals(ntfs.getClusterSize(), extents.get(0).getLength());
            ms.setPosition(extents.get(0).getStart());
            assertEquals(0xAE, ms.readByte());
            assertEquals(0x3F, ms.readByte());
            assertEquals(0x8D, ms.readByte());
            // Check resident attribute
            try (Stream s = ntfs.openFile("file2", FileMode.Create, FileAccess.ReadWrite)) {
                s.writeByte((byte) 0xBA);
                s.writeByte((byte) 0x82);
                s.writeByte((byte) 0x2C);
            }
            extents = ntfs.pathToExtents("file2");
            assertEquals(1, extents.size());
            assertEquals(3, extents.get(0).getLength());
            byte[] read = new byte[100];
            ms.setPosition(extents.get(0).getStart());
            ms.read(read, 0, 100);
            assertEquals((byte) 0xBA, read[0]);
            assertEquals((byte) 0x82, read[1]);
            assertEquals((byte) 0x2C, read[2]);
        }
    }

    @Test
    public void manyAttributes() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        try (Stream s = ntfs.openFile("file", FileMode.Create, FileAccess.ReadWrite)) {
            s.writeByte((byte) 32);
        }
        for (int i = 0; i < 50; ++i) {
            ntfs.createHardLink("file", "hl" + i);
        }
        try (Stream s = ntfs.openFile("hl35", FileMode.Open, FileAccess.ReadWrite)) {
            assertEquals(32, s.readByte());
            s.setPosition(0);
            s.writeByte((byte) 12);
        }
        try (Stream s = ntfs.openFile("hl5", FileMode.Open, FileAccess.ReadWrite)) {
            assertEquals(12, s.readByte());
        }
        for (int i = 0; i < 50; ++i) {
            ntfs.deleteFile("hl" + i);
        }
        assertEquals(1, ntfs.getFiles("\\").size());
        ntfs.deleteFile("file");
        assertEquals(0, ntfs.getFiles("\\").size());
    }

    @Test
    public void shortNames() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        // Check we can find a short name in the same directory
        try (Stream s = ntfs.openFile("ALongFileName.txt", FileMode.CreateNew)) {
        }
        ntfs.setShortName("ALongFileName.txt", "ALONG~01.TXT");
        assertEquals("ALONG~01.TXT", ntfs.getShortName("ALongFileName.txt"));
        assertTrue(ntfs.fileExists("ALONG~01.TXT"));
        // Check path handling
        ntfs.createDirectory("DIR");
        try (Stream s = ntfs.openFile("DIR\\ALongFileName2.txt", FileMode.CreateNew)) {
        }
        ntfs.setShortName("DIR\\ALongFileName2.txt", "ALONG~02.TXT");
        assertEquals("ALONG~02.TXT", ntfs.getShortName("DIR\\ALongFileName2.txt"));
        assertTrue(ntfs.fileExists("DIR\\ALONG~02.TXT"));
        // Check we can open a file by the short name
        try (Stream s = ntfs.openFile("ALONG~01.TXT", FileMode.Open)) {
        }
        // Delete the long name, and make sure the file is gone
        ntfs.deleteFile("ALONG~01.TXT");
        assertFalse(ntfs.fileExists("ALONG~01.TXT"));
        // Delete the short name, and make sure the file is gone
        ntfs.deleteFile("DIR\\ALONG~02.TXT");
        assertFalse(ntfs.fileExists("DIR\\ALongFileName2.txt"));
    }

    @Test
    public void hardLinkCount() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        try (Stream s = ntfs.openFile("ALongFileName.txt", FileMode.CreateNew)) {
        }
        assertEquals(1, ntfs.getHardLinkCount("ALongFileName.txt"));
        ntfs.createHardLink("ALongFileName.txt", "AHardLink.TXT");
        assertEquals(2, ntfs.getHardLinkCount("ALongFileName.txt"));
        ntfs.createDirectory("DIR");
        ntfs.createHardLink("ALongFileName.txt", "DIR\\SHORTLNK.TXT");
        assertEquals(3, ntfs.getHardLinkCount("ALongFileName.txt"));
        // If we enumerate short names, then the initial long name results in two 'hardlinks'
        ntfs.getNtfsOptions().setHideDosFileNames(false);
        assertEquals(4, ntfs.getHardLinkCount("ALongFileName.txt"));
    }

    @Test
    public void hasHardLink() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        try (Stream s = ntfs.openFile("ALongFileName.txt", FileMode.CreateNew)) {
        }
        assertFalse(ntfs.hasHardLinks("ALongFileName.txt"));
        ntfs.createHardLink("ALongFileName.txt", "AHardLink.TXT");
        assertTrue(ntfs.hasHardLinks("ALongFileName.txt"));
        try (Stream s = ntfs.openFile("ALongFileName2.txt", FileMode.CreateNew)) {
        }
        // If we enumerate short names, then the initial long name results in two 'hardlinks'
        ntfs.getNtfsOptions().setHideDosFileNames(false);
        assertTrue(ntfs.hasHardLinks("ALongFileName2.txt"));
    }

    @Test
    public void moveLongName() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        try (Stream s = ntfs.openFile("ALongFileName.txt", FileMode.CreateNew)) {
        }
        assertTrue(ntfs.fileExists("ALONGF~1.TXT"));
        ntfs.moveFile("ALongFileName.txt", "ADifferentLongFileName.txt");
        assertFalse(ntfs.fileExists("ALONGF~1.TXT"));
        assertTrue(ntfs.fileExists("ADIFFE~1.TXT"));
        ntfs.createDirectory("ALongDirectoryName");
        assertTrue(ntfs.directoryExists("ALONGD~1"));
        ntfs.moveDirectory("ALongDirectoryName", "ADifferentLongDirectoryName");
        assertFalse(ntfs.directoryExists("ALONGD~1"));
        assertTrue(ntfs.directoryExists("ADIFFE~1"));
    }

    @Test
    public void openRawStream() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        assertNull(ntfs.openRawStream("$Extend\\$ObjId", AttributeType.Data, null, FileAccess.Read));
    }

    @Test
    public void getAlternateDataStreams() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.openFile("AFILE.TXT", FileMode.Create).close();
        assertEquals(0, ntfs.getAlternateDataStreams("AFILE.TXT").size());
        ntfs.openFile("AFILE.TXT:ALTSTREAM", FileMode.Create).close();
        assertEquals(1, ntfs.getAlternateDataStreams("AFILE.TXT").size());
        assertEquals("ALTSTREAM", ntfs.getAlternateDataStreams("AFILE.TXT").get(0));
    }

    @Test
    public void deleteAlternateDataStreams() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.openFile("AFILE.TXT", FileMode.Create).close();
        ntfs.openFile("AFILE.TXT:ALTSTREAM", FileMode.Create).close();
        assertEquals(1, ntfs.getAlternateDataStreams("AFILE.TXT").size());
        ntfs.deleteFile("AFILE.TXT:ALTSTREAM");
        assertEquals(1, ntfs.getFileSystemEntries("").size());
        assertEquals(0, ntfs.getAlternateDataStreams("AFILE.TXT").size());
    }

    @Test
    public void deleteShortNameDir() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.createDirectory("\\TestLongName1\\TestLongName2");
        ntfs.setShortName("\\TestLongName1\\TestLongName2", "TESTLO~1");
        assertTrue(ntfs.directoryExists("\\TestLongName1\\TESTLO~1"));
        assertTrue(ntfs.directoryExists("\\TestLongName1\\TestLongName2"));
        ntfs.deleteDirectory("\\TestLongName1", true);
        assertFalse(ntfs.directoryExists("\\TestLongName1"));
    }

    @Test
    public void getFileLength() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.openFile("AFILE.TXT", FileMode.Create).close();
        assertEquals(0, ntfs.getFileLength("AFILE.TXT"));
        try (Stream stream = ntfs.openFile("AFILE.TXT", FileMode.Open)) {
            stream.write(new byte[14325], 0, 14325);
        }
        assertEquals(14325, ntfs.getFileLength("AFILE.TXT"));
        try (Stream attrStream = ntfs.openFile("AFILE.TXT:altstream", FileMode.Create)) {
            attrStream.write(new byte[122], 0, 122);
        }
        assertEquals(122, ntfs.getFileLength("AFILE.TXT:altstream"));
        // Test NTFS options for hardlink behaviour
        ntfs.createDirectory("Dir");
        ntfs.createHardLink("AFILE.TXT", "Dir\\OtherLink.txt");
        try (Stream stream = ntfs.openFile("AFILE.TXT", FileMode.Open, FileAccess.ReadWrite)) {
            stream.setLength(50);
        }
        assertEquals(50, ntfs.getFileLength("AFILE.TXT"));
        assertEquals(14325, ntfs.getFileLength("Dir\\OtherLink.txt"));
        ntfs.getNtfsOptions().setFileLengthFromDirectoryEntries(false);
        assertEquals(50, ntfs.getFileLength("Dir\\OtherLink.txt"));
    }

    @Test
    public void fragmented() throws Exception {
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        ntfs.createDirectory("DIR");
        byte[] buffer = new byte[4096];
        for (int i = 0; i < 2500; ++i) {
            try (Stream stream = ntfs.openFile("DIR\\file" + i + ".bin", FileMode.Create, FileAccess.ReadWrite)) {
                stream.write(buffer, 0, buffer.length);
            }
            try (Stream stream = ntfs.openFile("DIR\\" + i + ".bin", FileMode.Create, FileAccess.ReadWrite)) {
                stream.write(buffer, 0, buffer.length);
            }
        }
//Debug.println("T1: ----");
        for (int i = 0; i < 2500; ++i) {
            ntfs.deleteFile("DIR\\file" + i + ".bin");
        }
//Debug.println("T2: ----");
//NonResidentAttributeBuffer.debug = true;
        // Create fragmented file (lots of small writes)
        try (Stream stream = ntfs.openFile("DIR\\fragmented.bin", FileMode.Create, FileAccess.ReadWrite)) {
            for (int i = 0; i < 2500; ++i) {
                stream.write(buffer, 0, buffer.length);
            }
        }
        // Try a large write
        byte[] largeWriteBuffer = new byte[200 * 1024];
        for (int i = 0; i < largeWriteBuffer.length / 4096; ++i) {
            largeWriteBuffer[i * 4096] = (byte) i;
        }
//Debug.println("T3: ----");
        try (Stream stream = ntfs.openFile("DIR\\fragmented.bin", FileMode.OpenOrCreate, FileAccess.ReadWrite)) {
//NonResidentAttributeBuffer.debug = false;
            stream.setPosition(stream.getLength() - largeWriteBuffer.length);
            stream.write(largeWriteBuffer, 0, largeWriteBuffer.length);
        }
//Debug.println("T4: ----");
        // And a large read
        byte[] largeReadBuffer = new byte[largeWriteBuffer.length];
        try (Stream stream = ntfs.openFile("DIR\\fragmented.bin", FileMode.OpenOrCreate, FileAccess.ReadWrite)) {
            stream.setPosition(stream.getLength() - largeReadBuffer.length);
            stream.read(largeReadBuffer, 0, largeReadBuffer.length);
        }
        assertArrayEquals(largeWriteBuffer, largeReadBuffer);
    }

    @Test
    public void sparse() throws Exception {
        int fileSize = 1 * 1024 * 1024;
        NtfsFileSystem ntfs = FileSystemSource.ntfsFileSystem();
        byte[] data = new byte[fileSize];
        for (int i = 0; i < fileSize; i++) {
            data[i] = (byte) i;
        }
        try (SparseStream s = ntfs.openFile("file.bin", FileMode.CreateNew)) {
            s.write(data, 0, fileSize);
            Map<String, Object> attrs = ntfs.getAttributes("file.bin");
            attrs.put(FileAttributes.SparseFile.name(), true);
            ntfs.setAttributes("file.bin", attrs);
            s.setPosition(64 * 1024);
            s.clear(128 * 1024);
            s.setPosition(fileSize - 64 * 1024);
            s.clear(128 * 1024);
        }
        try (SparseStream s = ntfs.openFile("file.bin", FileMode.Open)) {
            assertEquals(fileSize + 64 * 1024, s.getLength());
            List<StreamExtent> extents = s.getExtents();
            assertEquals(2, extents.size());
            assertEquals(0, extents.get(0).getStart());
            assertEquals(64 * 1024, extents.get(0).getLength());
            assertEquals((64 + 128) * 1024, extents.get(1).getStart());
            assertEquals(fileSize - (64 * 1024) - ((64 + 128) * 1024), extents.get(1).getLength());
            s.setPosition(72 * 1024);
            s.writeByte((byte) 99);
            byte[] readBuffer = new byte[fileSize];
            s.setPosition(0);
            s.read(readBuffer, 0, fileSize);
            for (int i = 64 * 1024; i < (128 + 64) * 1024; ++i) {
                data[i] = 0;
            }
            for (int i = fileSize - (64 * 1024); i < fileSize; ++i) {
                data[i] = 0;
            }
            data[72 * 1024] = 99;
            assertArrayEquals(data, readBuffer);
        }
    }
}
