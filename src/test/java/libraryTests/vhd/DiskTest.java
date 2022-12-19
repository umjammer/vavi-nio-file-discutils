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

package libraryTests.vhd;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import discUtils.core.Geometry;
import discUtils.streams.util.Ownership;
import discUtils.vhd.Disk;
import discUtils.vhd.DiskImageFile;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DiskTest {

    private static final String FS = java.io.File.separator;

    @Test
    public void initializeFixed() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeFixed(ms, Ownership.None, 8 * 1024 * 1024)) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 7.5 * 1024 * 1024
                    && disk.getGeometry().getCapacity() <= 8 * 1024 * 1024);
        }
        // Check the stream is still valid
        ms.readByte();
        ms.close();
    }

    @Test
    public void initializeFixedOwnStream() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeFixed(ms, Ownership.Dispose, 8 * 1024 * 1024)) {
        }
        assertThrows(IOException.class, ms::readByte);
    }

    @Test
    public void initializeDynamic() throws Exception {
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024
                    && disk.getGeometry().getCapacity() <= 16 * 1024L * 1024 * 1024);
        }
        assertTrue(1 * 1024 * 1024 > ms.getLength());
        try (Disk disk = new Disk(ms, Ownership.Dispose)) {
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024
                    && disk.getGeometry().getCapacity() <= 16 * 1024L * 1024 * 1024);
        }
    }

    @Test
    public void initializeDifferencing() throws Exception {
        MemoryStream baseStream = new MemoryStream();
        MemoryStream diffStream = new MemoryStream();
        DiskImageFile baseFile = DiskImageFile.initializeDynamic(baseStream, Ownership.Dispose, 16 * 1024L * 1024 * 1024);

        try (Disk disk = Disk.initializeDifferencing(diffStream,
                                                     Ownership.None,
                                                     baseFile,
                                                     Ownership.Dispose,
                                                     "C:" + FS + "TEMP" + FS + "base.vhd",
                                                     "." + FS + "base.vhd",
                                                     Instant.now().toEpochMilli())) {
            assertNotNull(disk);
            assertTrue(disk.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024
                    && disk.getGeometry().getCapacity() <= 16 * 1024L * 1024 * 1024);
            assertEquals(disk.getGeometry().getCapacity(), baseFile.getGeometry().getCapacity());
            assertEquals(2, (new ArrayList<>(disk.getLayers())).size());
        }
        assertTrue(1 * 1024 * 1024 > diffStream.getLength());
        diffStream.close();
    }

    @Test
    public void constructorDynamic() throws Exception {
        Geometry geometry;
        MemoryStream ms = new MemoryStream();
        try (Disk disk = Disk.initializeDynamic(ms, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            geometry = disk.getGeometry();
        }
        try (Disk disk = new Disk(ms, Ownership.None)) {
            assertEquals(geometry, disk.getGeometry());
            assertNotNull(disk.getContent());
        }
        try (Disk disk = new Disk(ms, Ownership.Dispose)) {
            assertEquals(geometry, disk.getGeometry());
            assertNotNull(disk.getContent());
        }
    }

    @Test
    public void constructorFromFiles() throws Exception {
        MemoryStream baseStream = new MemoryStream();
        DiskImageFile baseFile = DiskImageFile.initializeDynamic(baseStream, Ownership.Dispose, 16 * 1024L * 1024 * 1024);
        MemoryStream childStream = new MemoryStream();
        DiskImageFile childFile = DiskImageFile.initializeDifferencing(childStream,
                                                                       Ownership.Dispose,
                                                                       baseFile,
                                                                       "C:" + FS + "temp" + FS + "foo.vhd",
                                                                       "." + FS + "foo.vhd",
                                                                       Instant.now().toEpochMilli());
        MemoryStream grandChildStream = new MemoryStream();
        DiskImageFile grandChildFile = DiskImageFile.initializeDifferencing(grandChildStream,
                                                                            Ownership.Dispose,
                                                                            childFile,
                                                                            "C:" + FS + "temp" + FS + "child1.vhd",
                                                                            "." + FS + "child1.vhd",
                                                                            Instant.now().toEpochMilli());
        try (Disk disk = new Disk(Arrays.asList(grandChildFile, childFile, baseFile), Ownership.Dispose)) {
            assertNotNull(disk.getContent());
        }
    }

    @Test
    public void undisposedChangedDynamic() throws Exception {
        byte[] firstSector = new byte[512];
        byte[] lastSector = new byte[512];
        MemoryStream ms = new MemoryStream();

        try (Disk newDisk = Disk.initializeDynamic(ms, Ownership.None, 16 * 1024L * 1024 * 1024)) {
        }

        try (Disk disk = new Disk(ms, Ownership.None)) {
            disk.getContent().write(new byte[1024], 0, 1024);
            ms.position(0);
            ms.read(firstSector, 0, 512);
            ms.seek(-512, SeekOrigin.End);
            ms.read(lastSector, 0, 512);
            assertArrayEquals(firstSector, lastSector);
        }
        // Check disabling AutoCommit really doesn't do the commit

        try (Disk disk = new Disk(ms, Ownership.None)) {
            disk.setAutoCommitFooter(false);
            disk.getContent().position(10 * 1024 * 1024);
            disk.getContent().write(new byte[1024], 0, 1024);
            ms.position(0);
            ms.read(firstSector, 0, 512);
            ms.seek(-512, SeekOrigin.End);
            ms.read(lastSector, 0, 512);
            assertNotEquals(firstSector, lastSector);
        }
        // Also check that after disposing, the commit happens
        ms.position(0);
        ms.read(firstSector, 0, 512);
        ms.seek(-512, SeekOrigin.End);
        ms.read(lastSector, 0, 512);
        assertArrayEquals(firstSector, lastSector);
        // Finally, check default value for AutoCommit lines up with behaviour
        try (Disk disk = new Disk(ms, Ownership.None)) {
            assertTrue(disk.getAutoCommitFooter());
        }
    }
}
