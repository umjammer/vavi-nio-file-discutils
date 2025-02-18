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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import discUtils.core.DiscFileSystem;
import discUtils.vmdk.Disk;
import discUtils.vmdk.DiskAdapterType;
import discUtils.vmdk.DiskCreateType;
import discUtils.vmdk.DiskImageFile;
import dotnet4j.io.FileAccess;
import libraryTests.InMemoryFileSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DiskTest {

    private static final String FS = File.separator;

    public DiskTest() {
//        SetupHelper.setupComplete();
    }

    @Test
    public void initializeFixed() throws Exception {
        try (Disk disk = Disk.initialize(new InMemoryFileSystem(), "a.vmdk", 8 * 1024 * 1024, DiskCreateType.MonolithicFlat)) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 7.9 * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 8.1 * 1024 * 1024);
            assertEquals(disk.getGeometry().getCapacity(), disk.getContent().getLength());
            List<DiskImageFile> links = new ArrayList<>(disk.getLinks());
            List<String> paths = new ArrayList<>(links.get(0).getExtentPaths());
            assertEquals(1, paths.size());
            assertEquals("a-flat.vmdk", paths.get(0));
        }
    }

    @Test
    public void initializeFixedIDE() throws Exception {
        try (Disk disk = Disk.initialize(new InMemoryFileSystem(),
                                         "a.vmdk",
                                         8 * 1024 * 1024,
                                         DiskCreateType.MonolithicFlat,
                                         DiskAdapterType.Ide)) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 7.9 * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 8.1 * 1024 * 1024);
            assertEquals(disk.getGeometry().getCapacity(), disk.getContent().getLength());
            List<DiskImageFile> links = new ArrayList<>(disk.getLinks());
            List<String> paths = new ArrayList<>(links.get(0).getExtentPaths());
            assertEquals(1, paths.size());
            assertEquals("a-flat.vmdk", paths.get(0));
        }
    }

    @Test
    public void initializeDynamic() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse)) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() <= 16 * 1024L * 1024 * 1024);
            assertEquals(16 * 1024L * 1024 * 1024, disk.getContent().getLength());
        }
        assertTrue(fs.getFileLength("a.vmdk") > 2 * 1024 * 1024);
        assertTrue(fs.getFileLength("a.vmdk") < 4 * 1024 * 1024);
        try (Disk disk = new Disk(fs, "a.vmdk", FileAccess.Read)) {
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() <= 16 * 1024L * 1024 * 1024);
            assertEquals(16 * 1024L * 1024 * 1024, disk.getContent().getLength());
            List<DiskImageFile> links = new ArrayList<>(disk.getLinks());
            List<String> paths = new ArrayList<>(links.get(0).getExtentPaths());
            assertEquals(1, paths.size());
            assertEquals("a.vmdk", paths.get(0));
        }
    }

    @Test
    public void initializeDifferencing() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        DiskImageFile baseFile = DiskImageFile
                .initialize(fs, FS + "base" + FS + "base.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse);
        try (Disk disk = Disk
                .initializeDifferencing(fs, FS + "diff" + FS + "diff.vmdk", DiskCreateType.MonolithicSparse, FS + "base" + FS + "base.vmdk")) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
            assertEquals(16 * 1024L * 1024 * 1024, disk.getContent().getLength());
            assertEquals(2, (new ArrayList<>(disk.getLayers())).size());
            List<DiskImageFile> links = new ArrayList<>(disk.getLinks());
            assertEquals(2, links.size());
            List<String> paths = new ArrayList<>(links.get(0).getExtentPaths());
            assertEquals(1, paths.size());
            assertEquals("diff.vmdk", paths.get(0));
        }
        assertTrue(fs.getFileLength(FS + "diff" + FS + "diff.vmdk") > 2 * 1024 * 1024);
        assertTrue(fs.getFileLength(FS + "diff" + FS + "diff.vmdk") < 4 * 1024 * 1024);
    }

    @Test
    public void initializeDifferencingRelPath() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        DiskImageFile baseFile = DiskImageFile
                .initialize(fs, FS + "dir" + FS + "subdir" + FS + "base.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse);
        try (Disk disk = Disk
                .initializeDifferencing(fs, FS + "dir" + FS + "diff.vmdk", DiskCreateType.MonolithicSparse, "subdir" + FS + "base.vmdk")) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
            assertEquals(16 * 1024L * 1024 * 1024, disk.getContent().getLength());
            assertEquals(2, (new ArrayList<>(disk.getLayers())).size());
        }
        assertTrue(fs.getFileLength(FS + "dir" + FS + "diff.vmdk") > 2 * 1024 * 1024);
        assertTrue(fs.getFileLength(FS + "dir" + FS + "diff.vmdk") < 4 * 1024 * 1024);
    }

    @Test
    public void readOnlyHosted() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        try (Disk disk = Disk.initialize(fs, "a.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse)) {
        }
        Disk d2 = new Disk(fs, "a.vmdk", FileAccess.Read);
        assertFalse(d2.getContent().canWrite());
    }
}
