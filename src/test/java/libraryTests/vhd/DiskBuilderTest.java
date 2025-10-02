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

import java.util.List;

import discUtils.core.DiskImageFileSpecification;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import discUtils.vhd.Disk;
import discUtils.vhd.DiskBuilder;
import discUtils.vhd.FileType;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class DiskBuilderTest {
    private SparseStream diskContent;

    public DiskBuilderTest() throws Exception {
        MemoryStream fileStream = new MemoryStream();
        Disk baseFile = Disk.initializeDynamic(fileStream, Ownership.Dispose, 16 * 1024L * 1024);
        for (int i = 0; i < 8; i += 1024 * 1024) {
            baseFile.getContent().position(i);
            baseFile.getContent().writeByte((byte) i);
        }

        baseFile.getContent().position(15 * 1024 * 1024);
        baseFile.getContent().writeByte((byte) 0xFF);

        diskContent = baseFile.getContent();
    }

    @Test
    void buildFixed() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(FileType.Fixed);
        builder.setContent(diskContent);

        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(1, fileSpecs.size());
        assertEquals("foo.vhd", fileSpecs.get(0).getName());

        try (Disk disk = new Disk(fileSpecs.get(0).openStream(), Ownership.Dispose)) {
            for (int i = 0; i < 8; i += 1024 * 1024) {
                disk.getContent().position(i);
                assertEquals(i, disk.getContent().readByte());
            }

            disk.getContent().position(15 * 1024 * 1024);
            assertEquals(0xFF, disk.getContent().readByte());
        }
    }

    @Test
    void buildDynamic() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(FileType.Dynamic);
        builder.setContent(diskContent);

        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(1, fileSpecs.size());
        assertEquals("foo.vhd", fileSpecs.get(0).getName());

        try (Disk disk = new Disk(fileSpecs.get(0).openStream(), Ownership.Dispose)) {
            for (int i = 0; i < 8; i += 1024 * 1024) {
                disk.getContent().position(i);
                assertEquals(i, disk.getContent().readByte());
            }

            disk.getContent().position(15 * 1024 * 1024);
            assertEquals(0xFF, disk.getContent().readByte());
        }
    }
}
