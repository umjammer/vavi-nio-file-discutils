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

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import discUtils.core.Geometry;
import discUtils.streams.util.Ownership;
import discUtils.vhd.Disk;
import discUtils.vhd.DiskImageFile;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DiskImageFileTest {

    private static final String FS = File.separator;

    @Test
    public void initializeDifferencing() throws Exception {
        MemoryStream baseStream = new MemoryStream();
        MemoryStream diffStream = new MemoryStream();

        try (DiskImageFile baseFile = DiskImageFile
                .initializeDynamic(baseStream, Ownership.Dispose, 16 * 1024L * 1024 * 1024)) {
            try (DiskImageFile diffFile = DiskImageFile.initializeDifferencing(
                    diffStream,
                    Ownership.None,
                    baseFile,
                    "C:" + FS + "TEMP" + FS + "base.vhd",
                    "." + FS + "base.vhd",
                    LocalDateTime.of(2007, 12, 31, 0, 0, 0, 0)
                           .atZone(ZoneId.of("UTC"))
                           .toInstant()
                           .toEpochMilli())) {
                assertNotNull(diffFile);
                assertTrue(diffFile.getGeometry().getCapacity() > 15.8 * 1024L * 1024 * 1024 &&
                           diffFile.getGeometry().getCapacity() < 16 * 1024L * 1024 * 1024);
                assertTrue(diffFile.isSparse());
                assertNotEquals(diffFile.getCreationTimestamp(),
                                LocalDateTime.of(2007, 12, 31, 0, 0, 0, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
            }
        }
        assertTrue(1 * 1024 * 1024 > diffStream.getLength());
    }

    @Test
    public void getParentLocations() throws Exception {
        MemoryStream baseStream = new MemoryStream();
        MemoryStream diffStream = new MemoryStream();

        try (DiskImageFile baseFile = DiskImageFile
                .initializeDynamic(baseStream, Ownership.Dispose, 16 * 1024L * 1024 * 1024)) {
            // Write some data - exposes bug if mis-calculating where to write data
            try (DiskImageFile diffFile = DiskImageFile.initializeDifferencing(
                    diffStream,
                    Ownership.None,
                    baseFile,
                    "C:" + FS + "TEMP" + FS + "base.vhd",
                    "." + FS + "base.vhd",
                    LocalDateTime.of(2007, 12, 31, 0, 0, 0, 0)
                            .atZone(ZoneId.of("UTC"))
                            .toInstant()
                            .toEpochMilli())) {
                Disk disk = new Disk(Arrays.asList(diffFile, baseFile), Ownership.None);
                disk.getContent().write(new byte[512], 0, 512);
            }
        }

        try (DiskImageFile diffFile = new DiskImageFile(diffStream)) {
            // Testing the obsolete method - disable warning
            List<String> locations = diffFile.getParentLocations("E:" + FS + "FOO" + FS);
            assertEquals(2, locations.size());
            assertEquals("C:" + FS + "TEMP" + FS + "base.vhd", locations.get(0));
            assertEquals("E:" + FS + "FOO" + FS + "base.vhd", locations.get(1));
        }

        try (DiskImageFile diffFile = new DiskImageFile(diffStream)) {
            // Testing the new method - note relative path because diff file initialized without a path
            List<String> locations = diffFile.getParentLocations();
            assertEquals(2, locations.size());
            assertEquals("C:" + FS + "TEMP" + FS + "base.vhd", locations.get(0));
            assertEquals("." + FS + "base.vhd", locations.get(1));
        }
    }

    @Test
    public void footerMissing() throws Exception {
        //
        // Simulates a partial failure extending the file, that the file footer is corrupt - should read start of the file instead.
        //
        Geometry geometry;

        MemoryStream stream = new MemoryStream();
        try (DiskImageFile file = DiskImageFile.initializeDynamic(stream, Ownership.None, 16 * 1024L * 1024 * 1024)) {
            geometry = file.getGeometry();
        }

        stream.setLength(stream.getLength() - 512);

        try (DiskImageFile file = new DiskImageFile(stream)) {
            assertEquals(geometry, file.getGeometry());
        }
    }
}
