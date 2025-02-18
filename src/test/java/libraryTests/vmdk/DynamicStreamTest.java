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

package libraryTests.vmdk;

import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.streams.StreamExtent;
import discUtils.vmdk.Disk;
import discUtils.vmdk.DiskCreateType;
import dotnet4j.io.FileAccess;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import libraryTests.InMemoryFileSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class DynamicStreamTest {
    @Test
    public void attributes() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse)) {
            Stream s = disk.getContent();
            assertTrue(s.canRead());
            assertTrue(s.canWrite());
            assertTrue(s.canSeek());
        }
    }

    @Test
    public void readWriteSmall() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.TwoGbMaxExtentSparse)) {
            byte[] content = new byte[100];
            for (int i = 0; i < content.length; ++i) {
                content[i] = (byte) i;
            }
            Stream s = disk.getContent();
            s.write(content, 10, 40);
            assertEquals(40, s.position());
            s.write(content, 50, 50);
            assertEquals(90, s.position());
            s.position(0);
            byte[] buffer = new byte[100];
            s.read(buffer, 10, 60);
            assertEquals(60, s.position());
            for (int i = 0; i < 10; ++i) {
                assertEquals(0, buffer[i]);
            }
            for (int i = 10; i < 60; ++i) {
                assertEquals(i, buffer[i]);
            }
        }
        // Check the data persisted
        try (Disk disk = new Disk(fs, "a.vmdk", FileAccess.Read)) {
            Stream s = disk.getContent();
            byte[] buffer = new byte[100];
            s.read(buffer, 10, 20);
            assertEquals(20, s.position());
            for (int i = 0; i < 10; ++i) {
                assertEquals(0, buffer[i]);
            }
            for (int i = 10; i < 20; ++i) {
                assertEquals(i, buffer[i]);
            }
        }
    }

    @Test
    public void readWriteLarge() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.TwoGbMaxExtentSparse)) {
            byte[] content = new byte[3 * 1024 * 1024];
            for (int i = 0; i < content.length / 4; ++i) {
                content[i * 4 + 0] = (byte) ((i >>> 24) & 0xFF);
                content[i * 4 + 1] = (byte) ((i >>> 16) & 0xFF);
                content[i * 4 + 2] = (byte) ((i >>> 8) & 0xFF);
                content[i * 4 + 3] = (byte) (i & 0xFF);
            }
            Stream s = disk.getContent();
            s.position(10);
            s.write(content, 0, content.length);
            byte[] buffer = new byte[content.length];
            s.position(10);
            s.read(buffer, 0, buffer.length);
            for (int i = 0; i < content.length; ++i) {
                if (buffer[i] != content[i]) {
                    fail();
                }
            }
        }
    }

    @Test
    public void disposeTest() throws Exception {
        Stream contentStream;
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.TwoGbMaxExtentSparse)) {
            contentStream = disk.getContent();
        }
        try {
            contentStream.position(0);
            fail();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void disposeTestMonolithicSparse() throws Exception {
        Stream contentStream;
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse)) {
            contentStream = disk.getContent();
        }
        try {
            contentStream.position(0);
            fail();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void readNotPresent() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.TwoGbMaxExtentSparse)) {
            byte[] buffer = new byte[100];
            disk.getContent().seek(2 * 1024 * 1024, SeekOrigin.Current);
            disk.getContent().read(buffer, 0, buffer.length);
            for (int i = 0; i < 100; ++i) {
                if (buffer[i] != 0) {
                    fail();
                }
            }
        }
    }

    @Test
    public void attributesVmfs() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.VmfsSparse)) {
            Stream s = disk.getContent();
            assertTrue(s.canRead());
            assertTrue(s.canWrite());
            assertTrue(s.canSeek());
        }
    }

    @Test
    public void readWriteSmallVmfs() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.VmfsSparse)) {
            byte[] content = new byte[100];
            for (int i = 0; i < content.length; ++i) {
                content[i] = (byte) i;
            }
            Stream s = disk.getContent();
            s.write(content, 10, 40);
            assertEquals(40, s.position());
            s.write(content, 50, 50);
            assertEquals(90, s.position());
            s.position(0);
            byte[] buffer = new byte[100];
            s.read(buffer, 10, 60);
            assertEquals(60, s.position());
            for (int i = 0; i < 10; ++i) {
                assertEquals(0, buffer[i]);
            }
            for (int i = 10; i < 60; ++i) {
                assertEquals(i, buffer[i]);
            }
        }
        // Check the data persisted
        try (Disk disk = new Disk(fs, "a.vmdk", FileAccess.Read)) {
            Stream s = disk.getContent();
            byte[] buffer = new byte[100];
            s.read(buffer, 10, 20);
            assertEquals(20, s.position());
            for (int i = 0; i < 10; ++i) {
                assertEquals(0, buffer[i]);
            }
            for (int i = 10; i < 20; ++i) {
                assertEquals(i, buffer[i]);
            }
        }
    }

    @Test
    public void readWriteLargeVmfs() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.VmfsSparse)) {
            byte[] content = new byte[3 * 1024 * 1024];
            for (int i = 0; i < content.length / 4; ++i) {
                content[i * 4 + 0] = (byte) ((i >>> 24) & 0xFF);
                content[i * 4 + 1] = (byte) ((i >>> 16) & 0xFF);
                content[i * 4 + 2] = (byte) ((i >>> 8) & 0xFF);
                content[i * 4 + 3] = (byte) (i & 0xFF);
            }
            Stream s = disk.getContent();
            s.position(10);
            s.write(content, 0, content.length);
            byte[] buffer = new byte[content.length];
            s.position(10);
            s.read(buffer, 0, buffer.length);
            for (int i = 0; i < content.length; ++i) {
                if (buffer[i] != content[i]) {
                    fail();
                }
            }
        }
    }

    @Test
    public void disposeTestVmfs() throws Exception {
        Stream contentStream;
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.VmfsSparse)) {
            contentStream = disk.getContent();
        }
        try {
            contentStream.position(0);
            fail();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void readNotPresentVmfs() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.VmfsSparse)) {
            byte[] buffer = new byte[100];
            disk.getContent().seek(2 * 1024 * 1024, SeekOrigin.Current);
            disk.getContent().read(buffer, 0, buffer.length);

            for (int i = 0; i < 100; ++i) {
                if (buffer[i] != 0) {
                    fail();
                }
            }
        }
    }

    @Test
    public void extents() throws Exception {
        // Fragile - this is the grain size in bytes of the VMDK file, so dependant on algorithm that
        // determines grain size for new VMDKs...
        final int unit = 128 * 512;

        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.TwoGbMaxExtentSparse)) {
            disk.getContent().position(20 * unit);
            disk.getContent().write(new byte[4 * unit], 0, 4 * unit);

            // Starts before first extent, ends before end of extent
            List<StreamExtent> extents = disk.getContent().getExtentsInRange(0, 21 * unit);
            assertEquals(1, extents.size());
            assertEquals(20 * unit, extents.get(0).getStart());
            assertEquals(1 * unit, extents.get(0).getLength());

            // Limit to disk content length
            extents = disk.getContent().getExtentsInRange(21 * unit, 20 * unit);
            assertEquals(1, extents.size());
            assertEquals(21 * unit, extents.get(0).getStart());
            assertEquals(3 * unit, extents.get(0).getLength());

            // Out of range
            extents = disk.getContent().getExtentsInRange(25 * unit, 4 * unit);
            assertEquals(0, extents.size());

            // Non-unit multiples
            extents = disk.getContent().getExtentsInRange(21 * unit + 10, 20 * unit);
            assertEquals(1, extents.size());
            assertEquals(21 * unit + 10, extents.get(0).getStart());
            assertEquals(3 * unit - 10, extents.get(0).getLength());
        }
    }
}
