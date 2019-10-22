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

package LibraryTests.Vmdk;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Vmdk.Disk;
import DiscUtils.Vmdk.DiskAdapterType;
import DiscUtils.Vmdk.DiskCreateType;
import DiscUtils.Vmdk.DiskImageFile;
import LibraryTests.InMemoryFileSystem;
import moe.yo3explorer.dotnetio4j.FileAccess;


public class DiskTest {
    public DiskTest() {
//        SetupHelper.setupComplete();
    }

    @Test
    public void initializeFixed() throws Exception {
        try (Disk disk = Disk.initialize(new InMemoryFileSystem(), "a.vmdk", 8 * 1024 * 1024, DiskCreateType.MonolithicFlat)) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 7.9 * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 8.1 * 1024 * 1024);
            assertTrue(disk.getGeometry().getCapacity() == disk.getContent().getLength());
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
            assertTrue(disk.getGeometry().getCapacity() == disk.getContent().getLength());
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
            assertTrue(disk.getContent().getLength() == 16 * 1024L * 1024 * 1024);
        }
        assertTrue(fs.getFileLength("a.vmdk") > 2 * 1024 * 1024);
        assertTrue(fs.getFileLength("a.vmdk") < 4 * 1024 * 1024);
        try (Disk disk = new Disk(fs, "a.vmdk", FileAccess.Read)) {
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() <= 16 * 1024L * 1024 * 1024);
            assertTrue(disk.getContent().getLength() == 16 * 1024L * 1024 * 1024);
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
                .initialize(fs, "\\base\\base.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse);
        try (Disk disk = Disk
                .initializeDifferencing(fs, "\\diff\\diff.vmdk", DiskCreateType.MonolithicSparse, "\\base\\base.vmdk")) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
            assertTrue(disk.getContent().getLength() == 16 * 1024L * 1024 * 1024);
            assertEquals(2, (new ArrayList<>(disk.getLayers())).size());
            List<DiskImageFile> links = new ArrayList<>(disk.getLinks());
            assertEquals(2, links.size());
            List<String> paths = new ArrayList<>(links.get(0).getExtentPaths());
            assertEquals(1, paths.size());
            assertEquals("diff.vmdk", paths.get(0));
        }
        assertTrue(fs.getFileLength("\\diff\\diff.vmdk") > 2 * 1024 * 1024);
        assertTrue(fs.getFileLength("\\diff\\diff.vmdk") < 4 * 1024 * 1024);
    }

    @Test
    public void initializeDifferencingRelPath() throws Exception {
        DiscFileSystem fs = new InMemoryFileSystem();
        DiskImageFile baseFile = DiskImageFile
                .initialize(fs, "\\dir\\subdir\\base.vmdk", 16 * 1024L * 1024 * 1024, DiskCreateType.MonolithicSparse);
        try (Disk disk = Disk
                .initializeDifferencing(fs, "\\dir\\diff.vmdk", DiskCreateType.MonolithicSparse, "subdir\\base.vmdk")) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
            assertTrue(disk.getContent().getLength() == 16 * 1024L * 1024 * 1024);
            assertEquals(2, (new ArrayList<>(disk.getLayers())).size());
        }
        assertTrue(fs.getFileLength("\\dir\\diff.vmdk") > 2 * 1024 * 1024);
        assertTrue(fs.getFileLength("\\dir\\diff.vmdk") < 4 * 1024 * 1024);
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
