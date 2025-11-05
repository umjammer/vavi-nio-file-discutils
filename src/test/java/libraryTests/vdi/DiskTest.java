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

package libraryTests.vdi;

import discUtils.core.Geometry;
import discUtils.streams.util.Ownership;
import discUtils.vdi.Disk;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DiskTest {

    @Test
    void initializeFixed() throws Exception {
        MemoryStream ms = new MemoryStream();

        try (Disk disk = Disk.initializeFixed(ms, Ownership.None, 8 * 1024 * 1024)) {
            assertNotNull(disk);
//System.err.println(disk.getGeometry().getCapacity());
            assertTrue(disk.getGeometry().getCapacity() > 7.5 * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 8 * 1024 * 1024);
            assertTrue(disk.getGeometry().getCapacity() <= disk.getContent().getLength());
        }
        // Check the stream is still valid
        ms.readByte();
        ms.close();
    }

    @Test
    void initializeFixedOwnStream() throws Exception {
        MemoryStream ms = new MemoryStream();

        try (Disk disk = Disk.initializeFixed(ms, Ownership.Dispose, 8 * 1024 * 1024)) {
        }
        assertThrows(dotnet4j.io.IOException.class, ms::readByte);
    }

    @Test
    void initializeDynamic() throws Exception {
        MemoryStream ms = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(ms, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            assertNotNull(disk);
//System.err.println(disk.getGeometry().getCapacity());
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
            assertTrue(disk.getGeometry().getCapacity() <= disk.getContent().getLength());
        }
        assertTrue(1 * 1024 * 1024 > ms.getLength());

        try (Disk disk = new Disk(ms)) {
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                       disk.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
            assertTrue(disk.getGeometry().getCapacity() <= disk.getContent().getLength());
        }
    }

    @Test
    void constructorDynamic() throws Exception {
        Geometry geometry;
        MemoryStream ms = new MemoryStream();

        try (Disk disk = Disk.initializeDynamic(ms, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            geometry = disk.getGeometry();
        }

        try (Disk disk = new Disk(ms)) {
            assertEquals(geometry, disk.getGeometry());
            assertNotNull(disk.getContent());
        }
        try (Disk disk = new Disk(ms, Ownership.Dispose)) {
            assertEquals(geometry, disk.getGeometry());
            assertNotNull(disk.getContent());
        }
    }
}
